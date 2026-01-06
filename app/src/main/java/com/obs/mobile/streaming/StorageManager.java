package com.obs.mobile.streaming;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * StorageManager - Manages local storage for streams and recordings
 */
public class StorageManager {

    private static final String TAG = "StorageManager";
    private static final String STORAGE_DIR = "OBS_Streams";
    private final Context context;

    public StorageManager(Context context) {
        this.context = context;
    }

    /**
     * Get recordings directory
     */
    public File getRecordingsDirectory() {
        File recordingsDir;

        // Try external storage first (Public directory)
        if (isExternalStorageWritable()) {
            recordingsDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    STORAGE_DIR
            );
        } else {
            // Fallback to app's cache directory
            recordingsDir = new File(context.getCacheDir(), STORAGE_DIR);
        }

        // Create directory if it doesn't exist
        if (!recordingsDir.exists()) {
            if (recordingsDir.mkdirs()) {
                Log.d(TAG, "Created recordings directory: " + recordingsDir.getAbsolutePath());
            } else {
                Log.w(TAG, "Failed to create recordings directory");
            }
        }

        return recordingsDir;
    }

    /**
     * Get streams directory
     */
    public File getStreamsDirectory() {
        File streamsDir;

        if (isExternalStorageWritable()) {
            streamsDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    STORAGE_DIR + "/Streams"
            );
        } else {
            streamsDir = new File(context.getCacheDir(), STORAGE_DIR + "/Streams");
        }

        if (!streamsDir.exists()) {
            if (streamsDir.mkdirs()) {
                Log.d(TAG, "Created streams directory: " + streamsDir.getAbsolutePath());
            } else {
                Log.w(TAG, "Failed to create streams directory");
            }
        }

        return streamsDir;
    }

    /**
     * Generate recording file path with timestamp
     */
    public String generateRecordingPath() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String filename = "Recording_" + timestamp + ".mp4";
        return new File(getRecordingsDirectory(), filename).getAbsolutePath();
    }

    /**
     * Generate stream file path with timestamp
     */
    public String generateStreamPath() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String filename = "Stream_" + timestamp + ".mp4";
        return new File(getStreamsDirectory(), filename).getAbsolutePath();
    }

    /**
     * Get list of all recordings
     */
    public File[] getRecordings() {
        File recordingsDir = getRecordingsDirectory();
        return recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));
    }

    /**
     * Get total storage size used
     */
    public long getStorageUsed() {
        long totalSize = 0;
        File recordingsDir = getRecordingsDirectory();

        if (recordingsDir.exists()) {
            File[] files = recordingsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        totalSize += file.length();
                    }
                }
            }
        }

        return totalSize;
    }

    /**
     * Delete recording file
     */
    public boolean deleteRecording(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Check if external storage is writable
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Format file size for display
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s",
                bytes / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }
}

