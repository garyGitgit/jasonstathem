package com.example.sensor;

import android.content.Intent;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageReceiverService extends WearableListenerService {

    public static final String START_MEASUREMENT = "/start";
    public static final String STOP_MEASUREMENT = "/stop";

    private DeviceClient deviceClient;

    @Override
    public void onCreate() {
        super.onCreate();

        deviceClient = DeviceClient.getInstance(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {


        if (messageEvent.getPath().equals(START_MEASUREMENT)) {
            startService(new Intent(this, SensorService.class));
        }

        if (messageEvent.getPath().equals(STOP_MEASUREMENT)) {
            stopService(new Intent(this, SensorService.class));
        }
    }
}
