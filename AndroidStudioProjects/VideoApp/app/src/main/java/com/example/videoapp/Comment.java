package com.example.videoapp;

import com.google.firebase.Timestamp;

public class Comment {
    public String id; // Document ID from Firestore
    public String text;
    public String userId;
    public String userName; // Optional: to display user name
    public Timestamp timestamp;

    public Comment() {}

    // Optional constructor for creating new comments
    public Comment(String text, String userId, String userName) {
        this.text = text;
        this.userId = userId;
        this.userName = userName;
    }
}