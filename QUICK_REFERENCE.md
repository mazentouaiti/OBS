# Quick Reference Guide - Streaming Implementation

## 🎬 What You Got

A complete streaming system with:
- ✅ **Live streaming** to RTMP servers
- ✅ **Local MP4 recording** (H.264 + AAC)
- ✅ **Live chat capture** (WebSocket-based)
- ✅ **Stream playback** with video player
- ✅ **Synchronized chat replay**
- ✅ **Stream library** to browse saved recordings
- ✅ **SQLite database** for persistence

---

## 🚀 Quick Start (5 Minutes)

### 1. Build the project
```bash
cd /home/mazen/StudioProjects/OBS
./gradlew clean build
```

### 2. Start a stream
```java
// In StreamingActivity
StreamManager streamManager = new StreamManager(this);
streamManager.startStreaming("rtmp://server/live", "key123", true);
```

### 3. Send video frames
```java
// In camera callback
streamManager.sendVideoFrame(frameData, timestamp);
streamManager.sendAudioData(audioData, timestamp);
```

### 4. Stop streaming
```java
streamManager.stopStreaming();
// MP4 file saved automatically
// Chat messages stored in database
```

### 5. View saved streams
```java
// Open from menu
Intent intent = new Intent(this, StreamsLibraryActivity.class);
startActivity(intent);
```

---

## 📁 File Locations

### Core Classes
```
app/src/main/java/com/obs/mobile/
├── streaming/ChatAdapter.java
├── streaming/LocalRecorder.java
├── streaming/StreamDatabaseHelper.java
├── streaming/StreamManager.java (ENHANCED)
├── streaming/StreamMetadata.java (UPDATED)
├── StreamsLibraryActivity.java
├── StreamLibraryAdapter.java
├── StreamPlaybackActivity.java
└── ChatReplayAdapter.java
```

### Layouts
```
app/src/main/res/layout/
├── activity_streams_library.xml
├── item_stream_library.xml
├── activity_stream_playback.xml
├── item_chat_message.xml
└── item_chat_replay.xml
```

---

## 🎯 Key Classes

### StreamManager
**Purpose:** Orchestrate streaming and recording
```java
new StreamManager(context)
  .startStreaming(serverUrl, streamKey, recordLocally)
  .sendVideoFrame(frameData, timestamp)
  .sendAudioData(audioData, timestamp)
  .sendChatMessage(message)
  .stopStreaming()
```

### LocalRecorder
**Purpose:** Encode and save video/audio to MP4
```java
new LocalRecorder(outputPath, width, height)
  .startRecording()
  .writeVideoFrame(frameData, timestamp)
  .writeAudioData(audioData, timestamp)
  .stopRecording()
```

### StreamDatabaseHelper
**Purpose:** Persist streams and chat to SQLite
```java
new StreamDatabaseHelper(context)
  .saveStream(metadata)
  .getChatMessages(streamId)
  .getAllStreams()
  .deleteStream(streamId)
```

### StreamsLibraryActivity
**Purpose:** Browse and manage saved streams
- Display all saved streams in a list
- Click to play any stream
- Click delete to remove stream

### StreamPlaybackActivity
**Purpose:** Play stream with synchronized chat
- VideoView for playback
- SeekBar for navigation
- Chat replay that syncs with video time

---

## ⚙️ Configuration

### Change Video Quality
**File:** `StreamManager.java`
```java
private static final int VIDEO_WIDTH = 1280;      // 1920 for 4K
private static final int VIDEO_HEIGHT = 720;      // 1080 for 4K
private static final int VIDEO_BITRATE = 2500 * 1000;  // Increase for quality
private static final int VIDEO_FPS = 30;          // 60 for smoother
```

### Change Audio Quality
**File:** `StreamManager.java`
```java
private static final int AUDIO_BITRATE = 128 * 1000;   // 256000 for HQ
private static final int AUDIO_SAMPLE_RATE = 44100;    // 48000 for pro audio
```

### Set RTMP Server
**File:** `StreamingActivity.java`
```java
String serverUrl = "rtmp://your-server.com/live";  // Change this
String streamKey = "your_stream_key_123";
```

---

## 📊 Data Flow

### Recording a Stream
```
StreamingActivity.startStreaming()
    ↓
StreamManager initializes
    ├→ Create StreamMetadata with unique ID
    ├→ Start LocalRecorder
    ├→ Set up ChatManager listener
    └→ Connect to RTMP server
    ↓
During streaming:
    ├→ sendVideoFrame() → LocalRecorder → MP4
    ├→ sendAudioData() → LocalRecorder → MP4
    └→ onNewMessage() → StreamDatabaseHelper → SQLite
    ↓
StreamingActivity.stopStreaming()
    ↓
StreamManager finalizes
    ├→ LocalRecorder.stopRecording()
    ├→ Save metadata to database
    ├→ Disconnect RTMP
    └→ Close ChatManager
```

### Playing Back a Stream
```
StreamsLibraryActivity displays streams
    ↓ (user clicks stream)
StreamPlaybackActivity opens
    ├→ Load MP4 file into VideoView
    ├→ Load chat from database
    ├→ Display timeline with seekBar
    ↓
User plays video
    ├→ VideoView plays MP4
    ├→ Chat replays synchronized
    └→ Seek updates chat display
```

---

## 🗄️ Database Queries

### Get All Streams
```java
StreamDatabaseHelper db = new StreamDatabaseHelper(context);
List<StreamMetadata> streams = db.getAllStreams();
```

### Get Chat for Stream
```java
List<ChatManager.ChatMessage> chat = db.getChatMessages(streamId);
for (ChatManager.ChatMessage msg : chat) {
    Log.d("Chat", msg.username + ": " + msg.message);
}
```

### Delete Stream
```java
boolean success = db.deleteStream(streamId);
// Also deletes all associated chat messages
```

### Save Stream Manually
```java
StreamMetadata metadata = new StreamMetadata();
metadata.streamTitle = "My Stream";
metadata.localRecordingPath = "/path/to/video.mp4";
metadata.startTime = System.currentTimeMillis();
db.saveStream(metadata);
```

---

## 🔐 Permissions (Already Added)

Required permissions are automatically added in AndroidManifest.xml:
```xml
<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Audio -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Storage -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
```

Request at runtime (Android 6+):
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    requestPermissions(
        new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        },
        PERMISSION_REQUEST_CODE
    );
}
```

---

## 📱 File Locations on Device

### Recorded Streams
```
Android/data/com.obs.mobile/files/streams/
├── stream_20260105_120000.mp4
├── stream_20260105_121530.mp4
└── stream_20260105_150000.mp4
```

### Database
```
data/data/com.obs.mobile/databases/
└── streams.db
```

### Check with ADB
```bash
# Find stream files
adb shell find /data/data/com.obs.mobile/files/streams -name "*.mp4"

# View database
adb shell sqlite3 /data/data/com.obs.mobile/databases/streams.db
  > SELECT * FROM streams;

# Check logs
adb logcat | grep -E "StreamManager|LocalRecorder|ChatManager"
```

---

## 🧪 Testing Checklist

### Basic Recording
- [ ] Click "Go Live"
- [ ] Verify MP4 file appears in `/files/streams/`
- [ ] Stream metadata saved in database
- [ ] Chat messages captured during streaming

### Playback
- [ ] Open Streams Library
- [ ] See list of saved streams
- [ ] Click stream to open playback
- [ ] Video plays correctly
- [ ] Chat appears alongside video
- [ ] Seek bar updates chat display

### Storage
- [ ] Check file sizes are reasonable (~1GB per hour)
- [ ] Delete stream removes both file and database entries
- [ ] No orphaned files left behind

### Permissions
- [ ] First run asks for camera permission
- [ ] First run asks for storage permission
- [ ] Decline permission gracefully
- [ ] Accept permission works correctly

---

## 🐛 Debugging Tips

### Enable Logging
```java
// In StreamManager
Log.d(TAG, "Starting stream: " + currentMetadata.streamTitle);

// In LocalRecorder
Log.d(TAG, "Video frame written: " + frameData.length + " bytes");

// In Database
Log.d(TAG, "Stream saved: " + metadata.streamId);
```

### Check File Integrity
```bash
# Verify MP4 file
ffprobe /path/to/stream.mp4

# Extract info
ffmpeg -i stream.mp4
```

### Monitor Memory
```bash
# Check recording thread
adb shell ps | grep StreamManager

# Monitor heap
Android Studio → Profiler → Memory
```

### Check Database
```bash
# Dump database
adb pull /data/data/com.obs.mobile/databases/streams.db
sqlite3 streams.db "SELECT * FROM streams LIMIT 1;"
sqlite3 streams.db "SELECT * FROM chat_messages LIMIT 5;"
```

---

## 🎯 Common Issues & Solutions

### Problem: No MP4 file created
**Solution:** 
- Check storage permissions granted
- Verify `getExternalFilesDir()` has write access
- Check disk space available

### Problem: Chat not saving
**Solution:**
- Verify ChatManager is connected
- Check database is initialized
- Ensure streamId is not null

### Problem: Playback fails
**Solution:**
- Check file path is correct
- Verify file is not corrupted
- Try with different stream
- Check VideoView initialization

### Problem: Crashes on startStreaming
**Solution:**
- Check all permissions granted
- Verify camera access
- Check RTMP server URL is valid
- Review logcat for specific error

---

## 📈 Performance Notes

### File Size Estimation
- **1 hour @ 2.5 Mbps video + 128 kbps audio:**
  - `(2500 + 128) × 3600 ÷ 8 = ~1.1 GB`

### Bitrate Reference
- **720p @ 30fps:** 2.5 Mbps good, 5 Mbps excellent
- **1080p @ 30fps:** 5 Mbps good, 10 Mbps excellent
- **Audio:** 128 kbps stereo sufficient, 256 kbps high quality

### Disk Space Required
- **1 hour @ default:** ~1.1 GB
- **Safe to record:** Check available > 2 GB
- **Recommended:** 10+ GB for multiple streams

---

## 🚀 Advanced Features

### Custom Frame Formats
The recorder accepts:
- Raw video bytes (NV21, I420, etc.)
- PCM audio bytes
- Video from Camera2 API
- Audio from AudioRecord API

### Statistics
Access streaming stats via:
```java
streamManager.setStatsListener(new StreamManager.StreamStatsListener() {
    @Override
    public void onFrameSent(int bytes) {
        Log.d(TAG, "Frame: " + bytes + " bytes");
    }
    
    @Override
    public void onBytesSent(long totalBytes) {
        Log.d(TAG, "Total: " + totalBytes + " bytes");
    }
    
    @Override
    public void onViewerCountChanged(int count) {
        Log.d(TAG, "Viewers: " + count);
    }
});
```

### Chat Events
Listen to chat events:
```java
chatManager.setChatEventListener(new ChatManager.ChatEventListener() {
    @Override
    public void onNewMessage(ChatManager.ChatMessage msg) { }
    
    @Override
    public void onViewerCountChanged(int count) { }
    
    @Override
    public void onSystemMessage(String msg) { }
    
    @Override
    public void onChatDisconnected() { }
});
```

---

## 📞 Support Resources

### Documentation Files
1. **STREAMING_IMPLEMENTATION.md** - Technical deep dive
2. **INTEGRATION_GUIDE.md** - Step-by-step setup
3. **STREAMING_SUMMARY.md** - Feature overview
4. **IMPLEMENTATION_CHECKLIST.md** - Verification checklist

### Code Examples
All main classes have well-commented code showing usage patterns.

### Logcat Output
Enable logging to debug:
```bash
adb logcat | grep -E "Streaming|LocalRecorder|StreamManager|ChatManager"
```

---

## ✅ You're Ready!

Everything is implemented and ready to use. Start by:

1. **Build:** `./gradlew build`
2. **Test:** Run on device
3. **Integrate:** Follow INTEGRATION_GUIDE.md
4. **Customize:** Adjust configuration as needed
5. **Deploy:** Ship your streaming app!

Good luck! 🚀

