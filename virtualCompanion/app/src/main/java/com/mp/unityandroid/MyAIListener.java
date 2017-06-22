package com.mp.unityandroid;

import android.util.Log;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

import ai.api.AIListener;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

/**
 * Created by garyNoh on 2017. 6. 1..
 */

public class MyAIListener implements AIListener {
    public static String TAG = "gary";

    @Override
    public void onResult(AIResponse response) {

        Log.i(TAG, "Received success response");

        // this is example how to get different parts of result object
        final Status status = response.getStatus();
        Log.i(TAG, "Status code: " + status.getCode());
        Log.i(TAG, "Status type: " + status.getErrorType());

        final Result result = response.getResult();
        Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

        Log.i(TAG, "Action: " + result.getAction());

        final String speech = result.getFulfillment().getSpeech();
        Log.i(TAG, "Speech: " + speech);
        //TTS.speak(speech);
        Log.e("gary", speech);

        final Metadata metadata = result.getMetadata();
        if (metadata != null) {
            Log.i(TAG, "Intent id: " + metadata.getIntentId());
            Log.i(TAG, "Intent name: " + metadata.getIntentName());
        }

        final HashMap<String, JsonElement> params = result.getParameters();
        if (params != null && !params.isEmpty()) {
            Log.i(TAG, "Parameters: ");
            for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
            }
        }





    }

    @Override
    public void onError(AIError error) {
        Log.e(TAG, "onError");

    }

    @Override
    public void onAudioLevel(float level) {
        Log.e(TAG, "onAudioLevel");

    }

    @Override
    public void onListeningStarted() {
        Log.e(TAG, "onListeningStarted");

    }

    public void onListeningCanceled() {
        Log.e(TAG, "onListeningCanceled");

    }

    @Override
    public void onListeningFinished() {
        Log.e(TAG, "onListeningFinished");

    }
}
