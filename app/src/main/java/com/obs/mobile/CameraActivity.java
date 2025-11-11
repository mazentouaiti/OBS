package com.obs.mobile;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Rational;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Collections;

/**
 * CameraActivity - Camera Preview and Recording Screen (no sensors)
 *
 * Uses TextureView for camera preview with Camera2 API
 * Supports PiP mode and floating window
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TextureView textureView;
    private TextView tvStatus;
    private Button btnRecord;
    private Button btnSwitchCamera;
    private Button btnFloating;  // Removed btnPip
    private View recordingIndicator;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private boolean isFrontCamera = false;
    private boolean isRecording = false;

    // ActivityResultLauncher for overlay permission
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_camera);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.camera_title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            textureView = findViewById(R.id.texture_view);
            tvStatus = findViewById(R.id.tv_status);
            btnRecord = findViewById(R.id.btn_record);
            btnSwitchCamera = findViewById(R.id.btn_switch_camera);
            btnFloating = findViewById(R.id.btn_floating);
            recordingIndicator = findViewById(R.id.recording_indicator);

            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            btnRecord.setOnClickListener(v -> toggleRecording());
            btnSwitchCamera.setOnClickListener(v -> switchCamera());

            // Only set listeners if buttons exist in layout
            if (btnFloating != null) {
                btnFloating.setOnClickListener(v -> startFloatingCamera());
            }

            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "TextureView available: " + width + "x" + height);
                    if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        openCamera();
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "TextureView size changed: " + width + "x" + height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    Log.d(TAG, "TextureView destroyed");
                    closeCamera();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    // Called for each frame - don't log here
                }
            });

            // Register ActivityResultLauncher for overlay permission
            overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Handle the result of the overlay permission request
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Permission granted! Try again", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

            // Set up modern PiP mode change listener
            addOnPictureInPictureModeChangedListener(info -> {
                boolean isInPictureInPictureMode = info.isInPictureInPictureMode();
                Log.d(TAG, "onPictureInPictureModeChanged: " + isInPictureInPictureMode);

                if (isInPictureInPictureMode) {
                    // Hide UI controls in PiP mode
                    if (btnRecord != null) btnRecord.setVisibility(View.GONE);
                    if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.GONE);
                    if (btnFloating != null) btnFloating.setVisibility(View.GONE);
                    if (tvStatus != null) tvStatus.setVisibility(View.GONE);
                } else {
                    // Show UI controls when exiting PiP
                    if (btnRecord != null) btnRecord.setVisibility(View.VISIBLE);
                    if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.VISIBLE);
                    if (btnFloating != null) btnFloating.setVisibility(View.VISIBLE);
                }
            });

            checkCameraPermission();
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: ", e);
            Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            tvStatus.setText(R.string.permission_already_granted);
            startBackgroundThread();
            if (textureView.isAvailable()) {
                openCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText(R.string.camera_permission_granted);
                startBackgroundThread();
                if (textureView.isAvailable()) {
                    openCamera();
                }
            } else {
                tvStatus.setText(R.string.camera_permission_missing);
                Toast.makeText(this, R.string.camera_permission_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera opened successfully");
            cameraDevice = camera;
            runOnUiThread(() -> tvStatus.setText(R.string.camera_opened));
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera disconnected");
            camera.close();
            cameraDevice = null;
            runOnUiThread(() -> tvStatus.setText(R.string.camera_disconnected));
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            camera.close();
            cameraDevice = null;
            runOnUiThread(() -> {
                tvStatus.setText(getString(R.string.camera_error, error));
                Toast.makeText(CameraActivity.this, "Camera error: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    };

    private void openCamera() {
        if (cameraManager == null) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        if (cameraManager == null) {
            Log.e(TAG, "openCamera: CameraManager is null");
            runOnUiThread(() -> Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show());
            return;
        }

        try {
            String cameraId = chooseCameraId();
            if (cameraId == null) {
                Log.e(TAG, "openCamera: no camera available");
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "openCamera: camera permission not granted");
                return;
            }

            Log.d(TAG, "Opening camera: " + cameraId);
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "openCamera error: ", e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to open camera", Toast.LENGTH_LONG).show());
        }
    }

    private String chooseCameraId() throws CameraAccessException {
        if (cameraManager == null) return null;

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

    private void createCameraPreviewSession() {
        try {
            if (cameraDevice == null || !textureView.isAvailable()) {
                Log.w(TAG, "Cannot create preview session - camera or texture not ready");
                return;
            }

            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                Log.w(TAG, "SurfaceTexture is null");
                return;
            }

            // Set buffer size to 1920x1080
            texture.setDefaultBufferSize(1920, 1080);

            Surface surface = new Surface(texture);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (cameraDevice == null) return;

                        captureSession = session;
                        try {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON);
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO);

                            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                            Log.d(TAG, "✅ Preview started - YOU SHOULD SEE THE CAMERA NOW!");

                            runOnUiThread(() -> {
                                tvStatus.setVisibility(View.GONE);
                                Toast.makeText(CameraActivity.this,
                                    "✅ Camera preview is ACTIVE!", Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to start preview", e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Failed to configure camera session");
                    }
                }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "createCameraPreviewSession error: ", e);
        }
    }

    private void closeCamera() {
        Log.d(TAG, "Closing camera");
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "closeCamera error: ", e);
        }
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.w(TAG, "stopBackgroundThread interrupted", e);
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            btnRecord.setText(R.string.btn_stop_record);
            recordingIndicator.setVisibility(View.VISIBLE);
        } else {
            btnRecord.setText(R.string.btn_record);
            recordingIndicator.setVisibility(View.GONE);
        }
        Toast.makeText(this, isRecording ? "Recording..." : "Stopped", Toast.LENGTH_SHORT).show();
    }

    private void switchCamera() {
        isFrontCamera = !isFrontCamera;
        closeCamera();
        if (textureView != null) {
            textureView.postDelayed(this::openCamera, 300);
        }
    }

    /**
     * Enter Picture-in-Picture mode
     */
    private void enterPipMode() {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();

        // Set aspect ratio (16:9 for camera)
        builder.setAspectRatio(new Rational(16, 9));

        try {
            enterPictureInPictureMode(builder.build());
            Toast.makeText(this, "Entering PiP mode", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot enter PiP", e);
            Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start floating camera window
     */
    private void startFloatingCamera() {
        Log.d(TAG, "🟡 Starting floating camera...");

        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "⚠️ Overlay permission not granted");
            // Request overlay permission
            new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Floating camera needs permission to display over other apps")
                .setPositiveButton("Grant", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    // Use ActivityResultLauncher to request permission
                    overlayPermissionLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ Camera permission not granted");
            Toast.makeText(this, "Camera permission required for floating camera", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Start floating camera service
            Intent serviceIntent = new Intent(this, FloatingCameraService.class);
            startForegroundService(serviceIntent);

            Log.d(TAG, "✅ Floating camera service started");
            Toast.makeText(this, "Floating camera started", Toast.LENGTH_SHORT).show();

            // Optionally minimize the app
            moveTaskToBack(true);

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start floating camera", e);
            Toast.makeText(this, "Failed to start floating camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // onActivityResult is no longer used for overlay permission result
    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable() && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
