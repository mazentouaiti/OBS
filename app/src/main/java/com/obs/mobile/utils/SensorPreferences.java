package com.obs.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SensorPreferences - Manages sensor enable/disable states across activities
 */
public class SensorPreferences {

    private static final String PREFS_NAME = "sensor_preferences";
    private static final String KEY_GYROSCOPE_ENABLED = "gyroscope_enabled";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setGyroscopeEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(KEY_GYROSCOPE_ENABLED, enabled);
        editor.apply();
    }

    public static boolean isGyroscopeEnabled(Context context) {
        return getPreferences(context).getBoolean(KEY_GYROSCOPE_ENABLED, false);
    }
}
