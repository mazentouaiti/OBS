package com.example.Rocks.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.Rocks.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText fullnameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signupButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_activity);

        mAuth = FirebaseAuth.getInstance();

        fullnameEditText = findViewById(R.id.fullname_edit_text);
        emailEditText = findViewById(R.id.signup_email_edit_text);
        passwordEditText = findViewById(R.id.signup_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        signupButton = findViewById(R.id.signup_button);

        signupButton.setOnClickListener(v -> signUp());
        findViewById(R.id.login_redirect_text).setOnClickListener(v -> finish());
    }

    private void signUp() {
        String fullName = fullnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirm = confirmPasswordEditText.getText().toString().trim();

        new com.example.Rocks.Controller.SignUpController().signUp(
                fullName, email, password, confirm,
                new com.example.Rocks.Controller.SignUpController.OnSignUpListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(SignUpActivity.this, "Compte créé !", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, ProfileActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}