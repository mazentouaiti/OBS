package com.example.Rocks.Controller;

import com.example.Rocks.Models.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Gère l'inscription + création du profil Firestore.
 * - Valide mot de passe (min 6 caractères, correspondance).
 * - Crée compte Firebase Auth.
 * - Crée document Firestore "users/{uid}".
 */
public class SignUpController {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Crée un compte et un profil utilisateur.
     * @param fullName Nom complet
     * @param email Email
     * @param password Mot de passe
     * @param confirmPassword Confirmation
     * @param listener Callback UI
     */
    public void signUp(String fullName, String email, String password,
                       String confirmPassword, OnSignUpListener listener) {
        // 1. Validation locale
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            listener.onError("Tous les champs sont requis.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            listener.onError("Mots de passe non identiques.");
            return;
        }
        if (password.length() < 6) {
            listener.onError("Mot de passe ≥ 6 caractères.");
            return;
        }

        // 2. Création compte Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 3. Création profil Firestore
                        String uid = mAuth.getCurrentUser().getUid();
                        Users user = new Users(uid, fullName, email);
                        db.collection("users").document(uid).set(user)
                                .addOnCompleteListener(ftask -> {
                                    if (ftask.isSuccessful()) listener.onSuccess();
                                    else listener.onError("Erreur profil.");
                                });
                    } else {
                        listener.onError("Email déjà utilisé.");
                    }
                });
    }

    public interface OnSignUpListener {
        void onSuccess();
        void onError(String message);
    }
}