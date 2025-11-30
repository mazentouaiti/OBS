package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * ProximitySensor
 * ---------------------------------------------------------
 * A reusable helper class that wraps Android's proximity sensor
 * and provides easy-to-use callbacks for NEAR/FAR detection.
 *
 * Main features:
 *  - Detects if an object is close to the front of the device.
 *  - Debounces noisy sensor data.
 *  - Supports three callbacks:
 *      -> onNear()
 *      -> onFar()
 *      -> onProximityChanged(distance, isNear)
 *
 * Usage:
 *      ProximitySensor ps = new ProximitySensor(this);
 *      ps.initialize();
 *      ps.setOnNearListener(() -> pauseRecording());
 *      ps.startListening();       // onResume
 *      ps.stopListening();        // onPause
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

    // State
    private boolean isNear = false;
    private float maxRange = 5f;
    private long lastTriggerTime = 0;

    private static final float NEAR_THRESHOLD = 3f; // in centimeters
    private static final int DEBOUNCE_DELAY = 300; // ms

    /**
     * Constructor
     *
     * @param context Activity or Service context
     */
    public ProximitySensor(Context context) {
        this.context = context;
    }

    /**
     * Initializes the proximity sensor.
     *
     * Steps:
     *  1. Retrieve SensorManager
     *  2. Get TYPE_PROXIMITY sensor
     *  3. Retrieve max range
     *
     * @return true if sensor exists on this device
     */
    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return false;

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor != null) {
            maxRange = proximitySensor.getMaximumRange();
        }

        return proximitySensor != null;
    }

    /**
     * Starts listening to proximity sensor updates.
     * Must be called in Activity.onResume().
     *
     * Logic:
     *  - Reads distance from sensor
     *  - Performs debouncing
     *  - Detects state change (near/far)
     *  - Calls appropriate callbacks
     */
    public void startListening() {
        if (sensorManager == null || proximitySensor == null) return;

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float distance = event.values[0];
                boolean currentlyNear = distance < NEAR_THRESHOLD;

                long now = System.currentTimeMillis();
                if (now - lastTriggerTime < DEBOUNCE_DELAY) return;

                if (currentlyNear != isNear) {
                    isNear = currentlyNear;
                    lastTriggerTime = now;

                    // Global callback
                    if (onProximityChangedListener != null) {
                        onProximityChangedListener.onProximityChanged(distance, isNear);
                    }

                    // Specific callbacks
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

        sensorManager.registerListener(
                listener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }

    /**
     * Stops listening to sensor updates.
     * Must be called in Activity.onPause().
     *
     * Critical for:
     *  - Battery life
     *  - Avoiding background sensor leaks
     */
    public void stopListening() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    /**
     * @return true if proximity sensor is present
     */
    public boolean isAvailable() {
        return proximitySensor != null;
    }

    /**
     * @return true if last detected state is "near"
     */
    public boolean isObjectNear() {
        return isNear;
    }

    /**
     * @return maximum measurable distance in cm
     */
    public float getMaxRange() {
        return maxRange;
    }

    /**
     * Sets a callback for ANY proximity change.
     */
    public void setOnProximityChangedListener(OnProximityChangedListener listener) {
        this.onProximityChangedListener = listener;
    }

    /**
     * Sets callback for when object becomes near.
     */
    public void setOnNearListener(OnNearListener listener) {
        this.onNearListener = listener;
    }

    /**
     * Sets callback for when object becomes far.
     */
    public void setOnFarListener(OnFarListener listener) {
        this.onFarListener = listener;
    }

    // ============================================================
    // Callback Interfaces
    // ============================================================

    /**
     * Called on any proximity change.
     */
    public interface OnProximityChangedListener {
        void onProximityChanged(float distance, boolean isNear);
    }

    /**
     * Called when distance < NEAR_THRESHOLD.
     */
    public interface OnNearListener {
        void onNear();
    }

    /**
     * Called when distance >= NEAR_THRESHOLD.
     */
    public interface OnFarListener {
        void onFar();
    }
}
