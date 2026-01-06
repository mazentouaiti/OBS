package com.example.videoapp;

public class Video {

    public String id;
    public String title;
    public String uploaderName;
    public String videoUrl;
    public String thumbnailUrl;
    public String thumbnailColor;
    public long likes;
    public long views;

    public Video() {
        // Firestore requires empty constructor
    }

    public Video(
            String id,
            String title,
            String uploaderName,
            String videoUrl,
            String thumbnailUrl,
            String thumbnailColor,
            long likes,
            long views
    ) {
        this.id = id;
        this.title = title;
        this.uploaderName = uploaderName;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailColor = thumbnailColor;
        this.likes = likes;
        this.views = views;
    }
}
