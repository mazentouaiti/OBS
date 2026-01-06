package com.example.acceuil;

import com.google.firebase.firestore.PropertyName;

public class Stream {
    private String title;
    private String streamerName;
    private int viewerCount;

    @PropertyName("isLive")
    private boolean isLive;

    @PropertyName("thumbnailColor")
    private String thumbnailColor;

    private String category;
    private String thumbnailUrl;
    private String streamUrl;
    private String documentId;

    public Stream() {}

    public Stream(String title, String streamerName, int viewerCount, boolean isLive,
                  String thumbnailColor, String category, String thumbnailUrl, String streamUrl) {
        this.title = title;
        this.streamerName = streamerName;
        this.viewerCount = viewerCount;
        this.isLive = isLive;
        this.thumbnailColor = thumbnailColor;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.streamUrl = streamUrl;
    }

    // Getters
    public String getTitle() { return title; }
    public String getStreamerName() { return streamerName; }
    public int getViewerCount() { return viewerCount; }

    @PropertyName("isLive")
    public boolean isLive() { return isLive; }

    @PropertyName("thumbnailColor")
    public String getThumbnailColor() { return thumbnailColor; }

    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getStreamUrl() { return streamUrl; }
    public String getDocumentId() { return documentId; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }
    public void setViewerCount(int viewerCount) { this.viewerCount = viewerCount; }

    @PropertyName("isLive")
    public void setIsLive(boolean isLive) { this.isLive = isLive; }

    @PropertyName("thumbnailColor")
    public void setThumbnailColor(String thumbnailColor) { this.thumbnailColor = thumbnailColor; }

    public void setCategory(String category) { this.category = category; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFormattedViewerCount() {
        if (viewerCount >= 1000) {
            return String.format("%.1fK viewers", viewerCount / 1000.0);
        }
        return viewerCount + " viewers";
    }
}