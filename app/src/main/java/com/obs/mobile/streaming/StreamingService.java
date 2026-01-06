package com.obs.mobile.streaming;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.obs.mobile.R;
import com.obs.mobile.StreamingActivity;

import org.jspecify.annotations.NonNull;

import java.util.Collections;

public class StreamingService extends Service {
    private static final String TAG = "StreamingService";
    private static final String CHANNEL_ID = "StreamingServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private StreamManager streamManager;
    private boolean isStreaming = false;
    private boolean isCameraActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "StreamingService created");

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        streamManager = new StreamManager(this);
        startBackgroundThread();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "StreamingService started");

        // Start foreground service
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Start streaming
        if (intent != null) {
            String serverUrl = intent.getStringExtra("server_url");
            String streamKey = intent.getStringExtra("stream_key");
            boolean recordLocally = intent.getBooleanExtra("record_locally", true);

            if (serverUrl != null && streamKey != null) {
                startStreaming(serverUrl, streamKey, recordLocally);
            }
        }

        return START_STICKY;
    }

    private void startStreaming(String serverUrl, String streamKey, boolean recordLocally) {
        Log.d(TAG, "Starting streaming...");

        // Start camera in background
        openCamera();

        // Start streaming
        streamManager.startStreaming(serverUrl, streamKey, recordLocally);
        isStreaming = true;

        Log.d(TAG, "Streaming started to: " + serverUrl + "/" + streamKey);
    }

    private void stopStreaming() {
        if (isStreaming) {
            streamManager.stopStreaming();
            isStreaming = false;
            closeCamera();
            Log.d(TAG, "Streaming stopped");
        }
    }

    private void openCamera() {
        try {
            if (cameraManager == null) return;

            String cameraId = getBackCameraId();
            if (cameraId == null) {
                Log.e(TAG, "No back camera found");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        isCameraActive = true;
                        Log.d(TAG, "Camera opened in background");
                        createCameraPreviewSession();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        cameraDevice.close();
                        isCameraActive = false;
                        Log.d(TAG, "Camera disconnected");
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        cameraDevice.close();
                        isCameraActive = false;
                        Log.e(TAG, "Camera error: " + error);
                    }
                }, backgroundHandler);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
        }
    }

    private String getBackCameraId() throws CameraAccessException {
        if (cameraManager == null) return null;

        String[] cameraIdList = cameraManager.getCameraIdList();
        for (String id : cameraIdList) {
            android.hardware.camera2.CameraCharacteristics characteristics =
                    cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return cameraIdList.length > 0 ? cameraIdList[0] : null;
    }

    private void createCameraPreviewSession() {
        if (cameraDevice == null || !isCameraActive) return;

        try {
            // Create a Surface for the camera preview (even though we don't show it)
            // We'll use a dummy surface since we're not displaying the preview
            Surface dummySurface = streamManager.getInputSurface();
            if (dummySurface == null) {
                // Create a dummy surface texture if needed
                return;
            }

            cameraDevice.createCaptureSession(
                    Collections.singletonList(dummySurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                CaptureRequest.Builder previewRequestBuilder =
                                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                previewRequestBuilder.addTarget(dummySurface);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                session.setRepeatingRequest(previewRequestBuilder.build(),
                                        null, backgroundHandler);

                                Log.d(TAG, "Camera preview session configured in background");
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting up preview request", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera preview session configuration failed");
                        }
                    },
                    backgroundHandler
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating camera preview session", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        isCameraActive = false;
        Log.d(TAG, "Camera closed");
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("StreamingBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Streaming Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, StreamingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Live Streaming Active")
                .setContentText("Streaming in background...")
                .setSmallIcon(R.drawable.ic_live_stream)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStreaming();
        stopBackgroundThread();
        Log.d(TAG, "StreamingService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}