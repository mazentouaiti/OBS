package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * AccelerometerSensor - Independent sensor class for accelerometer
 *
 * ============================================================
 * TODO (Student 1 - Accelerometer):
 * ============================================================
 *
 * OBJECTIVE: Implement shake detection to control recording
 *
 * WHAT IS AN ACCELEROMETER?
 * - Measures acceleration forces in 3 axes (X, Y, Z)
 * - Detects device movement and orientation
 * - Values in m/s²
 *
 * YOUR IMPLEMENTATION STEPS:
 *
 * 1. Implement initialize() method
 * 2. Implement startListening() method
 * 3. Implement stopListening() method
 * 4. Calculate shake magnitude in onSensorChanged()
 * 5. Trigger callbacks when shake is detected
 *
 * USAGE IN ACTIVITIES:
 * - Create instance: accelerometerSensor = new AccelerometerSensor(this);
 * - Set callback: accelerometerSensor.setOnShakeListener((intensity) -> { ... });
 * - Initialize: accelerometerSensor.initialize();
 * - Start: accelerometerSensor.startListening(); (in onResume)
 * - Stop: accelerometerSensor.stopListening(); (in onPause)
 */
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
    private static final float SHAKE_THRESHOLD = 15.0f; // m/s²
    private static final int SHAKE_TIME_THRESHOLD = 500; // milliseconds

    /**
     * Constructor
     */
    public AccelerometerSensor(Context context) {
        this.context = context;
    }

    /**
     * TODO (Student 1): Implement sensor initialization
     *
     * Steps:
     * 1. Get SensorManager from context
     * 2. Get accelerometer sensor (TYPE_ACCELEROMETER)
     * 3. Check if sensor exists
     * 4. Return true if successful, false if sensor not available
     */
    public boolean initialize() {
        // TODO: Implement initialization
        // sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // return accelerometer != null;

        return false; // Replace with actual implementation
    }

    /**
     * TODO (Student 1): Implement sensor listener registration
     *
     * Steps:
     * 1. Create SensorEventListener
     * 2. In onSensorChanged():
     *    - Get X, Y, Z values from event.values[]
     *    - Calculate magnitude: sqrt(x² + y² + z²)
     *    - If magnitude > SHAKE_THRESHOLD, trigger onShakeListener
     *    - Also call onDataChangedListener with current values
     * 3. Register listener with SENSOR_DELAY_GAME
     */
    public void startListening() {
        // TODO: Implement listener

        /* EXAMPLE CODE:
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calculate total acceleration
                float acceleration = (float) Math.sqrt(x*x + y*y + z*z);

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
                // Optional: handle accuracy changes
            }
        };

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        }
        */
    }

    /**
     * TODO (Student 1): Implement sensor unregistration
     * CRITICAL: Always call this in onPause() to save battery!
     */
    public void stopListening() {
        // TODO: Unregister listener
        // if (sensorManager != null && listener != null) {
        //     sensorManager.unregisterListener(listener);
        // }
    }

    /**
     * Check if sensor is available on device
     */
    public boolean isAvailable() {
        return accelerometer != null;
    }

    /**
     * Set shake detection listener
     */
    public void setOnShakeListener(OnShakeListener listener) {
        this.onShakeListener = listener;
    }

    /**
     * Set data change listener (for real-time monitoring)
     */
    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    /**
     * Callback interface for shake detection
     */
    public interface OnShakeListener {
        void onShake(float intensity);
    }

    /**
     * Callback interface for data updates
     */
    public interface OnDataChangedListener {
        void onDataChanged(float x, float y, float z, float magnitude);
    }
}
