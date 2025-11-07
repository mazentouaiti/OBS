package com.obs.mobile;

import android.os.Bundle;
import android.view.MenuItem;
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

/**
 * SensorsActivity - Sensor Settings and Monitoring Screen
 *
 * This screen displays sensor status and allows configuration.
 * Now uses independent sensor classes - students implement the classes,
 * then use switches here to enable/disable and monitor data.
 */
public class SensorsActivity extends AppCompatActivity {

    // Sensor class instances
    private AccelerometerSensor accelerometerSensor;
    private GyroscopeSensor gyroscopeSensor;
    private LightSensor lightSensor;
    private ProximitySensor proximitySensor;
    private MagnetometerSensor magnetometerSensor;

    // Placeholder switches for each sensor
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
     * Setup sensor enable/disable switches
     */
    private void setupSensorSwitches() {
        /* TODO (Student 1 - Accelerometer):
         * ===================================
         * Implement accelerometer switch functionality
         *
         * When switch is ON:
         * - Initialize sensor
         * - Set data listener to update tvAccelData
         * - Start listening
         *
         * When switch is OFF:
         * - Stop listening
         */
        switchAccelerometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (accelerometerSensor.initialize()) {
                    // Set callback to display data
                    accelerometerSensor.setOnDataChangedListener(
                        new AccelerometerSensor.OnDataChangedListener() {
                            @Override
                            public void onDataChanged(float x, float y, float z, float magnitude) {
                                runOnUiThread(() -> {
                                    String data = String.format("X: %.2f | Y: %.2f | Z: %.2f\nMagnitude: %.2f m/s²",
                                        x, y, z, magnitude);
                                    tvAccelData.setText(data);
                                });
                            }
                        }
                    );
                    accelerometerSensor.startListening();
                    tvAccelData.setText("Accelerometer: Active - Waiting for data...");
                } else {
                    tvAccelData.setText("Accelerometer: Not available on this device");
                    switchAccelerometer.setChecked(false);
                }
            } else {
                accelerometerSensor.stopListening();
                tvAccelData.setText("Accelerometer: Disabled");
            }
        });

        /* TODO (Student 2 - Gyroscope):
         * ===============================
         * Implement gyroscope switch functionality
         */
        switchGyroscope.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (gyroscopeSensor.initialize()) {
                    gyroscopeSensor.setOnRotationListener(
                        new GyroscopeSensor.OnRotationListener() {
                            @Override
                            public void onRotation(float rotationX, float rotationY, float rotationZ) {
                                runOnUiThread(() -> {
                                    String data = String.format("X: %.2f rad/s | Y: %.2f rad/s | Z: %.2f rad/s",
                                        rotationX, rotationY, rotationZ);
                                    tvGyroData.setText(data);
                                });
                            }
                        }
                    );
                    gyroscopeSensor.startListening();
                    tvGyroData.setText("Gyroscope: Active - Waiting for data...");
                } else {
                    tvGyroData.setText("Gyroscope: Not available on this device");
                    switchGyroscope.setChecked(false);
                }
            } else {
                gyroscopeSensor.stopListening();
                tvGyroData.setText("Gyroscope: Disabled");
            }
        });

        /* TODO (Student 3 - Light Sensor):
         * ==================================
         * Implement light sensor switch functionality
         */
        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (lightSensor.initialize()) {
                    lightSensor.setOnLightChangedListener(
                        new LightSensor.OnLightChangedListener() {
                            @Override
                            public void onLightChanged(float lux, LightSensor.LightCategory category) {
                                runOnUiThread(() -> {
                                    String data = String.format("Light: %.0f lux\nCategory: %s (%s)",
                                        lux, category.getName(), category.getRange());
                                    tvLightData.setText(data);
                                });
                            }
                        }
                    );
                    lightSensor.startListening();
                    tvLightData.setText("Light Sensor: Active - Waiting for data...");
                } else {
                    tvLightData.setText("Light Sensor: Not available on this device");
                    switchLight.setChecked(false);
                }
            } else {
                lightSensor.stopListening();
                tvLightData.setText("Light Sensor: Disabled");
            }
        });

        /* TODO (Student 4 - Proximity Sensor):
         * ======================================
         * Implement proximity sensor switch functionality
         */
        switchProximity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (proximitySensor.initialize()) {
                    proximitySensor.setOnProximityChangedListener(
                        new ProximitySensor.OnProximityChangedListener() {
                            @Override
                            public void onProximityChanged(float distance, boolean isNear) {
                                runOnUiThread(() -> {
                                    String state = isNear ? "NEAR" : "FAR";
                                    String data = String.format("Distance: %.1f cm\nState: %s",
                                        distance, state);
                                    tvProximityData.setText(data);
                                });
                            }
                        }
                    );
                    proximitySensor.startListening();
                    tvProximityData.setText("Proximity: Active - Waiting for data...");
                } else {
                    tvProximityData.setText("Proximity: Not available on this device");
                    switchProximity.setChecked(false);
                }
            } else {
                proximitySensor.stopListening();
                tvProximityData.setText("Proximity: Disabled");
            }
        });

        /* TODO (Student 5 - Magnetometer):
         * ==================================
         * Implement magnetometer switch functionality
         */
        switchMagnetometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (magnetometerSensor.initialize()) {
                    magnetometerSensor.setOnCompassChangeListener(
                        new MagnetometerSensor.OnCompassChangeListener() {
                            @Override
                            public void onCompassChange(float azimuth,
                                                       MagnetometerSensor.CompassDirection direction) {
                                runOnUiThread(() -> {
                                    String data = String.format("Direction: %s (%s)\nAzimuth: %.0f°",
                                        direction.getName(),
                                        direction.getAbbreviation(),
                                        azimuth);
                                    tvMagnetData.setText(data);
                                });
                            }
                        }
                    );
                    magnetometerSensor.startListening();
                    tvMagnetData.setText("Magnetometer: Active - Waiting for data...");
                } else {
                    tvMagnetData.setText("Magnetometer: Not available on this device");
                    switchMagnetometer.setChecked(false);
                }
            } else {
                magnetometerSensor.stopListening();
                tvMagnetData.setText("Magnetometer: Disabled");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop all active sensors
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

    @Override
    protected void onResume() {
        super.onResume();
        // Restart sensors based on switch states
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
}
