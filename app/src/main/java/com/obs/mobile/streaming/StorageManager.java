package com.obs.mobile.streaming;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * StorageManager - Manages local storage for streams and recordings with MediaStore integration
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
            // Fallback to app's private directory
            recordingsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), STORAGE_DIR);
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
            streamsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    STORAGE_DIR + "/Streams");
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
     * Create a new recording file using MediaStore (Android 10+)
     * Returns output stream to write to
     */
    public MediaStoreFile createRecordingFile(String filename) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            try {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();

                values.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + STORAGE_DIR);
                values.put(MediaStore.Video.Media.IS_PENDING, 1);

                Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = resolver.insert(collection, values);

                if (itemUri != null) {
                    OutputStream outputStream = resolver.openOutputStream(itemUri);
                    Log.d(TAG, "Created MediaStore file: " + itemUri);
                    return new MediaStoreFile(itemUri, outputStream, filename);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating MediaStore file", e);
            }
        }

        // Fallback to regular file
        File file = new File(getRecordingsDirectory(), filename);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            return new MediaStoreFile(Uri.fromFile(file), outputStream, filename);
        } catch (Exception e) {
            Log.e(TAG, "Error creating file", e);
            return null;
        }
    }

    /**
     * Mark MediaStore file as complete (not pending)
     */
    public void finalizeMediaStoreFile(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
                Log.d(TAG, "Finalized MediaStore file: " + uri);
            } catch (Exception e) {
                Log.e(TAG, "Error finalizing MediaStore file", e);
            }
        }
    }

    /**
     * Get list of all recordings from both file system and MediaStore
     */
    public File[] getRecordings() {
        List<File> recordings = new ArrayList<>();

        // Get from file system
        File recordingsDir = getRecordingsDirectory();
        File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));
        if (files != null) {
            for (File file : files) {
                recordings.add(file);
            }
        }

        // Get from MediaStore (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = context.getContentResolver();
                String[] projection = {
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATA
                };

                String selection = MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
                String[] selectionArgs = {"Movies/" + STORAGE_DIR + "%"};

                Cursor cursor = resolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        MediaStore.Video.Media.DATE_ADDED + " DESC"
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        if (path != null) {
                            File file = new File(path);
                            if (file.exists() && !recordings.contains(file)) {
                                recordings.add(file);
                            }
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying MediaStore", e);
            }
        }

        return recordings.toArray(new File[0]);
    }

    /**
     * Get total storage size used
     */
    public long getStorageUsed() {
        long totalSize = 0;
        File[] recordings = getRecordings();

        if (recordings != null) {
            for (File file : recordings) {
                if (file.isFile()) {
                    totalSize += file.length();
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
            // Try to delete from MediaStore first (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentResolver resolver = context.getContentResolver();
                    String selection = MediaStore.Video.Media.DISPLAY_NAME + "=?";
                    String[] selectionArgs = {file.getName()};

                    int deleted = resolver.delete(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            selection,
                            selectionArgs
                    );

                    Log.d(TAG, "Deleted from MediaStore: " + deleted + " items");
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting from MediaStore", e);
                }
            }

            // Delete actual file
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

    /**
     * Helper class to hold MediaStore file information
     */
    public static class MediaStoreFile {
        public final Uri uri;
        public final OutputStream outputStream;
        public final String filename;

        public MediaStoreFile(Uri uri, OutputStream outputStream, String filename) {
            this.uri = uri;
            this.outputStream = outputStream;
            this.filename = filename;
        }
    }
    /**
     * Verify if a file is saved and accessible
     */
    public boolean isFileAccessible(String filePath) {
        try {
            File file = new File(filePath);

            // Check basic file properties
            boolean exists = file.exists();
            boolean canRead = file.canRead();
            boolean hasSize = file.length() > 0;

            Log.d(TAG, "File check - Path: " + filePath);
            Log.d(TAG, "File check - Exists: " + exists);
            Log.d(TAG, "File check - Can read: " + canRead);
            Log.d(TAG, "File check - Size: " + file.length() + " bytes");
            Log.d(TAG, "File check - Path: " + file.getAbsolutePath());

            return exists && canRead && hasSize;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when checking file", e);
            return false;
        }
    }

    /**
     * Get all recording files including MediaStore entries
     */
    public List<File> getAllRecordingFiles() {
        List<File> allFiles = new ArrayList<>();

        // Get from local directory
        File recordingsDir = getRecordingsDirectory();
        if (recordingsDir.exists() && recordingsDir.isDirectory()) {
            File[] files = recordingsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".mp4") ||
                            file.getName().endsWith(".mkv") ||
                            file.getName().endsWith(".mov"))) {
                        allFiles.add(file);
                    }
                }
            }
        }

        return allFiles;
    }
}