package com.obs.mobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.OnBackPressedCallback;

/**
 * MainMenuActivity - Main Menu Screen
 *
 * Displays menu buttons to navigate to different features:
 * - Start Camera/Recording
 * - Sensor Settings
 * - Scenes
 * - Exit
 *
 * This is the main hub of the application after splash screen
 */
public class MainMenuActivity extends AppCompatActivity {

    private Button btnCamera;
    private Button btnSensors;
    private Button btnScenes;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("OBS Mobile - Main Menu");
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnCamera = findViewById(R.id.btn_camera);
        btnSensors = findViewById(R.id.btn_sensors);
        btnScenes = findViewById(R.id.btn_scenes);
        btnExit = findViewById(R.id.btn_exit);
    }

    /**
     * Setup click listeners for all buttons
     */
    private void setupClickListeners() {
        // Camera button - Navigate to Camera Preview
        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        // Sensors button - Navigate to Sensor Settings
        btnSensors.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, SensorsActivity.class);
            startActivity(intent);
        });

        // Scenes button - Navigate to Scenes Screen
        btnScenes.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ScenesActivity.class);
            startActivity(intent);
        });

        // Exit button - Show confirmation dialog
        btnExit.setOnClickListener(v -> showExitDialog());
    }

    /**
     * Show confirmation dialog before exiting the app
     */
    private void showExitDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Exit OBS Mobile")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes", (dialog, which) -> finish())
            .setNegativeButton("No", null)
            .show();
    }
}
