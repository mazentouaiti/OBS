package com.obs.mobile.streaming;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BackgroundStreamLauncher {
    private static final String TAG = "BackgroundStreamLauncher";

    public static void startBackgroundStream(Context context,
                                             String serverUrl,
                                             String streamKey,
                                             boolean recordLocally) {
        try {
            Intent serviceIntent = new Intent(context, StreamingService.class);
            serviceIntent.putExtra("server_url", serverUrl);
            serviceIntent.putExtra("stream_key", streamKey);
            serviceIntent.putExtra("record_locally", recordLocally);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.i(TAG, "Background streaming service started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start background streaming", e);
        }
    }

    public static void stopBackgroundStream(Context context) {
        try {
            Intent serviceIntent = new Intent(context, StreamingService.class);
            context.stopService(serviceIntent);
            Log.i(TAG, "Background streaming service stopped");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop background streaming", e);
        }
    }
}