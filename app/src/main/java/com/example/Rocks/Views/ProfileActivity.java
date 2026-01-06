package com.example.Rocks.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.Rocks.Controller.ProfileController;
import com.example.Rocks.Models.Users;
import com.example.Rocks.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        btnLogout = findViewById(R.id.btn_logout);

        // ✅ Use ProfileController to load from Firestore
        ProfileController profileCtrl = new ProfileController();
        profileCtrl.loadProfile(new ProfileController.OnProfileLoadListener() {
            @Override
            public void onProfileLoaded(Users user) {
                runOnUiThread(() -> {
                    etName.setText(user.getFullName());
                    etEmail.setText(user.getEmail());
                    // Optional: update followers/following UI
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish(); // or redirect to login
                });
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Déconnecté", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}