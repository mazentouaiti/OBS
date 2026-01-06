package com.obs.mobile.streaming;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * StreamManager - Handles RTMP streaming and local recording simultaneously
 * Uses Camera2 API for video and MediaCodec for encoding
 */
public class StreamManager {

    private static final String TAG = "StreamManager";

    // Streaming states
    public enum StreamState {
        IDLE, CONNECTING, STREAMING, ERROR, STOPPED
    }

    // Configuration
    @SuppressWarnings("unused")
    private static final int VIDEO_WIDTH = 1280;
    @SuppressWarnings("unused")
    private static final int VIDEO_HEIGHT = 720;
    @SuppressWarnings("unused")
    private static final int VIDEO_BITRATE = 2500 * 1000; // 2.5 Mbps
    @SuppressWarnings("unused")
    private static final int VIDEO_FPS = 30;
    @SuppressWarnings("unused")
    private static final int AUDIO_SAMPLE_RATE = 44100;
    @SuppressWarnings("unused")
    private static final int AUDIO_BITRATE = 128 * 1000; // 128 kbps

    // Components
    private final Context context;
    private final RTMPClient rtmpClient;
    private final ChatManager chatManager;
    private final StreamDatabaseHelper dbHelper;
    private final StorageManager storageManager;
    private LocalRecorder localRecorder;

    // Threading
    private Handler streamHandler;

    // State
    private StreamState currentState = StreamState.IDLE;
    private boolean isStreaming = false;
    private boolean isRecording = false;
    private StreamMetadata currentMetadata;
    private int currentViewerCount = 0;

    // Callbacks
    private StreamStateListener stateListener;
    private StreamStatsListener statsListener;
    private ChatMessageListener chatMessageListener;

    public StreamManager(Context context) {
        this.context = context;
        this.rtmpClient = new RTMPClient();
        this.chatManager = new ChatManager(context);
        this.dbHelper = new StreamDatabaseHelper(context);
        this.storageManager = new StorageManager(context);

        initialize();
    }

    private void initialize() {
        HandlerThread streamThread = new HandlerThread("StreamManager");
        streamThread.start();
        streamHandler = new Handler(streamThread.getLooper());
    }

    /**
     * Start streaming to RTMP server
     */
    @SuppressWarnings("unused")
    public void startStreaming(String serverUrl, String streamKey, boolean recordLocally) {
        if (isStreaming) {
            Log.w(TAG, "Already streaming");
            return;
        }

        streamHandler.post(() -> {
            try {
                setState(StreamState.CONNECTING);

                // Create metadata
                currentMetadata = new StreamMetadata();
                currentMetadata.startTime = System.currentTimeMillis();
                currentMetadata.streamTitle = generateStreamTitle();
                currentMetadata.rtmpUrl = serverUrl;
                currentMetadata.streamKey = streamKey;

                // Connect to RTMP server
                String fullUrl = serverUrl + "/" + streamKey;
                boolean rtmpConnected = false;
                try {
                    rtmpConnected = rtmpClient.connect(fullUrl);
                    if (!rtmpConnected) {
                        Log.w(TAG, "Failed to connect to RTMP server, continuing with local recording");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "RTMP connection error: " + e.getMessage());
                    // Continue without RTMP connection - can still do local recording
                }

                // Initialize local recorder if enabled
                if (recordLocally) {
                    String recordingPath = storageManager.generateRecordingPath();
                    localRecorder = new LocalRecorder(recordingPath, VIDEO_WIDTH, VIDEO_HEIGHT);
                    localRecorder.startRecording();
                    currentMetadata.localRecordingPath = recordingPath;
                    isRecording = true;
                    Log.i(TAG, "Local recording started: " + recordingPath);
                }

                // Set streaming state to STREAMING (before connecting to RTMP)
                setState(StreamState.STREAMING);
                isStreaming = true;

                // Set up chat listener to save messages
                chatManager.setChatEventListener(new ChatManager.ChatEventListener() {
                    @Override
                    public void onNewMessage(ChatManager.ChatMessage message) {
                        // Save chat message to database
                        dbHelper.saveChatMessage(currentMetadata.streamId, message);

                        // Notify chat message listener
                        if (chatMessageListener != null) {
                            chatMessageListener.onNewMessage(message);
                        }
                    }

                    @Override
                    public void onViewerCountChanged(int count) {
                        currentViewerCount = count;
                        if (currentMetadata != null && count > currentMetadata.maxViewers) {
                            currentMetadata.maxViewers = count;
                        }
                        if (statsListener != null) {
                            statsListener.onViewerCountChanged(count);
                        }
                    }

                    @Override
                    public void onSystemMessage(String message) {
                        Log.d(TAG, "System message: " + message);
                    }

                    @Override
                    public void onChatDisconnected() {
                        Log.w(TAG, "Chat disconnected");
                    }
                });

                // Connect chat
                chatManager.connect(currentMetadata.streamId);


                Log.i(TAG, "Streaming started successfully");

            } catch (Exception e) {
                Log.e(TAG, "Failed to start streaming", e);
                setState(StreamState.ERROR);
                stopStreaming();
            }
        });
    }

    /**
     * Stop streaming
     */
    public void stopStreaming() {
        if (!isStreaming) return;

        streamHandler.post(() -> {
            try {
                setState(StreamState.STOPPED);

                // Stop local recording
                if (isRecording && localRecorder != null) {
                    localRecorder.stopRecording();
                    isRecording = false;

                    // Update metadata
                    currentMetadata.endTime = System.currentTimeMillis();
                    currentMetadata.duration = currentMetadata.endTime - currentMetadata.startTime;
                    currentMetadata.fileSize = localRecorder.getFileSize();
                    currentMetadata.chatMessageCount = dbHelper.getChatMessageCount(currentMetadata.streamId);

                    // Save metadata to database
                    saveStreamMetadata(currentMetadata);
                }

                // Disconnect from RTMP
                rtmpClient.disconnect();


                // Disconnect chat
                chatManager.disconnect();

                isStreaming = false;
                Log.i(TAG, "Streaming stopped");

            } catch (Exception e) {
                Log.e(TAG, "Error stopping stream", e);
            }
        });
    }

    /**
     * Get input surface for camera preview
     */


    /**
     * Send video frame to encoder
     */
    @SuppressWarnings("unused")
    public void sendVideoFrame(byte[] frameData, long timestamp) {
        if (!isStreaming || localRecorder == null) return;

        streamHandler.post(() -> {
            // Send to local recorder
            if (isRecording) {
                localRecorder.writeVideoFrame(frameData, timestamp);
            }

            // Update stats
            if (statsListener != null) {
                statsListener.onFrameSent(frameData.length);
            }
        });
    }

    /**
     * Send audio data to encoder
     */
    @SuppressWarnings("unused")
    public void sendAudioData(byte[] audioData, long timestamp) {
        if (!isStreaming || localRecorder == null) return;

        streamHandler.post(() -> {
            // Send to local recorder
            if (isRecording) {
                localRecorder.writeAudioData(audioData, timestamp);
            }
        });
    }

    public Surface getInputSurface() {
        // This should return the encoder input surface
        // You need to implement this based on your MediaCodec encoder setup
        if (localRecorder != null && isStreaming) {
            // Return the encoder surface from LocalRecorder
            // You'll need to modify LocalRecorder to expose its input surface
            return null; // TODO: Implement this
        }
        return null;
    }

    /**
     * Send chat message
     */
    public void sendChatMessage(String message) {

        chatManager.sendMessage(message);
    }

    /**
     * Get chat messages
     */
    public ConcurrentLinkedQueue<ChatManager.ChatMessage> getChatMessages() {
        return chatManager.getMessages();
    }

    /**
     * Generate unique stream title
     */
    private String generateStreamTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return "Stream " + sdf.format(new Date());
    }

    /**
     * Get recording file path
     */
    @SuppressWarnings("unused")
    private String getRecordingFilePath() {
        File streamsDir = new File(context.getExternalFilesDir(null), "streams");
        if (!streamsDir.exists()) {
            if (!streamsDir.mkdirs()) {
                Log.w(TAG, "Failed to create streams directory");
            }
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        String filename = "stream_" + timestamp + ".mp4";

        return new File(streamsDir, filename).getAbsolutePath();
    }

    /**
     * Save stream metadata to database
     */
    private void saveStreamMetadata(StreamMetadata metadata) {
        try {
            dbHelper.saveStream(metadata);
            Log.d(TAG, "Stream metadata saved: " + metadata.streamTitle);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save stream metadata", e);
        }
    }

    private void setState(StreamState newState) {
        this.currentState = newState;
        if (stateListener != null) {
            stateListener.onStateChanged(newState);
        }
    }

    // Getters
    @SuppressWarnings("unused")
    public StreamState getCurrentState() { return currentState; }

    @SuppressWarnings("unused")
    public boolean isStreaming() { return isStreaming; }

    @SuppressWarnings("unused")
    public StreamMetadata getCurrentMetadata() { return currentMetadata; }

    @SuppressWarnings("unused")
    public StorageManager getStorageManager() { return storageManager; }

    // Set listeners
    public void setStateListener(StreamStateListener listener) {
        this.stateListener = listener;
    }

    public void setStatsListener(StreamStatsListener listener) {
        this.statsListener = listener;
    }

    public void setChatMessageListener(ChatMessageListener listener) {
        this.chatMessageListener = listener;
    }

    // Interfaces
    public interface StreamStateListener {
        void onStateChanged(StreamState state);
    }

    public interface StreamStatsListener {
        void onFrameSent(int bytes);

        @SuppressWarnings("unused")
        void onBytesSent(long totalBytes);

        @SuppressWarnings("unused")
        void onViewerCountChanged(int count);
    }

    public interface ChatMessageListener {
        void onNewMessage(ChatManager.ChatMessage message);
    }
}