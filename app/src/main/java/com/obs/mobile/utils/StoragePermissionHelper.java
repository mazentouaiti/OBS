package com.obs.mobile.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * StoragePermissionHelper - Handles storage permissions across different Android versions
 */
public class StoragePermissionHelper {

    private static final String TAG = "StoragePermissionHelper";

    // Permission request codes
    public static final int REQUEST_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_MANAGE_STORAGE = 1002;

    /**
     * Check if storage permissions are granted
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 10 and below (API 29 and below)
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permissions
     */
    public static void requestStoragePermissions(Activity activity) {
        List<String> permissions = getRequiredStoragePermissions();

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissions.toArray(new String[0]),
                    REQUEST_STORAGE_PERMISSION
            );
        }
    }

    /**
     * Get list of required storage permissions based on Android version
     */
    private static List<String> getRequiredStoragePermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            // Android 10 and below (API 29 and below)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        return permissions;
    }

    /**
     * Check if we need to request MANAGE_EXTERNAL_STORAGE permission
     * (Only use this if you absolutely need full file system access)
     */
    public static boolean needsManageStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return !Environment.isExternalStorageManager();
        }
        return false;
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission
     * (Only use if absolutely necessary - most apps don't need this)
     */
    public static void requestManageStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting manage storage permission", e);
                // Fallback to general settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            }
        }
    }

    /**
     * Show rationale dialog explaining why permissions are needed
     */
    public static void showPermissionRationale(Activity activity, PermissionCallback callback) {
        new AlertDialog.Builder(activity)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs storage permission to save recordings and stream files to your device. " +
                        "Without this permission, recordings cannot be saved.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionGranted();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionDenied();
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Check if permission was permanently denied
     */
    public static boolean isPermissionPermanentlyDenied(Activity activity, String permission) {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Open app settings
     */
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    /**
     * Handle permission result
     */
    public static boolean handlePermissionResult(Activity activity, int requestCode,
                                                 String[] permissions, int[] grantResults,
                                                 PermissionCallback callback) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "Storage permissions granted");
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            } else {
                Log.w(TAG, "Storage permissions denied");

                // Check if permanently denied
                boolean permanentlyDenied = false;
                for (String permission : permissions) {
                    if (isPermissionPermanentlyDenied(activity, permission)) {
                        permanentlyDenied = true;
                        break;
                    }
                }

                if (permanentlyDenied) {
                    showSettingsDialog(activity);
                }

                if (callback != null) {
                    callback.onPermissionDenied();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Show dialog to open app settings when permission is permanently denied
     */
    private static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("Storage permission was denied. Please enable it in app settings to save recordings.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings(activity))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Callback interface for permission results
     */
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    /**
     * Get storage location description for user
     */
    public static String getStorageLocationDescription(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "Movies/OBS_Streams (using MediaStore)";
        } else {
            return Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES) + "/OBS_Streams";
        }
    }

    /**
     * Check if we're using scoped storage
     */
    public static boolean isUsingScopedStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    /**
     * Log current permission status (for debugging)
     */
    public static void logPermissionStatus(Context context) {
        Log.d(TAG, "=== Storage Permission Status ===");
        Log.d(TAG, "Android Version: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Using Scoped Storage: " + isUsingScopedStorage());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "READ_MEDIA_VIDEO: " +
                    (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                            == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            Log.d(TAG, "READ_MEDIA_IMAGES: " +
                    (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                            == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        } else {
            Log.d(TAG, "READ_EXTERNAL_STORAGE: " +
                    (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE: " +
                        (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "MANAGE_EXTERNAL_STORAGE: " + Environment.isExternalStorageManager());
        }

        Log.d(TAG, "Storage Location: " + getStorageLocationDescription(context));
        Log.d(TAG, "===============================");
    }
}