package com.example.Rocks.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.Rocks.Controller.ProfileController;
import com.example.Rocks.Models.Users;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnSave, btnLogout;
    private ProfileController profileController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // 👈 XML séparé

        initViews();
        profileController = new ProfileController();

        loadUserProfile();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save_profile);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserProfile() {
        profileController.loadProfile(new ProfileController.OnProfileLoadListener() {
            @Override
            public void onProfileLoaded(Users user) {
                etName.setText(user.getFullName());
                etEmail.setText(user.getEmail());
                // Ne jamais afficher le mot de passe
                etPassword.setText(""); // champ vide pour nouveau mot de passe
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Erreur: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void saveProfile() {
        String fullName = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nom et email requis", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mettre à jour profil (nom/email)
        profileController.updateProfile(fullName, email, new ProfileController.OnProfileUpdateListener() {
            @Override
            public void onSuccess() {
                if (!newPassword.isEmpty()) {
                    // Changer le mot de passe si fourni
                    profileController.updatePassword(newPassword, new ProfileController.OnPasswordUpdateListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ProfileActivity.this, "Profil et mot de passe mis à jour", Toast.LENGTH_SHORT).show();
                            clearPasswordField();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(ProfileActivity.this, "Erreur mot de passe: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Erreur: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearPasswordField() {
        etPassword.setText("");
    }

    private void logout() {
        profileController.logout();
        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();
    }
}