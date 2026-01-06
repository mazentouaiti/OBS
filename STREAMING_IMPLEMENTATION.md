# Streaming View with Live Chat & Local Recording Implementation Guide

## Overview
This document describes the complete streaming system implementation with live chat support and local stream recording functionality.

## What Was Implemented

### 1. **ChatAdapter** (`ChatAdapter.java`)
- RecyclerView adapter for displaying live chat messages
- Supports different user types: Regular, Moderator, Broadcaster, System
- Color-coded usernames based on user role
- Displays timestamps and user badges
- Layout: `item_chat_message.xml`

### 2. **Local Recording System**

#### LocalRecorder (`LocalRecorder.java`)
- Records video/audio streams to MP4 files
- Uses MediaCodec for H.264 video encoding (2.5 Mbps)
- Uses MediaCodec for AAC audio encoding (128 kbps, 44.1 kHz)
- Features:
  - Automatic directory creation
  - MediaMuxer for MP4 container
  - Frame and audio data buffering
  - Proper encoder lifecycle management
  - File size tracking

#### Enhanced StreamManager
- Integrated LocalRecorder initialization
- Records chat messages to database during streaming
- Saves stream metadata after session ends
- Methods:
  - `startStreaming()` - Initialize recording and chat listener
  - `stopStreaming()` - Finalize recording and save metadata
  - `sendVideoFrame()` - Write video frames to recorder
  - `sendAudioData()` - Write audio data to recorder

### 3. **Database Layer** (`StreamDatabaseHelper.java`)

#### Stream CRUD Operations
- `saveStream()` - Save stream metadata
- `getStream(streamId)` - Retrieve single stream
- `getAllStreams()` - Get all streams (sorted by date, newest first)
- `updateStream()` - Update stream information
- `deleteStream()` - Delete stream and associated chat

#### Chat Message CRUD Operations
- `saveChatMessage()` - Store chat message with stream reference
- `getChatMessages(streamId)` - Retrieve all messages for a stream
- `deleteChatMessages()` - Remove chat for a stream
- `getChatMessageCount()` - Get message count

#### Database Schema
**Streams Table:**
- stream_id, title, description
- start_time, end_time, duration
- file_path, file_size, thumbnail_path
- max_viewers, chat_count
- rtmp_url, stream_key, tags

**Chat Messages Table:**
- message_id, stream_id (FK), user_id
- username, message, timestamp
- message_type (USER, MODERATOR, BROADCASTER, SYSTEM)

### 4. **Streams Library Feature**

#### StreamsLibraryActivity
- Displays list of all saved streams
- Shows stream metadata (title, duration, date, size, viewers)
- Provides refresh functionality
- Click listeners for playback and deletion

#### StreamLibraryAdapter
- RecyclerView adapter for stream list
- Displays formatted information:
  - Stream title
  - Duration (HH:MM:SS format)
  - Date and time
  - File size in MB
  - Max viewer count
  - Chat message count
- Action buttons: Play, Delete
- Uses CardView for visual separation

#### Layout: `activity_streams_library.xml` & `item_stream_library.xml`

### 5. **Stream Playback with Chat Replay**

#### StreamPlaybackActivity
- Video player using VideoView
- Synchronized chat replay based on playback time
- Playback controls: Play/Pause, Fullscreen
- Current time display (MM:SS or HH:MM:SS)
- Duration tracking
- SeekBar for navigation

#### ChatReplayAdapter
- Displays chat messages from recording
- Time-synchronized with video playback
- Shows relative timestamp from stream start
- Color-coded user roles
- Auto-scrolls to latest messages

#### Layout: `activity_stream_playback.xml` & `item_chat_replay.xml`

### 6. **Permissions & Manifest Updates**

Added to `AndroidManifest.xml`:
- `READ_EXTERNAL_STORAGE` - Read stored streams
- `WRITE_EXTERNAL_STORAGE` - Write video files (API ≤28)
- `MANAGE_EXTERNAL_STORAGE` - Manage all files (API 30+)
- `requestLegacyExternalStorage="true"` - Backward compatibility
- New activities: StreamsLibraryActivity, StreamPlaybackActivity
- Activities registered in manifest

### 7. **Enhanced ChatManager Integration**

ChatManager now supports:
- `setChatEventListener()` - Listen to chat events
- `ChatEventListener` interface with callbacks:
  - `onNewMessage()` - New chat message received
  - `onViewerCountChanged()` - Viewer count updated
  - `onSystemMessage()` - System notifications
  - `onChatDisconnected()` - Connection lost

## File Structure

```
app/src/main/java/com/obs/mobile/
├── streaming/
│   ├── ChatAdapter.java                 (NEW)
│   ├── LocalRecorder.java               (NEW)
│   ├── StreamDatabaseHelper.java        (ENHANCED)
│   ├── StreamManager.java               (ENHANCED)
│   ├── StreamMetadata.java              (UPDATED)
│   └── ChatManager.java                 (SUPPORTS listeners)
├── StreamsLibraryActivity.java          (NEW)
├── StreamLibraryAdapter.java            (NEW)
├── StreamPlaybackActivity.java          (NEW)
└── ChatReplayAdapter.java               (NEW)

app/src/main/res/layout/
├── item_chat_message.xml                (NEW)
├── activity_streams_library.xml         (NEW)
├── item_stream_library.xml              (NEW)
├── activity_stream_playback.xml         (NEW)
└── item_chat_replay.xml                 (NEW)
```

## Configuration Constants

### VideoRecorder
- **Codec:** H.264 (video/avc)
- **Resolution:** 1280x720 (configurable)
- **Bitrate:** 2.5 Mbps
- **Frame Rate:** 30 FPS
- **I-Frame Interval:** 1 second

### AudioEncoder
- **Codec:** AAC (audio/mp4a-latm)
- **Sample Rate:** 44.1 kHz
- **Bitrate:** 128 kbps
- **Channels:** 2 (Stereo)
- **Profile:** AACObjectLC

### Storage
- **Location:** `context.getExternalFilesDir()` (API 30+ compatible)
- **Format:** MP4 container with H.264 video + AAC audio
- **File Naming:** `stream_YYYYMMDD_HHmmss.mp4`

## Usage Flow

### Recording a Stream
1. User clicks "Go Live" in StreamingActivity
2. StreamManager initializes:
   - Creates StreamMetadata with unique streamId
   - Initializes LocalRecorder
   - Sets up ChatManager with event listener
   - Connects to RTMP server
3. During streaming:
   - Video frames sent via `sendVideoFrame()`
   - Audio data sent via `sendAudioData()`
   - Chat messages saved to database via listener
4. User clicks "Stop":
   - LocalRecorder finalizes MP4 file
   - Stream metadata saved to database
   - ChatManager disconnected

### Viewing Streams
1. User navigates to Streams Library
2. StreamsLibraryActivity displays all saved streams
3. User clicks "Play":
   - StreamPlaybackActivity opens
   - VideoView loads MP4 file
   - Chat messages loaded from database
4. During playback:
   - SeekBar shows progress
   - Chat replays synchronized with video time
   - Play/Pause controls work normally

## API Integration Points

### StreamManager
```java
public void startStreaming(String serverUrl, String streamKey, boolean recordLocally)
public void stopStreaming()
public void sendVideoFrame(byte[] frameData, long timestamp)
public void sendAudioData(byte[] audioData, long timestamp)
public void sendChatMessage(String message)
```

### StreamDatabaseHelper
```java
public long saveStream(StreamMetadata metadata)
public StreamMetadata getStream(String streamId)
public List<StreamMetadata> getAllStreams()
public boolean updateStream(StreamMetadata metadata)
public boolean deleteStream(String streamId)
public long saveChatMessage(String streamId, ChatManager.ChatMessage message)
public List<ChatManager.ChatMessage> getChatMessages(String streamId)
public int getChatMessageCount(String streamId)
```

### LocalRecorder
```java
public void startRecording()
public void stopRecording()
public void writeVideoFrame(byte[] frameData, long timestamp)
public void writeAudioData(byte[] audioData, long timestamp)
public long getFileSize()
public String getOutputPath()
```

## Runtime Permissions

The following permissions need to be requested at runtime (Android 6.0+):
- `android.permission.CAMERA` - Capture video
- `android.permission.RECORD_AUDIO` - Capture audio
- `android.permission.READ_EXTERNAL_STORAGE` - Read files
- `android.permission.WRITE_EXTERNAL_STORAGE` - Write files

## Future Enhancements

1. **Video Thumbnails** - Generate thumbnail images for streams
2. **Bitrate Control** - Adaptive bitrate based on network
3. **Multiple Quality Options** - Record at different resolutions
4. **Stream Search & Filtering** - Search by title, date, duration
5. **Chat Export** - Export chat messages as text/JSON
6. **Stream Sharing** - Share stream metadata
7. **Extended Statistics** - Bitrate, frame drops, network stats
8. **Hardware Acceleration** - Use device's media encoder

## Testing Checklist

- [ ] LocalRecorder creates valid MP4 files
- [ ] Chat messages saved correctly to database
- [ ] StreamMetadata properly persisted
- [ ] Playback works with video and chat sync
- [ ] SeekBar updates chat messages correctly
- [ ] Storage permissions work on API 30+
- [ ] Database cleanup on stream deletion
- [ ] Chat emoji and special characters supported
- [ ] Long streams (>1 hour) handled correctly
- [ ] App resume/pause lifecycle managed

## Notes

- All database operations run on separate threads
- RecyclerView adapters use ConcurrentLinkedQueue for thread safety
- StreamManager uses Handler for off-UI-thread streaming operations
- Chat timestamps are relative to stream start time
- MP4 files stored in scoped storage directory (compatible with Android 11+)

