package com.obs.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SensorPreferences - Utility class for managing sensor preferences
 */
public class SensorPreferences {

    private static final String PREF_NAME = "sensor_preferences";
    private static final String KEY_GYROSCOPE_ENABLED = "gyroscope_enabled";
    private static final String KEY_ACCELEROMETER_ENABLED = "accelerometer_enabled";
    private static final String KEY_LIGHT_SENSOR_ENABLED = "light_sensor_enabled";
    private static final String KEY_PROXIMITY_ENABLED = "proximity_enabled";
    private static final String KEY_MAGNETOMETER_ENABLED = "magnetometer_enabled";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Accelerometer - Default: DISABLED
    public static boolean isAccelerometerEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_ACCELEROMETER_ENABLED, false);
    }

    public static void setAccelerometerEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_ACCELEROMETER_ENABLED, enabled)
                .apply();
    }

    // Gyroscope - Default: DISABLED
    public static boolean isGyroscopeEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_GYROSCOPE_ENABLED, false);
    }

    public static void setGyroscopeEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_GYROSCOPE_ENABLED, enabled)
                .apply();
    }

    // Light Sensor - Default: DISABLED
    public static boolean isLightSensorEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_LIGHT_SENSOR_ENABLED, false);
    }

    public static void setLightSensorEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_LIGHT_SENSOR_ENABLED, enabled)
                .apply();
    }

    // Proximity Sensor - Default: DISABLED
    public static boolean isProximityEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_PROXIMITY_ENABLED, false);
    }

    public static void setProximityEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_PROXIMITY_ENABLED, enabled)
                .apply();
    }

    // Magnetometer - Default: DISABLED
    public static boolean isMagnetometerEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_MAGNETOMETER_ENABLED, false);
    }

    public static void setMagnetometerEnabled(Context context, boolean enabled) {
        getPreferences(context).edit()
                .putBoolean(KEY_MAGNETOMETER_ENABLED, enabled)
                .apply();
    }

    /**
     * Get count of enabled sensors
     */
    public static int getEnabledSensorCount(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int count = 0;
        if (prefs.getBoolean(KEY_ACCELEROMETER_ENABLED, false)) count++;
        if (prefs.getBoolean(KEY_GYROSCOPE_ENABLED, false)) count++;
        if (prefs.getBoolean(KEY_LIGHT_SENSOR_ENABLED, false)) count++;
        if (prefs.getBoolean(KEY_PROXIMITY_ENABLED, false)) count++;
        if (prefs.getBoolean(KEY_MAGNETOMETER_ENABLED, false)) count++;
        return count;
    }
}