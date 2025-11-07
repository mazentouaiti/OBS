package com.obs.mobile;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * ScenesActivity - Scenes Management Screen
 *
 * This screen will display available recording scenes/presets.
 * Scenes can be switched manually or automatically based on sensor data.
 *
 * PLACEHOLDER SCREEN - To be implemented later
 */
public class ScenesActivity extends AppCompatActivity {

    private TextView tvSceneInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenes);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Scenes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        tvSceneInfo = findViewById(R.id.tv_scene_info);

        /* ========================================================================
         * TODO (ALL STUDENTS): SENSOR-BASED SCENE SWITCHING
         * ========================================================================
         *
         * Scenes can be automatically selected based on sensor data:
         *
         * SCENE IDEAS:
         * 1. Default Scene - Normal recording
         * 2. Night Scene - Low light (triggered by light sensor < 50 lux)
         * 3. Outdoor Scene - Bright light (triggered by light sensor > 10000 lux)
         * 4. Sport Scene - Fast motion (triggered by accelerometer/gyroscope)
         * 5. Direction-based Scenes - North/South/East/West (magnetometer)
         */

        setupSceneSwitching();
    }

    /**
     * Setup automatic scene switching based on sensors
     */
    private void setupSceneSwitching() {
        /* TODO (Student 1 - Accelerometer):
         * ===================================
         * Implement shake-to-switch-scene functionality
         *
         * - Light shake: Next scene
         * - Strong shake: Previous scene
         * - Double shake: Reset to default scene
         *
         * Example:
         * - Detect shake in accelerometer listener
         * - Call switchToNextScene() or switchToPreviousScene()
         */

        /* TODO (Student 2 - Gyroscope):
         * ===============================
         * Implement rotation-based scene switching
         *
         * - Rotate phone clockwise 90°: Next scene
         * - Rotate phone counter-clockwise 90°: Previous scene
         * - Flip phone upside down: Toggle special scene
         */

        /* TODO (Student 3 - Light Sensor):
         * ==================================
         * Implement auto scene switching based on ambient light
         *
         * Light levels -> Scenes:
         * - 0-50 lux (Dark) -> Night Scene
         * - 50-500 lux (Indoor) -> Default Scene
         * - 500-10000 lux (Outdoor shade) -> Outdoor Scene
         * - 10000+ lux (Direct sun) -> Bright Outdoor Scene
         *
         * Add hysteresis to avoid rapid switching
         */

        /* TODO (Student 4 - Proximity Sensor):
         * ======================================
         * Use proximity for quick scene toggle
         *
         * - Wave hand over sensor: Cycle to next scene
         * - Cover sensor for 2 seconds: Switch to privacy scene (blank screen)
         */

        /* TODO (Student 5 - Magnetometer):
         * ==================================
         * Implement direction-based scene switching
         *
         * Compass direction -> Scene:
         * - Facing North (0°): Scene 1
         * - Facing East (90°): Scene 2
         * - Facing South (180°): Scene 3
         * - Facing West (270°): Scene 4
         *
         * Useful for panoramic recording or location-based scenes
         */
    }

    /**
     * Switch to next scene
     */
    private void switchToNextScene() {
        // TODO: Implement scene switching logic
        tvSceneInfo.setText("Switched to next scene");
    }

    /**
     * Switch to previous scene
     */
    private void switchToPreviousScene() {
        // TODO: Implement scene switching logic
        tvSceneInfo.setText("Switched to previous scene");
    }

    /**
     * Switch to specific scene by name
     */
    private void switchToScene(String sceneName) {
        // TODO: Implement scene switching logic
        tvSceneInfo.setText("Switched to: " + sceneName);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

