package com.obs.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Import sensor classes
import com.obs.mobile.sensors.AccelerometerSensor;
import com.obs.mobile.sensors.GyroscopeSensor;
import com.obs.mobile.sensors.LightSensor;
import com.obs.mobile.sensors.ProximitySensor;
import com.obs.mobile.sensors.MagnetometerSensor;

/**
 * CameraActivity - Camera Preview and Recording Screen
 *
 * This is a placeholder screen for camera functionality.
 * Camera implementation will be added later.
 *
 * SENSOR INTEGRATION:
 * Now uses independent sensor classes from com.obs.mobile.sensors package
 * Each student implements their sensor class and uses callbacks here
 */
public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private SurfaceView surfaceView;
    private TextView tvStatus;
    private Button btnRecord;
    private Button btnSwitchCamera;

    private boolean isRecording = false;

    // Sensor class instances - Students will use these
    private AccelerometerSensor accelerometerSensor;
    private GyroscopeSensor gyroscopeSensor;
    private LightSensor lightSensor;
    private ProximitySensor proximitySensor;
    private MagnetometerSensor magnetometerSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Camera Preview");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Check camera permission
        checkCameraPermission();

        // Initialize sensor classes
        initializeSensors();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        surfaceView = findViewById(R.id.surface_view);
        tvStatus = findViewById(R.id.tv_status);
        btnRecord = findViewById(R.id.btn_record);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    /**
     * Initialize all sensor classes
     * Each student will uncomment and configure their sensor
     */
    private void initializeSensors() {
        /* ========================================================================
         * TODO (Student 1 - Accelerometer):
         * ========================================================================
         * Uncomment and implement:
         *
         * accelerometerSensor = new AccelerometerSensor(this);
         *
         * accelerometerSensor.setOnShakeListener(new AccelerometerSensor.OnShakeListener() {
         *     @Override
         *     public void onShake(float intensity) {
         *         // Shake detected - toggle recording
         *         runOnUiThread(() -> {
         *             toggleRecording();
         *             Toast.makeText(CameraActivity.this,
         *                 "Shake detected! Recording " + (isRecording ? "started" : "stopped"),
         *                 Toast.LENGTH_SHORT).show();
         *         });
         *     }
         * });
         *
         * if (accelerometerSensor.initialize()) {
         *     Toast.makeText(this, "Accelerometer ready", Toast.LENGTH_SHORT).show();
         * }
         */

        /* ========================================================================
         * TODO (Student 2 - Gyroscope):
         * ========================================================================
         * Uncomment and implement:
         *
         * gyroscopeSensor = new GyroscopeSensor(this);
         *
         * gyroscopeSensor.setOnRotationGestureListener(
         *     new GyroscopeSensor.OnRotationGestureListener() {
         *         @Override
         *         public void onFastRotation(float degrees) {
         *             runOnUiThread(() ->
         *                 Toast.makeText(CameraActivity.this,
         *                     "Fast rotation detected!",
         *                     Toast.LENGTH_SHORT).show()
         *             );
         *         }
         *     }
         * );
         *
         * if (gyroscopeSensor.initialize()) {
         *     Toast.makeText(this, "Gyroscope ready", Toast.LENGTH_SHORT).show();
         * }
         */

        /* ========================================================================
         * TODO (Student 3 - Light Sensor):
         * ========================================================================
         * Uncomment and implement:
         *
         * lightSensor = new LightSensor(this);
         *
         * lightSensor.setOnLightChangedListener(new LightSensor.OnLightChangedListener() {
         *     @Override
         *     public void onLightChanged(float lux, LightSensor.LightCategory category) {
         *         runOnUiThread(() -> {
         *             String message = category.getName() + " (" + Math.round(lux) + " lux)";
         *             tvStatus.setText("Light: " + message);
         *
         *             // Auto-adjust based on light
         *             if (category == LightSensor.LightCategory.VERY_DARK) {
         *                 Toast.makeText(CameraActivity.this,
         *                     "Low light - Enable night mode",
         *                     Toast.LENGTH_SHORT).show();
         *             }
         *         });
         *     }
         * });
         *
         * if (lightSensor.initialize()) {
         *     Toast.makeText(this, "Light sensor ready", Toast.LENGTH_SHORT).show();
         * }
         */

        /* ========================================================================
         * TODO (Student 4 - Proximity Sensor):
         * ========================================================================
         * Uncomment and implement:
         *
         * proximitySensor = new ProximitySensor(this);
         *
         * proximitySensor.setOnNearListener(new ProximitySensor.OnNearListener() {
         *     @Override
         *     public void onNear() {
         *         // Phone is covered - pause recording for privacy
         *         runOnUiThread(() -> {
         *             if (isRecording) {
         *                 toggleRecording();
         *                 Toast.makeText(CameraActivity.this,
         *                     "Recording paused - proximity detected",
         *                     Toast.LENGTH_SHORT).show();
         *             }
         *         });
         *     }
         * });
         *
         * proximitySensor.setOnFarListener(new ProximitySensor.OnFarListener() {
         *     @Override
         *     public void onFar() {
         *         runOnUiThread(() ->
         *             Toast.makeText(CameraActivity.this,
         *                 "Proximity clear",
         *                 Toast.LENGTH_SHORT).show()
         *         );
         *     }
         * });
         *
         * if (proximitySensor.initialize()) {
         *     Toast.makeText(this, "Proximity sensor ready", Toast.LENGTH_SHORT).show();
         * }
         */

        /* ========================================================================
         * TODO (Student 5 - Magnetometer):
         * ========================================================================
         * Uncomment and implement:
         *
         * magnetometerSensor = new MagnetometerSensor(this);
         *
         * magnetometerSensor.setOnCompassChangeListener(
         *     new MagnetometerSensor.OnCompassChangeListener() {
         *         @Override
         *         public void onCompassChange(float azimuth,
         *                                    MagnetometerSensor.CompassDirection direction) {
         *             runOnUiThread(() -> {
         *                 String compassText = direction.getName() + " (" +
         *                                     Math.round(azimuth) + "°)";
         *                 tvStatus.setText("Compass: " + compassText);
         *             });
         *         }
         *     }
         * );
         *
         * if (magnetometerSensor.initialize()) {
         *     Toast.makeText(this, "Magnetometer ready", Toast.LENGTH_SHORT).show();
         * } else {
         *     Toast.makeText(this, "Magnetometer not available", Toast.LENGTH_SHORT).show();
         * }
         */
    }

    /**
     * Check and request camera permission
     */
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // TODO: Initialize camera preview here
            tvStatus.setText("Camera preview will be implemented here\n\nPermission granted - Ready for camera implementation");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText("Camera preview will be implemented here\n\nPermission granted");
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                tvStatus.setText("Camera permission denied\n\nPlease grant permission in settings");
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Toggle recording state
     * This can be triggered by sensors (e.g., shake detection)
     */
    private void toggleRecording() {
        isRecording = !isRecording;

        if (isRecording) {
            btnRecord.setText("Stop Recording");
            tvStatus.setText("Recording... (Placeholder)\n\nSensor-triggered recording will work here");
            // TODO: Start actual video recording
        } else {
            btnRecord.setText("Start Recording");
            tvStatus.setText("Recording stopped\n\nCamera preview placeholder");
            // TODO: Stop video recording
        }

        Toast.makeText(this, isRecording ? "Recording started" : "Recording stopped",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Switch between front and back camera
     */
    private void switchCamera() {
        // TODO: Implement camera switching
        Toast.makeText(this, "Camera switch - To be implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop all sensors - CRITICAL for battery life!
        if (accelerometerSensor != null) {
            accelerometerSensor.stopListening();
        }

        if (gyroscopeSensor != null) {
            gyroscopeSensor.stopListening();
        }

        if (lightSensor != null) {
            lightSensor.stopListening();
        }

        if (proximitySensor != null) {
            proximitySensor.stopListening();
        }

        if (magnetometerSensor != null) {
            magnetometerSensor.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start all initialized sensors
        if (accelerometerSensor != null && accelerometerSensor.isAvailable()) {
            accelerometerSensor.startListening();
        }

        if (gyroscopeSensor != null && gyroscopeSensor.isAvailable()) {
            gyroscopeSensor.startListening();
        }

        if (lightSensor != null && lightSensor.isAvailable()) {
            lightSensor.startListening();
        }

        if (proximitySensor != null && proximitySensor.isAvailable()) {
            proximitySensor.startListening();
        }

        if (magnetometerSensor != null && magnetometerSensor.isAvailable()) {
            magnetometerSensor.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* TODO (ALL STUDENTS): CLEANUP
         * ==============================
         * Final cleanup of all sensor resources
         * Set listeners to null
         */
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
