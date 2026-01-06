package com.obs.mobile.streaming;


/**
 * StreamMetadata - Stores information about a stream
 */
public class StreamMetadata {

    public String streamId;
    public String streamTitle;
    public String description;
    public long startTime;
    public long endTime;
    public long duration; // in milliseconds

    public String rtmpUrl;
    public String streamKey;

    public String localRecordingPath;
    public long fileSize; // in bytes
    public String thumbnailPath;

    public int maxViewers;
    public int chatMessageCount;

    public String tags; // comma-separated

    // Constructor
    public StreamMetadata() {
        this.streamId = generateStreamId();
    }

    private String generateStreamId() {
        return "stream_" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 10000);
    }

    // Getters and setters...
}