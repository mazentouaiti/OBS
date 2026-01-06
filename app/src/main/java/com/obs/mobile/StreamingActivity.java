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
import com.obs.mobile.streaming.LocalRecorder;
import com.obs.mobile.streaming.StreamManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.obs.mobile.utils.StoragePermissionHelper;
import com.obs.mobile.utils.RecordingsManager;


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
    private RecordingsManager recordingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);
        // Initialize camera thread
        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        // Initialize recordings manager
        recordingsManager = new RecordingsManager(this);
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

        // Debug: Show the actual path
        String path = recordingsDir.getAbsolutePath();
        Log.d(TAG, "Recordings directory: " + path);

        // Check if directory exists
        if (!recordingsDir.exists()) {
            Toast.makeText(this, "No recordings directory exists yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // List files with more detailed logging
        java.io.File[] recordings = recordingsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".mp4") ||
                        name.toLowerCase().endsWith(".mkv") ||
                        name.toLowerCase().endsWith(".mov")
        );

        // Also check MediaStore for Android 10+
        List<File> allRecordings = new ArrayList<>();
        if (recordings != null) {
            for (java.io.File file : recordings) {
                allRecordings.add(file);
                Log.d(TAG, "Found file: " + file.getName() + " size: " + file.length());
            }
        }

        if (allRecordings.isEmpty()) {
            // Show dialog with path information
            showNoRecordingsDialog(path);
            return;
        }

        // Sort by last modified (newest first)
        Collections.sort(allRecordings, (f1, f2) ->
                Long.compare(f2.lastModified(), f1.lastModified()));

        // Create display names
        String[] fileNames = new String[allRecordings.size()];
        for (int i = 0; i < allRecordings.size(); i++) {
            java.io.File file = allRecordings.get(i);
            long size = file.length();
            String sizeStr = formatFileSize(size);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "MMM dd, yyyy HH:mm", java.util.Locale.US);
            String date = sdf.format(new java.util.Date(file.lastModified()));
            fileNames[i] = file.getName() + "\n" + sizeStr + " • " + date;
        }

        // Show dialog with recordings list
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📹 Enregistrements (" + allRecordings.size() + ")")
                .setItems(fileNames, (dialog, which) -> {
                    showFileOptionsDialog(allRecordings.get(which));
                })
                .setPositiveButton("Ouvrir Dossier", (dialog, which) -> {
                    openRecordingsInFileManager(recordingsDir);
                })
                .setNeutralButton("Copier Chemin", (dialog, which) -> {
                    copyToClipboard("Chemin des enregistrements", path);
                    Toast.makeText(this, "Chemin copié", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Fermer", null)
                .show();
    }
    private void showNoRecordingsDialog(String path) {
        String message = "Aucun enregistrement trouvé.\n\n" +
                "Chemin du dossier:\n" + path + "\n\n" +
                "Les enregistrements seront sauvegardés ici après le streaming.";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📁 Dossier d'Enregistrements")
                .setMessage(message)
                .setPositiveButton("Ouvrir Dossier", (dialog, which) -> {
                    openRecordingsInFileManager(new java.io.File(path));
                })
                .setNeutralButton("Copier Chemin", (dialog, which) -> {
                    copyToClipboard("Chemin du dossier", path);
                    Toast.makeText(this, "Chemin copié", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void openRecordingsInFileManager(java.io.File dir) {
        try {
            // Try to open with system file manager
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7+
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        dir
                );
                intent.setDataAndType(uri, "resource/folder");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // For older Android versions
                intent.setDataAndType(android.net.Uri.fromFile(dir), "resource/folder");
            }

            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            // Check if there's an app to handle this
            android.content.pm.PackageManager pm = getPackageManager();
            if (intent.resolveActivity(pm) != null) {
                startActivity(intent);
            } else {
                // Fallback: show path
                showPathDialog(dir.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file manager", e);
            showPathDialog(dir.getAbsolutePath());
        }
    }

    private void showPathDialog(String path) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📁 Emplacement des Fichiers")
                .setMessage("Dossier: " + path + "\n\n" +
                        "Utilisez un gestionnaire de fichiers pour accéder à ce dossier.\n" +
                        "Ou copiez le chemin et collez-le dans votre gestionnaire de fichiers.")
                .setPositiveButton("Copier Chemin", (dialog, which) -> {
                    copyToClipboard("Chemin du dossier", path);
                    Toast.makeText(this, "Chemin copié", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void copyToClipboard(String label, String text) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(java.util.Locale.US, "%.2f %s",
                bytes / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }
    private void showFileOptionsDialog(final java.io.File file) {
        String[] options = {"▶️ Play", "📤 Share", "🗑️ Delete", "📁 Show in Files"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Play
                            playRecordingFile(file);
                            break;
                        case 1: // Share
                            shareRecordingFile(file);
                            break;
                        case 2: // Delete
                            confirmDeleteRecording(file);
                            break;
                        case 3: // Show in Files
                            showInFileManager(file);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void playRecordingFile(java.io.File file) {
        if (recordingsManager.openWithVideoPlayer(file)) {
            Log.d(TAG, "Opened video player for: " + file.getName());
        } else {
            Toast.makeText(this, "Cannot play video. No video player found.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void shareRecordingFile(java.io.File file) {
        if (recordingsManager.shareFile(file)) {
            Log.d(TAG, "Sharing file: " + file.getName());
        } else {
            Toast.makeText(this, "Cannot share video", Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmDeleteRecording(final java.io.File file) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setMessage("Are you sure you want to delete:\n\n" + file.getName() + "\n\n" +
                        "Size: " + com.obs.mobile.streaming.StorageManager.formatFileSize(file.length()))
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (recordingsManager.deleteFile(file)) {
                        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
                        // Refresh the list
                        showRecordingsFolder();
                    } else {
                        Toast.makeText(this, "Failed to delete recording", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void testRecording() {
        // Create a test recorder
        String testPath = new File(getExternalFilesDir(null), "test_recording.mp4").getAbsolutePath();
        LocalRecorder testRecorder = new LocalRecorder(testPath, 1280, 720);

        // Start recording
        testRecorder.startRecording();

        // Get the surface
        Surface surface = testRecorder.getInputSurface();
        if (surface != null) {
            Log.d(TAG, "Test recorder surface is valid");
            Toast.makeText(this, "Test recorder started", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Test recorder surface is null!");
            Toast.makeText(this, "Test recorder failed - no surface", Toast.LENGTH_LONG).show();
        }

        // Stop after 3 seconds
        new Handler().postDelayed(() -> {
            testRecorder.stopRecording();
            long fileSize = testRecorder.getFileSize();

            String message = "Test recording complete. File size: " + fileSize + " bytes";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, message);
        }, 3000);
    }
    private void showInFileManager(java.io.File file) {
        if (!recordingsManager.openWithVideoPlayer(file)) {
            // Fallback: show path
            showFilePathDialog(file);
        }
    }
    private void showFilePathDialog(java.io.File file) {
        String path = file.getAbsolutePath();
        String size = com.obs.mobile.streaming.StorageManager.formatFileSize(file.length());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📹 " + file.getName())
                .setMessage("Size: " + size + "\n\nLocation:\n" + path +
                        "\n\nYou can access this file using any file manager app.")
                .setPositiveButton("Copy Path", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("File Path", path);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Path copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Try Play", (dialog, which) -> {
                    playRecordingFile(file);
                })
                .setNegativeButton("Close", null)
                .show();
    }
    private void openRecordingsInFileManager() {
        if (recordingsManager.openRecordingsFolder()) {
            Toast.makeText(this, "Opening recordings folder...", Toast.LENGTH_SHORT).show();
        } else {
            // Show path as fallback
            showFolderPathDialog();
        }
    }
    private void showFolderPathDialog() {
        java.io.File folder = streamManager.getStorageManager().getRecordingsDirectory();
        String path = folder.getAbsolutePath();
        String alternativePath = "Movies/OBS_Streams";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📁 Recordings Location")
                .setMessage("Your recordings are saved in:\n\n" + alternativePath +
                        "\n\nFull path:\n" + path +
                        "\n\nYou can access them using any file manager app or the Gallery/Photos app.")
                .setPositiveButton("Copy Path", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Recording Path", path);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Path copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Open Gallery", (dialog, which) -> {
                    openGalleryApp();
                })
                .setNegativeButton("Close", null)
                .show();
    }
    private void openGalleryApp() {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setType("video/*");
            intent.putExtra(android.content.Intent.EXTRA_LOCAL_ONLY, true);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open gallery", e);
            Toast.makeText(this, "Please open your Gallery or Photos app manually",
                    Toast.LENGTH_LONG).show();
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
