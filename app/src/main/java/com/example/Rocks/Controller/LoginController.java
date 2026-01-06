package com.example.Rocks.Controller;

import com.example.Rocks.Models.Users;
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
        // ... validation ...

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 👇 Add this: Load profile after successful login
                        new ProfileController().loadProfile(new ProfileController.OnProfileLoadListener() {
                            @Override
                            public void onProfileLoaded(Users user) {
                                listener.onSuccess(); // Proceed to ProfileActivity
                            }

                            @Override
                            public void onError(String message) {
                                listener.onError("Profil introuvable : " + message);
                            }
                        });
                    } else {
                        listener.onError("Échec de la connexion.");
                    }
                });
    }
    public interface OnLoginListener {
        void onSuccess();
        void onError(String message);
    }
}