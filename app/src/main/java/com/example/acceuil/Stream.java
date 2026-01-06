package com.example.acceuil;

public class Stream {
    private String title;
    private String streamerName;
    private int viewerCount;
    private boolean isLive;
    private String thumbnailColor;
    private String category;
    private String documentId; // ADDED for Firebase

    public Stream() {}

    public Stream(String title, String streamerName, int viewerCount, boolean isLive,
                  String thumbnailColor, String category) {
        this.title = title;
        this.streamerName = streamerName;
        this.viewerCount = viewerCount;
        this.isLive = isLive;
        this.thumbnailColor = thumbnailColor;
        this.category = category;
    }

    // Getters
    public String getTitle() { return title; }
    public String getStreamerName() { return streamerName; }
    public int getViewerCount() { return viewerCount; }
    public boolean isLive() { return isLive; } // CHANGED: was getIsLive()
    public String getThumbnailColor() { return thumbnailColor; }
    public String getCategory() { return category; }
    public String getDocumentId() { return documentId; } // ADDED

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }
    public void setViewerCount(int viewerCount) { this.viewerCount = viewerCount; }
    public void setIsLive(boolean isLive) { this.isLive = isLive; }
    public void setThumbnailColor(String thumbnailColor) { this.thumbnailColor = thumbnailColor; }
    public void setCategory(String category) { this.category = category; }
    public void setDocumentId(String documentId) { this.documentId = documentId; } // ADDED

    public String getFormattedViewerCount() {
        if (viewerCount >= 1000) {
            return String.format("%.1fK viewers", viewerCount / 1000.0);
        }
        return viewerCount + " viewers";
    }
}