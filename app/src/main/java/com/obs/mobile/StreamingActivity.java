package com.obs.mobile;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.obs.mobile.streaming.ChatAdapter;
import com.obs.mobile.streaming.StreamManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.jspecify.annotations.NonNull;
import java.util.Collections;
import java.util.Locale;
import com.obs.mobile.utils.StoragePermissionHelper;

/**
 * StreamingActivity - Main screen for live streaming with chat and camera feed in background
 * The camera feed is displayed in the TextureView with the chat overlay on top
 */
public class StreamingActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "StreamingActivity";
    private StreamManager streamManager;
    private ChatAdapter chatAdapter;
    // Camera components
    private TextureView textureView;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Handler cameraHandler;
    private HandlerThread cameraThread;
    private String currentCameraId = null;
    private boolean isFrontCamera = false;
    // UI Components
    private Button btnStartStream, btnStopStream;
    private EditText etChatMessage;
    private RecyclerView rvChat;
    private LinearLayout chatOverlay;
    private TextView tvViewerCount, tvStreamStats, tvStorageInfo;
    private boolean isChatVisible = true;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        // Initialize camera thread
        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        // Get camera manager
        cameraManager = getSystemService(CameraManager.class);
        initializeViews();
        initializeStreamManager();
        setupChatRecyclerView();
        setupChatInput();
        // Setup camera preview with TextureView callback
        textureView.setSurfaceTextureListener(this);
        checkAndRequestStoragePermissions();
    }
    private void initializeViews() {
        textureView = findViewById(R.id.texture_view_stream);
        btnStartStream = findViewById(R.id.btn_start_stream);
        btnStopStream = findViewById(R.id.btn_stop_stream);
        Button btnToggleChat = findViewById(R.id.btn_toggle_chat);
        Button btnToggleCamera = findViewById(R.id.btn_toggle_camera);
        etChatMessage = findViewById(R.id.et_chat_message);
        rvChat = findViewById(R.id.rv_chat);
        chatOverlay = findViewById(R.id.chat_overlay);
        tvViewerCount = findViewById(R.id.tv_viewer_count);
        tvStreamStats = findViewById(R.id.tv_stream_stats);
        tvStorageInfo = findViewById(R.id.tv_storage_info);

        Button btnRecordings = findViewById(R.id.btn_recordings);
        btnRecordings.setOnClickListener(v -> showRecordingsFolder());
        // Button listeners
        btnStartStream.setOnClickListener(v -> startStreaming());
        btnStopStream.setOnClickListener(v -> stopStreaming());
        btnToggleChat.setOnClickListener(v -> toggleChatVisibility());
        btnToggleCamera.setOnClickListener(v -> switchCamera());
    }
    // Check permissions
    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    @SuppressLint("MissingPermission")
    private void initializeCamera() {
        try {
            if (currentCameraId == null) {
                currentCameraId = getBackCameraId();
                isFrontCamera = false;
            }
            cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview();
                }
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }
                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    Log.e(TAG, "Camera error: " + error);
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error", e);
        }
    }
    @SuppressLint("MissingPermission")
    private void createCameraPreview() {
        if (textureView.getSurfaceTexture() == null) {
            return;
        }
        Surface surface = new Surface(textureView.getSurfaceTexture());
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            updatePreview();
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure camera session");
                        }
                    }, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating preview", e);
        }
    }
    private void updatePreview() {
        if (cameraDevice == null || captureSession == null) {
            return;
        }
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(new Surface(textureView.getSurfaceTexture()));
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            captureSession.setRepeatingRequest(builder.build(), null, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error updating preview", e);
        }
    }
    @NonNull
    private String getBackCameraId() throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return cameraIds[0]; // Fallback to first camera
    }
    @NonNull
    private String getFrontCameraId() throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }
        return cameraIds[0]; // Fallback to first camera
    }
    private void stopCamera() {
        if (captureSession != null) {
            try {
                captureSession.stopRepeating();
                captureSession.close();
            } catch (CameraAccessException e) {
                Log.e(TAG, "Error stopping camera session", e);
            }
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    // TextureView.SurfaceTextureListener callbacks - Camera feed management
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        if (checkPermissions()) {
            initializeCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        stopCamera();
        return true;
    }
    public void onSurfaceTextureFrameAvailable(@NonNull SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
    }
    private void initializeStreamManager() {
        streamManager = new StreamManager(this);

        streamManager.setStateListener(state -> updateStreamState(state));

        // Add chat message listener to refresh adapter
        streamManager.setChatMessageListener(message -> {
            if (chatAdapter != null) {
                runOnUiThread(() -> {
                    chatAdapter.updateMessages(streamManager.getChatMessages());
                    if (rvChat != null && chatAdapter.getItemCount() > 0) {
                        rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                });
            }
        });

        streamManager.setStatsListener(new StreamManager.StreamStatsListener() {
            @Override
            public void onFrameSent(int bytes) {
                // Update stats UI
            }
            @Override
            public void onBytesSent(long totalBytes) {
                runOnUiThread(() -> {
                    String stats = String.format(Locale.US, "Sent: %d KB", totalBytes / 1024);
                    tvStreamStats.setText(stats);
                });
            }
            @Override
            public void onViewerCountChanged(int count) {
                runOnUiThread(() ->
                    tvViewerCount.setText(String.format(Locale.US, "%d", count))
                );
            }
        });
    }
    private void setupChatRecyclerView() {
        chatAdapter = new ChatAdapter(streamManager.getChatMessages());
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        // Auto-scroll to new messages
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                rvChat.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        });
    }
    private void setupChatInput() {
        // Send chat message when button is clicked
        findViewById(R.id.btn_send_chat).setOnClickListener(v -> sendChatMessage());
        // Also send chat message when Enter key is pressed
        etChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendChatMessage();
            return true;
        });
    }
    private void sendChatMessage() {
        String message = etChatMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            try {
                // Send message via StreamManager
                streamManager.sendChatMessage(message);
                // Clear input field first
                etChatMessage.setText("");
                // Update chat adapter with new message
                if (chatAdapter != null) {
                    chatAdapter.updateMessages(streamManager.getChatMessages());
                    // Scroll to show new message
                    if (rvChat != null && chatAdapter.getItemCount() > 0) {
                        rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }
                // Focus back on input field
                etChatMessage.requestFocus();
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startStreaming() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return;
        }
        // Check storage permissions before starting recording
        if (!StoragePermissionHelper.hasStoragePermissions(this)) {
            Toast.makeText(this,
                    "Storage permission required to save recordings",
                    Toast.LENGTH_LONG).show();
            StoragePermissionHelper.requestStoragePermissions(this);
            return;
        }
        // Get streaming settings
        String serverUrl = "rtmp://your-server.com/live"; // Change to your server
        String streamKey = "stream_" + System.currentTimeMillis();
        boolean recordLocally = true;
        // Start streaming
        streamManager.startStreaming(serverUrl, streamKey, recordLocally);
        // Update UI
        btnStartStream.setEnabled(false);
        btnStopStream.setEnabled(true);
        tvStreamStats.setText("🔴 LIVE");
        Toast.makeText(this, "Streaming started", Toast.LENGTH_SHORT).show();
    }
    private void stopStreaming() {
        // Stop streaming
        streamManager.stopStreaming();
        // Update UI
        btnStartStream.setEnabled(true);
        btnStopStream.setEnabled(false);
        tvStreamStats.setText("OFFLINE");
        Toast.makeText(this, "Streaming stopped", Toast.LENGTH_SHORT).show();
    }
    private void toggleChatVisibility() {
        isChatVisible = !isChatVisible;
        chatOverlay.setVisibility(isChatVisible ? View.VISIBLE : View.GONE);
    }
    private void switchCamera() {
        stopCamera();
        try {
            if (isFrontCamera) {
                currentCameraId = getBackCameraId();
                isFrontCamera = false;
                Toast.makeText(this, "Switched to back camera", Toast.LENGTH_SHORT).show();
            } else {
                currentCameraId = getFrontCameraId();
                isFrontCamera = true;
                Toast.makeText(this, "Switched to front camera", Toast.LENGTH_SHORT).show();
            }
            initializeCamera();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error switching camera", e);
            Toast.makeText(this, "Failed to switch camera", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateStreamState(StreamManager.StreamState state) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTING:
                    tvStreamStats.setText("Connecting...");
                    break;
                case STREAMING:
                    tvStreamStats.setText("Live");
                    break;
                case ERROR:
                    tvStreamStats.setText("Error - check connection");
                    break;
                case STOPPED:
                    tvStreamStats.setText("Stream ended");
                    break;
                case IDLE:
                    tvStreamStats.setText("Offline");
                    break;
            }
            // Update storage info
            updateStorageInfo();
        });
    }

    private void updateStorageInfo() {
        if (streamManager != null) {
            long storageUsed = streamManager.getStorageManager().getStorageUsed();
            String storageText = String.format("💾 %s",
                    com.obs.mobile.streaming.StorageManager.formatFileSize(storageUsed));
            tvStorageInfo.setText(storageText);
        }
    }

    private void showRecordingsFolder() {
        if (streamManager == null) {
            Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
            return;
        }

        java.io.File recordingsDir = streamManager.getStorageManager().getRecordingsDirectory();
        java.io.File[] recordings = recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));

        if (recordings == null || recordings.length == 0) {
            Toast.makeText(this, "No recordings yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show list of recordings
        String[] fileNames = new String[recordings.length];
        for (int i = 0; i < recordings.length; i++) {
            long size = recordings[i].length();
            String sizeStr = com.obs.mobile.streaming.StorageManager.formatFileSize(size);
            fileNames[i] = recordings[i].getName() + " (" + sizeStr + ")";
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("📁 Recorded Files (" + recordings.length + ")")
                .setItems(fileNames, (dialog, which) -> {
                    // File selected
                    Toast.makeText(this, "File: " + recordings[which].getName(),
                            Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Open Folder", (dialog, which) -> {
                    // Open file manager
                    openFileManager(recordingsDir);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openFileManager(java.io.File folder) {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW);
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    folder
            );
            intent.setData(fileUri);
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file manager", e);
            Toast.makeText(this, "Cannot open file manager", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
        if (streamManager != null && streamManager.isStreaming()) {
            streamManager.stopStreaming();
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
        }
    }


    private void checkAndRequestStoragePermissions() {
        if (!StoragePermissionHelper.hasStoragePermissions(this)) {
            // Show rationale first
            StoragePermissionHelper.showPermissionRationale(this,
                    new StoragePermissionHelper.PermissionCallback() {
                        @Override
                        public void onPermissionGranted() {
                            StoragePermissionHelper.requestStoragePermissions(StreamingActivity.this);
                        }

                        @Override
                        public void onPermissionDenied() {
                            Toast.makeText(StreamingActivity.this,
                                    "Storage permission is required to save recordings",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Permissions already granted
            Log.d(TAG, "Storage permissions already granted");
            StoragePermissionHelper.logPermissionStatus(this);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle camera and audio permissions (existing code)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Permissions granted, initialize camera
                if (textureView.isAvailable()) {
                    initializeCamera();
                }
            } else {
                Toast.makeText(this, "Permissions denied. Camera cannot start.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Handle storage permissions
        StoragePermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults,
                new StoragePermissionHelper.PermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        Toast.makeText(StreamingActivity.this,
                                "Storage permission granted! Recordings will be saved.",
                                Toast.LENGTH_SHORT).show();
                        StoragePermissionHelper.logPermissionStatus(StreamingActivity.this);
                    }

                    @Override
                    public void onPermissionDenied() {
                        Toast.makeText(StreamingActivity.this,
                                "Storage permission denied. Recordings cannot be saved.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
