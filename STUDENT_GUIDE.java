/*
 * ========================================================================
 * OBS MOBILE - STUDENT IMPLEMENTATION GUIDE
 * ========================================================================
 *
 * This file provides detailed implementation steps for all 5 students.
 * Read this carefully before starting your implementation.
 */

// ========================================================================
// STUDENT 1 - ACCELEROMETER IMPLEMENTATION GUIDE
// ========================================================================

/*
 * OBJECTIVE: Implement shake detection to control recording
 *
 * STEP 1: Declare class variables in CameraActivity.java
 */

private SensorManager sensorManager;
private Sensor accelerometer;
private SensorEventListener accelerometerListener;
private long lastShakeTime = 0;
private static final float SHAKE_THRESHOLD = 15.0f; // m/s²
private static final int SHAKE_TIME_THRESHOLD = 500; // milliseconds

/*
 * STEP 2: Initialize sensor in initializeSensors() method
 */

// Get SensorManager
sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

// Get accelerometer sensor
accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

// Check if sensor exists
if (accelerometer == null) {
    Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show();
    return;
}

/*
 * STEP 3: Create SensorEventListener
 */

accelerometerListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get X, Y, Z values
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calculate magnitude (total acceleration)
        float acceleration = (float) Math.sqrt(x*x + y*y + z*z);

        // Detect shake
        if (acceleration > SHAKE_THRESHOLD) {
            long currentTime = System.currentTimeMillis();

            // Check if enough time has passed since last shake
            if (currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD) {
                lastShakeTime = currentTime;

                // Trigger recording toggle
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toggleRecording();
                        Toast.makeText(CameraActivity.this,
                            "Shake detected!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Can log accuracy changes if needed
    }
};

/*
 * STEP 4: Register listener in onResume()
 */

@Override
protected void onResume() {
    super.onResume();
    if (sensorManager != null && accelerometer != null && accelerometerListener != null) {
        sensorManager.registerListener(accelerometerListener, accelerometer,
            SensorManager.SENSOR_DELAY_GAME);
    }
}

/*
 * STEP 5: Unregister listener in onPause() - CRITICAL!
 */

@Override
protected void onPause() {
    super.onPause();
    if (sensorManager != null && accelerometerListener != null) {
        sensorManager.unregisterListener(accelerometerListener);
    }
}


// ========================================================================
// STUDENT 2 - GYROSCOPE IMPLEMENTATION GUIDE
// ========================================================================

/*
 * OBJECTIVE: Detect device rotation and implement rotation-based controls
 *
 * STEP 1: Declare class variables
 */

private SensorManager sensorManager;
private Sensor gyroscope;
private SensorEventListener gyroscopeListener;

/*
 * STEP 2: Initialize and create listener
 */

sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

if (gyroscope == null) {
    Toast.makeText(this, "Gyroscope not available", Toast.LENGTH_SHORT).show();
    return;
}

gyroscopeListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get rotation rates in rad/s
        float rotationX = event.values[0]; // Around X axis (pitch)
        float rotationY = event.values[1]; // Around Y axis (roll)
        float rotationZ = event.values[2]; // Around Z axis (yaw)

        // Convert to degrees/s if needed
        float rotationZDegrees = (float) Math.toDegrees(rotationZ);

        // Detect significant rotation (e.g., phone spinning)
        if (Math.abs(rotationZDegrees) > 100) { // 100 deg/s threshold
            // Phone is rotating fast - maybe switch camera or scene
        }

        // Display rotation data (for debugging)
        // Update UI TextView with rotation values
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

// Register in onResume(), unregister in onPause()


// ========================================================================
// STUDENT 3 - LIGHT SENSOR IMPLEMENTATION GUIDE
// ========================================================================

/*
 * OBJECTIVE: Measure ambient light and auto-adjust camera settings
 *
 * STEP 1: Declare class variables
 */

private SensorManager sensorManager;
private Sensor lightSensor;
private SensorEventListener lightListener;

/*
 * STEP 2: Initialize and create listener
 */

sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

if (lightSensor == null) {
    Toast.makeText(this, "Light sensor not available", Toast.LENGTH_SHORT).show();
    return;
}

lightListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get lux value (illuminance)
        float lux = event.values[0];

        // Categorize light level
        String category;
        if (lux < 10) {
            category = "Very Dark";
            // Show warning or enable night mode
        } else if (lux < 50) {
            category = "Dark";
        } else if (lux < 500) {
            category = "Normal";
        } else if (lux < 10000) {
            category = "Bright";
        } else {
            category = "Very Bright";
        }

        // Update UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update TextView with light level
                // tvStatus.setText("Light: " + lux + " lux (" + category + ")");
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

// Use SENSOR_DELAY_NORMAL for light sensor (doesn't change rapidly)
sensorManager.registerListener(lightListener, lightSensor,
    SensorManager.SENSOR_DELAY_NORMAL);


// ========================================================================
// STUDENT 4 - PROXIMITY SENSOR IMPLEMENTATION GUIDE
// ========================================================================

/*
 * OBJECTIVE: Detect when phone is covered and auto-pause recording
 *
 * STEP 1: Declare class variables
 */

private SensorManager sensorManager;
private Sensor proximitySensor;
private SensorEventListener proximityListener;
private float maxProximityRange = 5f;
private boolean isNear = false;

/*
 * STEP 2: Initialize and create listener
 */

sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

if (proximitySensor == null) {
    Toast.makeText(this, "Proximity sensor not available", Toast.LENGTH_SHORT).show();
    return;
}

maxProximityRange = proximitySensor.getMaximumRange();

proximityListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];

        // Most proximity sensors return 0 (near) or max (far)
        boolean currentlyNear = distance < maxProximityRange / 2;

        // Detect state change
        if (currentlyNear != isNear) {
            isNear = currentlyNear;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isNear) {
                        // Phone is covered - pause recording
                        if (isRecording) {
                            toggleRecording();
                            Toast.makeText(CameraActivity.this,
                                "Paused (proximity detected)", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Phone is clear - can resume
                        Toast.makeText(CameraActivity.this,
                            "Proximity clear", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};


// ========================================================================
// STUDENT 5 - MAGNETOMETER (COMPASS) IMPLEMENTATION GUIDE
// ========================================================================

/*
 * OBJECTIVE: Implement compass functionality to show direction
 *
 * IMPORTANT: Magnetometer needs BOTH magnetic field sensor AND accelerometer!
 *
 * STEP 1: Declare class variables
 */

private SensorManager sensorManager;
private Sensor magnetometer;
private Sensor accelerometer;
private SensorEventListener magnetometerListener;
private SensorEventListener accelerometerListenerForCompass;

private float[] gravity = new float[3];
private float[] geomagnetic = new float[3];
private float[] rotationMatrix = new float[9];
private float[] orientation = new float[3];

private boolean hasGravity = false;
private boolean hasGeomagnetic = false;

/*
 * STEP 2: Initialize sensors
 */

sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

if (magnetometer == null || accelerometer == null) {
    Toast.makeText(this, "Compass sensors not available", Toast.LENGTH_SHORT).show();
    return;
}

/*
 * STEP 3: Create magnetometer listener
 */

magnetometerListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        geomagnetic[0] = event.values[0];
        geomagnetic[1] = event.values[1];
        geomagnetic[2] = event.values[2];
        hasGeomagnetic = true;

        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            // Show calibration message
            Toast.makeText(CameraActivity.this,
                "Compass accuracy low - calibrate by moving phone in figure-8",
                Toast.LENGTH_SHORT).show();
        }
    }
};

/*
 * STEP 4: Create accelerometer listener (for compass)
 */

accelerometerListenerForCompass = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        gravity[0] = event.values[0];
        gravity[1] = event.values[1];
        gravity[2] = event.values[2];
        hasGravity = true;

        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

/*
 * STEP 5: Calculate orientation (compass direction)
 */

private void calculateOrientation() {
    if (!hasGravity || !hasGeomagnetic) {
        return; // Need both sensors
    }

    boolean success = SensorManager.getRotationMatrix(rotationMatrix, null,
        gravity, geomagnetic);

    if (success) {
        SensorManager.getOrientation(rotationMatrix, orientation);

        // orientation[0] is azimuth (rotation around Z axis) in radians
        float azimuthRad = orientation[0];

        // Convert to degrees
        float azimuthDeg = (float) Math.toDegrees(azimuthRad);

        // Normalize to 0-360
        azimuthDeg = (azimuthDeg + 360) % 360;

        // Determine direction
        String direction;
        if (azimuthDeg >= 315 || azimuthDeg < 45) {
            direction = "North";
        } else if (azimuthDeg >= 45 && azimuthDeg < 135) {
            direction = "East";
        } else if (azimuthDeg >= 135 && azimuthDeg < 225) {
            direction = "South";
        } else {
            direction = "West";
        }

        // Update UI
        final float finalAzimuth = azimuthDeg;
        final String finalDirection = direction;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update TextView with compass data
                // tvStatus.setText("Direction: " + finalDirection + " (" +
                //     Math.round(finalAzimuth) + "°)");
            }
        });
    }
}

/*
 * STEP 6: Register BOTH listeners
 */

@Override
protected void onResume() {
    super.onResume();
    if (sensorManager != null) {
        sensorManager.registerListener(magnetometerListener, magnetometer,
            SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(accelerometerListenerForCompass, accelerometer,
            SensorManager.SENSOR_DELAY_UI);
    }
}

/*
 * STEP 7: Unregister BOTH listeners
 */

@Override
protected void onPause() {
    super.onPause();
    if (sensorManager != null) {
        sensorManager.unregisterListener(magnetometerListener);
        sensorManager.unregisterListener(accelerometerListenerForCompass);
    }
}


// ========================================================================
// GENERAL TIPS FOR ALL STUDENTS
// ========================================================================

/*
 * 1. ALWAYS check if sensor exists before using it
 * 2. ALWAYS unregister listeners in onPause() - prevents battery drain!
 * 3. Choose appropriate sensor delay:
 *    - SENSOR_DELAY_FASTEST: Only if absolutely needed (drains battery)
 *    - SENSOR_DELAY_GAME: For accelerometer, gyroscope (fast updates)
 *    - SENSOR_DELAY_UI: For compass, normal UI updates
 *    - SENSOR_DELAY_NORMAL: For light, proximity (slow changes)
 *
 * 4. Test on REAL devices - emulator sensors are not accurate
 * 5. Add proper error handling and null checks
 * 6. Update UI from runOnUiThread() if calling from sensor callback
 * 7. Add debouncing to prevent rapid repeated actions
 * 8. Show helpful messages to users when sensors trigger actions
 */

// ========================================================================
// END OF STUDENT IMPLEMENTATION GUIDE
// ========================================================================

