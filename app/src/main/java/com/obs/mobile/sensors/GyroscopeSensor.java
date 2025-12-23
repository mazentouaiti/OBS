package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GyroscopeSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener listener;

    // Callbacks
    private OnRotationListener onRotationListener;
    private OnRotationGestureListener onRotationGestureListener;

    private static final float FAST_ROTATION_THRESHOLD = 100f;
    private static final int GESTURE_TIME_THRESHOLD = 300;
    private long lastFastRotationTime = 0;

    public GyroscopeSensor(Context context) {
        this.context = context;
    }

    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return false;
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        return gyroscope != null;
    }

    public void startListening() {
        if (sensorManager == null || gyroscope == null || listener != null) return;

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float rotationX = event.values[0];
                float rotationY = event.values[1];
                float rotationZ = event.values[2];

                // Convert to degrees per second
                float rotationZDeg = radiansToDegrees(rotationZ);

                // Notify rotation listener
                if (onRotationListener != null) {
                    onRotationListener.onRotation(rotationX, rotationY, rotationZ);
                }

                // Detect fast rotation gestures
                long currentTime = System.currentTimeMillis();
                if (Math.abs(rotationZDeg) > FAST_ROTATION_THRESHOLD) {
                    if (currentTime - lastFastRotationTime > GESTURE_TIME_THRESHOLD) {
                        lastFastRotationTime = currentTime;
                        boolean clockwise = rotationZ > 0;
                        if (onRotationGestureListener != null) {
                            onRotationGestureListener.onFastRotation(rotationZDeg, clockwise);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListening() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
            listener = null;
        }
    }

    public boolean isAvailable() {
        return gyroscope != null;
    }

    public static float radiansToDegrees(float radians) {
        return (float) Math.toDegrees(radians);
    }

    public void setOnRotationListener(OnRotationListener listener) {
        this.onRotationListener = listener;
    }

    public void setOnRotationGestureListener(OnRotationGestureListener listener) {
        this.onRotationGestureListener = listener;
    }

    public interface OnRotationListener {
        void onRotation(float rotationX, float rotationY, float rotationZ);
    }

    public interface OnRotationGestureListener {
        void onFastRotation(float degreesPerSecond, boolean clockwise);
        // OR if you want to keep speed:
        // void onFastRotation(float degreesPerSecond, boolean clockwise, float speed);
    }
}