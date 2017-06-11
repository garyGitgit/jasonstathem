package com.mp.unityandroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by garyNoh on 2017. 6. 12..
 */

public class Jason {

    Context context;

    //텍스트 음성 변환
    private TextToSpeech myTTS;


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
            context.startActivity(intent);
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }





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
