package com.mp.unityandroid;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class MainActivity extends UnityPlayerActivity{

    private RemoteSensorManager remoteSensorManager;

    // 유니티에서 스크립트가 붙을 오브젝트 이름
    public static String UnityObjName = "pluginUnity";
    // 유니티에서 에러메세지 받을 함수 이름
    public static String UnityMsg = "msgUnity";
    //유니티에서 음성인식 결과받을 함수 이름
    public static String UnitySTTresult = "sttUnity";
    // 핸들러 관련
    private final int STTSTART = 1;
    private final int STTREADY = 2;
    private final int STTEND = 3;
    private msgHandle mHandler = null;

    // 음성인식 관련
    private SpeechRecognizer mRecognizer;
    private static Context context;

    // "en-US" : 미국영어 / "ko" : 한국어 /  "zh-CN" : 중국어 /  "ja" : 일본어
    private String recogLang = "en-US"; //디폴트 언어
    private String ACCESS_TOKEN = "58ff38b68acb4c699c2f96226d7c4dee";


    //rule based decision making 명령어
    public static final String ACTION_CALL = "0";
    public static final String ACTION_MESSAGE = "1";
    public static final String ACTION_WEATHER = "2";
    public static final String ACTION_LAUNCH_APP = "3";
    public static final String ACTION_CALL_PARAM = "target_name";
    public static final String ACTION_MESSAGE_PARAM = ACTION_CALL_PARAM;
    public static final String ACTION_LAUNCH_APP_PARAM = "app_name";

    public static final int talkingDelay = 2500;

    //gps 매니저
    LocationManager locationManager;


    //전화 / 메시지 할 대상
    String targetContact = "";

    //api.ai 변수
    private AIService aiService;
    AIDataService aiDataService;
    AIRequest aiRequest;

    //Jason 관련 변수
    JasonBrain jasonBrain;
    Jason jason;

    //message 변수
    boolean isWaitingMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO : 와치 센서
        //remoteSensorManager = RemoteSensorManager.getInstance(this);

        Toast.makeText(getApplicationContext(), "Starting app", Toast.LENGTH_SHORT).show();

        mHandler = new msgHandle();

        //음성인식 리스너등록
        context = getApplicationContext();
        if (mRecognizer == null) {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mRecognizer.setRecognitionListener(listener);
        }

        //위치정보를 가져온다
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); //위치정보 매니저를 가져옴
        MyLocationListener locationListener = new MyLocationListener(getApplicationContext());

        //TODO : 와치에 올릴 떄는 주석처리해야한다
        if(locationListener != null){
            try{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
            }
            catch (SecurityException e){
                e.printStackTrace();
            }
        }

//////////api ai 를 사용하기 위한 초기화 /////////
        //인터넷 퍼미션 에러가 있는지 체크
        try{
            //내 API 토큰을 등록 : 여기서부터 에러 : 빌드를 할 때 유니티로 하는데, 라이브러리를 직접 import 하지않아서 그런듯
            final AIConfiguration config = new AIConfiguration(ACCESS_TOKEN,TELEPHONY_SUBSCRIPTION_SERVICE,
                    ai.api.AIConfiguration.SupportedLanguages.English,
                    ai.api.AIConfiguration.RecognitionEngine.System);

            aiService = AIService.getService(this, config);

            //사용자 정의 AI Listener 를 생성
            aiService.setListener(new MyAIListener());
            //요청할 변수 객체화
            aiDataService = new AIDataService(getApplicationContext(), config);
            aiRequest = new AIRequest();
        }
        catch (SecurityException e){
            e.printStackTrace();
        }
////////////////////////////////////////////

        //Jason 과 JasonBrain 생성
        jasonBrain = new JasonBrain(getApplicationContext());
        jason = Jason.getInstance();
        jason.init(getApplicationContext());
    }


    /**
     * startMeasurement 는 스마트 워치 측정 시작인데 지금은 작동 안 함
     */
    @Override
    protected void onResume() {
        super.onResume();
        //TODO : sensor
        //remoteSensorManager.startMeasurement();
        System.out.println("start measurement");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO : sensor
        //remoteSensorManager.stopMeasurement();
        System.out.println("stop measurement");

    }

    class msgHandle extends Handler{
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case STTSTART:
                    //실제 음성인식을 작동시킬 것입니다.
                    StartSpeechRecoService();
                    Log.e("Jason", "Android : STTStart");
                    break;
                case STTREADY:
                    // 유니티에 음성인식이 실제로 시작했다는 메세지를 보냅니다.
                    UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, "START");
                    Log.e("Jason", "Android : STTReady");
                    break;
                case STTEND:
                    // 유니티에 음성인식을 종료했다는 메세지를 보냅니다.
                    UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, "END");
                    Log.e("Jason", "Android : STTSend");
                    break;
            }
        }
    }

    /**
     * 유니티에서 호출
     * @param Lang : 음성인식 언어 (cf. 한국어로 해도 영어 발음하면 영어로 인식한다. 하지만 정확도는 약간 떨어짐)
     */
    public void StartSpeechReco(String Lang) {

        recogLang = Lang;
        EndSpeechReco();
        //핸들러에 시작을 알리고 있습니다.
        try {
            Message msg1 = new Message();
            msg1.what = STTSTART;
            mHandler.sendMessage(msg1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 실제 음성인식을 호출
     */
    public void StartSpeechRecoService(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // 음성인식
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLang);
        mRecognizer.startListening(i);
    }


    /**
     * 음성인식 종료
     */
    public void EndSpeechReco() {
        this.mHandler.removeMessages(STTREADY);
        this.mHandler.removeMessages(STTEND);
        this.mHandler.removeMessages(STTSTART);
    }

    /**
     * RecognitionListener 모듈
     */
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            // ★ 음성인식이 실제로 작동하면 여기가 호출됩니다.
            mHandler.sendEmptyMessage(STTREADY);
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {
            // ★ 음성인식이 끝났다는 거구요
            mHandler.sendEmptyMessage(STTEND);
        }

        //★ 음성인식이 실패하면 나오는 에러입니다.
            // 스피치 타임아웃은 실제 음성 입력이 대기시간내에 없을 경우입니다.
            // 노매치는 음성인식 결과가 없을 경우입니다.
            // 레코그나이저비지는 음성인식 서버가 바쁘다고 합니다.
        @Override
        public void onError(int error) {
            String errMsg = "";
            switch (error) {
                case 1: errMsg = "ERROR_NETWORK_TIMEOUT"; break;
                case 2: errMsg = "ERROR_NETWORK"; break;
                case 3: errMsg = "ERROR_AUDIO"; break;
                case 4: errMsg = "ERROR_SERVER"; break;
                case 5: errMsg = "ERROR_CLIENT"; break;
                case 6: errMsg = "ERROR_SPEECH_TIMEOUT"; break;
                case 7: errMsg = "ERROR_NO_MATCH"; break;
                case 8: errMsg = "ERROR_RECOGNIZER_BUSY"; break;
            }
            try{
                UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, errMsg);
            }
            catch (Exception e) {
            }
        //★ 핸들러 메세지 초기화
            EndSpeechReco();
        }

        //★ 음성인식 결과입니다. matches는 음성인식결과가 배열로 여러개 들어오는데 그중에 첫번째 것만 사용합니다.
        @Override
        public void onResults(Bundle bundle) {
            mHandler.removeMessages(0);
            ArrayList matches = bundle.getStringArrayList("results_recognition");

            if(matches != null){
                try{
                    String userMessage = (String)matches.get(0);

                    //유니티 UI 에 텍스트를 띄워줌
                    UnityPlayer.UnitySendMessage(UnityObjName, UnitySTTresult, userMessage);

                    //메시지 보내려고 대기한거였으면 메시지를 보낸다
                    if(isWaitingMessage){
                        if(!userMessage.equalsIgnoreCase("cancel"))
                            jason.sendSMSMessage(targetContact, userMessage);
                        else
                            jason.say("Message is cancelled");
                        isWaitingMessage = false;
                        return;
                    }

                    //api ai 로 보낼 쿼리
                    aiRequest.setQuery(userMessage);
                    //setQuery 를 한 다음 aiDataService.request 를 해야한다
                    /**
                     * AIRequest 모듈
                     */
                    AsyncTask<AIRequest, Void, AIResponse> aiServiceModule = new AsyncTask<AIRequest, Void, AIResponse>() {
                        @Override
                        protected AIResponse doInBackground(AIRequest... requests) {
                            final AIRequest request = requests[0];
                            try {
                                //aiDataService.request 로  query 를 날린다.
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                        @Override
                        protected void onPostExecute(AIResponse aiResponse) {
                            if (aiResponse != null) {
                                //응답이 온 내용을 TTS 로 말함
                                String response = aiResponse.getResult().getFulfillment().getSpeech();
                                jason.say(response);
                                Log.e("gary ai response", response);



                                //////////////////// rule based decision making ////////////////////
                                //사용자의 intent 가 action 에 담아져서 옴
                                String intent = aiResponse.getResult().getAction();

                                String target = "";
                                switch (intent){
                                    //전화
                                    case ACTION_CALL:
                                        //action 을 가져와서 어떤 응답 종류인지 확인한다
                                        if(aiResponse.getResult().getParameters().get(ACTION_CALL_PARAM) != null){
                                            target = aiResponse.getResult().getParameters().get(ACTION_CALL_PARAM).toString();
                                            Log.e("gary what is target ", target);
                                            //target_name 이 누군지 알면 주소록에서 찾는다
                                            if(!target.equals("")){
                                                final String parsedTarget = target.substring(1, target.length()-1);

                                                //2.5초 있다가 검색 시작 후 전화걸기
                                                mHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        final String result = jasonBrain.searchContact(parsedTarget);
                                                        if(result.startsWith("OK")){
                                                            jason.makeCall(result.substring(2));
                                                        }
                                                        else{
                                                            jason.say("Sorry, I can't find " + parsedTarget + " on your list");
                                                        }
                                                    }
                                                }, talkingDelay);
                                            }
                                            //target 없이 말한 경우
                                        }
                                        else{
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    StartSpeechReco("en");
                                                }
                                            }, talkingDelay);
                                        }
                                        break;
                                    case ACTION_MESSAGE:
                                        //action 을 가져와서 어떤 응답 종류인지 확인한다
                                        if(aiResponse.getResult().getParameters().get(ACTION_CALL_PARAM) != null){
                                            target = aiResponse.getResult().getParameters().get(ACTION_CALL_PARAM).toString();
                                            //target_name 이 누군지 알면 주소록에서 찾는다
                                            if(!target.equals("")){

                                                final String parsedTarget = target.substring(1, target.length()-1);


                                                //TODO : 가정 - 문자보내고싶다 할 때는 이름을 꼭 붙여서 보내야한다
                                                //2.5초 있다가 검색 시작 후 되묻기
                                                mHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        final String result = jasonBrain.searchContact(parsedTarget);
                                                        if(result.startsWith("OK")){
                                                            targetContact = result.substring(2);
                                                            jason.say("What message do I send?");
                                                            mHandler.postDelayed(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    StartSpeechReco("en");
                                                                    isWaitingMessage = true;
                                                                }
                                                            }, talkingDelay);
                                                        }
                                                        else{
                                                            jason.say("Sorry, I can't find " + parsedTarget + " on your list");
                                                        }
                                                    }
                                                }, talkingDelay);
                                            }
                                        }
                                        //target 없이 말한 경우
                                        else{
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    StartSpeechReco("en");
                                                }
                                            }, talkingDelay);
                                        }
                                        break;
                                    //weather
                                    case ACTION_WEATHER:
                                        jason.getWeather(MainActivity.this);
                                        break;
                                    //외부 앱 실행 (카메라 포함)
                                    case ACTION_LAUNCH_APP:
                                        if(aiResponse.getResult().getParameters().get(ACTION_LAUNCH_APP_PARAM) != null) {
                                            target = aiResponse.getResult().getParameters().get(ACTION_LAUNCH_APP_PARAM).toString();
                                            final String parsedTarget = target.substring(1, target.length()-1);
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    jason.launchApp(parsedTarget);
                                                }
                                            }, talkingDelay);
                                        }
                                        break;
                                }
                            }
                        }
                    }.execute(aiRequest);


                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }; // ★ 음성인식 리스너 여기까지입니다.





}
