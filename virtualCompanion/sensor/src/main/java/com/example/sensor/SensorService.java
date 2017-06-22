package com.example.sensor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import java.util.concurrent.ScheduledExecutorService;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorService";
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    SensorManager mSensorManager;
    private DeviceClient client;
    private ScheduledExecutorService mScheduler;
    public static final float alpha = 0.8f;
    public float prevX, prevY, prevZ = 0;
    public static final int THRESHOLD_LEFT = 5;
    public static final int THRESHOLD_RIGHT = -2;
    public static final int THRESHOLD_UP = -2;
    public static final float THRESHOLD_RATIO = 0.7f;


    @Override
    public void onCreate() {
        super.onCreate();

        client = DeviceClient.getInstance(this);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Sensor Dashboard");
        builder.setContentText("Collecting sensor data..");
        startForeground(1, builder.build());

        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor(SENS_GYROSCOPE);

        // Register the listener
        if (mSensorManager != null) {

            if (gyroscopeSensor != null) {
                mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    private void stopMeasurement() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (mScheduler != null && !mScheduler.isTerminated()) {
            mScheduler.shutdown();
        }
    }

    @Override
    public void onSensorChanged (SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            int state = findpeak ( LowpassFilter (event.values) );

            if (state != -1)
                client.sendSensorData(state);
        }
    }

    public float[] LowpassFilter (float[] values)
    {
        float[] fvalues = new float[3];

        fvalues[0] = alpha * prevX + (1-alpha) * values[0];
        fvalues[1] = alpha * prevY + (1-alpha) * values[1];
        fvalues[2] = alpha * prevZ + (1-alpha) * values[2];

        prevX = fvalues[0];
        prevY = fvalues[1];
        prevZ = fvalues[2];

        return fvalues;
    }

    public int findpeak (float[] values)
    {
        double sum_square = (double) values[0] * values[0] + values[1] * values[1] + values[2] * values[2];
        double magnitude = Math.sqrt(sum_square);

        int motion = -1;

        if ( (values[0] > THRESHOLD_LEFT) && (values[0] / magnitude >THRESHOLD_RATIO) )
            motion = 100;
        else if ( (values[1] > THRESHOLD_UP) && (values[1] / magnitude >THRESHOLD_RATIO) )
            motion = 200;
        else if ( (values[2] > THRESHOLD_RIGHT) && (values[2] / magnitude >THRESHOLD_RATIO) )
            motion = 300;
        else
            motion = -1;

        return motion;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
