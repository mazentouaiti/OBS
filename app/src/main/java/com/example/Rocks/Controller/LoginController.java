package com.example.Rocks.Controller;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Gère la connexion utilisateur.
 * - Valide les champs.
 * - Utilise Firebase Auth.
 * - Callback UI via OnLoginListener.
 */
public class LoginController {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    /**
     * Connecte l'utilisateur avec email/mot de passe.
     * @param email Email saisi
     * @param password Mot de passe saisi
     * @param listener Callback UI
     */
    public void login(String email, String password, OnLoginListener listener) {
        // 1. Validation locale
        if (email.isEmpty() || password.isEmpty()) {
            listener.onError("Tous les champs sont requis.");
            return;
        }
        // 2. Appel Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) listener.onSuccess();
                    else listener.onError("Échec de la connexion.");
                });
    }

    public interface OnLoginListener {
        void onSuccess();
        void onError(String message);
    }
}