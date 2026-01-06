package com.example.Rocks.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.Rocks.Controller.SignUpController;
import com.example.Rocks.R;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText fullnameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signupButton;
    private TextView loginRedirectText;
    private SignUpController signUpController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.SignUp_activity);

        initViews();
        signUpController = new SignUpController();

        signupButton.setOnClickListener(v -> handleSignUp());
        loginRedirectText.setOnClickListener(v -> redirectToLogin());
    }

    private void initViews() {
        fullnameEditText = findViewById(R.id.fullname_edit_text);
        emailEditText = findViewById(R.id.signup_email_edit_text);
        passwordEditText = findViewById(R.id.signup_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.login_redirect_text);
    }

    private void handleSignUp() {
        String fullName = fullnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        signUpController.signUp(fullName, email, password, confirmPassword, new SignUpController.OnSignUpListener() {
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
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        finish();
    }
}