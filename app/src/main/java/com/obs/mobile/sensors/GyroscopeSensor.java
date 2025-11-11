package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * GyroscopeSensor - Independent sensor class for gyroscope
 *
 * ============================================================
 * TODO (Student 2 - Gyroscope):
 * ============================================================
 *
 * OBJECTIVE: Detect device rotation for video stabilization and gestures
 *
 * WHAT IS A GYROSCOPE?
 * - Measures rate of rotation around 3 axes
 * - Values in radians per second (rad/s)
 * - Used for detecting rotation gestures and stabilization
 *
 * USAGE IN ACTIVITIES:
 * - Create instance: gyroscopeSensor = new GyroscopeSensor(this);
 * - Set callback: gyroscopeSensor.setOnRotationListener((x, y, z) -> { ... });
 * - Initialize: gyroscopeSensor.initialize();
 * - Start: gyroscopeSensor.startListening(); (in onResume)
 * - Stop: gyroscopeSensor.stopListening(); (in onPause)
 */
public class GyroscopeSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener listener;

    // Callbacks
    private OnRotationListener onRotationListener;
    private OnRotationGestureListener onRotationGestureListener;

    private static final float ROTATION_THRESHOLD = 1.5f; // rad/s

    /**
     * Constructor
     */
    public GyroscopeSensor(Context context) {
        this.context = context;
    }

    /**
     * TODO (Student 2): Implement sensor initialization
     *
     * Steps:
     * 1. Get SensorManager
     * 2. Get gyroscope sensor (TYPE_GYROSCOPE)
     * 3. Check if sensor exists
     */
    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        return gyroscope != null;
    }

    /**
     * TODO (Student 2): Implement sensor listener
     *
     * Steps:
     * 1. Create SensorEventListener
     * 2. Read rotation rates (event.values[0], [1], [2])
     * 3. Convert rad/s to degrees/s if needed
     * 4. Detect rotation gestures
     * 5. Register with SENSOR_DELAY_GAME for fast updates
     */
    public void startListening() {
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float rotationX = event.values[0]; // Rotation around X (pitch)
                float rotationY = event.values[1]; // Rotation around Y (roll)
                float rotationZ = event.values[2]; // Rotation around Z (yaw)

                // Notify rotation listener
                if (onRotationListener != null) {
                    onRotationListener.onRotation(rotationX, rotationY, rotationZ);
                }

                // Detect rotation gestures
                float rotationZDegrees = (float) Math.toDegrees(rotationZ);
                if (Math.abs(rotationZDegrees) > 100) { // Fast rotation
                    if (onRotationGestureListener != null) {
                        onRotationGestureListener.onFastRotation(rotationZDegrees);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        if (sensorManager != null && gyroscope != null) {
            sensorManager.registerListener(listener, gyroscope,
                SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /**
     * TODO (Student 2): Unregister sensor listener
     */
    public void stopListening() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    /**
     * Check if sensor is available
     */
    public boolean isAvailable() {
        return gyroscope != null;
    }

    /**
     * Convert radians per second to degrees per second
     */
    public static float radiansToDegrees(float radians) {
        return (float) Math.toDegrees(radians);
    }

    /**
     * Set rotation data listener
     */
    public void setOnRotationListener(OnRotationListener listener) {
        this.onRotationListener = listener;
    }

    /**
     * Set rotation gesture listener
     */
    public void setOnRotationGestureListener(OnRotationGestureListener listener) {
        this.onRotationGestureListener = listener;
    }

    /**
     * Callback interface for rotation data
     */
    public interface OnRotationListener {
        void onRotation(float rotationX, float rotationY, float rotationZ);
    }

    /**
     * Callback interface for rotation gestures
     */
    public interface OnRotationGestureListener {
        void onFastRotation(float degrees);
    }
}
