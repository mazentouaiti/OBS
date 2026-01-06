package com.example.acceuil;

public class Video {
    private String title;
    private String uploaderName;
    private int views;
    private int durationSeconds;
    private String thumbnailColor;
    private String category;
    private String thumbnailUrl;
    private String videoUrl;
    private String documentId; // ADDED for Firebase

    public Video() {}

    public Video(String title, String uploaderName, int views, int durationSeconds,
                 String thumbnailColor, String category, String thumbnailUrl, String videoUrl) {
        this.title = title;
        this.uploaderName = uploaderName;
        this.views = views;
        this.durationSeconds = durationSeconds;
        this.thumbnailColor = thumbnailColor;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUploaderName() { return uploaderName; }
    public int getViews() { return views; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getThumbnailColor() { return thumbnailColor; }
    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
    public String getDocumentId() { return documentId; } // ADDED

    // Setters (ALL ADDED for Firebase)
    public void setTitle(String title) { this.title = title; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public void setViews(int views) { this.views = views; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setThumbnailColor(String thumbnailColor) { this.thumbnailColor = thumbnailColor; }
    public void setCategory(String category) { this.category = category; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setDocumentId(String documentId) { this.documentId = documentId; } // ADDED

    public String getFormattedViews() {
        if (views >= 1000) {
            return String.format("%.1fK", views / 1000.0);
        }
        return String.valueOf(views);
    }

    public String getFormattedDuration() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}