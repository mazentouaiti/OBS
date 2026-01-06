package com.obs.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Import all sensors
import com.obs.mobile.sensors.AccelerometerSensor;
import com.obs.mobile.sensors.GyroscopeSensor;
import com.obs.mobile.sensors.LightSensor;
import com.obs.mobile.sensors.ProximitySensor;
import com.obs.mobile.sensors.MagnetometerSensor;
import com.obs.mobile.streaming.StreamManager;
import com.obs.mobile.utils.SensorPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * CameraActivity - Camera Preview and Recording Screen with Sensors
 *
 * Uses TextureView for camera preview with Camera2 API
 * Supports PiP mode and floating window
 * Shows sensor data overlays on camera view when enabled
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TextureView textureView;
    private TextView tvStatus;
    private View recordingIndicator;

    // Sensor instances
    private AccelerometerSensor accelerometerSensor;
    private GyroscopeSensor gyroscopeSensor;
    private LightSensor lightSensor;
    private ProximitySensor proximitySensor;
    private MagnetometerSensor magnetometerSensor;

    // Sensor overlay views
    private View gyroscopeOverlay;
    private TextView tvGyroData;
    private View accelerometerOverlay;
    private TextView tvAccelData;
    private View lightOverlay;
    private TextView tvLightData;
    private View proximityOverlay;
    private TextView tvProximityData;
    private View magnetometerOverlay;
    private TextView tvMagnetData;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private boolean isFrontCamera = false;
    private boolean isRecording = false;
    private boolean autoBrightnessEnabled = false;
    private boolean autoFocusOnProximityEnabled = false;

    // Sensor data streamer for Python integration
    private SensorDataStreamer sensorDataStreamer;

    // ActivityResultLauncher for overlay permission
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    // Broadcast receiver for sensor state changes
    private BroadcastReceiver sensorStateReceiver;

    // Zoom variables
    private SeekBar zoomSlider;
    private TextView tvZoomValue;
    private ImageButton btnZoomIn;
    private ImageButton btnZoomOut;
    private ImageView btnRecord;
    private ImageButton btnSwitchCamera;
    private ImageButton btnFloating;
    private TextView tvRecordingTime;

    private float currentZoomLevel = 1.0f;
    private float minZoomLevel = 1.0f;
    private float maxZoomLevel = 10.0f;
    private Rect zoomRect;
    private Handler recordingTimeHandler;
    private Runnable recordingTimeRunnable;
    private long recordingStartTime = 0;
    private StreamManager streamManager;
    private Button btnStartStreaming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_camera);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Camera with Sensors");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Initialize UI components
            initializeViews();
            // Initialize zoom controls
            initializeZoomControls();
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            // Set button listeners
            btnRecord.setOnClickListener(v -> toggleRecording());
            btnSwitchCamera.setOnClickListener(v -> switchCamera());

            // Only set listeners if buttons exist in layout
            if (btnFloating != null) {
                btnFloating.setOnClickListener(v -> startFloatingCamera());
            }

            // Initialize all sensors
            initializeAllSensors();

            // Initialize sensor data streamer for Python integration
            sensorDataStreamer = new SensorDataStreamer(this);
            // Update with your computer IP address (e.g., 192.168.1.100)
            sensorDataStreamer.initialize("192.168.1.113", 5000);
            sensorDataStreamer.start();
            Log.d(TAG, "✅ Sensor data streamer started");

            // Initialize broadcast receiver
            initializeBroadcastReceiver();

            // Set up TextureView listener
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "TextureView available: " + width + "x" + height);
                    if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        openCamera();
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "TextureView size changed: " + width + "x" + height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    Log.d(TAG, "TextureView destroyed");
                    closeCamera();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    // Called for each frame - don't log here
                }
            });
            // Commented out: button not present in layout
            // btnStartStreaming = findViewById(R.id.btn_start_streaming);
            // btnStartStreaming.setOnClickListener(v -> {
            //     Intent intent = new Intent(CameraActivity.this, StreamingActivity.class);
            //     startActivity(intent);
            // });

            // Register ActivityResultLauncher for overlay permission
            overlayPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        // Handle the result of the overlay permission request
                        if (Settings.canDrawOverlays(this)) {
                            Toast.makeText(this, "Permission granted! Try again", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Set up modern PiP mode change listener
            addOnPictureInPictureModeChangedListener(info -> {
                boolean isInPictureInPictureMode = info.isInPictureInPictureMode();
                Log.d(TAG, "onPictureInPictureModeChanged: " + isInPictureInPictureMode);

                if (isInPictureInPictureMode) {
                    // Hide UI controls in PiP mode (including sensor overlays)
                    hideAllUI();
                } else {
                    // Show UI controls when exiting PiP
                    showAllUI();
                }
            });

            checkCameraPermission();
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: ", e);
            Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        textureView = findViewById(R.id.texture_view);
        tvStatus = findViewById(R.id.tv_status);
        recordingIndicator = findViewById(R.id.recording_indicator);

        // Button views (changed to ImageButton)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnFloating = findViewById(R.id.btn_floating);
        btnRecord = findViewById(R.id.btn_record);  // Changed to ImageButton

        // New zoom views
        zoomSlider = findViewById(R.id.zoom_slider);
        tvZoomValue = findViewById(R.id.tv_zoom_value);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        tvRecordingTime = findViewById(R.id.tv_recording_time);

        // Set button listeners
        btnSwitchCamera.setOnClickListener(v -> switchCamera());

        // Floating button listener (if exists)
        if (btnFloating != null) {
            btnFloating.setOnClickListener(v -> startFloatingCamera());
        }

        // Record button listener (now ImageButton)
        if (btnRecord != null) {
            btnRecord.setOnClickListener(v -> toggleRecording());
        }

        // Initialize recording time handler
        recordingTimeHandler = new Handler();

        initializeSensorOverlays();
    }
    private void initializeSensorOverlays() {
        // Gyroscope overlay
        gyroscopeOverlay = findViewById(R.id.gyroscope_overlay);
        tvGyroData = findViewById(R.id.tv_gyro_data);

        // Accelerometer overlay (check if exists)
        accelerometerOverlay = findViewById(R.id.accelerometer_overlay);
        if (accelerometerOverlay != null) {
            tvAccelData = findViewById(R.id.tv_accel_data);
        }

        // Light overlay (check if exists)
        lightOverlay = findViewById(R.id.light_overlay);
        if (lightOverlay != null) {
            tvLightData = findViewById(R.id.tv_light_data);
        }

        // Proximity overlay (check if exists)
        proximityOverlay = findViewById(R.id.proximity_overlay);
        if (proximityOverlay != null) {
            tvProximityData = findViewById(R.id.tv_proximity_data);
        }

        // Magnetometer overlay (check if exists)
        magnetometerOverlay = findViewById(R.id.magnetometer_overlay);
        if (magnetometerOverlay != null) {
            tvMagnetData = findViewById(R.id.tv_magnet_data);
        }
    }
    /**
     * Find existing overlays from XML layout
     */
//    private void findOrCreateOverlays() {
//        // Find all overlays from XML layout
//        gyroscopeOverlay = findViewById(R.id.gyroscope_overlay);
//        tvGyroData = findViewById(R.id.tv_gyro_data);
//
//        accelerometerOverlay = findViewById(R.id.accelerometer_overlay);
//        tvAccelData = findViewById(R.id.tv_accel_data);
//
//        lightOverlay = findViewById(R.id.light_overlay);
//        tvLightData = findViewById(R.id.tv_light_data);
//
//        proximityOverlay = findViewById(R.id.proximity_overlay);
//        tvProximityData = findViewById(R.id.tv_proximity_data);
//
//        magnetometerOverlay = findViewById(R.id.magnetometer_overlay);
//        tvMagnetData = findViewById(R.id.tv_magnet_data);
//    }

    /**
     * Initialize broadcast receiver for sensor state changes
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initializeBroadcastReceiver() {
        sensorStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sensorType = intent.getStringExtra("sensor_type");
                boolean isEnabled = intent.getBooleanExtra("is_enabled", false);

                Log.d(TAG, "Sensor state changed: " + sensorType + " = " + isEnabled);
                updateSensorState(sensorType, isEnabled);
            }
        };

        // Register receiver with proper flags for different Android versions
        IntentFilter filter = new IntentFilter("com.obs.mobile.SENSOR_STATE_CHANGED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit export flag
            registerReceiver(sensorStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ - use explicit flag
            registerReceiver(sensorStateReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            // Older Android versions
            registerReceiver(sensorStateReceiver, filter);
        }
    }

    /**
     * Initialize all sensors
     */
    private void initializeAllSensors() {
        setupGyroscopeSensor();
        setupAccelerometerSensor();
        setupLightSensor();
        setupProximitySensor();
        setupMagnetometerSensor();
    }

    /**
     * Setup gyroscope sensor
     */
    private void setupGyroscopeSensor() {
        gyroscopeSensor = new GyroscopeSensor(this);

        // Set rotation listener to update gyroscope data display
        gyroscopeSensor.setOnRotationListener((rotationX, rotationY, rotationZ) ->
                runOnUiThread(() -> {
                    if (tvGyroData != null && gyroscopeOverlay != null) {
                        String gyroData = String.format(Locale.US,
                                "X: %.1f°/s\nY: %.1f°/s\nZ: %.1f°/s",
                                GyroscopeSensor.radiansToDegrees(rotationX),
                                GyroscopeSensor.radiansToDegrees(rotationY),
                                GyroscopeSensor.radiansToDegrees(rotationZ));
                        tvGyroData.setText(gyroData);

                        // Make sure overlay is visible
                        if (gyroscopeOverlay.getVisibility() != View.VISIBLE) {
                            gyroscopeOverlay.setVisibility(View.VISIBLE);
                        }
                    }

                    // Stream sensor data to Python
                    if (sensorDataStreamer != null) {
                        float degX = (float)GyroscopeSensor.radiansToDegrees(rotationX);
                        float degY = (float)GyroscopeSensor.radiansToDegrees(rotationY);
                        float degZ = (float)GyroscopeSensor.radiansToDegrees(rotationZ);
                        sensorDataStreamer.updateGyroscope(degX, degY, degZ);
                    }
                })
        );

        // Initialize gyroscope sensor
        if (gyroscopeSensor.initialize()) {
            Log.d(TAG, "Gyroscope sensor initialized");
            // Start listening only if enabled
            if (SensorPreferences.isGyroscopeEnabled(this)) {
                gyroscopeSensor.startListening();
                if (gyroscopeOverlay != null) {
                    gyroscopeOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                if (gyroscopeOverlay != null) {
                    gyroscopeOverlay.setVisibility(View.GONE);
                }
            }
        } else {
            Log.w(TAG, "Gyroscope sensor not available on this device");
            if (gyroscopeOverlay != null) {
                gyroscopeOverlay.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Position sensor overlays in a vertical stack on the right side
     */
    private void positionSensorOverlays() {
        // Get screen dimensions
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        // Position each overlay with proper spacing
        List<View> overlays = new ArrayList<>();
        if (gyroscopeOverlay != null) overlays.add(gyroscopeOverlay);
        if (accelerometerOverlay != null) overlays.add(accelerometerOverlay);
        if (lightOverlay != null) overlays.add(lightOverlay);
        if (proximityOverlay != null) overlays.add(proximityOverlay);
        if (magnetometerOverlay != null) overlays.add(magnetometerOverlay);

        // Remove any existing layout params
        for (View overlay : overlays) {
            if (overlay.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) overlay.getLayoutParams();
                params.addRule(RelativeLayout.BELOW, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                overlay.setLayoutParams(params);
            }
        }

        // Stack them vertically below the toolbar
        int previousId = R.id.toolbar;
        for (View overlay : overlays) {
            if (overlay.getVisibility() == View.VISIBLE) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) overlay.getLayoutParams();
                params.addRule(RelativeLayout.BELOW, previousId);
                params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                params.topMargin = 16; // dp, you might want to convert from dp to px
                overlay.setLayoutParams(params);
                previousId = overlay.getId();
            }
        }
    }

    /**
     * Setup accelerometer sensor
     */
    private void setupAccelerometerSensor() {
        accelerometerSensor = new AccelerometerSensor(this);

        // Set data changed listener
        accelerometerSensor.setOnDataChangedListener((x, y, z, magnitude) ->
                runOnUiThread(() -> {
                    if (tvAccelData != null && accelerometerOverlay != null) {
                        String accelData = String.format(Locale.US,
                                "Accel:\nX: %.2f\nY: %.2f\nZ: %.2f",
                                x, y, z);
                        tvAccelData.setText(accelData);
                    }

                    // Stream sensor data to Python
                    if (sensorDataStreamer != null) {
                        sensorDataStreamer.updateAccelerometer(x, y, z, magnitude);
                    }
                })
        );

        // Initialize accelerometer sensor
        if (accelerometerSensor.initialize()) {
            Log.d(TAG, "Accelerometer sensor initialized");
            // Start listening only if enabled
            if (SensorPreferences.isAccelerometerEnabled(this)) {
                accelerometerSensor.startListening();
                if (accelerometerOverlay != null) {
                    accelerometerOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                if (accelerometerOverlay != null) {
                    accelerometerOverlay.setVisibility(View.GONE);
                    if (tvAccelData != null) {
                        tvAccelData.setText(R.string.accelerometer_disabled);
                    }
                }
            }
        } else {
            Log.w(TAG, "Accelerometer sensor not available on this device");
            if (accelerometerOverlay != null) {
                accelerometerOverlay.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Setup light sensor
     */
    private void setupLightSensor() {
        lightSensor = new LightSensor(this);

        // Set light changed listener
        lightSensor.setOnLightChangedListener((lux, category) ->
                runOnUiThread(() -> {
                    if (tvLightData != null && lightOverlay != null) {
                        String lightData = String.format(Locale.US,
                                "Light:\n%.0f lux\n%s",
                                lux, category.getName());
                        tvLightData.setText(lightData);
                    }

                    // Auto-adjust brightness based on light level
                    if (autoBrightnessEnabled) {
                        adjustScreenBrightness(lux);
                    }

                    // Stream sensor data to Python
                    if (sensorDataStreamer != null) {
                        sensorDataStreamer.updateLight(lux, category.getName());
                    }
                })
        );

        // Initialize light sensor
        if (lightSensor.initialize()) {
            Log.d(TAG, "Light sensor initialized");
            // Start listening only if enabled
            if (SensorPreferences.isLightSensorEnabled(this)) {
                lightSensor.startListening();
                autoBrightnessEnabled = true;
                if (lightOverlay != null) {
                    lightOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                if (lightOverlay != null) {
                    lightOverlay.setVisibility(View.GONE);
                    if (tvLightData != null) {
                        tvLightData.setText(R.string.light_sensor_disabled);
                    }
                }
                autoBrightnessEnabled = false;
            }
        } else {
            Log.w(TAG, "Light sensor not available on this device");
            if (lightOverlay != null) {
                lightOverlay.setVisibility(View.GONE);
            }
            autoBrightnessEnabled = false;
        }
    }

    /**
     * Setup proximity sensor
     */
    private void setupProximitySensor() {
        proximitySensor = new ProximitySensor(this);

        // Set proximity changed listener
        proximitySensor.setOnProximityChangedListener((distance, isNear) ->
                runOnUiThread(() -> {
                    if (tvProximityData != null && proximityOverlay != null) {
                        String state = isNear ? "NEAR" : "FAR";
                        String proxData = String.format(Locale.US,
                                "Proximity:\n%.1f cm\n%s",
                                distance, state);
                        tvProximityData.setText(proxData);
                    }

                    // Auto-focus on proximity detection
                    if (autoFocusOnProximityEnabled && captureSession != null && previewRequestBuilder != null) {
                        adjustCameraFocusByProximity(distance, isNear);
                    }

                    // Stream sensor data to Python
                    if (sensorDataStreamer != null) {
                        sensorDataStreamer.updateProximity(distance, isNear);
                    }
                })
        );

        // Initialize proximity sensor
        if (proximitySensor.initialize()) {
            Log.d(TAG, "Proximity sensor initialized");
            // Start listening only if enabled
            if (SensorPreferences.isProximityEnabled(this)) {
                proximitySensor.startListening();
                autoFocusOnProximityEnabled = true;
                if (proximityOverlay != null) {
                    proximityOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                if (proximityOverlay != null) {
                    proximityOverlay.setVisibility(View.GONE);
                    if (tvProximityData != null) {
                        tvProximityData.setText(R.string.proximity_disabled);
                    }
                }
                autoFocusOnProximityEnabled = false;
            }
        } else {
            Log.w(TAG, "Proximity sensor not available on this device");
            if (proximityOverlay != null) {
                proximityOverlay.setVisibility(View.GONE);
            }
            autoFocusOnProximityEnabled = false;
        }
    }

    /**
     * Setup magnetometer sensor
     */
    private void setupMagnetometerSensor() {
        magnetometerSensor = new MagnetometerSensor(this);

        // Set compass change listener
        magnetometerSensor.setOnCompassChangeListener((azimuth, direction) ->
                runOnUiThread(() -> {
                    if (tvMagnetData != null && magnetometerOverlay != null) {
                        String magnetData = String.format(Locale.US,
                                "Compass:\n%s\n%.0f°",
                                direction.getAbbreviation(), azimuth);
                        tvMagnetData.setText(magnetData);
                    }

                    // Stream sensor data to Python
                    if (sensorDataStreamer != null) {
                        sensorDataStreamer.updateMagnetometer(azimuth, direction.getName());
                    }
                })
        );

        // Initialize magnetometer sensor
        if (magnetometerSensor.initialize()) {
            Log.d(TAG, "Magnetometer sensor initialized");
            // Start listening only if enabled
            if (SensorPreferences.isMagnetometerEnabled(this)) {
                magnetometerSensor.startListening();
                if (magnetometerOverlay != null) {
                    magnetometerOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                if (magnetometerOverlay != null) {
                    magnetometerOverlay.setVisibility(View.GONE);
                    if (tvMagnetData != null) {
                        tvMagnetData.setText(R.string.magnetometer_disabled);
                    }
                }
            }
        } else {
            Log.w(TAG, "Magnetometer sensor not available on this device");
            if (magnetometerOverlay != null) {
                magnetometerOverlay.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Update sensor state when changed from Settings
     */
    private void updateSensorState(String sensorType, boolean isEnabled) {
        runOnUiThread(() -> {
            switch (sensorType) {
                case "accelerometer":
                    if (accelerometerSensor != null) {
                        if (isEnabled) {
                            accelerometerSensor.startListening();
                            if (accelerometerOverlay != null) {
                                accelerometerOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            accelerometerSensor.stopListening();
                            if (accelerometerOverlay != null) {
                                accelerometerOverlay.setVisibility(View.GONE);
                            }
                            if (tvAccelData != null) {
                                tvAccelData.setText(R.string.accelerometer_disabled);
                            }
                        }
                    }
                    break;

                case "gyroscope":
                    if (gyroscopeSensor != null) {
                        if (isEnabled) {
                            gyroscopeSensor.startListening();
                            if (gyroscopeOverlay != null) {
                                gyroscopeOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            gyroscopeSensor.stopListening();
                            if (gyroscopeOverlay != null) {
                                gyroscopeOverlay.setVisibility(View.GONE);
                            }
                            if (tvGyroData != null) {
                                tvGyroData.setText(R.string.gyroscope_disabled);
                            }
                        }
                    }
                    break;

                case "light":
                    if (lightSensor != null) {
                        if (isEnabled) {
                            lightSensor.startListening();
                            autoBrightnessEnabled = true;
                            if (lightOverlay != null) {
                                lightOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            lightSensor.stopListening();
                            autoBrightnessEnabled = false;
                            if (lightOverlay != null) {
                                lightOverlay.setVisibility(View.GONE);
                            }
                            if (tvLightData != null) {
                                tvLightData.setText(R.string.light_sensor_disabled);
                            }
                            // Reset brightness to default when disabled
                            resetScreenBrightness();
                        }
                    }
                    break;

                case "proximity":
                    if (proximitySensor != null) {
                        if (isEnabled) {
                            proximitySensor.startListening();
                            autoFocusOnProximityEnabled = true;
                            if (proximityOverlay != null) {
                                proximityOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            proximitySensor.stopListening();
                            autoFocusOnProximityEnabled = false;
                            if (proximityOverlay != null) {
                                proximityOverlay.setVisibility(View.GONE);
                            }
                            if (tvProximityData != null) {
                                tvProximityData.setText(R.string.proximity_disabled);
                            }
                        }
                    }
                    break;

                case "magnetometer":
                    if (magnetometerSensor != null) {
                        if (isEnabled) {
                            magnetometerSensor.startListening();
                            if (magnetometerOverlay != null) {
                                magnetometerOverlay.setVisibility(View.VISIBLE);
                            }
                        } else {
                            magnetometerSensor.stopListening();
                            if (magnetometerOverlay != null) {
                                magnetometerOverlay.setVisibility(View.GONE);
                            }
                            if (tvMagnetData != null) {
                                tvMagnetData.setText(R.string.magnetometer_disabled);
                            }
                        }
                    }
                    break;
            }
        });
    }

    /**
     * Hide all UI elements (for PiP mode)
     */
    private void hideAllUI() {
        if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.GONE);
        if (btnFloating != null) btnFloating.setVisibility(View.GONE);
        if (btnRecord != null) btnRecord.setVisibility(View.GONE);
        if (tvStatus != null) tvStatus.setVisibility(View.GONE);
        if (recordingIndicator != null) recordingIndicator.setVisibility(View.GONE);
        if (tvRecordingTime != null) tvRecordingTime.setVisibility(View.GONE);

        // Hide zoom controls
        if (zoomSlider != null) zoomSlider.setVisibility(View.GONE);
        if (tvZoomValue != null) tvZoomValue.setVisibility(View.GONE);
        if (btnZoomIn != null) btnZoomIn.setVisibility(View.GONE);
        if (btnZoomOut != null) btnZoomOut.setVisibility(View.GONE);

        // Hide all sensor overlays
        if (gyroscopeOverlay != null) gyroscopeOverlay.setVisibility(View.GONE);
        if (accelerometerOverlay != null) accelerometerOverlay.setVisibility(View.GONE);
        if (lightOverlay != null) lightOverlay.setVisibility(View.GONE);
        if (proximityOverlay != null) proximityOverlay.setVisibility(View.GONE);
        if (magnetometerOverlay != null) magnetometerOverlay.setVisibility(View.GONE);
    }

    /**
     * Show all UI elements (when exiting PiP mode)
     */
    private void showAllUI() {
        if (btnSwitchCamera != null) btnSwitchCamera.setVisibility(View.VISIBLE);
        if (btnFloating != null) btnFloating.setVisibility(View.VISIBLE);
        if (btnRecord != null) btnRecord.setVisibility(View.VISIBLE);
        if (recordingIndicator != null && isRecording) {
            recordingIndicator.setVisibility(View.VISIBLE);
        }

        // Show zoom controls
        if (zoomSlider != null) zoomSlider.setVisibility(View.VISIBLE);
        if (tvZoomValue != null) {
            tvZoomValue.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
        }
        if (btnZoomIn != null) btnZoomIn.setVisibility(View.VISIBLE);
        if (btnZoomOut != null) btnZoomOut.setVisibility(View.VISIBLE);

        // Show recording time if recording
        if (tvRecordingTime != null) {
            tvRecordingTime.setVisibility(isRecording ? View.VISIBLE : View.GONE);
        }

        // Show sensor overlays based on preferences
        if (gyroscopeOverlay != null && SensorPreferences.isGyroscopeEnabled(this)) {
            gyroscopeOverlay.setVisibility(View.VISIBLE);
        }
        if (accelerometerOverlay != null && SensorPreferences.isAccelerometerEnabled(this)) {
            accelerometerOverlay.setVisibility(View.VISIBLE);
        }
        if (lightOverlay != null && SensorPreferences.isLightSensorEnabled(this)) {
            lightOverlay.setVisibility(View.VISIBLE);
        }
        if (proximityOverlay != null && SensorPreferences.isProximityEnabled(this)) {
            proximityOverlay.setVisibility(View.VISIBLE);
        }
        if (magnetometerOverlay != null && SensorPreferences.isMagnetometerEnabled(this)) {
            magnetometerOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            tvStatus.setText(R.string.permission_already_granted);
            startBackgroundThread();
            if (textureView.isAvailable()) {
                openCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText(R.string.camera_permission_granted);
                startBackgroundThread();
                if (textureView.isAvailable()) {
                    openCamera();
                }
            } else {
                tvStatus.setText(R.string.camera_permission_missing);
                Toast.makeText(this, R.string.camera_permission_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera opened successfully");
            cameraDevice = camera;
            runOnUiThread(() -> tvStatus.setText(R.string.camera_opened));
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera disconnected");
            camera.close();
            cameraDevice = null;
            runOnUiThread(() -> tvStatus.setText(R.string.camera_disconnected));
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            camera.close();
            cameraDevice = null;
            runOnUiThread(() -> {
                tvStatus.setText(getString(R.string.camera_error, error));
                Toast.makeText(CameraActivity.this, "Camera error: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    };

    private void openCamera() {
        if (cameraManager == null) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        if (cameraManager == null) {
            Log.e(TAG, "openCamera: CameraManager is null");
            runOnUiThread(() -> Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show());
            return;
        }

        try {
            String cameraId = chooseCameraId();
            if (cameraId == null) {
                Log.e(TAG, "openCamera: no camera available");
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "openCamera: camera permission not granted");
                return;
            }

            Log.d(TAG, "Opening camera: " + cameraId);
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "openCamera error: ", e);
            runOnUiThread(() -> Toast.makeText(this, "Failed to open camera", Toast.LENGTH_LONG).show());
        }
    }

    private String chooseCameraId() throws CameraAccessException {
        if (cameraManager == null) return null;

        String[] cameraIdList = cameraManager.getCameraIdList();
        if (cameraIdList.length == 0) return null;

        for (String id : cameraIdList) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing == null) continue;
            if (isFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) return id;
            if (!isFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
        }

        return cameraIdList[0];
    }

    private void createCameraPreviewSession() {
        try {
            if (cameraDevice == null || !textureView.isAvailable()) {
                Log.w(TAG, "Cannot create preview session - camera or texture not ready");
                return;
            }

            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                Log.w(TAG, "SurfaceTexture is null");
                return;
            }

            // Set buffer size to 1920x1080
            texture.setDefaultBufferSize(1920, 1080);

            Surface surface = new Surface(texture);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) return;

                            captureSession = session;
                            try {
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                                        CaptureRequest.CONTROL_AWB_MODE_AUTO);

                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                                Log.d(TAG, "✅ Preview started - YOU SHOULD SEE THE CAMERA NOW!");

                                runOnUiThread(() -> {
                                    tvStatus.setVisibility(View.GONE);
                                    Toast.makeText(CameraActivity.this,
                                            "✅ Camera preview is ACTIVE!", Toast.LENGTH_LONG).show();
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to start preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure camera session");
                        }
                    }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "createCameraPreviewSession error: ", e);
        }
    }

    private void closeCamera() {
        Log.d(TAG, "Closing camera");
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "closeCamera error: ", e);
        }
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.w(TAG, "stopBackgroundThread interrupted", e);
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void switchCamera() {
        isFrontCamera = !isFrontCamera;
        closeCamera();
        if (textureView != null) {
            textureView.postDelayed(this::openCamera, 300);
        }
    }


    /**
     * Reset screen brightness to system default
     */
    private void resetScreenBrightness() {
        try {
            Window window = getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                // Set to -1 to use system brightness
                layoutParams.screenBrightness = -1f;
                window.setAttributes(layoutParams);
                Log.d(TAG, "Screen brightness reset to system default");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting brightness: " + e.getMessage());
        }
    }

    /**
     * Adjust screen brightness based on ambient light level
     * Converts lux values to brightness level (0.0 - 1.0)
     */
    private void adjustScreenBrightness(float lux) {
        try {
            // Map lux values to brightness level
            // 0 lux -> 0.2 brightness (minimum readable)
            // 100 lux -> 0.5 brightness
            // 1000 lux -> 0.8 brightness
            // 10000+ lux -> 1.0 brightness (maximum)

            float brightnessLevel;

            if (lux <= 0) {
                brightnessLevel = 0.2f;
            } else if (lux <= 100) {
                // Linear interpolation from 0.2 to 0.5
                brightnessLevel = 0.2f + (lux / 100f) * 0.3f;
            } else if (lux <= 1000) {
                // Linear interpolation from 0.5 to 0.8
                brightnessLevel = 0.5f + ((lux - 100) / 900f) * 0.3f;
            } else if (lux <= 10000) {
                // Linear interpolation from 0.8 to 1.0
                brightnessLevel = 0.8f + ((lux - 1000) / 9000f) * 0.2f;
            } else {
                // Maximum brightness
                brightnessLevel = 1.0f;
            }

            // Clamp brightness between 0.2 and 1.0
            brightnessLevel = Math.max(0.2f, Math.min(1.0f, brightnessLevel));

            // Apply brightness to window
            Window window = getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.screenBrightness = brightnessLevel;
                window.setAttributes(layoutParams);

                Log.d(TAG, String.format(Locale.US,
                        "Auto Brightness: %.0f lux -> %.2f brightness", lux, brightnessLevel));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting brightness: " + e.getMessage());
        }
    }

    /**
     * Start floating camera window
     */
    private void startFloatingCamera() {
        Log.d(TAG, "🟡 Starting floating camera...");

        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "⚠️ Overlay permission not granted");
            // Request overlay permission
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Floating camera needs permission to display over other apps")
                    .setPositiveButton("Grant", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        // Use ActivityResultLauncher to request permission
                        overlayPermissionLauncher.launch(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ Camera permission not granted");
            Toast.makeText(this, "Camera permission required for floating camera", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Start floating camera service
            Intent serviceIntent = new Intent(this, FloatingCameraService.class);
            startForegroundService(serviceIntent);

            Log.d(TAG, "✅ Floating camera service started");
            Toast.makeText(this, "Floating camera started", Toast.LENGTH_SHORT).show();

            // Optionally minimize the app
            moveTaskToBack(true);

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start floating camera", e);
            Toast.makeText(this, "Failed to start floating camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // onActivityResult is no longer used for overlay permission result
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable() && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }

        // Start sensor listening based on preferences
        restartSensors();

        // Update UI visibility
        showAllUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();

        // Stop all sensor listening
        stopAllSensors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
        stopBackgroundThread();

        // Stop sensor data streamer
        if (sensorDataStreamer != null) {
            sensorDataStreamer.stop();
        }

        // Unregister broadcast receiver
        try {
            if (sensorStateReceiver != null) {
                unregisterReceiver(sensorStateReceiver);
            }
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
            Log.d(TAG, "Receiver not registered: " + e.getMessage());
        }
        if (streamManager != null && streamManager.isStreaming()) {
            streamManager.stopStreaming();
        }
        // Clean up all sensors
        if (accelerometerSensor != null) accelerometerSensor.stopListening();
        if (gyroscopeSensor != null) gyroscopeSensor.stopListening();
        if (lightSensor != null) lightSensor.stopListening();
        if (proximitySensor != null) proximitySensor.stopListening();
        if (magnetometerSensor != null) magnetometerSensor.stopListening();
    }

    /**
     * Restart sensors based on preference settings
     */
    private void restartSensors() {
        if (accelerometerSensor != null && SensorPreferences.isAccelerometerEnabled(this)) {
            accelerometerSensor.startListening();
        }
        if (gyroscopeSensor != null && SensorPreferences.isGyroscopeEnabled(this)) {
            gyroscopeSensor.startListening();
        }
        if (lightSensor != null && SensorPreferences.isLightSensorEnabled(this)) {
            lightSensor.startListening();
        }
        if (proximitySensor != null && SensorPreferences.isProximityEnabled(this)) {
            proximitySensor.startListening();
        }
        if (magnetometerSensor != null && SensorPreferences.isMagnetometerEnabled(this)) {
            magnetometerSensor.startListening();
        }
    }

    /**
     * Stop all active sensors
     */
    private void stopAllSensors() {
        if (accelerometerSensor != null) accelerometerSensor.stopListening();
        if (gyroscopeSensor != null) gyroscopeSensor.stopListening();
        if (lightSensor != null) lightSensor.stopListening();
        if (proximitySensor != null) proximitySensor.stopListening();
        if (magnetometerSensor != null) magnetometerSensor.stopListening();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adjust camera focus based on proximity distance
     * Uses proximity data to automatically trigger focus on detected faces/objects
     */
    private void adjustCameraFocusByProximity(float distanceCm, boolean isNear) {
        try {
            if (captureSession == null || previewRequestBuilder == null || cameraDevice == null) {
                return;
            }

            if (isNear) {
                // Object is near - trigger autofocus
                Log.d(TAG, String.format(Locale.US,
                        "Auto Focus: Object detected at %.1f cm - focusing...", distanceCm));

                // Trigger autofocus on detected object
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_START);

                // Update to continuous autofocus for tracking
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                try {
                    captureSession.capture(previewRequestBuilder.build(), null, backgroundHandler);

                    // Reset trigger after focus attempt
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);

                } catch (CameraAccessException e) {
                    Log.w(TAG, "Could not trigger focus: " + e.getMessage());
                }
            } else {
                // Object is far - maintain continuous focus
                Log.d(TAG, String.format(Locale.US,
                        "Auto Focus: Object far (%.1f cm) - continuous focus active", distanceCm));

                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                try {
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                } catch (CameraAccessException e) {
                    Log.w(TAG, "Could not set continuous focus: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting camera focus by proximity: " + e.getMessage());
        }
    }

    /**
     * Reset camera focus to default continuous mode
     */
    private void resetCameraFocus() {
        try {
            if (captureSession == null || previewRequestBuilder == null) {
                return;
            }

            // Reset to continuous autofocus
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Cancel any active focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);

            Log.d(TAG, "Camera focus reset to continuous autofocus mode");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error resetting camera focus: " + e.getMessage());
        }
    }

    /**
     * Initialize zoom controls
     */
    private void initializeZoomControls() {
        // Set up zoom slider
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Convert progress (0-100) to zoom level (minZoom - maxZoom)
                    float zoom = minZoomLevel + ((maxZoomLevel - minZoomLevel) * progress / 100f);
                    setZoomLevel(zoom);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Visual feedback when user starts dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional: Visual feedback when user stops dragging
            }
        });

        // Set up zoom in button
        btnZoomIn.setOnClickListener(v -> {
            float newZoom = Math.min(currentZoomLevel + 0.5f, maxZoomLevel);
            setZoomLevel(newZoom);
            updateZoomSliderPosition(newZoom);
        });

        // Set up zoom out button
        btnZoomOut.setOnClickListener(v -> {
            float newZoom = Math.max(currentZoomLevel - 0.5f, minZoomLevel);
            setZoomLevel(newZoom);
            updateZoomSliderPosition(newZoom);
        });

        // Add pinch-to-zoom gesture
        textureView.setOnTouchListener(new View.OnTouchListener() {
            private float startDistance = 0f;
            private float startZoom = 1.0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 2) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            startDistance = getFingerDistance(event);
                            startZoom = currentZoomLevel;
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float currentDistance = getFingerDistance(event);
                            if (startDistance > 0) {
                                float scale = currentDistance / startDistance;
                                float newZoom = startZoom * scale;
                                newZoom = Math.max(minZoomLevel, Math.min(maxZoomLevel, newZoom));
                                setZoomLevel(newZoom);
                                updateZoomSliderPosition(newZoom);
                            }
                            break;
                    }
                    return true;
                }
                return false;
            }

            private float getFingerDistance(MotionEvent event) {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            }
        });
    }

    /**
     * Set zoom level and update camera
     */
    private void setZoomLevel(float zoomLevel) {
        if (zoomLevel < minZoomLevel || zoomLevel > maxZoomLevel) {
            return;
        }

        currentZoomLevel = zoomLevel;

        // Update UI
        runOnUiThread(() -> {
            String zoomText = String.format(Locale.US, "%.1fx", zoomLevel);
            tvZoomValue.setText(zoomText);

            // Show/hide zoom value temporarily
            tvZoomValue.setVisibility(View.VISIBLE);
            tvZoomValue.postDelayed(() -> {
                if (!isRecording) { // Keep visible during recording
                    tvZoomValue.setVisibility(View.INVISIBLE);
                }
            }, 1000);
        });

        // Apply zoom to camera
        applyZoomToCamera();
    }

    /**
     * Update zoom slider position based on zoom level
     */
    private void updateZoomSliderPosition(float zoomLevel) {
        int progress = (int) ((zoomLevel - minZoomLevel) * 100 / (maxZoomLevel - minZoomLevel));
        zoomSlider.setProgress(progress);
    }

    /**
     * Apply zoom to camera using Camera2 API
     */
    private void applyZoomToCamera() {
        if (captureSession == null || previewRequestBuilder == null) {
            return;
        }

        try {
            // Get camera characteristics to determine sensor size
            String cameraId = chooseCameraId();
            if (cameraId == null) return;

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            if (sensorArraySize == null) {
                return;
            }

            // Calculate zoom rectangle
            int width = sensorArraySize.width();
            int height = sensorArraySize.height();

            // Center of the sensor
            int centerX = width / 2;
            int centerY = height / 2;

            // Calculate zoomed area
            int cropWidth = (int) (width / currentZoomLevel);
            int cropHeight = (int) (height / currentZoomLevel);

            // Ensure crop area is within bounds
            int left = Math.max(0, centerX - cropWidth / 2);
            int top = Math.max(0, centerY - cropHeight / 2);
            int right = Math.min(width, left + cropWidth);
            int bottom = Math.min(height, top + cropHeight);

            zoomRect = new Rect(left, top, right, bottom);

            // Apply zoom by setting crop region
            previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

            // Update preview
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);

            Log.d(TAG, "Zoom applied: " + currentZoomLevel + "x, Crop: " + zoomRect);

        } catch (Exception e) {
            Log.e(TAG, "Error applying zoom: " + e.getMessage());
        }
    }

    /**
     * Toggle recording with visual feedback
     */
    private void toggleRecording() {
        isRecording = !isRecording;

        runOnUiThread(() -> {
            if (isRecording) {
                // Start recording
                startRecording();

                // Update recording button (make it a square when recording)
                if (btnRecord != null) {
                    // You can change the icon to a square or add animation
                    btnRecord.setImageResource(R.drawable.ic_stop_circle);
                    // Add scale animation
                    btnRecord.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(200)
                            .start();
                }

                // Show recording indicator and time
                recordingIndicator.setVisibility(View.VISIBLE);
                tvRecordingTime.setVisibility(View.VISIBLE);
                recordingStartTime = System.currentTimeMillis();
                startRecordingTimer();

                // Keep zoom value visible during recording
                tvZoomValue.setVisibility(View.VISIBLE);

                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } else {
                // Stop recording
                stopRecording();

                // Reset recording button
                if (btnRecord != null) {
                    btnRecord.setImageResource(R.drawable.ic_record_circle);
                    btnRecord.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                }

                // Hide recording indicator and time
                recordingIndicator.setVisibility(View.GONE);
                tvRecordingTime.setVisibility(View.GONE);
                stopRecordingTimer();

                // Hide zoom value after delay
                tvZoomValue.postDelayed(() -> {
                    tvZoomValue.setVisibility(View.INVISIBLE);
                }, 1000);

                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Start recording timer
     */
    private void startRecordingTimer() {
        recordingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsedTime = System.currentTimeMillis() - recordingStartTime;
                    long seconds = (elapsedTime / 1000) % 60;
                    long minutes = (elapsedTime / (1000 * 60)) % 60;

                    String timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                    tvRecordingTime.setText(timeText);

                    // Schedule next update in 1 second
                    recordingTimeHandler.postDelayed(this, 1000);
                }
            }
        };

        recordingTimeHandler.post(recordingTimeRunnable);
    }

    /**
     * Stop recording timer
     */
    private void stopRecordingTimer() {
        if (recordingTimeHandler != null && recordingTimeRunnable != null) {
            recordingTimeHandler.removeCallbacks(recordingTimeRunnable);
        }
        tvRecordingTime.setText("00:00");
    }

    /**
     * Start recording (implement your actual recording logic here)
     */
    private void startRecording() {
        // TODO: Implement actual recording logic
        Log.d(TAG, "Recording started at zoom: " + currentZoomLevel + "x");
    }

    /**
     * Stop recording (implement your actual recording logic here)
     */
    private void stopRecording() {
        // TODO: Implement actual recording logic
        Log.d(TAG, "Recording stopped");
    }
}

