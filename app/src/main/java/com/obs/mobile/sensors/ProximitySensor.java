package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * ProximitySensor - Independent sensor class for proximity detection
 *
 * ============================================================
 * TODO (Student 4 - Proximity Sensor):
 * ============================================================
 *
 * OBJECTIVE: Detect when phone is covered to auto-pause recording
 *
 * WHAT IS A PROXIMITY SENSOR?
 * - Detects how close an object is to the device
 * - Usually binary: NEAR (0) or FAR (max range)
 * - Located near front camera
 *
 * USE CASES:
 * - Auto-pause when phone is in pocket (privacy)
 * - Pause when phone is face-down
 * - Wave gesture detection
 *
 * USAGE IN ACTIVITIES:
 * - Create instance: proximitySensor = new ProximitySensor(this);
 * - Set callbacks:
 *   proximitySensor.setOnNearListener(() -> { ... });
 *   proximitySensor.setOnFarListener(() -> { ... });
 * - Initialize: proximitySensor.initialize();
 * - Start: proximitySensor.startListening(); (in onResume)
 * - Stop: proximitySensor.stopListening(); (in onPause) - CRITICAL!
 */
public class ProximitySensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener listener;

    // Callbacks
    private OnProximityChangedListener onProximityChangedListener;
    private OnNearListener onNearListener;
    private OnFarListener onFarListener;

    // Proximity state
    private boolean isNear = false;
    private float maxRange = 5f;
    private long lastTriggerTime = 0;

    private static final float NEAR_THRESHOLD = 3f; // cm
    private static final int DEBOUNCE_DELAY = 300; // milliseconds

    /**
     * Constructor
     */
    public ProximitySensor(Context context) {
        this.context = context;
    }

    /**
     * TODO (Student 4): Implement sensor initialization
     *
     * Steps:
     * 1. Get SensorManager
     * 2. Get proximity sensor (TYPE_PROXIMITY)
     * 3. Get maximum range
     * 4. Check if sensor exists
     */
    public boolean initialize() {
        // TODO: Implement initialization
        // sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // if (proximitySensor != null) {
        //     maxRange = proximitySensor.getMaximumRange();
        // }
        // return proximitySensor != null;

        return false; // Replace with actual implementation
    }

    /**
     * TODO (Student 4): Implement sensor listener
     *
     * Steps:
     * 1. Create SensorEventListener
     * 2. Read distance value (event.values[0])
     * 3. Determine if near or far
     * 4. Implement debouncing to avoid rapid triggers
     * 5. Detect state changes (near->far or far->near)
     * 6. Call appropriate callbacks
     */
    public void startListening() {
        // TODO: Implement listener

        /* EXAMPLE CODE:
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float distance = event.values[0];
                boolean currentlyNear = distance < NEAR_THRESHOLD;

                // Debouncing
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTriggerTime < DEBOUNCE_DELAY) {
                    return;
                }

                // Detect state change
                if (currentlyNear != isNear) {
                    isNear = currentlyNear;
                    lastTriggerTime = currentTime;

                    // Notify listeners
                    if (onProximityChangedListener != null) {
                        onProximityChangedListener.onProximityChanged(distance, isNear);
                    }

                    if (isNear && onNearListener != null) {
                        onNearListener.onNear();
                    } else if (!isNear && onFarListener != null) {
                        onFarListener.onFar();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        if (sensorManager != null && proximitySensor != null) {
            sensorManager.registerListener(listener, proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        }
        */
    }

    /**
     * TODO (Student 4): Unregister sensor listener
     * CRITICAL: Proximity sensor can drain battery!
     */
    public void stopListening() {
        // TODO: Unregister listener
        // if (sensorManager != null && listener != null) {
        //     sensorManager.unregisterListener(listener);
        // }
    }

    /**
     * Check if sensor is available
     */
    public boolean isAvailable() {
        return proximitySensor != null;
    }

    /**
     * Get current proximity state
     */
    public boolean isObjectNear() {
        return isNear;
    }

    /**
     * Get maximum sensor range
     */
    public float getMaxRange() {
        return maxRange;
    }

    /**
     * Set proximity change listener
     */
    public void setOnProximityChangedListener(OnProximityChangedListener listener) {
        this.onProximityChangedListener = listener;
    }

    /**
     * Set near detection listener
     */
    public void setOnNearListener(OnNearListener listener) {
        this.onNearListener = listener;
    }

    /**
     * Set far detection listener
     */
    public void setOnFarListener(OnFarListener listener) {
        this.onFarListener = listener;
    }

    /**
     * Callback interface for proximity changes
     */
    public interface OnProximityChangedListener {
        void onProximityChanged(float distance, boolean isNear);
    }

    /**
     * Callback interface for near detection
     */
    public interface OnNearListener {
        void onNear();
    }

    /**
     * Callback interface for far detection
     */
    public interface OnFarListener {
        void onFar();
    }
}

