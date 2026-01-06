# 📐 SENSORS IMPLEMENTATION GUIDE

## Quick Reference: How to Use Each Sensor in Your Code

This guide provides step-by-step implementation examples for each sensor class. Copy-paste ready code snippets.

---

## 🎯 General Sensor Lifecycle Pattern

Every sensor follows this pattern:

```java
// Step 1: Declare
private AccelerometerSensor accelerometerSensor;

// Step 2: Instantiate (onCreate)
accelerometerSensor = new AccelerometerSensor(this);

// Step 3: Initialize & Check Availability (onCreate)
if (!accelerometerSensor.initialize()) {
    Log.w("Sensor", "Accelerometer not available on this device");
    // Handle gracefully - disable feature or use fallback
}

// Step 4: Set Callbacks (onCreate or anytime)
accelerometerSensor.setOnDataChangedListener((x, y, z, mag) -> {
    // Handle sensor data
});

// Step 5: Start Listening (onResume)
accelerometerSensor.startListening();

// Step 6: Stop Listening (onPause - CRITICAL!)
accelerometerSensor.stopListening();
```

---

## 📱 ACCELEROMETER IMPLEMENTATION

### Complete Integration Example

```java
public class CameraActivity extends AppCompatActivity {
    
    private AccelerometerSensor accelerometerSensor;
    private TextView tvAccelData;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        // 1. Initialize UI
        tvAccelData = findViewById(R.id.tv_accel_data);
        
        // 2. Create sensor
        accelerometerSensor = new AccelerometerSensor(this);
        
        // 3. Check if available
        if (!accelerometerSensor.initialize()) {
            tvAccelData.setText("Accelerometer not available");
            return;
        }
        
        // 4. Set data callback
        accelerometerSensor.setOnDataChangedListener((x, y, z, magnitude) -> {
            // Update UI
            String text = String.format("X: %.2f\nY: %.2f\nZ: %.2f\nMag: %.2f m/s²",
                x, y, z, magnitude);
            tvAccelData.setText(text);
        });
        
        // 5. Set shake callback
        accelerometerSensor.setOnShakeListener(intensity -> {
            Toast.makeText(this, "Shake detected: " + intensity, 
                Toast.LENGTH_SHORT).show();
            
            // Example: Toggle recording on shake
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // START sensor in onResume
        accelerometerSensor.startListening();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // STOP sensor in onPause (saves battery!)
        accelerometerSensor.stopListening();
    }
}
```

### Use Case: Shake Detection for Recording

```java
// Simple implementation
accelerometerSensor.setOnShakeListener(intensity -> {
    // Only trigger if shake is strong enough
    if (intensity > 20) {
        startRecording();
    }
});

// Advanced: Track sustained motion
float motionTracker = 0;
accelerometerSensor.setOnDataChangedListener((x, y, z, mag) -> {
    motionTracker = (motionTracker * 0.9f) + (mag * 0.1f);  // Smooth
    
    if (motionTracker > 12) {
        if (!isRecording) startRecording();
    } else if (motionTracker < 10 && isRecording) {
        stopRecording();
    }
});
```

### Use Case: Scene Switching

```java
// Switch scene based on shake intensity
accelerometerSensor.setOnDataChangedListener((x, y, z, mag) -> {
    // Check X-axis for left-right motion
    if (Math.abs(x) > 15) {
        if (x > 0) {
            nextScene();  // Right shake = next scene
        } else {
            previousScene();  // Left shake = previous scene
        }
    }
});
```

---

## 🔄 GYROSCOPE IMPLEMENTATION

### Complete Integration Example

```java
public class CameraActivity extends AppCompatActivity {
    
    private GyroscopeSensor gyroscopeSensor;
    private TextView tvGyroData;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        tvGyroData = findViewById(R.id.tv_gyro_data);
        
        // Create and initialize
        gyroscopeSensor = new GyroscopeSensor(this);
        if (!gyroscopeSensor.initialize()) {
            tvGyroData.setText("Gyroscope not available");
            return;
        }
        
        // Set rotation listener (continuous updates)
        gyroscopeSensor.setOnRotationListener((rotX, rotY, rotZ) -> {
            // Convert to degrees for readability
            float xDeg = GyroscopeSensor.radiansToDegrees(rotX);
            float yDeg = GyroscopeSensor.radiansToDegrees(rotY);
            float zDeg = GyroscopeSensor.radiansToDegrees(rotZ);
            
            String text = String.format("Rotation:\nX: %.1f°/s\nY: %.1f°/s\nZ: %.1f°/s",
                xDeg, yDeg, zDeg);
            tvGyroData.setText(text);
        });
        
        // Set gesture listener (fast rotation only)
        gyroscopeSensor.setOnRotationGestureListener((speed, clockwise) -> {
            String direction = clockwise ? "Clockwise" : "Counter-clockwise";
            Toast.makeText(this, "Spin detected: " + direction, 
                Toast.LENGTH_SHORT).show();
            
            // Switch scenes
            if (clockwise) {
                nextScene();
            } else {
                previousScene();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        gyroscopeSensor.startListening();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        gyroscopeSensor.stopListening();
    }
}
```

### Use Case: Video Stabilization Warning

```java
gyroscopeSensor.setOnRotationListener((x, y, z) -> {
    // Calculate total rotation
    float totalRotation = Math.abs(x) + Math.abs(y) + Math.abs(z);
    
    if (totalRotation > 3.0) {
        // Device is moving too much
        showWarning("Stabilize your device!");
    } else {
        clearWarning();
    }
});
```

### Use Case: Smooth Zoom Control

```java
// Use gyroscope Y-axis for zoom (tilting up/down)
private float currentZoom = 1.0f;

gyroscopeSensor.setOnRotationListener((x, y, z) -> {
    // Tilt up = zoom in (y > 0)
    // Tilt down = zoom out (y < 0)
    
    float zoomChange = GyroscopeSensor.radiansToDegrees(y) * 0.01f;
    currentZoom += zoomChange;
    currentZoom = Math.max(1.0f, Math.min(10.0f, currentZoom));  // Clamp 1x-10x
    
    applyZoom(currentZoom);
});
```

---

## 💡 LIGHT SENSOR IMPLEMENTATION

### Complete Integration Example

```java
public class CameraActivity extends AppCompatActivity {
    
    private LightSensor lightSensor;
    private TextView tvLightData;
    private SeekBar screenBrightness;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        tvLightData = findViewById(R.id.tv_light_data);
        screenBrightness = findViewById(R.id.brightness_slider);
        
        // Create and initialize
        lightSensor = new LightSensor(this);
        if (!lightSensor.initialize()) {
            tvLightData.setText("Light sensor not available");
            return;
        }
        
        // Set light change callback
        lightSensor.setOnLightChangedListener((lux, category) -> {
            // Update UI
            String text = String.format("Light: %.0f lux\nCategory: %s\nRange: %s",
                lux, category.getName(), category.getRange());
            tvLightData.setText(text);
            
            // Get camera recommendation
            String recommendation = lightSensor.getCameraRecommendation(lux);
            tvRecommendation.setText(recommendation);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        lightSensor.startListening();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        lightSensor.stopListening();
    }
}
```

### Use Case: Auto-Brightness Control

```java
lightSensor.setOnLightChangedListener((lux, category) -> {
    // Map lux (0-50000) to brightness (0-255)
    float brightness = Math.min(255, (lux / 200));  // Adjust factor as needed
    
    WindowManager.LayoutParams params = getWindow().getAttributes();
    params.screenBrightness = brightness / 255.0f;  // Normalized to 0-1
    getWindow().setAttributes(params);
});
```

### Use Case: Camera Exposure Control

```java
lightSensor.setOnLightChangedListener((lux, category) -> {
    switch (category) {
        case VERY_DARK:
            enableNightMode();
            setISO(1600);
            break;
        case DARK:
            setISO(800);
            break;
        case NORMAL:
            setISO(400);
            break;
        case BRIGHT:
            setISO(200);
            disableFlash();
            break;
        case VERY_BRIGHT:
            setISO(100);
            reduceExposure();
            break;
    }
});
```

### Use Case: Flash Control

```java
lightSensor.setOnLightChangedListener((lux, category) -> {
    if (category == LightSensor.LightCategory.VERY_DARK) {
        enableFlash();
    } else if (category == LightSensor.LightCategory.DARK) {
        enableFlashIfNeeded();
    } else {
        disableFlash();
    }
});
```

---

## 📍 PROXIMITY SENSOR IMPLEMENTATION

### Complete Integration Example

```java
public class CameraActivity extends AppCompatActivity {
    
    private ProximitySensor proximitySensor;
    private TextView tvProximityData;
    private View proximityWarning;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        tvProximityData = findViewById(R.id.tv_proximity_data);
        proximityWarning = findViewById(R.id.proximity_warning);
        
        // Create and initialize
        proximitySensor = new ProximitySensor(this);
        if (!proximitySensor.initialize()) {
            tvProximityData.setText("Proximity sensor not available");
            return;
        }
        
        // Set detailed callback (all changes)
        proximitySensor.setOnProximityChangedListener((distance, isNear) -> {
            String status = isNear ? "NEAR" : "FAR";
            String text = String.format("Distance: %.1f cm\nStatus: %s",
                distance, status);
            tvProximityData.setText(text);
            
            // Visual warning
            if (isNear) {
                proximityWarning.setVisibility(View.VISIBLE);
            } else {
                proximityWarning.setVisibility(View.GONE);
            }
        });
        
        // Alternative: Simple near callback
        proximitySensor.setOnNearListener(() -> {
            pauseRecording();  // Object detected near camera
        });
        
        // Alternative: Simple far callback
        proximitySensor.setOnFarListener(() -> {
            resumeRecording();  // Object moved away
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        proximitySensor.startListening();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        proximitySensor.stopListening();
    }
}
```

### Use Case: Auto-Focus Trigger

```java
proximitySensor.setOnNearListener(() -> {
    // User's face is close to camera - trigger auto-focus
    cameraDevice.triggerAutoFocus();
    
    // Show focus indicator
    focusIndicator.startAnimation();
});
```

### Use Case: Recording Pause on Obstruction

```java
proximitySensor.setOnProximityChangedListener((distance, isNear) -> {
    if (isNear && isRecording) {
        pauseRecording();
        showMessage("Camera obstructed - paused recording");
    } else if (!isNear && isPaused) {
        resumeRecording();
    }
});
```

### Use Case: Screen Lock Control

```java
proximitySensor.setOnNearListener(() -> {
    // User put device to ear (like phone call)
    lockScreen();
    dimScreen();
});

proximitySensor.setOnFarListener(() -> {
    // Device moved away from ear
    unlockScreen();
    restoreBrightness();
});
```

---

## 🧭 MAGNETOMETER IMPLEMENTATION

### Complete Integration Example

```java
public class CameraActivity extends AppCompatActivity {
    
    private MagnetometerSensor magnetometerSensor;
    private ImageView compassNeedle;
    private TextView tvDirection;
    private TextView tvAzimuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        compassNeedle = findViewById(R.id.compass_needle);
        tvDirection = findViewById(R.id.tv_direction);
        tvAzimuth = findViewById(R.id.tv_azimuth);
        
        // Create and initialize
        magnetometerSensor = new MagnetometerSensor(this);
        if (!magnetometerSensor.initialize()) {
            tvDirection.setText("Magnetometer not available");
            return;
        }
        
        // Set continuous compass callback (smooth rotation)
        magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
            // Rotate compass needle
            compassNeedle.setRotation(-azimuth);  // Negative for correct direction
            
            // Update direction text
            tvDirection.setText(direction.getName());
            tvAzimuth.setText(String.format("%.0f°", azimuth));
        });
        
        // Set direction change callback (event-based)
        magnetometerSensor.setOnDirectionChangeListener(direction -> {
            Log.d("Compass", "Direction changed to: " + direction.getName());
            
            // Example: Update scene based on direction
            updateSceneOrientation(direction);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        magnetometerSensor.startListening();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        magnetometerSensor.stopListening();
    }
}
```

### Use Case: Compass Display

```java
magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
    // Rotate compass rose UI
    compassRose.setRotation(-azimuth);
    
    // Update cardinal direction
    tvCardinal.setText(direction.getAbbreviation());
    
    // Show full name
    tvFullDirection.setText(direction.getName());
});
```

### Use Case: Direction-Based Scene Selection

```java
magnetometerSensor.setOnDirectionChangeListener(direction -> {
    String sceneId = null;
    
    switch (direction) {
        case NORTH:
        case NORTH_EAST:
        case NORTH_WEST:
            sceneId = "landscape_north";
            break;
        case SOUTH:
        case SOUTH_EAST:
        case SOUTH_WEST:
            sceneId = "landscape_south";
            break;
        case EAST:
            sceneId = "landscape_east";
            break;
        case WEST:
            sceneId = "landscape_west";
            break;
    }
    
    loadScene(sceneId);
});
```

### Use Case: Video Recording Guidance

```java
// Show guidance overlay for optimal recording direction
magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
    // Ideal direction: North
    if (direction == MagnetometerSensor.CompassDirection.NORTH) {
        guidanceView.setText("✓ Perfect! Recording north");
        guidanceView.setTextColor(Color.GREEN);
    } else {
        guidanceView.setText("Rotate to face north");
        guidanceView.setTextColor(Color.YELLOW);
    }
});
```

### Important: Calibration Warning

```java
@Override
protected void onResume() {
    super.onResume();
    
    magnetometerSensor.startListening();
    
    // Show calibration tip
    showCalibrationTip(
        "If compass is inaccurate,\nmove device in figure-8 pattern"
    );
}
```

---

## 🔐 USING SENSORPREFERENCES FOR PERSISTENCE

### Save/Load Sensor State

```java
// In SensorsActivity - save when user toggles switch
switchAccelerometer.setOnCheckedChangeListener((button, isChecked) -> {
    SensorPreferences.setAccelerometerEnabled(this, isChecked);
    
    if (isChecked) {
        accelerometerSensor.startListening();
    } else {
        accelerometerSensor.stopListening();
    }
});

// In CameraActivity - restore saved state
if (SensorPreferences.isAccelerometerEnabled(this)) {
    accelerometerSensor.startListening();
}
```

### Check Enabled Sensor Count

```java
// Get total enabled sensors
int enabledCount = SensorPreferences.getEnabledSensorCount(this);

if (enabledCount == 0) {
    tvStatus.setText("No sensors enabled");
} else if (enabledCount <= 2) {
    tvStatus.setText(enabledCount + " sensors active - low battery impact");
} else {
    tvStatus.setText(enabledCount + " sensors active - moderate battery usage");
}
```

---

## 🔌 SENSOR DATA STREAMING TO PYTHON

The `SensorDataStreamer` class automatically collects all sensor data and sends it to Python:

### Setup Steps

```java
// 1. Create streamer instance
private SensorDataStreamer sensorDataStreamer;

// 2. Initialize in onCreate
sensorDataStreamer = new SensorDataStreamer(this);
sensorDataStreamer.start();

// 3. Update sensor values as they arrive
accelerometerSensor.setOnDataChangedListener((x, y, z, mag) -> {
    sensorDataStreamer.setAccelerometerData(x, y, z, mag);
});

gyroscopeSensor.setOnRotationListener((x, y, z) -> {
    sensorDataStreamer.setGyroscopeData(x, y, z);
});

lightSensor.setOnLightChangedListener((lux, category) -> {
    sensorDataStreamer.setLightData(lux, category.toString());
});

// ... etc for all sensors

// 4. Stop in onDestroy
@Override
protected void onDestroy() {
    super.onDestroy();
    sensorDataStreamer.stop();
}
```

### Python Receiver Example

```python
# sensor_receiver.py
import socket
import json

def receive_sensor_data():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', 5000))
    
    while True:
        data, addr = sock.recvfrom(1024)
        sensor_data = json.loads(data)
        
        print(f"Accelerometer: {sensor_data['accelerometer']}")
        print(f"Light: {sensor_data['light']['lux']} lux")
        print(f"Direction: {sensor_data['magnetometer']['direction']}")

receive_sensor_data()
```

---

## 📋 Checklist: Complete Implementation

For each sensor you implement, verify:

- [ ] Class instantiated in onCreate
- [ ] initialize() called and availability checked
- [ ] Callbacks registered in onCreate
- [ ] startListening() called in onResume
- [ ] stopListening() called in onPause
- [ ] UI elements updated with sensor data
- [ ] Sensor state saved to SharedPreferences
- [ ] Data streamed to Python (if needed)
- [ ] Error handling for missing sensors
- [ ] Battery optimization verified
- [ ] Broadcast sent to other components
- [ ] Tested on real device

---

## 🐛 Debugging Tips

### Check Sensor Availability
```java
SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
for (Sensor s : sensors) {
    Log.d("Sensor", s.getName() + " - " + s.getStringType());
}
```

### Monitor Sensor Data
```java
sensor.setOnDataChangedListener(data -> {
    Log.d("SensorData", "Value: " + data);  // Check logcat
});
```

### Check Battery Impact
1. Enable Developer Mode on device
2. Enable "Battery Historian" logging
3. Record before/after with sensors on
4. Compare battery drain

### Verify UDP Streaming
```bash
# On Python machine, check if data arrives:
python3 sensor_receiver.py
# Should print JSON packets every 100ms
```

---

**Implementation Guide Version:** 1.0  
**Last Updated:** January 2, 2026  
**Ready for Student Development**

