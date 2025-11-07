package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * LightSensor - Independent sensor class for ambient light detection
 *
 * ============================================================
 * TODO (Student 3 - Light Sensor):
 * ============================================================
 *
 * OBJECTIVE: Measure ambient light for auto-adjusting camera settings
 *
 * WHAT IS A LIGHT SENSOR?
 * - Measures ambient light illuminance in lux
 * - Helps adapt camera to lighting conditions
 *
 * LIGHT CATEGORIES:
 * - VERY_DARK: 0-10 lux (moonlight)
 * - DARK: 10-50 lux (candle, low light)
 * - NORMAL: 50-500 lux (indoor lighting)
 * - BRIGHT: 500-10000 lux (outdoor shade)
 * - VERY_BRIGHT: 10000+ lux (direct sunlight)
 *
 * USAGE IN ACTIVITIES:
 * - Create instance: lightSensor = new LightSensor(this);
 * - Set callback: lightSensor.setOnLightChangedListener((lux, category) -> { ... });
 * - Initialize: lightSensor.initialize();
 * - Start: lightSensor.startListening(); (in onResume)
 * - Stop: lightSensor.stopListening(); (in onPause)
 */
public class LightSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener listener;

    // Callbacks
    private OnLightChangedListener onLightChangedListener;

    // Light level thresholds
    private static final float VERY_DARK_THRESHOLD = 10f;
    private static final float DARK_THRESHOLD = 50f;
    private static final float NORMAL_THRESHOLD = 500f;
    private static final float BRIGHT_THRESHOLD = 10000f;

    /**
     * Light level categories
     */
    public enum LightCategory {
        VERY_DARK("Very Dark", "0-10 lux"),
        DARK("Dark", "10-50 lux"),
        NORMAL("Normal", "50-500 lux"),
        BRIGHT("Bright", "500-10000 lux"),
        VERY_BRIGHT("Very Bright", "10000+ lux");

        private final String name;
        private final String range;

        LightCategory(String name, String range) {
            this.name = name;
            this.range = range;
        }

        public String getName() { return name; }
        public String getRange() { return range; }
    }

    /**
     * Constructor
     */
    public LightSensor(Context context) {
        this.context = context;
    }

    /**
     * TODO (Student 3): Implement sensor initialization
     *
     * Steps:
     * 1. Get SensorManager
     * 2. Get light sensor (TYPE_LIGHT)
     * 3. Check if sensor exists
     */
    public boolean initialize() {
        // TODO: Implement initialization
        // sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        // return lightSensor != null;

        return false; // Replace with actual implementation
    }

    /**
     * TODO (Student 3): Implement sensor listener
     *
     * Steps:
     * 1. Create SensorEventListener
     * 2. Read lux value (event.values[0])
     * 3. Categorize light level
     * 4. Notify listener with lux and category
     * 5. Use SENSOR_DELAY_NORMAL (light changes slowly)
     */
    public void startListening() {
        // TODO: Implement listener

        /* EXAMPLE CODE:
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float lux = event.values[0];
                LightCategory category = categorizeLightLevel(lux);

                if (onLightChangedListener != null) {
                    onLightChangedListener.onLightChanged(lux, category);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        }
        */
    }

    /**
     * TODO (Student 3): Unregister sensor listener
     */
    public void stopListening() {
        // TODO: Unregister listener
        // if (sensorManager != null && listener != null) {
        //     sensorManager.unregisterListener(listener);
        // }
    }

    /**
     * TODO (Student 3): Categorize light level based on lux value
     */
    private LightCategory categorizeLightLevel(float lux) {
        // TODO: Implement categorization
        if (lux < VERY_DARK_THRESHOLD) {
            return LightCategory.VERY_DARK;
        } else if (lux < DARK_THRESHOLD) {
            return LightCategory.DARK;
        } else if (lux < NORMAL_THRESHOLD) {
            return LightCategory.NORMAL;
        } else if (lux < BRIGHT_THRESHOLD) {
            return LightCategory.BRIGHT;
        } else {
            return LightCategory.VERY_BRIGHT;
        }
    }

    /**
     * Get camera recommendation based on light level
     */
    public String getCameraRecommendation(float lux) {
        if (lux < VERY_DARK_THRESHOLD) {
            return "Enable night mode - Very low light";
        } else if (lux < DARK_THRESHOLD) {
            return "Low light detected - Increase ISO";
        } else if (lux < NORMAL_THRESHOLD) {
            return "Good lighting conditions";
        } else if (lux < BRIGHT_THRESHOLD) {
            return "Bright conditions - Good for recording";
        } else {
            return "Very bright - Reduce exposure";
        }
    }

    /**
     * Check if sensor is available
     */
    public boolean isAvailable() {
        return lightSensor != null;
    }

    /**
     * Set light change listener
     */
    public void setOnLightChangedListener(OnLightChangedListener listener) {
        this.onLightChangedListener = listener;
    }

    /**
     * Callback interface for light changes
     */
    public interface OnLightChangedListener {
        void onLightChanged(float lux, LightCategory category);
    }
}

