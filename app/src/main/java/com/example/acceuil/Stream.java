package com.example.acceuil;

public class Stream {
    private String title;
    private String streamerName;
    private int viewerCount;
    private boolean isLive;
    private String thumbnailColor;
    private String category;
    private String thumbnailUrl;  // AJOUTÉ
    private String documentId;

    public Stream() {}

    public Stream(String title, String streamerName, int viewerCount, boolean isLive,
                  String thumbnailColor, String category, String thumbnailUrl) {
        this.title = title;
        this.streamerName = streamerName;
        this.viewerCount = viewerCount;
        this.isLive = isLive;
        this.thumbnailColor = thumbnailColor;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Getters
    public String getTitle() { return title; }
    public String getStreamerName() { return streamerName; }
    public int getViewerCount() { return viewerCount; }
    public boolean isLive() { return isLive; }
    public String getThumbnailColor() { return thumbnailColor; }
    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }  // AJOUTÉ
    public String getDocumentId() { return documentId; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }
    public void setViewerCount(int viewerCount) { this.viewerCount = viewerCount; }
    public void setIsLive(boolean isLive) { this.isLive = isLive; }
    public void setThumbnailColor(String thumbnailColor) { this.thumbnailColor = thumbnailColor; }
    public void setCategory(String category) { this.category = category; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }  // AJOUTÉ
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFormattedViewerCount() {
        if (viewerCount >= 1000) {
            return String.format("%.1fK viewers", viewerCount / 1000.0);
        }
        return viewerCount + " viewers";
    }
}