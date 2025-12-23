package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class LightSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener listener;

    private OnLightChangedListener onLightChangedListener;

    private static final float VERY_DARK_THRESHOLD = 10f;
    private static final float DARK_THRESHOLD = 50f;
    private static final float NORMAL_THRESHOLD = 500f;
    private static final float BRIGHT_THRESHOLD = 10000f;

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

    public LightSensor(Context context) {
        this.context = context;
    }

    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return false;
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        return lightSensor != null;
    }

    public void startListening() {
        if (sensorManager == null || lightSensor == null || listener != null) return;

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values == null || event.values.length == 0) return;

                float lux = event.values[0];
                LightCategory category = categorizeLightLevel(lux);

                if (onLightChangedListener != null) {
                    onLightChangedListener.onLightChanged(lux, category);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListening() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
            listener = null;
        }
    }

    private LightCategory categorizeLightLevel(float lux) {
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

    public String getCameraRecommendation(float lux) {
        LightCategory category = categorizeLightLevel(lux);
        switch (category) {
            case VERY_DARK: return "Enable night mode";
            case DARK: return "Increase ISO";
            case NORMAL: return "Good indoor lighting";
            case BRIGHT: return "Optimal conditions";
            case VERY_BRIGHT: return "Reduce exposure";
            default: return "Normal mode";
        }
    }

    public boolean isAvailable() {
        return lightSensor != null;
    }

    public void setOnLightChangedListener(OnLightChangedListener listener) {
        this.onLightChangedListener = listener;
    }

    public interface OnLightChangedListener {
        void onLightChanged(float lux, LightCategory category);
    }
}