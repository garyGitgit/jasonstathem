package com.mp.unityandroid;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RemoteSensorManager {

    public static final String START_MEASUREMENT = "/start";
    public static final String STOP_MEASUREMENT = "/stop";

    private static final int CLIENT_CONNECTION_TIMEOUT = 3000;

    private static RemoteSensorManager instance;

    private Context context;
    private ExecutorService executorService;
    private GoogleApiClient googleApiClient;

    public static synchronized RemoteSensorManager getInstance(Context context) {
        if (instance == null) {
            instance = new RemoteSensorManager(context.getApplicationContext());
        }

        return instance;
    }

    private RemoteSensorManager(Context context) {
        this.context = context;
        this.googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.e("gary", Integer.toString(i));
                    }

                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e("gary Error code", Integer.toString(connectionResult.getErrorCode()));

                    }
                })
                .build();



        this.executorService = Executors.newCachedThreadPool();
    }

    private boolean validateConnection() {
        Log.e("gary", "validateConnection");
        if (googleApiClient.isConnected()) {
            Log.e("gary", "googleAPI client connected");
            return true;
        }
        Log.e("gary", "googleAPI client try connection");
        try{
            ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            return result.isSuccess();
        }
        catch(Exception e){
            Log.e("gary", e.toString());
            e.printStackTrace();
            return false;
        }
    }

    public void startMeasurement() {
        Log.e("gary", "start measurement");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground(START_MEASUREMENT);
            }
        });
    }

    public void stopMeasurement() {
        Log.e("gary", "stop measurement");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground(STOP_MEASUREMENT);
            }
        });
    }

    private void controlMeasurementInBackground(final String path) {
        Log.e("gary", "controlMeasurementInBackground");
        if (validateConnection()) {
            Log.e("gary", "connection success");
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await().getNodes();

            for (Node node : nodes) {
                Wearable.MessageApi.sendMessage(
                        googleApiClient, node.getId(), path, null
                ).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        System.out.println( "controlMeasurementInBackground(" + path + "): " + sendMessageResult.getStatus().isSuccess());
                        Log.e("gary", Boolean.toString(sendMessageResult.getStatus().isSuccess()));
                    }
                });
            }
        }
        else {
            Log.e("gary", "no connection possible");
        }
    }

}
