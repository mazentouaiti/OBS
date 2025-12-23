package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener listener;

    // Callbacks
    private OnShakeListener onShakeListener;
    private OnDataChangedListener onDataChangedListener;

    // Shake detection variables
    private long lastShakeTime = 0;
    private static final float SHAKE_THRESHOLD = 15.0f;
    private static final int SHAKE_TIME_THRESHOLD = 500;

    public AccelerometerSensor(Context context) {
        this.context = context;
    }

    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return false;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        return accelerometer != null;
    }

    public void startListening() {
        if (sensorManager == null || accelerometer == null || listener != null) return;

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calculate total acceleration
                float acceleration = (float) Math.sqrt(x * x + y * y + z * z);

                // Notify data listener
                if (onDataChangedListener != null) {
                    onDataChangedListener.onDataChanged(x, y, z, acceleration);
                }

                // Check for shake
                if (acceleration > SHAKE_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD) {
                        lastShakeTime = currentTime;
                        if (onShakeListener != null) {
                            onShakeListener.onShake(acceleration);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Handle accuracy changes
            }
        };

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListening() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
            listener = null;
        }
    }

    public boolean isAvailable() {
        return accelerometer != null;
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.onShakeListener = listener;
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public interface OnShakeListener {
        void onShake(float intensity);
    }

    public interface OnDataChangedListener {
        void onDataChanged(float x, float y, float z, float magnitude);
    }
}