package com.mp.unityandroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by garyNoh on 2017. 6. 12..
 */

public class Jason {

    Context context;
    Activity activity;



    //weather API
    //날씨 매니저
    ForeCastManager mForeCast;
    MainActivity mThis;
    ArrayList<ContentValues> mWeatherData;
    ArrayList<WeatherInfo> mWeatherInfomation;
    public static final int THREAD_HANDLER_SUCCESS_INFO = 1;


    //텍스트 음성 변환
    private TextToSpeech myTTS;

    private Jason () {}

    //싱글톤
    private static class Singleton {
        private static final Jason instance = new Jason();
    }

    public static Jason getInstance () {
        return Singleton.instance;
    }

    public void init(Context context){
        this.context = context;


        //TTS 를 등록한다 (Jason 의 목소리를 초기화)
        myTTS = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    //myTTS.setLanguage(Locale.ENGLISH); //언어 설정 영어
                    myTTS.setLanguage(Locale.ENGLISH); //언어 설정 한국어

                    //목소리 톤 설정 - 0 에 가까울수록 저음이 나는데 듣기 이상함
                    //myTTS.setPitch(0.5f);

                    //말하기 속도 0에 가까울수록 느림
                    myTTS.setSpeechRate(1.0f);
                }
            }
        });
    }



    public Jason(Context context){
        this.context = context;


        //TTS 를 등록한다 (Jason 의 목소리를 초기화)
        myTTS = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    //myTTS.setLanguage(Locale.ENGLISH); //언어 설정 영어
                    myTTS.setLanguage(Locale.ENGLISH); //언어 설정 한국어

                    //목소리 톤 설정 - 0 에 가까울수록 저음이 나는데 듣기 이상함
                    //myTTS.setPitch(0.5f);

                    //말하기 속도 0에 가까울수록 느림
                    myTTS.setSpeechRate(1.0f);
                }
            }
        });
    }

    public void makeCall(String targetContact){
        //검색한 연락처를 통해서 전화를 건다
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+targetContact));
        try {
            Log.e("gary", targetContact);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }


    public void getWeather(Activity activity){
        this.activity = activity;
        Initialize();
    }

    public void sendMessage(String targetContact, String msg){
        PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT_ACTION"), 0);
        PendingIntent deliveredIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch(getResultCode()){
                    case Activity.RESULT_OK:
                        // 전송 성공
                        Toast.makeText(context, "전송 완료", Toast.LENGTH_SHORT).show();
                        say("Message is sent successfully");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        // 전송 실패
                        Toast.makeText(context, "전송 실패", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        // 서비스 지역 아님
                        Toast.makeText(context, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        // 무선 꺼짐
                        Toast.makeText(context, "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        // PDU 실패
                        Toast.makeText(context, "PDU Null", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_SENT_ACTION"));

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        // 도착 완료
                        Toast.makeText(context, "SMS 도착 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        // 도착 안됨
                        Toast.makeText(context, "SMS 도착 실패", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_DELIVERED_ACTION"));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(targetContact, null, msg, sentIntent, deliveredIntent);

    }

    /**
     * 날씨 모듈 초기화
     */
    public void Initialize()
    {
        mWeatherInfomation = new ArrayList<>();
        mThis = (MainActivity)this.activity;
        //위도와 경도를 설정
        mForeCast = new ForeCastManager(Double.toString(MyLocationListener.getLongtitude()),Double.toString(MyLocationListener.getLatitude()),mThis);
        mForeCast.run();
    }

    /**
     * 날씨 정보에 대한 결과값 출력
     * clear sky - 날씨 상태
     * cloud amount - 구름의 양
     * min, max temperature - 최저, 최저 기온
     * @return 결과값
     */
    public String PrintValue()
    {
        //split 하려고 문자열을 좀 바꿈
        String mData = "";
        for (int i = 0; i < mWeatherInfomation.size(); i++) {
            mData = mData + "weather/" + mWeatherInfomation.get(i).getWeather_Day()
                    + "/" + mWeatherInfomation.get(i).getWeather_Name()
                    + "/" + mWeatherInfomation.get(i).getClouds_Sort()
                    + "/" + mWeatherInfomation.get(i).getClouds_Value()
                    + "/" + mWeatherInfomation.get(i).getClouds_Per()
                    + "/" + mWeatherInfomation.get(i).getWind_Name()
                    + "/" + mWeatherInfomation.get(i).getWind_Speed()/* + " mps"*/
                    + "/" + mWeatherInfomation.get(i).getTemp_Max()/* + "℃"*/
                    + "/" + mWeatherInfomation.get(i).getTemp_Min()/* + "℃"*/
                    + "/" + mWeatherInfomation.get(i).getHumidity()/* + "%"*/;

            mData = mData + "\r\n" + "----------------------------------------------" + "\r\n";
        }
        Log.e("gary", mData);

        //jason 이 말한다
        say(mWeatherInfomation.get(0).getWeather_Name() + "and the wind is " +
                mWeatherInfomation.get(0).getWind_Name() + "with " + mWeatherInfomation.get(0).getWind_Speed() + "meter per second speed");

        UnityPlayer.UnitySendMessage(MainActivity.UnityObjName, MainActivity.UnityMsg, mData);
        //Toast.makeText(getApplicationContext(), mData, Toast.LENGTH_SHORT).show();
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

    /**
     * 날씨 데이터를 가져옴
     */
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
        @Override
        public void handleMessage(Message msg) {
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





    /**
     * 음성 변환
     * @param txt : 음성으로 변환할 string
     */
    public void say(String txt){
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


}
