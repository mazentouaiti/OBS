# 📊 SENSORS - DETAILED TECHNICAL REPORT

## Overview

This document provides a comprehensive technical analysis of all 5 sensor implementations in the OBS Mobile Android application. Each sensor is independently implemented as a reusable class with callbacks, real-time monitoring capabilities, and integration with the camera activity.

---

## 🎯 Sensor Architecture Overview

### Design Pattern: **Callback-Based Observer Pattern**

All sensors follow the same architectural pattern:

```
┌─────────────────────────────────────────┐
│         Sensor Class (e.g., Accel)      │
├─────────────────────────────────────────┤
│ - SensorManager (Android OS)            │
│ - SensorEventListener (callback)        │
│ - Listener interfaces (custom callbacks)│
├─────────────────────────────────────────┤
│ Methods:                                │
│ • initialize()      → Get sensor        │
│ • startListening()  → Register listener │
│ • stopListening()   → Unregister        │
│ • setCallback()     → Set listener      │
├─────────────────────────────────────────┤
│ Listeners:                              │
│ • OnDataChangedListener                 │
│ • OnShakeListener (Accel-specific)      │
│ • OnRotationListener (Gyro-specific)    │
└─────────────────────────────────────────┘
```

### Key Principles

1. **Non-blocking** - Uses callbacks instead of polling
2. **Energy-efficient** - Sensors pause in background (onPause)
3. **Reusable** - Independent classes can be used anywhere
4. **Thread-safe** - Uses Android's SensorManager threading
5. **Debounced** - Events filtered to avoid noise (especially Proximity)
6. **Persistent** - States saved to SharedPreferences

---

## 📱 SENSOR 1: ACCELEROMETER

**Student Assignment:** Student 1  
**File:** `AccelerometerSensor.java` (101 lines)  
**Package:** `com.obs.mobile.sensors`

### 📌 What It Is

The accelerometer measures linear acceleration in 3 axes:
- **X-axis** (left-right tilt)
- **Y-axis** (up-down tilt)
- **Z-axis** (forward-backward motion)

**Units:** m/s² (meters per second squared)  
**Sensor Type:** `Sensor.TYPE_ACCELEROMETER`  
**Update Frequency:** `SENSOR_DELAY_GAME` (fastest, ~20ms)  

### 🔧 Implementation Details

#### Class Structure

```java
public class AccelerometerSensor {
    // Dependencies
    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener listener;
    
    // Callbacks
    private OnShakeListener onShakeListener;
    private OnDataChangedListener onDataChangedListener;
    
    // Shake detection
    private long lastShakeTime = 0;
    private static final float SHAKE_THRESHOLD = 15.0f;
    private static final int SHAKE_TIME_THRESHOLD = 500; // ms
}
```

#### Public Methods

##### **1. Constructor**
```java
public AccelerometerSensor(Context context)
```
- Stores application context
- Does NOT start sensor automatically
- Should be called once per activity lifecycle

**Usage:**
```java
AccelerometerSensor accel = new AccelerometerSensor(this);
```

---

##### **2. initialize()**
```java
public boolean initialize()
```
- Retrieves system SensorManager
- Gets default accelerometer sensor
- Returns `true` if sensor available, `false` if not

**Implementation:**
```java
public boolean initialize() {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    if (sensorManager == null) return false;
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    return accelerometer != null;
}
```

**Best Practice:**
```java
if (!accelerometerSensor.initialize()) {
    Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show();
    return;
}
```

---

##### **3. startListening()**
```java
public void startListening()
```
- Registers `SensorEventListener` with SensorManager
- Starts receiving real-time acceleration data
- **Must be called** in `Activity.onResume()`

**Flow Diagram:**
```
startListening()
    ↓
SensorEventListener created
    ↓
sensorManager.registerListener()
    ↓
System starts sending acceleration updates
    ↓
onSensorChanged() called ~50 times/second
    ↓
Callbacks invoked (OnDataChanged, OnShake)
```

**Key Logic:**
```java
listener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate magnitude
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
        
        // Notify data listener
        if (onDataChangedListener != null) {
            onDataChangedListener.onDataChanged(x, y, z, acceleration);
        }
        
        // Shake detection
        if (acceleration > SHAKE_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShakeTime > SHAKE_TIME_THRESHOLD) {
                lastShakeTime = currentTime;
                if (onShakeListener != null) {
                    onShakeListener.onShake(acceleration);
                }
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Called when sensor accuracy changes
    }
};

sensorManager.registerListener(listener, accelerometer, 
    SensorManager.SENSOR_DELAY_GAME);
```

---

##### **4. stopListening()**
```java
public void stopListening()
```
- Unregisters listener from SensorManager
- **CRITICAL** for battery life
- **Must be called** in `Activity.onPause()`

**Why Important:**
- Sensors consume ~10-30% battery when active
- Leaving them running in background drains battery
- Can cause system overload

**Implementation:**
```java
public void stopListening() {
    if (sensorManager != null && listener != null) {
        sensorManager.unregisterListener(listener);
        listener = null;
    }
}
```

---

##### **5. isAvailable()**
```java
public boolean isAvailable()
```
- Checks if accelerometer hardware exists on device
- Returns `true` if sensor found, `false` if not
- Useful for graceful degradation

---

##### **6. Callback Setters**
```java
public void setOnShakeListener(OnShakeListener listener)
public void setOnDataChangedListener(OnDataChangedListener listener)
```
- Register callbacks for events
- Can be set after initialization
- Callbacks invoked automatically on sensor data

---

#### Callback Interfaces

##### **OnDataChangedListener**
```java
public interface OnDataChangedListener {
    void onDataChanged(float x, float y, float z, float magnitude);
}
```
- Called every time sensor data arrives (~50 times/second)
- Parameters:
  - `x`: Left-right acceleration (m/s²)
  - `y`: Up-down acceleration (m/s²)
  - `z`: Forward-backward acceleration (m/s²)
  - `magnitude`: Total acceleration (calculated)

**Usage:**
```java
accelerometerSensor.setOnDataChangedListener((x, y, z, mag) -> {
    Log.d("Accel", String.format("X: %.2f, Y: %.2f, Z: %.2f, Mag: %.2f", x, y, z, mag));
});
```

---

##### **OnShakeListener**
```java
public interface OnShakeListener {
    void onShake(float intensity);
}
```
- Called when shake is detected
- Only triggered if `acceleration > SHAKE_THRESHOLD (15.0 m/s²)`
- Debounced to avoid repeated triggers (500ms minimum between shakes)

**Threshold Calculation:**
```
magnitude = sqrt(x² + y² + z²)
If magnitude > 15.0 m/s²  →  Shake detected
```

**Usage:**
```java
accelerometerSensor.setOnShakeListener(intensity -> {
    Toast.makeText(this, "Shake detected: " + intensity, Toast.LENGTH_SHORT).show();
    // Start recording
    startRecording();
});
```

### 📊 Sensor Values Reference

| Condition | X | Y | Z | Magnitude | Action |
|-----------|---|---|---|-----------|--------|
| Stationary | ~0 | ~0 | ~9.8 | ~9.8 | None |
| Slight tilt | ±2 | ±2 | ~9.8 | ~10 | Data only |
| Moderate shake | ±5 | ±5 | ±5 | ~10 | Data only |
| Strong shake | ±10 | ±10 | ±10 | ~17 | **SHAKE** |
| Freefall | ~0 | ~0 | ~0 | ~0 | Data only |

### 💡 Common Use Cases

1. **Shake-to-Record**
   ```java
   setOnShakeListener(intensity -> {
       if (isRecording) stopRecording();
       else startRecording();
   });
   ```

2. **Motion Detection**
   ```java
   setOnDataChangedListener((x, y, z, mag) -> {
       if (mag > 11) {  // Moving
           pauseFocus();
       }
   });
   ```

3. **Gesture Control**
   ```java
   // Detect X-axis shake (left-right)
   setOnDataChangedListener((x, y, z, mag) -> {
       if (Math.abs(x) > 20) {
           // Strong left-right motion
           switchCamera();
       }
   });
   ```

---

## 🔄 SENSOR 2: GYROSCOPE

**Student Assignment:** Student 2  
**File:** `GyroscopeSensor.java` (105 lines)  
**Package:** `com.obs.mobile.sensors`

### 📌 What It Is

The gyroscope measures rotational velocity (how fast device is rotating):
- **X-axis** (pitch - tilt up/down)
- **Y-axis** (roll - tilt left/right)
- **Z-axis** (yaw - rotate left/right)

**Units:** rad/s (radians per second)  
**Sensor Type:** `Sensor.TYPE_GYROSCOPE`  
**Update Frequency:** `SENSOR_DELAY_GAME` (~20ms)  
**Note:** 1 radian ≈ 57.3 degrees

### 🔧 Implementation Details

#### Class Structure

```java
public class GyroscopeSensor {
    // Dependencies
    private Context context;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private SensorEventListener listener;
    
    // Callbacks
    private OnRotationListener onRotationListener;
    private OnRotationGestureListener onRotationGestureListener;
    
    // Gesture detection
    private static final float FAST_ROTATION_THRESHOLD = 100f; // deg/s
    private static final int GESTURE_TIME_THRESHOLD = 300; // ms
    private long lastFastRotationTime = 0;
}
```

#### Public Methods

##### **1. Constructor & Initialization**
```java
public GyroscopeSensor(Context context)
public boolean initialize()
```
- Same pattern as Accelerometer
- Retrieves default gyroscope sensor
- Returns false if not available

---

##### **2. startListening()**
```java
public void startListening()
```
- Creates `SensorEventListener` for gyroscope data
- Registers with SensorManager at `SENSOR_DELAY_GAME`
- Detects fast rotations for gesture recognition

**Key Logic:**
```java
listener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        float rotationX = event.values[0];  // X-axis rotation (rad/s)
        float rotationY = event.values[1];  // Y-axis rotation (rad/s)
        float rotationZ = event.values[2];  // Z-axis rotation (rad/s)
        
        // Convert Z rotation to degrees per second
        float rotationZDeg = radiansToDegrees(rotationZ);
        
        // Notify rotation listener (called every ~20ms)
        if (onRotationListener != null) {
            onRotationListener.onRotation(rotationX, rotationY, rotationZ);
        }
        
        // Detect fast rotation gestures (spin phone around Z-axis)
        long currentTime = System.currentTimeMillis();
        if (Math.abs(rotationZDeg) > FAST_ROTATION_THRESHOLD) {
            if (currentTime - lastFastRotationTime > GESTURE_TIME_THRESHOLD) {
                lastFastRotationTime = currentTime;
                boolean clockwise = rotationZ > 0;
                if (onRotationGestureListener != null) {
                    onRotationGestureListener.onFastRotation(rotationZDeg, clockwise);
                }
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

sensorManager.registerListener(listener, gyroscope, 
    SensorManager.SENSOR_DELAY_GAME);
```

---

##### **3. stopListening()**
```java
public void stopListening()
```
- Unregisters listener
- Resets state to prevent memory leaks
- Essential for battery life

---

##### **4. Utility Methods**
```java
public boolean isAvailable()
public static float radiansToDegrees(float radians)
```
- Conversion helper for human-readable values
- `radiansToDegrees()` uses `Math.toDegrees()`

**Conversion Reference:**
```
π radians = 180°
1 radian ≈ 57.3°
1° ≈ 0.0175 radians
```

---

##### **5. Callback Setters**
```java
public void setOnRotationListener(OnRotationListener listener)
public void setOnRotationGestureListener(OnRotationGestureListener listener)
```

---

#### Callback Interfaces

##### **OnRotationListener**
```java
public interface OnRotationListener {
    void onRotation(float rotationX, float rotationY, float rotationZ);
}
```
- Called continuously (~50 times/second) with rotation values
- All values in **radians per second**
- Useful for:
  - Tracking device orientation changes
  - Video stabilization hints
  - Smooth rotation following

**Usage:**
```java
gyroscopeSensor.setOnRotationListener((x, y, z) -> {
    float xDeg = GyroscopeSensor.radiansToDegrees(x);
    float yDeg = GyroscopeSensor.radiansToDegrees(y);
    float zDeg = GyroscopeSensor.radiansToDegrees(z);
    Log.d("Gyro", String.format("Rotation: X:%.0f° Y:%.0f° Z:%.0f°", xDeg, yDeg, zDeg));
});
```

---

##### **OnRotationGestureListener**
```java
public interface OnRotationGestureListener {
    void onFastRotation(float degreesPerSecond, boolean clockwise);
}
```
- Called when user **spins phone around Z-axis** quickly
- Only triggered if rotation speed > **100 deg/s** (FAST_ROTATION_THRESHOLD)
- Debounced to avoid repeated triggers (300ms minimum)
- Parameters:
  - `degreesPerSecond`: Rotation speed (negative = counter-clockwise)
  - `clockwise`: Direction indicator

**Threshold Reference:**
```
100 deg/s = Very fast rotation (quick twist)
50 deg/s = Moderate rotation
10 deg/s = Slow rotation (won't trigger)
```

**Usage:**
```java
gyroscopeSensor.setOnRotationGestureListener((speed, clockwise) -> {
    String direction = clockwise ? "Clockwise" : "Counter-clockwise";
    Log.d("Gesture", "Fast rotation: " + speed + " deg/s, " + direction);
    
    // Switch to next scene (clockwise = next, counter = previous)
    if (clockwise) {
        nextScene();
    } else {
        previousScene();
    }
});
```

### 📊 Sensor Values Reference

| Movement | X (rad/s) | Y (rad/s) | Z (rad/s) | Action |
|----------|-----------|-----------|-----------|--------|
| Stationary | ~0 | ~0 | ~0 | Nothing |
| Slow pitch | ±0.5 | ~0 | ~0 | Data only |
| Moderate yaw | ~0 | ~0 | ±1 | Data only |
| Quick Z spin | ~0 | ~0 | ±2 | **GESTURE** |

**Z-axis rotation in degrees/second:**
```
0.5 rad/s ≈ 28.6 deg/s
1.0 rad/s ≈ 57.3 deg/s
2.0 rad/s ≈ 114.6 deg/s
3.0 rad/s ≈ 171.9 deg/s (VERY FAST)
```

### 💡 Common Use Cases

1. **Scene Rotation Control**
   ```java
   setOnRotationGestureListener((speed, clockwise) -> {
       if (clockwise) {
           sceneManager.nextScene();
       } else {
           sceneManager.previousScene();
       }
   });
   ```

2. **Video Stabilization Warning**
   ```java
   setOnRotationListener((x, y, z) -> {
       float totalRotation = Math.abs(x) + Math.abs(y) + Math.abs(z);
       if (totalRotation > 2.0) {
           showStabilizationWarning();  // User moving too much
       }
   });
   ```

3. **Portrait/Landscape Detection**
   ```java
   setOnRotationListener((x, y, z) -> {
       if (Math.abs(y) > 1.5) {
           // Device tilted significantly (landscape mode)
           rotateUIToLandscape();
       }
   });
   ```

---

## 💡 SENSOR 3: LIGHT SENSOR

**Student Assignment:** Student 3  
**File:** `LightSensor.java` (120 lines)  
**Package:** `com.obs.mobile.sensors`

### 📌 What It Is

The light sensor measures ambient illumination intensity:
- Detects how bright the environment is
- Used for auto-brightness adjustment
- Helps with camera exposure control

**Units:** Lux (lm/m²) - measure of light intensity  
**Sensor Type:** `Sensor.TYPE_LIGHT`  
**Update Frequency:** `SENSOR_DELAY_NORMAL` (~200ms)  
**Range:** Typically 0 to 100,000 lux (varies by device)

### 🔧 Implementation Details

#### Class Structure

```java
public class LightSensor {
    // Dependencies
    private Context context;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener listener;
    
    // Callback
    private OnLightChangedListener onLightChangedListener;
    
    // Light categorization thresholds
    private static final float VERY_DARK_THRESHOLD = 10f;
    private static final float DARK_THRESHOLD = 50f;
    private static final float NORMAL_THRESHOLD = 500f;
    private static final float BRIGHT_THRESHOLD = 10000f;
}
```

#### Light Categories Enum

```java
public enum LightCategory {
    VERY_DARK("Very Dark", "0-10 lux"),
    DARK("Dark", "10-50 lux"),
    NORMAL("Normal", "50-500 lux"),
    BRIGHT("Bright", "500-10000 lux"),
    VERY_BRIGHT("Very Bright", "10000+ lux");
    
    private final String name;
    private final String range;
    
    LightCategory(String name, String range) { ... }
    public String getName() { return name; }
    public String getRange() { return range; }
}
```

### 📊 Light Levels Reference

| Category | Lux Range | Real-World Examples |
|----------|-----------|-------------------|
| **VERY_DARK** | 0-10 | Dark room, night (no lights) |
| **DARK** | 10-50 | Candlelit room, dimly lit room |
| **NORMAL** | 50-500 | Office indoor, cloudy day |
| **BRIGHT** | 500-10k | Sunny indoor, office bright |
| **VERY_BRIGHT** | 10k+ | Direct sunlight, outdoor noon |

---

#### Public Methods

##### **1. initialize()**
```java
public boolean initialize()
```
- Gets light sensor from SensorManager
- Returns false if not available

---

##### **2. startListening()**
```java
public void startListening()
```
- Registers listener with `SENSOR_DELAY_NORMAL` (less frequent updates)
- Calls `categorizeLightLevel()` to classify brightness
- Invokes callback with lux value and category

**Key Logic:**
```java
listener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values == null || event.values.length == 0) return;
        
        float lux = event.values[0];  // Single value sensor
        LightCategory category = categorizeLightLevel(lux);
        
        if (onLightChangedListener != null) {
            onLightChangedListener.onLightChanged(lux, category);
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

sensorManager.registerListener(listener, lightSensor, 
    SensorManager.SENSOR_DELAY_NORMAL);
```

---

##### **3. categorizeLightLevel()**
```java
private LightCategory categorizeLightLevel(float lux)
```
- Classifies lux value into 5 categories
- Returns appropriate `LightCategory` enum
- Used for user-friendly display

**Implementation:**
```java
private LightCategory categorizeLightLevel(float lux) {
    if (lux < VERY_DARK_THRESHOLD) {
        return LightCategory.VERY_DARK;
    } else if (lux < DARK_THRESHOLD) {
        return LightCategory.DARK;
    } else if (lux < NORMAL_THRESHOLD) {
        return LightCategory.NORMAL;
    } else if (lux < BRIGHT_THRESHOLD) {
        return LightCategory.BRIGHT;
    } else {
        return LightCategory.VERY_BRIGHT;
    }
}
```

---

##### **4. getCameraRecommendation()**
```java
public String getCameraRecommendation(float lux)
```
- Provides camera settings advice based on light level
- Helps camera operator adjust settings

**Recommendations:**
```
VERY_DARK  → "Enable night mode"
DARK       → "Increase ISO"
NORMAL     → "Good indoor lighting"
BRIGHT     → "Optimal conditions"
VERY_BRIGHT→ "Reduce exposure"
```

**Usage:**
```java
lightSensor.setOnLightChangedListener((lux, category) -> {
    String recommendation = lightSensor.getCameraRecommendation(lux);
    tvRecommendation.setText(recommendation);
});
```

---

##### **5. Callback & Availability**
```java
public boolean isAvailable()
public void setOnLightChangedListener(OnLightChangedListener listener)
```

---

#### Callback Interface

##### **OnLightChangedListener**
```java
public interface OnLightChangedListener {
    void onLightChanged(float lux, LightCategory category);
}
```
- Called when light level changes (updates every ~200ms)
- Parameters:
  - `lux`: Exact light value (e.g., 250.5)
  - `category`: Categorized level (e.g., NORMAL)

**Usage:**
```java
lightSensor.setOnLightChangedListener((lux, category) -> {
    // Update UI
    tvLightLevel.setText(String.format("Light: %.0f lux (%s)", lux, category.getName()));
    
    // Auto-adjust brightness
    if (category == LightCategory.VERY_DARK) {
        increaseISO();
        enableNightMode();
    } else if (category == LightCategory.VERY_BRIGHT) {
        decreaseExposure();
        enableHDR();
    }
});
```

### 💡 Common Use Cases

1. **Auto-Brightness Control**
   ```java
   lightSensor.setOnLightChangedListener((lux, category) -> {
       float brightness = Math.min(255, lux / 100);  // Map to 0-255
       setScreenBrightness(brightness);
   });
   ```

2. **Camera Exposure Control**
   ```java
   lightSensor.setOnLightChangedListener((lux, category) -> {
       switch (category) {
           case VERY_DARK: cameraExposure = -2; break;
           case DARK: cameraExposure = -1; break;
           case NORMAL: cameraExposure = 0; break;
           case BRIGHT: cameraExposure = 1; break;
           case VERY_BRIGHT: cameraExposure = 2; break;
       }
       applyExposure(cameraExposure);
   });
   ```

3. **Flash Control**
   ```java
   lightSensor.setOnLightChangedListener((lux, category) -> {
       if (category == LightCategory.VERY_DARK) {
           enableFlash();
       } else {
           disableFlash();
       }
   });
   ```

---

## 📍 SENSOR 4: PROXIMITY SENSOR

**Student Assignment:** Student 4  
**File:** `ProximitySensor.java` (213 lines)  
**Package:** `com.obs.mobile.sensors`

### 📌 What It Is

The proximity sensor detects how close an object is to the device:
- Typically mounted near the earpiece
- Detects if hand/object is near screen
- Binary or distance-based (varies by device)

**Units:** Centimeters (cm) or binary (near/far)  
**Sensor Type:** `Sensor.TYPE_PROXIMITY`  
**Update Frequency:** `SENSOR_DELAY_NORMAL` (~200ms)  
**Typical Max Range:** 5 cm (some devices up to 10cm)

### 🔧 Implementation Details

#### Class Structure

```java
public class ProximitySensor {
    // Dependencies
    private Context context;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener listener;
    
    // Callbacks
    private OnProximityChangedListener onProximityChangedListener;
    private OnNearListener onNearListener;
    private OnFarListener onFarListener;
    
    // State tracking
    private boolean isNear = false;
    private float maxRange = 5f;
    private long lastTriggerTime = 0;
    
    // Debouncing
    private static final float NEAR_THRESHOLD = 3f;  // cm
    private static final int DEBOUNCE_DELAY = 300;   // ms
}
```

### 🔧 Key Concept: Debouncing

Proximity sensors are **noisy** - they fluctuate rapidly. Debouncing prevents false triggers:

```
Raw sensor data:
Time  Distance  Status
0ms   5.0 cm    FAR
20ms  2.5 cm    NEAR ← Noise
40ms  5.0 cm    FAR  ← Noise
60ms  2.0 cm    NEAR ← Real event!
80ms  1.5 cm    NEAR
100ms 1.8 cm    NEAR
...

After debouncing (300ms threshold):
Only triggers if state change persists for 300ms
Ignores rapid fluctuations
```

---

#### Public Methods

##### **1. Constructor & Initialization**
```java
public ProximitySensor(Context context)
public boolean initialize()
```
- Stores context
- Gets proximity sensor from SensorManager
- Stores max range for reference
- Returns false if sensor unavailable

**Important Note:**
```java
maxRange = proximitySensor.getMaximumRange();
// Typical value: 5.0 cm
// This varies by device!
```

---

##### **2. startListening()**
```java
public void startListening()
```
- Creates listener with debouncing logic
- Registers with `SENSOR_DELAY_NORMAL`
- Tracks state changes (near/far)

**Key Logic (Debounced):**
```java
listener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];  // Single value
        boolean currentlyNear = distance < NEAR_THRESHOLD;  // 3cm
        
        long now = System.currentTimeMillis();
        // Skip if too soon since last trigger
        if (now - lastTriggerTime < DEBOUNCE_DELAY) return;
        
        // State changed (near to far or far to near)
        if (currentlyNear != isNear) {
            isNear = currentlyNear;
            lastTriggerTime = now;
            
            // Global callback
            if (onProximityChangedListener != null) {
                onProximityChangedListener.onProximityChanged(distance, isNear);
            }
            
            // Specific callbacks
            if (isNear && onNearListener != null) {
                onNearListener.onNear();
            } else if (!isNear && onFarListener != null) {
                onFarListener.onFar();
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};

sensorManager.registerListener(listener, proximitySensor, 
    SensorManager.SENSOR_DELAY_NORMAL);
```

---

##### **3. stopListening()**
```java
public void stopListening()
```
- Unregisters listener
- Essential for battery life

---

##### **4. State Query Methods**
```java
public boolean isObjectNear()      // Returns current state
public float getMaxRange()         // Returns sensor max range
public boolean isAvailable()       // Returns if sensor exists
```

**Usage:**
```java
if (proximitySensor.isObjectNear()) {
    // User's hand is near device
    pauseRecording();
}

float maxRange = proximitySensor.getMaxRange();
Log.d("Proximity", "Sensor max range: " + maxRange + " cm");
```

---

##### **5. Callback Setters**
```java
public void setOnProximityChangedListener(OnProximityChangedListener listener)
public void setOnNearListener(OnNearListener listener)
public void setOnFarListener(OnFarListener listener)
```

---

#### Callback Interfaces

##### **OnProximityChangedListener** (Most Detailed)
```java
public interface OnProximityChangedListener {
    void onProximityChanged(float distance, boolean isNear);
}
```
- Called when state changes (after debouncing)
- Parameters:
  - `distance`: Actual distance in cm
  - `isNear`: Boolean flag (true if < 3cm)

**Usage:**
```java
proximitySensor.setOnProximityChangedListener((distance, isNear) -> {
    String status = isNear ? "NEAR" : "FAR";
    Log.d("Proximity", String.format("Distance: %.1f cm (%s)", distance, status));
    
    if (isNear) {
        tvStatus.setText("Object detected!");
        tvDistance.setText(String.format("%.1f cm away", distance));
    }
});
```

---

##### **OnNearListener** (Simple Callback)
```java
public interface OnNearListener {
    void onNear();
}
```
- Called only when object becomes NEAR (< 3cm)
- Simple trigger without parameters
- Good for simple actions

**Usage:**
```java
proximitySensor.setOnNearListener(() -> {
    pauseRecording();  // User's face is too close
});
```

---

##### **OnFarListener** (Simple Callback)
```java
public interface OnFarListener {
    void onFar();
}
```
- Called only when object becomes FAR (>= 3cm)
- Opposite of OnNearListener

**Usage:**
```java
proximitySensor.setOnFarListener(() -> {
    resumeRecording();  // User moved face away
});
```

### 📊 Sensor Values Reference

| Distance | Category | Real-World Scenario |
|----------|----------|-------------------|
| 0-1 cm | **Very Near** | Object touching sensor |
| 1-3 cm | **Near** | Hand/face close to screen |
| 3-5 cm | **Far** | Normal usage distance |
| 5+ cm | **Very Far** | Outside sensor range |

### 💡 Common Use Cases

1. **Auto-Focus Trigger**
   ```java
   proximitySensor.setOnNearListener(() -> {
       // User's face is close - trigger auto-focus
       cameraManager.triggerAutoFocus();
   });
   ```

2. **Recording Pause on Proximity**
   ```java
   proximitySensor.setOnProximityChangedListener((distance, isNear) -> {
       if (isNear && isRecording) {
           pauseRecording();  // User blocked camera
       } else if (!isNear && isPaused) {
           resumeRecording();  // Clear again
       }
   });
   ```

3. **Screen Lock Control**
   ```java
   proximitySensor.setOnNearListener(() -> {
       lockScreen();  // User put phone to ear (like phone call)
   });
   
   proximitySensor.setOnFarListener(() -> {
       unlockScreen();
   });
   ```

---

## 🧭 SENSOR 5: MAGNETOMETER

**Student Assignment:** Student 5  
**File:** `MagnetometerSensor.java` (293 lines)  
**Package:** `com.obs.mobile.sensors`

### 📌 What It Is

The magnetometer measures Earth's magnetic field in 3 axes:
- Creates a digital compass
- **REQUIRES accelerometer** to function properly
- Uses sensor fusion (accelerometer + magnetometer)

**Units:** Microteslas (µT)  
**Sensor Types:** `Sensor.TYPE_MAGNETIC_FIELD` + `Sensor.TYPE_ACCELEROMETER`  
**Update Frequency:** `SENSOR_DELAY_UI` (~67ms)  
**Output:** Azimuth (0-360°) + 8 compass directions

### 🔧 Implementation Details

#### Class Structure

```java
public class MagnetometerSensor {
    // Dependencies
    private Context context;
    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;  // REQUIRED!
    private SensorEventListener magnetometerListener;
    private SensorEventListener accelerometerListener;
    
    // Callbacks
    private OnCompassChangeListener onCompassChangeListener;
    private OnDirectionChangeListener onDirectionChangeListener;
    
    // Sensor data (sensor fusion)
    private float[] gravity = new float[3];          // From accelerometer
    private float[] geomagnetic = new float[3];      // From magnetometer
    private float[] rotationMatrix = new float[9];   // Calculated
    private float[] orientation = new float[3];      // Result (azimuth in [0])
    
    // State
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;
    private CompassDirection currentDirection = null;
}
```

#### Compass Direction Enum

```java
public enum CompassDirection {
    NORTH("North", 0, "N"),
    NORTH_EAST("North-East", 45, "NE"),
    EAST("East", 90, "E"),
    SOUTH_EAST("South-East", 135, "SE"),
    SOUTH("South", 180, "S"),
    SOUTH_WEST("South-West", 225, "SW"),
    WEST("West", 270, "W"),
    NORTH_WEST("North-West", 315, "NW");
    
    private final String name;
    private final int degrees;
    private final String abbreviation;
    
    // getters...
}
```

### 🧭 How Compass Works (Sensor Fusion)

```
Step 1: Get accelerometer data (gravity vector)
   ↓
Step 2: Get magnetometer data (magnetic field vector)
   ↓
Step 3: Use SensorManager.getRotationMatrix()
        Calculates 9x9 rotation matrix from both vectors
   ↓
Step 4: Use SensorManager.getOrientation()
        Extracts Azimuth from rotation matrix
   ↓
Step 5: Convert azimuth to degrees and compass direction
   ↓
Step 6: Invoke callbacks
```

**Why Both Sensors?**
- **Accelerometer alone** → Knows which way is "down" (gravity)
- **Magnetometer alone** → Knows which way is "North" (magnetic field)
- **Both together** → Full 3D orientation + compass direction

---

#### Public Methods

##### **1. Constructor & Initialization**
```java
public MagnetometerSensor(Context context)
public boolean initialize()
```
- Gets **both** magnetometer AND accelerometer
- Returns false if either is missing
- Initializes state flags

**Critical Check:**
```java
public boolean initialize() {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    return magnetometer != null && accelerometer != null;  // BOTH required!
}
```

---

##### **2. startListening()**
```java
public void startListening()
```
- Creates **two** listeners (magnetometer + accelerometer)
- Registers both with SensorManager
- Calls `calculateOrientation()` when new data arrives

**Magnetometer Listener:**
```java
magnetometerListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Store magnetic field vector
        geomagnetic[0] = event.values[0];
        geomagnetic[1] = event.values[1];
        geomagnetic[2] = event.values[2];
        hasGeomagnetic = true;
        calculateOrientation();  // Try to calculate if both ready
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            // Magnetic interference detected!
            // Recommend calibration (figure-8 motion)
        }
    }
};
```

**Accelerometer Listener:**
```java
accelerometerListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Store gravity vector
        gravity[0] = event.values[0];
        gravity[1] = event.values[1];
        gravity[2] = event.values[2];
        hasGravity = true;
        calculateOrientation();  // Try to calculate if both ready
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
};
```

---

##### **3. calculateOrientation()** (Core Algorithm)
```java
private void calculateOrientation()
```
- **Most important method**
- Only executes if BOTH sensors have data
- Uses Android's sensor fusion algorithms

**Key Steps:**
```java
private void calculateOrientation() {
    // Step 1: Check if both sensors are ready
    if (!hasGravity || !hasGeomagnetic) {
        return;  // Wait for both
    }
    
    // Step 2: Calculate rotation matrix (sensor fusion magic!)
    boolean success = SensorManager.getRotationMatrix(
        rotationMatrix,      // Output: 9x9 rotation matrix
        null,                // Optional: geomagnetic inclination
        gravity,             // Input: accelerometer vector
        geomagnetic          // Input: magnetometer vector
    );
    
    if (!success) return;  // Calculation failed
    
    // Step 3: Extract orientation from rotation matrix
    SensorManager.getOrientation(rotationMatrix, orientation);
    // orientation[0] = azimuth (radians)
    // orientation[1] = pitch (radians)
    // orientation[2] = roll (radians)
    
    // Step 4: Convert azimuth from radians to degrees
    float azimuthRad = orientation[0];
    float azimuthDeg = (float) Math.toDegrees(azimuthRad);
    
    // Step 5: Normalize to 0-360 range
    azimuthDeg = (azimuthDeg + 360) % 360;
    // (adds 360 in case negative, then mod 360)
    
    // Step 6: Convert degree azimuth to compass direction
    CompassDirection direction = getDirectionFromAzimuth(azimuthDeg);
    
    // Step 7: Invoke compass callback
    if (onCompassChangeListener != null) {
        onCompassChangeListener.onCompassChange(azimuthDeg, direction);
    }
    
    // Step 8: Invoke direction callback only if changed
    if (direction != currentDirection) {
        currentDirection = direction;
        if (onDirectionChangeListener != null) {
            onDirectionChangeListener.onDirectionChange(direction);
        }
    }
}
```

---

##### **4. getDirectionFromAzimuth()** (Direction Mapping)
```java
private CompassDirection getDirectionFromAzimuth(float azimuth)
```
- Maps 360 degrees to 8 compass directions
- Uses 45° sectors around each cardinal direction

**Mapping Logic:**
```
0° (North)    ← azimuth -22 to +22
45° (NE)      ← azimuth 22 to 67
90° (East)    ← azimuth 67 to 112
135° (SE)     ← azimuth 112 to 157
180° (South)  ← azimuth 157 to 202
225° (SW)     ← azimuth 202 to 247
270° (West)   ← azimuth 247 to 292
315° (NW)     ← azimuth 292 to 338
360° (North)  ← azimuth 338 to 360
```

**Implementation:**
```java
private CompassDirection getDirectionFromAzimuth(float azimuth) {
    int rounded = Math.round(azimuth);
    
    if ((rounded >= 0 && rounded < 22) || (rounded >= 338 && rounded <= 360)) {
        return CompassDirection.NORTH;
    } else if (rounded >= 22 && rounded < 67) {
        return CompassDirection.NORTH_EAST;
    } else if (rounded >= 67 && rounded < 112) {
        return CompassDirection.EAST;
    } else if (rounded >= 112 && rounded < 157) {
        return CompassDirection.SOUTH_EAST;
    } else if (rounded >= 157 && rounded < 202) {
        return CompassDirection.SOUTH;
    } else if (rounded >= 202 && rounded < 247) {
        return CompassDirection.SOUTH_WEST;
    } else if (rounded >= 247 && rounded < 292) {
        return CompassDirection.WEST;
    } else {
        return CompassDirection.NORTH_WEST;
    }
}
```

---

##### **5. stopListening()**
```java
public void stopListening()
```
- Unregisters BOTH listeners
- Resets state flags
- Critical for battery life

```java
public void stopListening() {
    if (sensorManager != null) {
        if (magnetometerListener != null) {
            sensorManager.unregisterListener(magnetometerListener);
        }
        if (accelerometerListener != null) {
            sensorManager.unregisterListener(accelerometerListener);
        }
    }
    
    hasGravity = false;
    hasGeomagnetic = false;
}
```

---

##### **6. State & Utility Methods**
```java
public boolean isAvailable()
public CompassDirection getCurrentDirection()
public void setOnCompassChangeListener(OnCompassChangeListener listener)
public void setOnDirectionChangeListener(OnDirectionChangeListener listener)
```

---

#### Callback Interfaces

##### **OnCompassChangeListener** (Continuous)
```java
public interface OnCompassChangeListener {
    void onCompassChange(float azimuth, CompassDirection direction);
}
```
- Called whenever compass reading updates (~15 times/second)
- Provides exact degree + compass direction
- Good for smooth compass needle rotation

**Usage:**
```java
magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
    // Update compass UI needle
    compassNeedle.setRotation(azimuth);
    
    // Update direction text
    tvDirection.setText(direction.getName());
    tvAzimuth.setText(String.format("%.0f°", azimuth));
});
```

---

##### **OnDirectionChangeListener** (Events)
```java
public interface OnDirectionChangeListener {
    void onDirectionChange(CompassDirection direction);
}
```
- Called only when compass direction **changes** (8 possible directions)
- Less frequent than `OnCompassChangeListener`
- Good for trigger-based actions

**Usage:**
```java
magnetometerSensor.setOnDirectionChangeListener(direction -> {
    Log.d("Compass", "Direction changed to: " + direction.getName());
    
    switch (direction) {
        case NORTH: setSceneOrientation(0); break;
        case EAST: setSceneOrientation(90); break;
        case SOUTH: setSceneOrientation(180); break;
        case WEST: setSceneOrientation(270); break;
        default: break;
    }
});
```

### ⚠️ Important: Calibration

Magnetometer requires calibration if accuracy drops:

```java
onAccuracyChanged() {
    if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
        // Show calibration prompt
        showCalibrationDialog(
            "Move device in figure-8 pattern\nto calibrate magnetometer"
        );
    }
}
```

**Calibration Motion:**
```
Move device in a "figure-8" pattern
for 30 seconds to recalibrate
```

### 📊 Sensor Values Reference

| Azimuth | Direction | Practical Use |
|---------|-----------|--------------|
| 0° ± 22° | **NORTH** | Face camera north |
| 45° ± 22° | **NORTH_EAST** | Intermediate |
| 90° ± 22° | **EAST** | Face camera east |
| 135° ± 22° | **SOUTH_EAST** | Intermediate |
| 180° ± 22° | **SOUTH** | Face camera south |
| 225° ± 22° | **SOUTH_WEST** | Intermediate |
| 270° ± 22° | **WEST** | Face camera west |
| 315° ± 22° | **NORTH_WEST** | Intermediate |

### 💡 Common Use Cases

1. **Compass Display**
   ```java
   magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
       compassView.setAzimuth(azimuth);
       tvDirection.setText(direction.getAbbreviation());
   });
   ```

2. **Direction-Based Scene Selection**
   ```java
   magnetometerSensor.setOnDirectionChangeListener(direction -> {
       switch (direction) {
           case NORTH: loadScene("Landscape North"); break;
           case EAST: loadScene("Landscape East"); break;
           case SOUTH: loadScene("Landscape South"); break;
           case WEST: loadScene("Landscape West"); break;
       }
   });
   ```

3. **Video Orientation Guidance**
   ```java
   magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> {
       if (direction == CompassDirection.NORTH) {
           tvGuidance.setText("✓ Good! Face north");
           tvGuidance.setTextColor(Color.GREEN);
       } else {
           tvGuidance.setText("Rotate to face north");
           tvGuidance.setTextColor(Color.YELLOW);
       }
   });
   ```

---

## 🔐 UTILITY CLASS: SensorPreferences

**File:** `SensorPreferences.java` (90 lines)  
**Package:** `com.obs.mobile.utils`

### Purpose
Manages persistent sensor on/off states using Android's SharedPreferences

### Implementation

```java
public class SensorPreferences {
    private static final String PREF_NAME = "sensor_preferences";
    
    // Storage keys
    private static final String KEY_ACCELEROMETER_ENABLED = "accelerometer_enabled";
    private static final String KEY_GYROSCOPE_ENABLED = "gyroscope_enabled";
    private static final String KEY_LIGHT_SENSOR_ENABLED = "light_sensor_enabled";
    private static final String KEY_PROXIMITY_ENABLED = "proximity_enabled";
    private static final String KEY_MAGNETOMETER_ENABLED = "magnetometer_enabled";
    
    // Helper to get preferences object
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
```

### Public API

For **each sensor**, there are two methods:

```java
// Accelerometer example:
public static boolean isAccelerometerEnabled(Context context)
public static void setAccelerometerEnabled(Context context, boolean enabled)

// Gyroscope example:
public static boolean isGyroscopeEnabled(Context context)
public static void setGyroscopeEnabled(Context context, boolean enabled)

// ... same pattern for Light, Proximity, Magnetometer
```

### Utility Method

```java
public static int getEnabledSensorCount(Context context)
```
- Returns how many sensors are currently enabled (0-5)
- Useful for checking overall system load

### Usage Example

```java
// In SensorsActivity
if (SensorPreferences.isAccelerometerEnabled(this)) {
    // Accelerometer is on
    accelerometerSensor.startListening();
}

// Save state when user toggles switch
switchAccelerometer.setOnCheckedChangeListener((button, isChecked) -> {
    SensorPreferences.setAccelerometerEnabled(this, isChecked);
    if (isChecked) {
        accelerometerSensor.startListening();
    } else {
        accelerometerSensor.stopListening();
    }
});
```

---

## 📊 Sensors Comparison Table

| Feature | Accel | Gyro | Light | Proximity | Magnetometer |
|---------|-------|------|-------|-----------|--------------|
| **Axes** | 3 (X,Y,Z) | 3 (X,Y,Z) | 1 | 1 | 3 + Accel |
| **Units** | m/s² | rad/s | Lux | cm | µT + degrees |
| **Update Rate** | GAME | GAME | NORMAL | NORMAL | UI |
| **Frequency** | ~50/s | ~50/s | ~5/s | ~5/s | ~15/s |
| **Features** | Shake detection | Fast rotation | Light category | Debouncing | Compass |
| **Battery Impact** | High | High | Low | Low | Medium |
| **Availability** | ~100% | ~95% | ~70% | ~80% | ~100% |
| **Complexity** | Low | Low | Low | Medium | High |
| **Uses** | Recording trigger | Scene switch | Auto-brightness | Focus trigger | Navigation |

---

## 🔧 Integration Flow Diagram

```
CameraActivity
    ├─→ Create sensor instances
    │   ├─ AccelerometerSensor accel
    │   ├─ GyroscopeSensor gyro
    │   ├─ LightSensor light
    │   ├─ ProximitySensor proximity
    │   └─ MagnetometerSensor magnetometer
    │
    ├─→ Initialize in onCreate()
    │   └─ sensor.initialize()  ← Check availability
    │
    ├─→ Start listening in onResume()
    │   └─ sensor.startListening()  ← Register callbacks
    │
    ├─→ Set callbacks
    │   ├─ accel.setOnDataChangedListener()
    │   ├─ gyro.setOnRotationListener()
    │   ├─ light.setOnLightChangedListener()
    │   ├─ proximity.setOnProximityChangedListener()
    │   └─ magnetometer.setOnCompassChangeListener()
    │
    ├─→ Handle sensor data
    │   ├─ Update camera settings
    │   ├─ Update UI overlays
    │   ├─ Control recording
    │   └─ Stream to Python
    │
    └─→ Stop listening in onPause()
        └─ sensor.stopListening()  ← Unregister (battery)
```

---

## 🔌 How SensorDataStreamer Integrates

```
Sensor Data (Real-time)
    ↓
SensorDataStreamer.currentSensorValues updated
    ↓
Every 100ms: Convert to JSON
    ↓
Send via UDP to Python (192.168.1.100:5000)
    ↓
Python receives and processes
    ├─→ Face detection
    ├─→ Emotion recognition
    └─→ Display on stream
```

**JSON Packet Structure:**
```json
{
  "accelerometer": { "x": 0.5, "y": 0.5, "z": 9.8, "magnitude": 10.0 },
  "gyroscope": { "x": 0.1, "y": 0.1, "z": 0.05 },
  "light": { "lux": 250.0, "category": "Normal" },
  "proximity": { "distance": 5.0, "isNear": false },
  "magnetometer": { "azimuth": 45.0, "direction": "NE" }
}
```

---

## ⚡ Performance Optimization Tips

### 1. **Update Frequency Selection**
```java
// For high-frequency needs (gaming, video):
SensorManager.SENSOR_DELAY_GAME        // ~20ms, HIGH power

// For normal UI updates:
SensorManager.SENSOR_DELAY_UI          // ~67ms, MEDIUM power

// For background/monitoring:
SensorManager.SENSOR_DELAY_NORMAL      // ~200ms, LOW power

// For absolute minimum power:
SensorManager.SENSOR_DELAY_FASTEST     // Device maximum, HIGHEST power
```

### 2. **Battery Conservation**
```java
// DO: Always stop sensors in onPause()
@Override
protected void onPause() {
    super.onPause();
    accelerometerSensor.stopListening();
    gyroscopeSensor.stopListening();
    // etc...
}

// DON'T: Leave sensors running in background
// = 30% battery drain in 1 hour
```

### 3. **Filter Noisy Data**
```java
// Apply low-pass filter to proximity
private float previousDistance = 0;
private static final float FILTER_ALPHA = 0.7f;

proximitySensor.setOnProximityChangedListener((distance, isNear) -> {
    // Smooth value
    previousDistance = (FILTER_ALPHA * distance) + 
                      ((1 - FILTER_ALPHA) * previousDistance);
});
```

---

## 📋 Summary Table

| Aspect | Details |
|--------|---------|
| **Total Sensor Classes** | 5 independent classes |
| **Total Lines of Code** | ~832 lines (all sensors) |
| **Pattern Used** | Observer/Callback pattern |
| **Lifecycle** | init() → start() → [listen] → stop() |
| **Thread Safety** | Android SensorManager handles threading |
| **Persistence** | SharedPreferences via SensorPreferences |
| **Python Integration** | SensorDataStreamer (UDP JSON) |
| **Default State** | All sensors disabled (off) |
| **Broadcast Support** | Intent broadcasts for state changes |

---

## 🎓 Learning Outcome for Students

After completing sensor implementations, students will understand:

✅ Android sensor architecture and SensorManager  
✅ Real-time event-driven programming (callbacks)  
✅ Multi-threaded sensor data handling  
✅ Debouncing and filtering techniques  
✅ Sensor fusion (magnetometer + accelerometer)  
✅ Energy efficiency and lifecycle management  
✅ Android permissions and capability detection  
✅ Persistent data storage (SharedPreferences)  
✅ Inter-process communication (IPC/Broadcasting)  
✅ Integration with camera and recording systems  

---

**Document Version:** 1.0  
**Last Updated:** January 2, 2026  
**Status:** Complete Technical Reference  

