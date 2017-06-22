package com.example.sensor;

import android.content.Context;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeviceClient {

    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;

    public static DeviceClient instance;

    public static DeviceClient getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceClient(context.getApplicationContext());
        }
        return instance;
    }

    private Context context;
    private GoogleApiClient googleApiClient;
    private ExecutorService executorService;

    private DeviceClient(Context context) {
        this.context = context;

        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        executorService = Executors.newCachedThreadPool();
    }

    public void sendSensorData(final int value) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendSensorDataInBackground(value);
            }
        });
    }

    private void sendSensorDataInBackground(int value) {

        PutDataMapRequest dataMap = PutDataMapRequest.create("/sensors/");
        dataMap.getDataMap().putInt("status", value);
        PutDataRequest putDataRequest = dataMap.asPutDataRequest();
        send(putDataRequest);
        System.out.println("sending...");
    }

    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }

        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

        return result.isSuccess();
    }

    private void send(PutDataRequest putDataRequest) {
        if (validateConnection()) {
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                }
            });
        }
    }

}
