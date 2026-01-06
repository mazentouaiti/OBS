package com.example.Rocks.Controller;

import com.example.Rocks.Models.Users; // 🔴 Manquait : import du modèle Users
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Gère le profil utilisateur (lecture/écriture Firestore + Auth).
 * - Charge le profil depuis Firestore.
 * - Met à jour nom/email (Firestore).
 * - Met à jour mot de passe (Firebase Auth).
 * - Gère la déconnexion.
 */
public class ProfileController {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Charge le profil utilisateur connecté. */
    public void loadProfile(OnProfileLoadListener listener) {
        String uid = mAuth.getUid();
        if (uid == null) {
            listener.onError("Non connecté.");
            return;
        }

        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Users user = document.toObject(Users.class);
                            if (user != null) {
                                listener.onProfileLoaded(user);
                            } else {
                                listener.onError("Données du profil invalides.");
                            }
                        } else {
                            listener.onError("Profil introuvable.");
                        }
                    } else {
                        listener.onError("Erreur réseau ou accès refusé.");
                    }
                });
    }

    /** Met à jour nom et email dans Firestore. */
    public void updateProfile(String fullName, String email, OnProfileUpdateListener listener) {
        String uid = mAuth.getUid();
        if (uid == null) {
            listener.onError("Non connecté.");
            return;
        }
        if (fullName == null || fullName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            listener.onError("Nom et email requis.");
            return;
        }

        db.collection("users").document(uid)
                .update("fullName", fullName.trim(), "email", email.trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onError("Échec mise à jour.");
                    }
                });
    }

    /** Met à jour le mot de passe via Firebase Auth. */
    public void updatePassword(String newPassword, OnPasswordUpdateListener listener) {
        if (newPassword == null || newPassword.length() < 6) {
            listener.onError("Mot de passe ≥ 6 caractères.");
            return;
        }

        com.google.firebase.auth.FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onError("Utilisateur non connecté.");
            return;
        }

        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onError("Échec changement mot de passe.");
                    }
                });
    }

    /** Déconnecte l'utilisateur (Auth + Navigation). */
    public void logout() {
        mAuth.signOut(); // À appeler avant de lancer LoginActivity
    }

    // 🔵 Callbacks complets (pas de /* ... */)
    public interface OnProfileLoadListener {
        void onProfileLoaded(Users user);
        void onError(String message);
    }

    public interface OnProfileUpdateListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnPasswordUpdateListener {
        void onSuccess();
        void onError(String message);
    }
}