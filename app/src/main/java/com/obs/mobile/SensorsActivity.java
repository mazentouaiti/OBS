package com.obs.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

// Import sensor classes
import com.obs.mobile.sensors.AccelerometerSensor;
import com.obs.mobile.sensors.GyroscopeSensor;
import com.obs.mobile.sensors.LightSensor;
import com.obs.mobile.sensors.ProximitySensor;
import com.obs.mobile.sensors.MagnetometerSensor;
import com.obs.mobile.utils.SensorPreferences;

import java.util.Locale;

/**
 * SensorsActivity - Sensor Settings and Monitoring Screen
 */
public class SensorsActivity extends AppCompatActivity {

    // Sensor class instances
    private AccelerometerSensor accelerometerSensor;
    private GyroscopeSensor gyroscopeSensor;
    private LightSensor lightSensor;
    private ProximitySensor proximitySensor;
    private MagnetometerSensor magnetometerSensor;

    // Switches for each sensor
    private Switch switchAccelerometer;
    private Switch switchGyroscope;
    private Switch switchLight;
    private Switch switchProximity;
    private Switch switchMagnetometer;

    // TextViews to display sensor data
    private TextView tvAccelData;
    private TextView tvGyroData;
    private TextView tvLightData;
    private TextView tvProximityData;
    private TextView tvMagnetData;
    private TextView tvHeader;

    private boolean autoBrightnessEnabled = false;

    private static final String TAG = "SensorsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sensor Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        initializeViews();

        // Create sensor instances
        createSensorInstances();

        // Setup listeners
        setupSensorSwitches();

        // Update header with sensor availability
        updateHeader();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        // Switches
        switchAccelerometer = findViewById(R.id.switch_accelerometer);
        switchGyroscope = findViewById(R.id.switch_gyroscope);
        switchLight = findViewById(R.id.switch_light);
        switchProximity = findViewById(R.id.switch_proximity);
        switchMagnetometer = findViewById(R.id.switch_magnetometer);

        // Data TextViews
        tvAccelData = findViewById(R.id.tv_accel_data);
        tvGyroData = findViewById(R.id.tv_gyro_data);
        tvLightData = findViewById(R.id.tv_light_data);
        tvProximityData = findViewById(R.id.tv_proximity_data);
        tvMagnetData = findViewById(R.id.tv_magnet_data);
        tvHeader = findViewById(R.id.tv_header);

        // Restore saved sensor states from preferences
        restoreSensorStates();
    }

    /**
     * Restore sensor states from preferences
     */
    private void restoreSensorStates() {
        // All sensors should start DISABLED
        switchAccelerometer.setChecked(false);
        switchGyroscope.setChecked(false);
        switchLight.setChecked(false);
        switchProximity.setChecked(false);
        switchMagnetometer.setChecked(false);

        // Save the disabled state to preferences
        SensorPreferences.setAccelerometerEnabled(this, false);
        SensorPreferences.setGyroscopeEnabled(this, false);
        SensorPreferences.setLightSensorEnabled(this, false);
        SensorPreferences.setProximityEnabled(this, false);
        SensorPreferences.setMagnetometerEnabled(this, false);

        // Send initial broadcasts to ensure CameraActivity knows all sensors are disabled
        sendSensorBroadcast("accelerometer", false);
        sendSensorBroadcast("gyroscope", false);
        sendSensorBroadcast("light", false);
        sendSensorBroadcast("proximity", false);
        sendSensorBroadcast("magnetometer", false);
    }

    /**
     * Send broadcast to CameraActivity about sensor state change
     */
    private void sendSensorBroadcast(String sensorType, boolean isEnabled) {
        Intent intent = new Intent("com.obs.mobile.SENSOR_STATE_CHANGED");
        intent.putExtra("sensor_type", sensorType);
        intent.putExtra("is_enabled", isEnabled);
        sendBroadcast(intent);
    }

    /**
     * Create sensor class instances
     */
    private void createSensorInstances() {
        accelerometerSensor = new AccelerometerSensor(this);
        gyroscopeSensor = new GyroscopeSensor(this);
        lightSensor = new LightSensor(this);
        proximitySensor = new ProximitySensor(this);
        magnetometerSensor = new MagnetometerSensor(this);
    }

    /**
     * Update header with sensor availability info
     */
    private void updateHeader() {
        int enabledCount = SensorPreferences.getEnabledSensorCount(this);
        int availableCount = getAvailableSensorCount();

        String headerText = String.format(Locale.US,
                "Sensor Dashboard\nEnabled: %d/%d | Available: %d/5",
                enabledCount, 5, availableCount);

        tvHeader.setText(headerText);
    }

    /**
     * Count how many sensors are available on this device
     */
    private int getAvailableSensorCount() {
        int count = 0;
        if (accelerometerSensor.isAvailable()) count++;
        if (gyroscopeSensor.isAvailable()) count++;
        if (lightSensor.isAvailable()) count++;
        if (proximitySensor.isAvailable()) count++;
        if (magnetometerSensor.isAvailable()) count++;
        return count;
    }

    /**
     * Setup sensor enable/disable switches with complete implementations
     */
    private void setupSensorSwitches() {
        // Accelerometer - Student 1
        switchAccelerometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorPreferences.setAccelerometerEnabled(this, isChecked);

            // Send broadcast to CameraActivity
            sendSensorBroadcast("accelerometer", isChecked);

            if (isChecked) {
                if (accelerometerSensor.initialize()) {
                    // Set shake detection callback
                    accelerometerSensor.setOnShakeListener(intensity -> {
                        runOnUiThread(() -> {
                            String shakeMsg = String.format(Locale.US,
                                    "SHAKE DETECTED! Intensity: %.2f m/s²", intensity);
                            tvAccelData.setText(shakeMsg);
                        });
                    });

                    // Set data changed callback
                    accelerometerSensor.setOnDataChangedListener(
                            (x, y, z, magnitude) -> {
                                runOnUiThread(() -> {
                                    String data = String.format(Locale.US,
                                            "X: %.2f | Y: %.2f | Z: %.2f m/s²\n" +
                                                    "Magnitude: %.2f m/s²",
                                            x, y, z, magnitude);
                                    tvAccelData.setText(data);
                                });
                            }
                    );
                    accelerometerSensor.startListening();
                    tvAccelData.setText("Accelerometer: Active\nShake to test!");
                } else {
                    tvAccelData.setText("Accelerometer: Not available");
                    switchAccelerometer.setChecked(false);
                    // Send correction broadcast
                    sendSensorBroadcast("accelerometer", false);
                }
            } else {
                if (accelerometerSensor != null) {
                    accelerometerSensor.stopListening();
                }
                tvAccelData.setText("Accelerometer: Disabled");
            }
            updateHeader();
        });

        // Gyroscope - Student 2
        switchGyroscope.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorPreferences.setGyroscopeEnabled(this, isChecked);

            // Send broadcast to CameraActivity
            sendSensorBroadcast("gyroscope", isChecked);

            if (isChecked) {
                if (gyroscopeSensor.initialize()) {
                    // Set rotation gesture callback
                    gyroscopeSensor.setOnRotationGestureListener(
                            (degreesPerSecond, clockwise) -> {
                                runOnUiThread(() -> {
                                    String direction = clockwise ? "Clockwise" : "Counter-clockwise";
                                    String gestureMsg = String.format(Locale.US,
                                            "FAST ROTATION!\nSpeed: %.1f°/s (%s)",
                                            degreesPerSecond, direction);
                                    tvGyroData.setText(gestureMsg);
                                });
                            }
                    );

                    // Set rotation data callback
                    gyroscopeSensor.setOnRotationListener(
                            (rotationX, rotationY, rotationZ) -> {
                                runOnUiThread(() -> {
                                    float xDeg = GyroscopeSensor.radiansToDegrees(rotationX);
                                    float yDeg = GyroscopeSensor.radiansToDegrees(rotationY);
                                    float zDeg = GyroscopeSensor.radiansToDegrees(rotationZ);

                                    String data = String.format(Locale.US,
                                            "X: %.1f°/s\nY: %.1f°/s\nZ: %.1f°/s",
                                            xDeg, yDeg, zDeg);
                                    tvGyroData.setText(data);
                                });
                            }
                    );
                    gyroscopeSensor.startListening();
                    tvGyroData.setText("Gyroscope: Active\nRotate to test!");
                } else {
                    tvGyroData.setText("Gyroscope: Not available");
                    switchGyroscope.setChecked(false);
                    // Send correction broadcast
                    sendSensorBroadcast("gyroscope", false);
                }
            } else {
                if (gyroscopeSensor != null) {
                    gyroscopeSensor.stopListening();
                }
                tvGyroData.setText("Gyroscope: Disabled");
            }
            updateHeader();
        });

        // Light Sensor - Student 3
        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorPreferences.setLightSensorEnabled(this, isChecked);

            // Send broadcast to CameraActivity
            sendSensorBroadcast("light", isChecked);

            if (isChecked) {
                if (lightSensor.initialize()) {
                    lightSensor.setOnLightChangedListener(
                            (lux, category) -> {
                                runOnUiThread(() -> {
                                    String recommendation = lightSensor.getCameraRecommendation(lux);
                                    String data = String.format(Locale.US,
                                            "Light: %.0f lux\n" +
                                                    "Category: %s\n" +
                                                    "Recommendation: %s",
                                            lux, category.getName(), recommendation);
                                    tvLightData.setText(data);

                                    // Auto-adjust brightness based on light level
                                    if (autoBrightnessEnabled) {
                                        adjustScreenBrightness(lux);
                                    }
                                });
                            }
                    );
                    lightSensor.startListening();
                    autoBrightnessEnabled = true;
                    tvLightData.setText("Light Sensor: Active\nMove to test!");
                } else {
                    tvLightData.setText("Light Sensor: Not available");
                    switchLight.setChecked(false);
                    // Send correction broadcast
                    sendSensorBroadcast("light", false);
                    autoBrightnessEnabled = false;
                }
            } else {
                if (lightSensor != null) {
                    lightSensor.stopListening();
                }
                tvLightData.setText("Light Sensor: Disabled");
                autoBrightnessEnabled = false;
                // Reset brightness when disabled
                resetScreenBrightness();
            }
            updateHeader();
        });

        // Proximity Sensor - Student 4
        switchProximity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorPreferences.setProximityEnabled(this, isChecked);

            // Send broadcast to CameraActivity
            sendSensorBroadcast("proximity", isChecked);

            if (isChecked) {
                if (proximitySensor.initialize()) {
                    proximitySensor.setOnProximityChangedListener(
                            (distance, isNear) -> {
                                runOnUiThread(() -> {
                                    String state = isNear ? "NEAR" : "FAR";
                                    String data = String.format(Locale.US,
                                            "Distance: %.1f cm\n" +
                                                    "State: %s",
                                            distance, state);
                                    tvProximityData.setText(data);
                                });
                            }
                    );

                    proximitySensor.setOnNearListener(() -> {
                        runOnUiThread(() -> {
                            tvProximityData.setText("OBJECT NEAR!\n" + tvProximityData.getText());
                        });
                    });

                    proximitySensor.setOnFarListener(() -> {
                        runOnUiThread(() -> {
                            tvProximityData.setText("OBJECT FAR!\n" + tvProximityData.getText());
                        });
                    });

                    proximitySensor.startListening();
                    tvProximityData.setText("Proximity: Active\nCover sensor\n⭐ Auto-focus enabled in Camera");
                } else {
                    tvProximityData.setText("Proximity: Not available");
                    switchProximity.setChecked(false);
                    // Send correction broadcast
                    sendSensorBroadcast("proximity", false);
                }
            } else {
                if (proximitySensor != null) {
                    proximitySensor.stopListening();
                }
                tvProximityData.setText("Proximity: Disabled");
            }
            updateHeader();
        });

        // Magnetometer - Student 5
        switchMagnetometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorPreferences.setMagnetometerEnabled(this, isChecked);

            // Send broadcast to CameraActivity
            sendSensorBroadcast("magnetometer", isChecked);

            if (isChecked) {
                if (magnetometerSensor.initialize()) {
                    magnetometerSensor.setOnCompassChangeListener(
                            (azimuth, direction) -> {
                                runOnUiThread(() -> {
                                    String data = String.format(Locale.US,
                                            "Direction: %s (%s)\n" +
                                                    "Azimuth: %.0f°",
                                            direction.getName(),
                                            direction.getAbbreviation(),
                                            azimuth);
                                    tvMagnetData.setText(data);
                                });
                            }
                    );

                    magnetometerSensor.setOnDirectionChangeListener(direction -> {
                        runOnUiThread(() -> {
                            String dirMsg = String.format(Locale.US,
                                    "New Direction: %s",
                                    direction.getName());
                            tvMagnetData.setText(dirMsg);
                        });
                    });

                    magnetometerSensor.startListening();
                    tvMagnetData.setText("Magnetometer: Active\nMove to test!");
                } else {
                    tvMagnetData.setText("Magnetometer: Not available");
                    switchMagnetometer.setChecked(false);
                    // Send correction broadcast
                    sendSensorBroadcast("magnetometer", false);
                }
            } else {
                if (magnetometerSensor != null) {
                    magnetometerSensor.stopListening();
                }
                tvMagnetData.setText("Magnetometer: Disabled");
            }
            updateHeader();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop all active sensors
        stopAllSensors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart sensors based on switch states
        restartSensors();
        updateHeader();
    }

    /**
     * Stop all active sensors
     */
    private void stopAllSensors() {
        if (switchAccelerometer.isChecked() && accelerometerSensor != null) {
            accelerometerSensor.stopListening();
        }
        if (switchGyroscope.isChecked() && gyroscopeSensor != null) {
            gyroscopeSensor.stopListening();
        }
        if (switchLight.isChecked() && lightSensor != null) {
            lightSensor.stopListening();
        }
        if (switchProximity.isChecked() && proximitySensor != null) {
            proximitySensor.stopListening();
        }
        if (switchMagnetometer.isChecked() && magnetometerSensor != null) {
            magnetometerSensor.stopListening();
        }
    }

    /**
     * Restart sensors based on switch states
     */
    private void restartSensors() {
        if (switchAccelerometer.isChecked() && accelerometerSensor != null) {
            accelerometerSensor.startListening();
        }
        if (switchGyroscope.isChecked() && gyroscopeSensor != null) {
            gyroscopeSensor.startListening();
        }
        if (switchLight.isChecked() && lightSensor != null) {
            lightSensor.startListening();
        }
        if (switchProximity.isChecked() && proximitySensor != null) {
            proximitySensor.startListening();
        }
        if (switchMagnetometer.isChecked() && magnetometerSensor != null) {
            magnetometerSensor.startListening();
        }
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
}
