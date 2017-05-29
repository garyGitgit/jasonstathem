package com.mp.unityandroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends UnityPlayerActivity {

    // 유니티에서 스크립트가 붙을 오브젝트 이름
    private String UnityObjName = "pluginUnity";
    // 유니티에서 에러메세지 받을 함수 이름
    private String UnityMsg = "msgUnity";
    //유니티에서 음성인식 결과받을 함수 이름
    private String UnitySTTresult = "sttUnity";
    // 핸들러 관련
    private final int STTSTART = 1;
    private final int STTREADY = 2;
    private final int STTEND = 3;
    private msgHandle mHandler = null;
    // 음성인식 관련
    private SpeechRecognizer mRecognizer;
    private static Context context;
    private String recogLang = "en-US";

    //weather API
    public static final int THREAD_HANDLER_SUCCESS_INFO = 1;

    ForeCastManager mForeCast;

    //gps 정보
    LocationManager locationManager;


    String lon = "128.3910799"; // 좌표 설정
    String lat = "36.1444292";  // 좌표 설정
    MainActivity mThis;
    ArrayList<ContentValues> mWeatherData;
    ArrayList<WeatherInfo> mWeatherInfomation;

    //텍스트 음성 변환 변수
    private TextToSpeech myTTS;


    //현재 커맨드 상태를 알려주는 변수
    /*
    * 0 : 처음 청취하는 상황
    * 1 : 전화 대상을 청취하는 상황
    * 2 : 메시지 대상을 청취하는 상황
    * 3 : 메시지 내용을 청취하는 상황
    *
    * */

    int status = 0;
    String targetContact = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();

        //핸들러 붙이고
        mHandler = new msgHandle();

        //음성인식 리스너등록합니다.
        context = getApplicationContext();
        if (mRecognizer == null) {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mRecognizer.setRecognitionListener(listener);
        }
        Log.e("TAGGER", "OnCreate");

        //TTS 를 등록한다
        myTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
//                    myTTS.setLanguage(Locale.ENGLISH); //언어 설정 영어
                    myTTS.setLanguage(Locale.KOREAN); //언어 설정 한국어
//                    myTTS.setPitch(0.5f);
                    myTTS.setSpeechRate(1.0f); //말하기 속도
                }
            }
        });


        //위치정보를 가져온다
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); //위치정보 매니저를 가져옴
        if(locationManager == null) Log.e("gary", "location manager is null");
        //LocationListener locationListener = new MyLocationListener(getApplicationContext());

        //permission 체크
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Log.e("gary", "Latitude : " + Double.toString(location.getLatitude()));
            Log.e("gary", "Longitude : " + Double.toString(location.getLongitude()));
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

            //도시 알아오기
            List<Address> addresses;
            String cityName = null;
            Geocoder gcd = new Geocoder(context, Locale.getDefault());
            try {
                addresses = gcd.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String s = location.getLongitude() + "\n" + location.getLatitude() + "\n\nMy Current City is: "
                    + cityName;
            Log.e("gary", s);
        }catch(SecurityException e){
            e.printStackTrace();
            Log.e("gary", "security error : " + e.toString());
        }


        //첫번째 줄에서 에러발생
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
////            ActivityCompat.requestPermissions(this,
////                    new String[]{Manifest.permission. ACCESS_FINE_LOCATION},
////                    99);
////               public void onRequestPermissionsResult(int requestCode, String[] permissions,
////                                                      int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(getApplicationContext(), "GPS 권한 설정이 필요합니다", Toast.LENGTH_SHORT).show();
//            return;
//        }
        //requestLocationUpdates : GPS 제공자, 업데이트 주기 milsec, 업데이트 거리 미터

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

    // 유니티에서 호출할 함수입니다.
    public void StartSpeechReco(String Lang) {
    // "en-US" : 미국영어 / "ko" : 한국어 /  "zh-CN" : 중국어 /  "ja" : 일본어
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

    // 실제 음성인식을 호출합니다. recogLang은 음성인식할 언어로 주시면됩니다.
    public void StartSpeechRecoService(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // 음성인식
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLang);
        mRecognizer.startListening(i);
    }

    //메세지 초기화
    public void EndSpeechReco() {
        this.mHandler.removeMessages(STTREADY);
        this.mHandler.removeMessages(STTEND);
        this.mHandler.removeMessages(STTSTART);
    }


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
                    UnityPlayer.UnitySendMessage(UnityObjName, UnitySTTresult,((String)matches.get(0)));

                    String userMessage = (String)matches.get(0);

                    //두번째 이상의 청취
                    switch (status){
                        //전화 또는 메시지 대상을 청취할 때
                        case 1:
                            //대상을 주소록에서 찾는다
                            targetContact = getPhoneNumber(userMessage, getApplicationContext());
                            //주소록에서 찾을 수 없음
                            if(targetContact.equals("Unsaved")){
                                String jasonReply = userMessage + "를 찾을 수 없습니다";
                                //음성으로 알리기
                                jasonSays(jasonReply);
                                //텍스트로 알리기
                                UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, jasonReply);
                            }
                            else{
                                Log.e("Jason", targetContact);
                                //검색한 연락처를 통해서 전화를 건다
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+targetContact));
                                try {
                                    startActivity(intent);
                                }catch(SecurityException e){
                                    e.printStackTrace();
                                }
                            }
                            status = 0;
                            return;
                        case 2:
                            targetContact = getPhoneNumber(userMessage, getApplicationContext());
                            //주소록에서 찾을 수 없음
                            if(targetContact.equals("Unsaved")){
                                String jasonReply = userMessage + "를 찾을 수 없습니다";
                                //음성으로 알리기
                                jasonSays(jasonReply);
                                //텍스트로 알리기
                                UnityPlayer.UnitySendMessage(UnityObjName, UnityMsg, jasonReply);
                                status = 0;
                            }
                            else{
                                Log.e("Jason", targetContact);
                                jasonSays("뭐라고 보낼까요?");
                                //3초 후 다시 음성인식을 한다
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        StartSpeechReco("ko");
                                    }
                                }, 3000);
                                status = 3;
                            }
                            return;
                        case 3:
                            PendingIntent sentIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("SMS_SENT_ACTION"), 0);
                            PendingIntent deliveredIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

                            registerReceiver(new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    switch(getResultCode()){
                                        case Activity.RESULT_OK:
                                            // 전송 성공
                                            Toast.makeText(MainActivity.this, "전송 완료", Toast.LENGTH_SHORT).show();
                                            break;
                                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                            // 전송 실패
                                            Toast.makeText(MainActivity.this, "전송 실패", Toast.LENGTH_SHORT).show();
                                            break;
                                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                                            // 서비스 지역 아님
                                            Toast.makeText(MainActivity.this, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
                                            break;
                                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                                            // 무선 꺼짐
                                            Toast.makeText(MainActivity.this, "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
                                            break;
                                        case SmsManager.RESULT_ERROR_NULL_PDU:
                                            // PDU 실패
                                            Toast.makeText(MainActivity.this, "PDU Null", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            }, new IntentFilter("SMS_SENT_ACTION"));

                            registerReceiver(new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    switch (getResultCode()){
                                        case Activity.RESULT_OK:
                                            // 도착 완료
                                            Toast.makeText(MainActivity.this, "SMS 도착 완료", Toast.LENGTH_SHORT).show();
                                            break;
                                        case Activity.RESULT_CANCELED:
                                            // 도착 안됨
                                            Toast.makeText(MainActivity.this, "SMS 도착 실패", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            }, new IntentFilter("SMS_DELIVERED_ACTION"));

                            Log.e("textmeshmessage", (String)matches.get(0));
                            SmsManager sms = SmsManager.getDefault();
                            sms.sendTextMessage(targetContact, null, userMessage, sentIntent, deliveredIntent);

                            status = 0;
                            return;
                    }

                    //첫번째 청취
                    switch(userMessage){
                        case "전화":
                            //1차적으로 누구에게 보낼 것인지 물어본다
//                            Log.e("Jason", getResources().getString(R.string.voice_call_ask1));
                            jasonSays("누구에게 전화를 거실겁니까?");
                            status = 1;

                            //2초 후 다시 음성인식을 한다
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    StartSpeechReco("ko");
                                }
                            }, 3000);
                            break;
                        case "메시지":
                            jasonSays("누구에게 메시지를 보내실겁니까?");
                            status = 2;
                            //다시 음성인식을 한다
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    StartSpeechReco("ko");
                                }
                            }, 3000);
                            break;
                        case "날씨":
                            //http://warguss.blogspot.kr/2016/01/openweather-2.html
                            Initialize();
                            break;
                    }

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


    //TTS
    private void jasonSays(String txt){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsGreater21(txt);
        } else {
            ttsUnder20(txt);
        }
    }

    //TTS를 안드로이드 롤리팝 버전에 따라서 다르게 적용
    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        myTTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        myTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    /**
     * 연락처 검색
     * @param name
     * @param context
     * @return
     */
    public String getPhoneNumber(String name, Context context) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if(c == null) return null;

        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if(ret==null)
            ret = "Unsaved";
        return ret;
    }

    public void Initialize()
    {
        mWeatherInfomation = new ArrayList<>();
        mThis = this;
        //위도와 경도를 설정
        //mForeCast = new ForeCastManager(lon,lat, mThis);
        mForeCast = new ForeCastManager(Double.toString(MyLocationListener.getLongtitude()),Double.toString(MyLocationListener.getLatitude()),mThis);
        mForeCast.run();
    }
    public String PrintValue()
    {
        String mData = "";
        for(int i = 0; i < mWeatherInfomation.size(); i ++)
        {
            mData = mData + mWeatherInfomation.get(i).getWeather_Day() + "\r\n"
                    +  mWeatherInfomation.get(i).getWeather_Name() + "\r\n"
                    +  mWeatherInfomation.get(i).getClouds_Sort()
                    +  " /Cloud amount: " + mWeatherInfomation.get(i).getClouds_Value()
                    +  mWeatherInfomation.get(i).getClouds_Per() +"\r\n"
                    +  mWeatherInfomation.get(i).getWind_Name()
                    +  " /WindSpeed: " + mWeatherInfomation.get(i).getWind_Speed() + " mps" + "\r\n"
                    +  "Max: " + mWeatherInfomation.get(i).getTemp_Max() + "℃"
                    +  " /Min: " + mWeatherInfomation.get(i).getTemp_Min() + "℃" +"\r\n"
                    +  "Humidity: " + mWeatherInfomation.get(i).getHumidity() + "%";

            mData = mData + "\r\n" + "----------------------------------------------" + "\r\n";
        }
        return mData;
    }

    public void DataChangedToHangeul()
    {
        for(int i = 0 ; i <mWeatherInfomation.size(); i ++)
        {
            WeatherToHangeul mHangeul = new WeatherToHangeul(mWeatherInfomation.get(i));
            mWeatherInfomation.set(i,mHangeul.getHangeulWeather());
        }
    }

    public void DataToInformation()
    {
        for(int i = 0; i < mWeatherData.size(); i++)
        {
            mWeatherInfomation.add(new WeatherInfo(
                    String.valueOf(mWeatherData.get(i).get("weather_Name")),
                    String.valueOf(mWeatherData.get(i).get("weather_Number")),
                    String.valueOf(mWeatherData.get(i).get("weather_Much")),
                    String.valueOf(mWeatherData.get(i).get("weather_Type")),
                    String.valueOf(mWeatherData.get(i).get("wind_Direction")),
                    String.valueOf(mWeatherData.get(i).get("wind_SortNumber")),
                    String.valueOf(mWeatherData.get(i).get("wind_SortCode")),
                    String.valueOf(mWeatherData.get(i).get("wind_Speed")),
                    String.valueOf(mWeatherData.get(i).get("wind_Name")),
                    String.valueOf(mWeatherData.get(i).get("temp_Min")),
                    String.valueOf(mWeatherData.get(i).get("temp_Max")),
                    String.valueOf(mWeatherData.get(i).get("humidity")),
                    String.valueOf(mWeatherData.get(i).get("Clouds_Value")),
                    String.valueOf(mWeatherData.get(i).get("Clouds_Sort")),
                    String.valueOf(mWeatherData.get(i).get("Clouds_Per")),
                    String.valueOf(mWeatherData.get(i).get("day"))
            ));
        }
    }
    public Handler handler = new Handler(){
        @Override      public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case THREAD_HANDLER_SUCCESS_INFO :
                    mForeCast.getmWeather();
                    mWeatherData = mForeCast.getmWeather();
                    String data = "";

                    if(mWeatherData.size() ==0)
                        data = "데이터가 없습니다";
                    else {
                        DataToInformation(); // 자료 클래스로 저장,

                        data = PrintValue();
                        //DataChangedToHangeul();
                        //data = data + PrintValue();
                    }

                    Log.d("Weather", "Weather: " + data);
                    //send weather info to Unity
                    break;
                default:
                    break;
            }
        }
    };
}
