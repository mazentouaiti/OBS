package com.example.Rocks.Models;

public class Users {

    // Identifiant unique (Généré par Firebase ou Base de données)
    private String userId;

    // Champs visibles dans SignUp_activity.xml et Profile_activity.xml
    private String fullName;
    private String email;

    // Champs visibles dans Profile_activity.xml
    private String profileImageUrl;
    private int followersCount;
    private int followingCount;

    // Métadonnées système
    private long createdAt;

    // 1. Constructeur vide (Requis pour Firebase / Firestore)
    public Users() {
    }

    // 2. Constructeur simplifié pour l'inscription (SignUp)
    // À la création, 0 followers et pas d'image
    public Users(String userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
        this.followersCount = 0;
        this.followingCount = 0;
        this.profileImageUrl = ""; // Ou URL d'une image par défaut
    }

    // 3. Constructeur complet
    public Users(String userId, String fullName, String email, String profileImageUrl, int followersCount, int followingCount, long createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.createdAt = createdAt;
    }

    // --- Getters et Setters ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
