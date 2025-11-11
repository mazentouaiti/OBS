package com.obs.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Collections;

/**
 * FloatingCameraService - Service that shows a resizable floating camera window
 */
public class FloatingCameraServiceFixed extends Service {

    private static final String TAG = "FloatingCameraService";
    private static final String CHANNEL_ID = "FloatingCameraChannel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private TextureView textureView;
    private ImageButton btnClose;
    private ImageButton btnResize;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private final boolean isFrontCamera = true;
    private int currentSize = 1; // 0=small, 1=medium, 2=large

    // Window parameters for drag
    private WindowManager.LayoutParams params;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🟢 FloatingCameraService created");

        try {
            // Verify overlay permission first
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "❌ CRITICAL: Overlay permission not granted!");
                stopSelf();
                return;
            }
            Log.d(TAG, "✅ Overlay permission verified");

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

            if (windowManager == null || cameraManager == null) {
                Log.e(TAG, "❌ Failed to get system services");
                stopSelf();
                return;
            }
            Log.d(TAG, "✅ System services obtained");

            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
            Log.d(TAG, "📱 Foreground service started");

            // Create floating window immediately
            if (createFloatingWindow()) {
                startBackgroundThread();
                // Delay camera opening to ensure TextureView is ready
                new Handler().postDelayed(this::openCamera, 1000);
            } else {
                Log.e(TAG, "❌ Failed to create floating window");
                stopSelf();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onCreate", e);
            stopSelf();
        }
    }

    private boolean createFloatingWindow() {
        Log.d(TAG, "🪟 Creating floating window...");

        try {
            // Try XML layout first
            FrameLayout container = new FrameLayout(this);
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_camera, container, false);
            Log.d(TAG, "✅ XML layout inflated successfully");

            // Window parameters with more compatible settings
            params = new WindowManager.LayoutParams();
            params.width = dpToPx(200);
            params.height = dpToPx(300);
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            params.format = PixelFormat.TRANSLUCENT;
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 50;
            params.y = 100;

            Log.d(TAG, "🪟 Adding view to WindowManager with params: " + params.width + "x" + params.height);

            // Add the view to WindowManager
            windowManager.addView(floatingView, params);
            Log.d(TAG, "✅ Floating window added to WindowManager successfully!");

            setupXMLViewListeners();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception with XML layout, trying programmatic", e);

            // Try fallback programmatic method
            try {
                floatingView = createProgrammaticLayout();
                windowManager.addView(floatingView, params);
                Log.d(TAG, "✅ Programmatic layout worked!");
                setupViewListeners();
                return true;
            } catch (Exception fallbackException) {
                Log.e(TAG, "❌ Both methods failed", fallbackException);
                return false;
            }
        }
    }

    private View createProgrammaticLayout() {
        // Create a simple programmatic layout
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(0x80000000); // Semi-transparent black

        // Create TextureView
        textureView = new TextureView(this);
        FrameLayout.LayoutParams textureParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        container.addView(textureView, textureParams);

        // Create close button
        btnClose = new ImageButton(this);
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackgroundColor(0xAA000000);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(40));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(8, 8, 8, 8);
        container.addView(btnClose, closeParams);

        // Create resize button
        btnResize = new ImageButton(this);
        btnResize.setImageResource(android.R.drawable.ic_menu_crop);
        btnResize.setBackgroundColor(0xAA000000);
        FrameLayout.LayoutParams resizeParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(40));
        resizeParams.gravity = Gravity.TOP | Gravity.START;
        resizeParams.setMargins(8, 8, 8, 8);
        container.addView(btnResize, resizeParams);

        // Create status text
        TextView statusText = new TextView(this);
        statusText.setText(R.string.live_indicator);
        statusText.setTextColor(0xFFFF4444);
        statusText.setBackgroundColor(0xAA000000);
        statusText.setPadding(8, 4, 8, 4);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        statusParams.setMargins(0, 0, 0, 8);
        container.addView(statusText, statusParams);

        return container;
    }

    private void setupViewListeners() {
        // Close button
        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "🔴 Close button clicked");
            stopSelf();
        });

        // Resize button
        btnResize.setOnClickListener(v -> {
            Log.d(TAG, "📏 Resize button clicked");
            currentSize = (currentSize + 1) % 3;
            resizeWindow();
        });

        // Drag functionality with performClick for accessibility
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_UP:
                    // Perform click for accessibility
                    v.performClick();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });

        // TextureView listener
        if (textureView != null) {
            textureView.setSurfaceTextureListener(createTextureListener());
        }
    }

    private void setupXMLViewListeners() {
        textureView = floatingView.findViewById(R.id.texture_view_floating);
        btnClose = floatingView.findViewById(R.id.btn_close_floating);
        btnResize = floatingView.findViewById(R.id.btn_resize_floating);
        setupViewListeners();
    }

    private TextureView.SurfaceTextureListener createTextureListener() {
        return new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "📹 TextureView surface available: " + width + "x" + height);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "📹 TextureView size changed: " + width + "x" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(TAG, "📹 TextureView surface destroyed");
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // Called for each frame - don't log here
            }
        };
    }

    private void resizeWindow() {
        int[] sizes = {dpToPx(150), dpToPx(200), dpToPx(300)};
        int[] heights = {dpToPx(200), dpToPx(300), dpToPx(450)};

        params.width = sizes[currentSize];
        params.height = heights[currentSize];
        windowManager.updateViewLayout(floatingView, params);
        Log.d(TAG, "📏 Resized to: " + params.width + "x" + params.height);
    }

    private void openCamera() {
        Log.d(TAG, "📹 Opening camera...");

        if (cameraManager == null || textureView == null || !textureView.isAvailable()) {
            Log.w(TAG, "⚠️ Cannot open camera - not ready");
            return;
        }

        try {
            String cameraId = chooseCameraId();
            if (cameraId == null) {
                Log.e(TAG, "❌ No camera available");
                return;
            }

            Log.d(TAG, "📹 Opening camera ID: " + cameraId);
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "✅ Camera opened successfully");
                    cameraDevice = camera;
                    createCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "📹 Camera disconnected");
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "❌ Camera error: " + error);
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);

        } catch (SecurityException | CameraAccessException e) {
            Log.e(TAG, "❌ Error opening camera", e);
        }
    }

    private String chooseCameraId() throws CameraAccessException {
        String[] cameraIdList = cameraManager.getCameraIdList();
        if (cameraIdList.length == 0) return null;

        for (String id : cameraIdList) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == null) continue;
            if (isFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) return id;
            if (!isFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
        }

        return cameraIdList[0];
    }

    private void createCameraPreview() {
        try {
            if (cameraDevice == null || !textureView.isAvailable()) return;

            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;

            texture.setDefaultBufferSize(640, 480);
            Surface surface = new Surface(texture);

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (cameraDevice == null) return;

                        captureSession = session;
                        try {
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler);
                            Log.d(TAG, "✅ Camera preview started in floating window!");
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "❌ Failed to start preview", e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "❌ Failed to configure camera");
                    }
                }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "❌ Error creating preview", e);
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
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        Log.d(TAG, "✅ Background thread started");
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping thread", e);
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Floating Camera",
            NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows floating camera window");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, CameraActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Camera Active")
            .setContentText("Floating window should be visible")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🟡 onStartCommand called");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔴 Service destroyed");

        closeCamera();
        stopBackgroundThread();

        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                Log.d(TAG, "✅ Floating view removed");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error removing view", e);
            }
            floatingView = null;
        }
    }
}

