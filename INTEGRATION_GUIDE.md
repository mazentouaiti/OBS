# Quick Integration Guide - Streaming & Chat

## Step 1: Update MainMenuActivity
Add button to launch Streams Library from main menu:

```java
// In MainMenuActivity.java onCreate()
Button btnStreamsLibrary = findViewById(R.id.btn_streams_library);
btnStreamsLibrary.setOnClickListener(v -> {
    Intent intent = new Intent(MainMenuActivity.this, StreamsLibraryActivity.class);
    startActivity(intent);
});
```

Add to your menu layout XML:
```xml
<Button
    android:id="@+id/btn_streams_library"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Saved Streams Library"
    android:layout_margin="8dp" />
```

## Step 2: Initialize Camera Capture in StreamingActivity
When you capture camera frames, send them to StreamManager:

```java
// In your camera callback (Camera2/CameraX)
private void onFrameAvailable(byte[] frameData, long timestamp) {
    if (streamManager != null && streamManager.isStreaming()) {
        streamManager.sendVideoFrame(frameData, timestamp);
    }
}

// For audio data
private void onAudioData(byte[] audioData, long timestamp) {
    if (streamManager != null && streamManager.isStreaming()) {
        streamManager.sendAudioData(audioData, timestamp);
    }
}
```

## Step 3: Handle Runtime Permissions
Add permission request helper:

```java
private void requestStoragePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Android 6.0 - 10
        requestPermissions(
            new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            },
            PERMISSION_REQUEST_CODE
        );
    }
}
```

## Step 4: Configure RTMP Server Details
Update StreamingActivity with your server info:

```java
private void startStreaming() {
    String serverUrl = "rtmp://your-rtmp-server.com/live"; // Update this
    String streamKey = "your_stream_key_" + System.currentTimeMillis();
    boolean recordLocally = true;

    streamManager.startStreaming(serverUrl, streamKey, recordLocally);
    // ... rest of code
}
```

## Step 5: Handle Chat Integration
The ChatManager automatically handles chat events. Customize in StreamManager:

```java
// In StreamManager.startStreaming()
chatManager.setChatEventListener(new ChatManager.ChatEventListener() {
    @Override
    public void onNewMessage(ChatManager.ChatMessage message) {
        // Message already saved to database
        // Update UI if needed
        Log.d(TAG, "New message from " + message.username);
    }

    @Override
    public void onViewerCountChanged(int count) {
        currentViewerCount = count;
        if (statsListener != null) {
            statsListener.onViewerCountChanged(count);
        }
    }

    @Override
    public void onSystemMessage(String message) {
        Log.d(TAG, "System: " + message);
    }

    @Override
    public void onChatDisconnected() {
        Log.w(TAG, "Chat disconnected");
    }
});
```

## Step 6: Add Navigation to MainMenuActivity
Update your main menu to include streaming and library options:

```java
// In MainMenuActivity onCreate()
findViewById(R.id.btn_go_live).setOnClickListener(v -> {
    Intent intent = new Intent(MainMenuActivity.this, StreamingActivity.class);
    startActivity(intent);
});

findViewById(R.id.btn_streams_library).setOnClickListener(v -> {
    Intent intent = new Intent(MainMenuActivity.this, StreamsLibraryActivity.class);
    startActivity(intent);
});
```

## Step 7: Layout XML Updates
Update activity_main_menu.xml to include new buttons:

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- ... existing buttons ... -->

    <Button
        android:id="@+id/btn_go_live"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Go Live (Stream)"
        android:layout_margin="8dp"
        android:backgroundTint="#FF4444" />

    <Button
        android:id="@+id/btn_streams_library"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Saved Streams Library"
        android:layout_margin="8dp"
        android:backgroundTint="#4CAF50" />

</LinearLayout>
```

## Step 8: Update Existing StreamingActivity
Integrate chat message sending in StreamingActivity:

```java
private void sendChatMessage() {
    String message = etChatMessage.getText().toString().trim();
    if (!message.isEmpty()) {
        streamManager.sendChatMessage(message);
        etChatMessage.setText("");
        Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
    }
}
```

## Configuration Values

Edit in StreamManager.java constants if needed:

```java
// Video encoder
private static final int VIDEO_WIDTH = 1280;
private static final int VIDEO_HEIGHT = 720;
private static final int VIDEO_BITRATE = 2500 * 1000; // 2.5 Mbps
private static final int VIDEO_FPS = 30;

// Audio encoder
private static final int AUDIO_SAMPLE_RATE = 44100;
private static final int AUDIO_BITRATE = 128 * 1000; // 128 kbps
```

## Database Queries Examples

Get all streams:
```java
StreamDatabaseHelper dbHelper = new StreamDatabaseHelper(context);
List<StreamMetadata> streams = dbHelper.getAllStreams();
```

Delete old streams:
```java
dbHelper.deleteStream(streamId);
```

Get chat for specific stream:
```java
List<ChatManager.ChatMessage> messages = dbHelper.getChatMessages(streamId);
```

## Testing Local Recording

1. Start streaming from StreamingActivity
2. Check for created MP4 file in:
   ```
   Android/data/com.obs.mobile/files/streams/stream_YYYYMMDD_HHmmss.mp4
   ```
3. Verify database contains stream metadata:
   ```
   adb shell sqlite3 /data/data/com.obs.mobile/databases/streams.db
   ```
4. Navigate to Streams Library and verify stream appears
5. Click Play and verify video plays with chat replay

## Troubleshooting

### "Cannot resolve symbol" errors
- Clean project: `./gradlew clean`
- Rebuild: `./gradlew build`
- Invalidate Android Studio cache: File > Invalidate Caches > Invalidate and Restart

### LocalRecorder creates empty files
- Ensure video frames are actually being sent
- Check frame data size is > 0
- Verify timestamps are increasing

### Chat not saving
- Check ChatEventListener is properly set
- Ensure database is initialized
- Verify streamId is not null

### Playback fails
- Check file path is valid
- Ensure storage permissions granted
- Verify MP4 file is not corrupted

## Next Steps

1. Implement actual camera capture in StreamingActivity
2. Set up RTMP server connection details
3. Test with real camera input
4. Customize UI colors/themes
5. Add thumbnail generation
6. Implement stream statistics dashboard
7. Add search/filter to streams library

