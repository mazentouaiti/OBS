# Sensor Classes - Usage Guide

## 📦 Independent Sensor Classes Created

I've created 5 independent, reusable sensor classes in the `com.obs.mobile.sensors` package:

1. **AccelerometerSensor.java** - Shake detection
2. **GyroscopeSensor.java** - Rotation detection  
3. **LightSensor.java** - Ambient light measurement
4. **ProximitySensor.java** - Proximity detection
5. **MagnetometerSensor.java** - Compass/direction

---

## 🎯 Benefits of Independent Classes

### ✅ Advantages:
- **Reusable** - Use same sensor class in multiple activities
- **Cleaner code** - Activities don't have messy sensor logic
- **Easy testing** - Test sensors independently
- **Better organization** - Each student works on their own class
- **Callbacks** - Simple listener pattern for sensor events

---

## 📖 How to Use Sensor Classes

### General Pattern (All Sensors):

```java
// 1. Declare instance variable
private AccelerometerSensor accelerometerSensor;

// 2. In onCreate() - Create instance
accelerometerSensor = new AccelerometerSensor(this);

// 3. Set up callbacks
accelerometerSensor.setOnShakeListener(new AccelerometerSensor.OnShakeListener() {
    @Override
    public void onShake(float intensity) {
        // Handle shake event
        Toast.makeText(this, "Shake detected!", Toast.LENGTH_SHORT).show();
    }
});

// 4. Initialize sensor
if (accelerometerSensor.initialize()) {
    // Sensor available
} else {
    // Sensor not available on this device
}

// 5. In onResume() - Start listening
accelerometerSensor.startListening();

// 6. In onPause() - Stop listening (CRITICAL!)
accelerometerSensor.stopListening();
```

---

## 📝 Specific Examples for Each Sensor

### Student 1 - AccelerometerSensor

```java
// In CameraActivity.java

private AccelerometerSensor accelerometerSensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    // Create sensor instance
    accelerometerSensor = new AccelerometerSensor(this);
    
    // Set shake listener
    accelerometerSensor.setOnShakeListener(new AccelerometerSensor.OnShakeListener() {
        @Override
        public void onShake(float intensity) {
            // Toggle recording on shake
            toggleRecording();
            Toast.makeText(CameraActivity.this, 
                "Shake detected! Intensity: " + intensity, 
                Toast.LENGTH_SHORT).show();
        }
    });
    
    // Set data listener (optional - for real-time display)
    accelerometerSensor.setOnDataChangedListener(
        new AccelerometerSensor.OnDataChangedListener() {
            @Override
            public void onDataChanged(float x, float y, float z, float magnitude) {
                // Update UI with sensor data
                // tvAccelData.setText(String.format("X: %.2f Y: %.2f Z: %.2f", x, y, z));
            }
        }
    );
    
    // Initialize
    if (accelerometerSensor.initialize()) {
        // Sensor ready
    }
}

@Override
protected void onResume() {
    super.onResume();
    if (accelerometerSensor != null) {
        accelerometerSensor.startListening();
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (accelerometerSensor != null) {
        accelerometerSensor.stopListening();
    }
}
```

### Student 2 - GyroscopeSensor

```java
private GyroscopeSensor gyroscopeSensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    gyroscopeSensor = new GyroscopeSensor(this);
    
    // Rotation data listener
    gyroscopeSensor.setOnRotationListener(new GyroscopeSensor.OnRotationListener() {
        @Override
        public void onRotation(float rotationX, float rotationY, float rotationZ) {
            // Display rotation rates
            // Convert to degrees if needed
            float degreesPerSecond = GyroscopeSensor.radiansToDegrees(rotationZ);
        }
    });
    
    // Rotation gesture listener
    gyroscopeSensor.setOnRotationGestureListener(
        new GyroscopeSensor.OnRotationGestureListener() {
            @Override
            public void onFastRotation(float degrees) {
                // Phone is spinning fast
                Toast.makeText(CameraActivity.this, 
                    "Fast rotation detected!", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    );
    
    gyroscopeSensor.initialize();
}

@Override
protected void onResume() {
    super.onResume();
    if (gyroscopeSensor != null) {
        gyroscopeSensor.startListening();
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (gyroscopeSensor != null) {
        gyroscopeSensor.stopListening();
    }
}
```

### Student 3 - LightSensor

```java
private LightSensor lightSensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    lightSensor = new LightSensor(this);
    
    lightSensor.setOnLightChangedListener(new LightSensor.OnLightChangedListener() {
        @Override
        public void onLightChanged(float lux, LightSensor.LightCategory category) {
            // Update UI
            String message = category.getName() + " (" + lux + " lux)";
            // tvLightLevel.setText(message);
            
            // Auto-switch scenes based on light
            if (category == LightSensor.LightCategory.VERY_DARK) {
                // Switch to night scene
            } else if (category == LightSensor.LightCategory.VERY_BRIGHT) {
                // Switch to outdoor scene
            }
            
            // Get recommendation
            String recommendation = lightSensor.getCameraRecommendation(lux);
            // tvRecommendation.setText(recommendation);
        }
    });
    
    lightSensor.initialize();
}

@Override
protected void onResume() {
    super.onResume();
    if (lightSensor != null) {
        lightSensor.startListening();
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (lightSensor != null) {
        lightSensor.stopListening();
    }
}
```

### Student 4 - ProximitySensor

```java
private ProximitySensor proximitySensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    proximitySensor = new ProximitySensor(this);
    
    // Method 1: Use specific listeners
    proximitySensor.setOnNearListener(new ProximitySensor.OnNearListener() {
        @Override
        public void onNear() {
            // Phone is covered - pause recording
            if (isRecording) {
                toggleRecording();
                Toast.makeText(CameraActivity.this, 
                    "Recording paused - proximity detected", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    });
    
    proximitySensor.setOnFarListener(new ProximitySensor.OnFarListener() {
        @Override
        public void onFar() {
            // Phone is clear
            Toast.makeText(CameraActivity.this, 
                "Proximity clear", 
                Toast.LENGTH_SHORT).show();
        }
    });
    
    // Method 2: Use combined listener
    proximitySensor.setOnProximityChangedListener(
        new ProximitySensor.OnProximityChangedListener() {
            @Override
            public void onProximityChanged(float distance, boolean isNear) {
                // Display distance and state
            }
        }
    );
    
    proximitySensor.initialize();
}

@Override
protected void onResume() {
    super.onResume();
    if (proximitySensor != null) {
        proximitySensor.startListening();
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (proximitySensor != null) {
        proximitySensor.stopListening(); // CRITICAL!
    }
}
```

### Student 5 - MagnetometerSensor

```java
private MagnetometerSensor magnetometerSensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    magnetometerSensor = new MagnetometerSensor(this);
    
    // Compass change listener
    magnetometerSensor.setOnCompassChangeListener(
        new MagnetometerSensor.OnCompassChangeListener() {
            @Override
            public void onCompassChange(float azimuth, 
                                       MagnetometerSensor.CompassDirection direction) {
                // Display compass data
                String compassText = direction.getName() + " (" + 
                                    Math.round(azimuth) + "°)";
                // tvCompass.setText(compassText);
            }
        }
    );
    
    // Direction change listener (only when direction changes)
    magnetometerSensor.setOnDirectionChangeListener(
        new MagnetometerSensor.OnDirectionChangeListener() {
            @Override
            public void onDirectionChange(MagnetometerSensor.CompassDirection direction) {
                // Direction changed - switch scene
                Toast.makeText(CameraActivity.this, 
                    "Now facing: " + direction.getName(), 
                    Toast.LENGTH_SHORT).show();
                
                // Auto-switch scenes based on direction
                switch (direction) {
                    case NORTH:
                        // Switch to scene 1
                        break;
                    case EAST:
                        // Switch to scene 2
                        break;
                    case SOUTH:
                        // Switch to scene 3
                        break;
                    case WEST:
                        // Switch to scene 4
                        break;
                }
            }
        }
    );
    
    magnetometerSensor.initialize();
}

@Override
protected void onResume() {
    super.onResume();
    if (magnetometerSensor != null) {
        magnetometerSensor.startListening();
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (magnetometerSensor != null) {
        magnetometerSensor.stopListening();
    }
}
```

---

## 🔧 Integration in SensorsActivity

```java
// In SensorsActivity.java

private AccelerometerSensor accelerometerSensor;
private GyroscopeSensor gyroscopeSensor;
private LightSensor lightSensor;
private ProximitySensor proximitySensor;
private MagnetometerSensor magnetometerSensor;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
    
    // Initialize all sensors
    accelerometerSensor = new AccelerometerSensor(this);
    gyroscopeSensor = new GyroscopeSensor(this);
    lightSensor = new LightSensor(this);
    proximitySensor = new ProximitySensor(this);
    magnetometerSensor = new MagnetometerSensor(this);
    
    // Set up switches
    setupSensorSwitches();
}

private void setupSensorSwitches() {
    // Accelerometer switch
    switchAccelerometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
            if (accelerometerSensor.initialize()) {
                accelerometerSensor.setOnDataChangedListener(
                    (x, y, z, mag) -> runOnUiThread(() -> 
                        tvAccelData.setText(String.format("X: %.2f Y: %.2f Z: %.2f", x, y, z))
                    )
                );
                accelerometerSensor.startListening();
            }
        } else {
            accelerometerSensor.stopListening();
            tvAccelData.setText("Accelerometer: Disabled");
        }
    });
    
    // Similar for other sensors...
}
```

---

## ✅ Key Features of Sensor Classes

### 1. **Callback Interfaces**
Each sensor class uses Java interfaces for callbacks - clean and type-safe.

### 2. **Availability Checking**
All classes have `isAvailable()` method to check if sensor exists.

### 3. **Lifecycle Management**
Simple `initialize()`, `startListening()`, `stopListening()` pattern.

### 4. **Enums for Categories**
- `LightSensor.LightCategory` - Light levels
- `MagnetometerSensor.CompassDirection` - Compass directions

### 5. **Helper Methods**
- `GyroscopeSensor.radiansToDegrees()` - Unit conversion
- `LightSensor.getCameraRecommendation()` - Suggestions
- `ProximitySensor.getMaxRange()` - Sensor info

---

## 📂 Project Structure

```
com.obs.mobile/
├── sensors/                          # NEW PACKAGE
│   ├── AccelerometerSensor.java     # Student 1
│   ├── GyroscopeSensor.java         # Student 2
│   ├── LightSensor.java             # Student 3
│   ├── ProximitySensor.java         # Student 4
│   └── MagnetometerSensor.java      # Student 5
├── SplashActivity.java
├── MainMenuActivity.java
├── CameraActivity.java              # Use sensors here
├── SensorsActivity.java             # Monitor sensors here
└── ScenesActivity.java              # Auto-switch scenes
```

---

## 🎓 Student Workflow

Each student now:

1. **Opens their sensor class** (e.g., `AccelerometerSensor.java`)
2. **Implements TODO methods**:
   - `initialize()`
   - `startListening()`
   - `stopListening()`
   - Helper methods
3. **Tests in SensorsActivity** (enable switch, see data)
4. **Integrates in CameraActivity** (use callbacks for actions)

---

## ✅ Advantages Over Inline Code

| Before (Inline) | After (Independent Classes) |
|----------------|----------------------------|
| ❌ Messy activity code | ✅ Clean activity code |
| ❌ Hard to reuse | ✅ Reusable everywhere |
| ❌ Difficult to test | ✅ Easy to test |
| ❌ Code duplication | ✅ DRY principle |
| ❌ Hard to maintain | ✅ Easy to maintain |

---

## 🚀 Next Steps

1. **Students implement their sensor class**
2. **Test with switches in SensorsActivity**
3. **Add callbacks in CameraActivity for actions**
4. **Optional: Add scene switching in ScenesActivity**

This architecture makes the project much more professional and maintainable! 🎉

