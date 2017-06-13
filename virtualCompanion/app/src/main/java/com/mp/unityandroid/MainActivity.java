package com.mp.unityandroid;

import android.content.ContentValues;
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
import android.speech.tts.TextToSpeech;
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

//    //weather API
//    public static final int THREAD_HANDLER_SUCCESS_INFO = 1;

    //날씨 매니저
    ForeCastManager mForeCast;

    //gps 매니저
    LocationManager locationManager;

    MainActivity mThis;
    ArrayList<ContentValues> mWeatherData;
    ArrayList<WeatherInfo> mWeatherInfomation;

    //텍스트 음성 변환
    private TextToSpeech myTTS;


    //현재 커맨드 상태
    /*
    * 0 : 처음 청취하는 상태
    * 1 : 전화 대상을 청취 상태
    * 2 : 메시지 대상을 청취 상태
    * 3 : 메시지 내용을 청취 상태
    * */

    int status = 0;

    //전화 / 메시지 할 대상
    String targetContact = "";

    //api.ai 변수
    private AIService aiService;
    AIDataService aiDataService;
    AIRequest aiRequest;

    //JasonBrain
    JasonBrain jasonBrain;
    Jason jason;

    //message 변수
    boolean isWaitingMessage = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //UI 가 없음
        //setContentView(R.layout.activity_main);
        Toast.makeText(getApplicationContext(), "Starting app", Toast.LENGTH_SHORT).show();

        //핸들러 붙이고
        mHandler = new msgHandle();

        //음성인식 리스너등록
        context = getApplicationContext();
        if (mRecognizer == null) {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mRecognizer.setRecognitionListener(listener);
        }

        //위치정보를 가져온다
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); //위치정보 매니저를 가져옴
        if(locationManager == null) Log.e("gary", "location manager is null");
        MyLocationListener locationListener = new MyLocationListener(getApplicationContext());

        //TODO : onLocationChanged 를 사용할 수 있는 함수인데 작동을 안해서 일단 킵
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
        }
        catch (SecurityException e){
            e.printStackTrace();
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

        //jason 초기화
        jasonBrain = new JasonBrain(getApplicationContext());
        jason = Jason.getInstance();
        jason.init(getApplicationContext());
        //앱 시작하면서 날씨 초기화
        //Initialize();
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

    // 실제 음성인식을 호출

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
     * 메시지 초기화
     */
    public void EndSpeechReco() {
        this.mHandler.removeMessages(STTREADY);
        this.mHandler.removeMessages(STTEND);
        this.mHandler.removeMessages(STTSTART);
    }


    /**
     * 음성인식 리스너
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
                        if(!userMessage.equals("cancel"))
                            jason.sendMessage(targetContact, userMessage);
                        else
                            jason.say("Sending message is cancelled");
                        isWaitingMessage = false;
                        return;
                    }

                    //TODO : api.ai 로 전송
                    aiRequest.setQuery(userMessage);

                    //setQuery 를 한 다음 aiDataService.request 를 해야한다
                    new AsyncTask<AIRequest, Void, AIResponse>() {
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
                                // process aiResponse here
                                String answer = aiResponse.getResult().getFulfillment().getSpeech();
                                jason.say(answer);
                                Log.e("gary ai response", answer);

                                String action = aiResponse.getResult().getAction();

                                String target = "";
                                //action 0 : 전화, action 1 : 메시지
                                switch (action){
                                    case "0":
                                        //action 을 가져와서 어떤 응답 종류인지 확인한다
                                        if(aiResponse.getResult().getParameters().get("target_name") != null){
                                            target = aiResponse.getResult().getParameters().get("target_name").toString();
                                            Log.e("gary what is target ", target);
                                            //target_name 이 누군지 알면 주소록에서 찾는다
                                            if(!target.equals("")){
                                                final String parsedTarget = target.substring(1, target.length()-1);

                                                //1.5초 있다가 검색 시작 후 전화걸기
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
                                                }, 2500);
                                            }
                                            //target 없이 말한 경우
                                        }
                                        else{
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    StartSpeechReco("en");
                                                }
                                            }, 2500);
                                        }
                                        break;
                                    case "1":
                                        //action 을 가져와서 어떤 응답 종류인지 확인한다
                                        if(aiResponse.getResult().getParameters().get("target_name") != null){
                                            target = aiResponse.getResult().getParameters().get("target_name").toString();
                                            //target_name 이 누군지 알면 주소록에서 찾는다
                                            if(!target.equals("")){

                                                final String parsedTarget = target.substring(1, target.length()-1);

                                                //1.5초 있다가 검색 시작 후 되묻기
                                                mHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        final String result = jasonBrain.searchContact(parsedTarget);
                                                        if(result.startsWith("OK")){
                                                            //jason.makeCall(result.substring(2));
                                                            targetContact = result.substring(2);
                                                            jason.say("What message do I send?");
                                                            mHandler.postDelayed(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    StartSpeechReco("en");
                                                                    isWaitingMessage = true;
                                                                }
                                                            }, 2500);
                                                        }
                                                        else{
                                                            jason.say("Sorry, I can't find " + parsedTarget + " on your list");
                                                        }
                                                    }
                                                }, 2500);
                                            }
                                        }
                                        //target 없이 말한 경우
                                        else{
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    StartSpeechReco("en");
                                                }
                                            }, 2500);
                                        }
                                        break;
                                    //weather
                                    case "2":
                                        jason.getWeather(MainActivity.this);
                                        break;
                                }

                                //api.ai 로 부터 온 응답이 call 관련이면
//                                switch (status){
//                                    case 0:
//                                        //결과값을 그대로 말해준다
//                                        jason.say(aiResponse.getResult().getFulfillment().getSpeech());
//                                        status = 1;
//                                        break;
//                                    //TODO : 여기 시나리오에 따라서
//                                    case 1:
//                                        //주소록 찾기
//                                        final String result = jasonBrain.searchContact(target);
//                                        if(result.startsWith("OK")){
//                                            jason.say("I'm gonna call " + target); // 못 찾았다고 알려줌
//                                            mHandler.postDelayed(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    jason.makeCall(result.substring(2)); // OK 는 제거
//                                                }
//                                            }, 2000);
//
//                                        }
//                                        else{
//                                            jason.say(result); // 못 찾았다고 알려줌
//                                        }
//                                        status = 0;
//                                        break;
//                                }
                            }
                        }
                    }.execute(aiRequest);



//                    //두 번째 이상의 음성인식인지 확인
//                    switch (status){
//                        //전화 대상을 청취할 때
//                        case 1:
//                            //대상을 주소록에서 찾는다
//                            targetContact = getPhoneNumber(userMessage, getApplicationContext());
//
//                            //주소록에서 찾을 수 없음
//                            if(targetContact.equals("Unsaved")){
//                                String jasonReply = userMessage + "를 찾을 수 없습니다";
//
//                                //음성 전달
//                                jasonSays(jasonReply);
//                                //유니티로 텍스트 날리기
//                                UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, jasonReply);
//                            }
//                            //주소록에서 찾을 수 있음
//                            else{
//                                //검색한 연락처를 통해서 전화를 건다
//                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+targetContact));
//                                try {
//                                    startActivity(intent);
//                                }catch(SecurityException e){
//                                    e.printStackTrace();
//                                }
//                            }
//                            //완료했으므로 상태를 초기로 만든다
//                            status = 0;
//                            return;
//                        //메시지 대상을 청취할 떄
//                        case 2:
//                            targetContact = getPhoneNumber(userMessage, getApplicationContext());
//                            //주소록에서 찾을 수 없음
//                            if(targetContact.equals("Unsaved")){
//                                String jasonReply = userMessage + "를 찾을 수 없습니다";
//                                //음성 전달
//                                jasonSays(jasonReply);
//                                //유니티로 텍스트 날리기
//                                UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, jasonReply);
//                                //완료했으므로 상태를 초기로 만든다
//                                status = 0;
//                            }
//                            else{
//                                jasonSays("뭐라고 보낼까요?");
//                                //3초 후 다시 음성인식을 한다
//                                mHandler.postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        StartSpeechReco(recogLang);
//                                    }
//                                }, 3000);
//                                status = 3;
//                            }
//                            return;
//                        //문자 내용 청취할 때
//                        case 3:
//                            PendingIntent sentIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("SMS_SENT_ACTION"), 0);
//                            PendingIntent deliveredIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);
//
//                            registerReceiver(new BroadcastReceiver() {
//                                @Override
//                                public void onReceive(Context context, Intent intent) {
//                                    switch(getResultCode()){
//                                        case Activity.RESULT_OK:
//                                            // 전송 성공
//                                            Toast.makeText(MainActivity.this, "전송 완료", Toast.LENGTH_SHORT).show();
//                                            break;
//                                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                                            // 전송 실패
//                                            Toast.makeText(MainActivity.this, "전송 실패", Toast.LENGTH_SHORT).show();
//                                            break;
//                                        case SmsManager.RESULT_ERROR_NO_SERVICE:
//                                            // 서비스 지역 아님
//                                            Toast.makeText(MainActivity.this, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
//                                            break;
//                                        case SmsManager.RESULT_ERROR_RADIO_OFF:
//                                            // 무선 꺼짐
//                                            Toast.makeText(MainActivity.this, "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
//                                            break;
//                                        case SmsManager.RESULT_ERROR_NULL_PDU:
//                                            // PDU 실패
//                                            Toast.makeText(MainActivity.this, "PDU Null", Toast.LENGTH_SHORT).show();
//                                            break;
//                                    }
//                                }
//                            }, new IntentFilter("SMS_SENT_ACTION"));
//
//                            registerReceiver(new BroadcastReceiver() {
//                                @Override
//                                public void onReceive(Context context, Intent intent) {
//                                    switch (getResultCode()){
//                                        case Activity.RESULT_OK:
//                                            // 도착 완료
//                                            Toast.makeText(MainActivity.this, "SMS 도착 완료", Toast.LENGTH_SHORT).show();
//                                            break;
//                                        case Activity.RESULT_CANCELED:
//                                            // 도착 안됨
//                                            Toast.makeText(MainActivity.this, "SMS 도착 실패", Toast.LENGTH_SHORT).show();
//                                            break;
//                                    }
//                                }
//                            }, new IntentFilter("SMS_DELIVERED_ACTION"));
//
//                            Log.e("textmeshmessage", (String)matches.get(0));
//                            SmsManager sms = SmsManager.getDefault();
//                            sms.sendTextMessage(targetContact, null, userMessage, sentIntent, deliveredIntent);
//
//                            status = 0;
//                            return;
//                    }

                    //첫번째 청취
//                    switch(userMessage){
//                        case "call":
//                            //string res 에서 가져오면 에러가 발생한다
////                            Log.e("Jason", getResources().getString(R.string.voice_call_ask1));
//                            jasonSays("누구에게 전화를 거실겁니까?");
//
//                            //전화 대상 청취모드로 바꿈
//                            status = 1;
//
//                            //3초 후 다시 음성인식
//                            mHandler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    StartSpeechReco("ko");
//                                }
//                            }, 3000);
//                            break;
//                        case "message":
//                            jasonSays("누구에게 메시지를 보내실겁니까?");
//                            status = 2;
//
//                            //메시지 전송 대상 청취모드로 바꿈
//                            mHandler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    StartSpeechReco("ko");
//                                }
//                            }, 3000);
//                            break;
//                        //TODO : 날씨 정보를 가져와서 음성으로 알려줘야 함
//                        case "weather":
//                            //http://warguss.blogspot.kr/2016/01/openweather-2.html
//                            Initialize();
//                            break;
//                    }
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


//    /**
//     * 날씨 모듈 초기화
//     */
//    public void Initialize()
//    {
//        mWeatherInfomation = new ArrayList<>();
//        mThis = this;
//        //위도와 경도를 설정
//        mForeCast = new ForeCastManager(Double.toString(MyLocationListener.getLongtitude()),Double.toString(MyLocationListener.getLatitude()),mThis);
//        mForeCast.run();
//    }
//
//    /**
//     * 날씨 정보에 대한 결과값 출력
//     * clear sky - 날씨 상태
//     * cloud amount - 구름의 양
//     * min, max temperature - 최저, 최저 기온
//     * @return 결과값
//     */
//    public String PrintValue()
//    {
//        //split 하려고 문자열을 좀 바꿈
//        String mData = "";
//        for (int i = 0; i < mWeatherInfomation.size(); i++) {
//            mData = mData + "weather/" + mWeatherInfomation.get(i).getWeather_Day()
//                    + "/" + mWeatherInfomation.get(i).getWeather_Name()
//                    + "/" + mWeatherInfomation.get(i).getClouds_Sort()
//                    + "/" + mWeatherInfomation.get(i).getClouds_Value()
//                    + "/" + mWeatherInfomation.get(i).getClouds_Per()
//                    + "/" + mWeatherInfomation.get(i).getWind_Name()
//                    + "/" + mWeatherInfomation.get(i).getWind_Speed()/* + " mps"*/
//                    + "/" + mWeatherInfomation.get(i).getTemp_Max()/* + "℃"*/
//                    + "/" + mWeatherInfomation.get(i).getTemp_Min()/* + "℃"*/
//                    + "/" + mWeatherInfomation.get(i).getHumidity()/* + "%"*/;
//
//            mData = mData + "\r\n" + "----------------------------------------------" + "\r\n";
//        }
//        Log.e("gary", mData);
//
//        UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, mData);
//        //Toast.makeText(getApplicationContext(), mData, Toast.LENGTH_SHORT).show();
//        return mData;
//    }
//
//    public void DataChangedToHangeul()
//    {
//        for(int i = 0 ; i <mWeatherInfomation.size(); i ++)
//        {
//            WeatherToHangeul mHangeul = new WeatherToHangeul(mWeatherInfomation.get(i));
//            mWeatherInfomation.set(i,mHangeul.getHangeulWeather());
//        }
//    }
//
//    /**
//     * 날씨 데이터를 가져옴
//     */
//    public void DataToInformation()
//    {
//        for(int i = 0; i < mWeatherData.size(); i++)
//        {
//            mWeatherInfomation.add(new WeatherInfo(
//                    String.valueOf(mWeatherData.get(i).get("weather_Name")),
//                    String.valueOf(mWeatherData.get(i).get("weather_Number")),
//                    String.valueOf(mWeatherData.get(i).get("weather_Much")),
//                    String.valueOf(mWeatherData.get(i).get("weather_Type")),
//                    String.valueOf(mWeatherData.get(i).get("wind_Direction")),
//                    String.valueOf(mWeatherData.get(i).get("wind_SortNumber")),
//                    String.valueOf(mWeatherData.get(i).get("wind_SortCode")),
//                    String.valueOf(mWeatherData.get(i).get("wind_Speed")),
//                    String.valueOf(mWeatherData.get(i).get("wind_Name")),
//                    String.valueOf(mWeatherData.get(i).get("temp_Min")),
//                    String.valueOf(mWeatherData.get(i).get("temp_Max")),
//                    String.valueOf(mWeatherData.get(i).get("humidity")),
//                    String.valueOf(mWeatherData.get(i).get("Clouds_Value")),
//                    String.valueOf(mWeatherData.get(i).get("Clouds_Sort")),
//                    String.valueOf(mWeatherData.get(i).get("Clouds_Per")),
//                    String.valueOf(mWeatherData.get(i).get("day"))
//            ));
//        }
//    }
//
//    public Handler handler = new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch(msg.what){
//                case THREAD_HANDLER_SUCCESS_INFO :
//                    mForeCast.getmWeather();
//                    mWeatherData = mForeCast.getmWeather();
//                    String data = "";
//
//                    if(mWeatherData.size() ==0)
//                        data = "데이터가 없습니다";
//                    else {
//                        DataToInformation(); // 자료 클래스로 저장,
//
//                        data = PrintValue();
//                        //DataChangedToHangeul();
//                        //data = data + PrintValue();
//                    }
//
//                    Log.d("Weather", "Weather: " + data);
//                    //send weather info to Unity
//                    break;
//                default:
//                    break;
//            }
//        }
//    };
}
