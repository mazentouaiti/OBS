package com.obs.mobile.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * RecordingsManager - Handles all recording file operations with proper MediaStore integration
 */
public class RecordingsManager {

    private static final String TAG = "RecordingsManager";
    private final Context context;

    public RecordingsManager(Context context) {
        this.context = context;
    }

    /**
     * Add recording to MediaStore so it appears in Gallery and Files app
     */
    public Uri addRecordingToMediaStore(File file) {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
            return null;
        }

        try {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();

            values.put(MediaStore.Video.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.SIZE, file.length());
            values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            values.put(MediaStore.Video.Media.DATE_MODIFIED, file.lastModified() / 1000);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use relative path
                values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/OBS_Streams");
                values.put(MediaStore.Video.Media.IS_PENDING, 1);

                Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = resolver.insert(collection, values);

                if (itemUri != null) {
                    // Copy file to MediaStore
                    try (FileInputStream in = new FileInputStream(file);
                         OutputStream out = resolver.openOutputStream(itemUri)) {

                        if (out != null) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    }

                    // Mark as not pending anymore
                    values.clear();
                    values.put(MediaStore.Video.Media.IS_PENDING, 0);
                    resolver.update(itemUri, values, null, null);

                    Log.d(TAG, "File added to MediaStore: " + itemUri);
                    return itemUri;
                }
            } else {
                // Android 9 and below - Use DATA field
                values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());

                Uri itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                if (itemUri != null) {
                    // Notify media scanner
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(file));
                    context.sendBroadcast(mediaScanIntent);

                    Log.d(TAG, "File added to MediaStore: " + itemUri);
                    return itemUri;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding file to MediaStore", e);
        }

        return null;
    }

    /**
     * Open file with default video player
     */
    public boolean openWithVideoPlayer(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri videoUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
            intent.setDataAndType(videoUri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening video player", e);
            return false;
        }
    }

    /**
     * Share file using ShareSheet
     */
    public boolean shareFile(File file) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri videoUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
            shareIntent.setType("video/mp4");
            shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OBS Recording");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my recording!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent chooser = Intent.createChooser(shareIntent, "Share video via");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            return false;
        }
    }

    /**
     * Delete file from storage and MediaStore
     */
    public boolean deleteFile(File file) {
        try {
            // Delete from MediaStore first (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                String selection = MediaStore.Video.Media.DISPLAY_NAME + "=?";
                String[] selectionArgs = {file.getName()};

                int deletedRows = resolver.delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                );

                Log.d(TAG, "Deleted from MediaStore: " + deletedRows + " rows");
            }

            // Delete actual file
            boolean deleted = file.delete();
            Log.d(TAG, "File deleted: " + deleted);
            return deleted;

        } catch (Exception e) {
            Log.e(TAG, "Error deleting file", e);
            return false;
        }
    }

    /**
     * Open folder in file manager (multiple methods)
     */
    public boolean openRecordingsFolder() {
        // Method 1: Try to open Videos/Movies folder
        if (openVideosFolder()) {
            return true;
        }

        // Method 2: Try generic file browser
        if (openFileBrowser()) {
            return true;
        }

        return false;
    }

    /**
     * Open system Videos/Movies folder
     */
    private boolean openVideosFolder() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } else {
                // For older versions, try to open Downloads/Files app
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(intent, "Open file manager"));
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to open videos folder", e);
            return false;
        }
    }

    /**
     * Open generic file browser
     */
    private boolean openFileBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("resource/folder");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Failed to open file browser", e);
            return false;
        }
    }

    /**
     * Get content URI for file (for sharing/viewing)
     */
    public Uri getFileUri(File file) {
        try {
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
        } catch (Exception e) {
            Log.e(TAG, "Error getting file URI", e);
            return null;
        }
    }

    /**
     * Get MIME type for file
     */
    public String getMimeType(File file) {
        String extension = getFileExtension(file.getName());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return "video/mp4"; // Default
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Format file size for display
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(java.util.Locale.US, "%.2f %s",
                bytes / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * Check if file exists and is readable
     */
    public boolean isFileValid(File file) {
        return file != null && file.exists() && file.canRead() && file.length() > 0;
    }
}