# 📊 SENSORS QUICK REFERENCE CARD

## At a Glance: All 5 Sensors

```
┌─────────────────┬─────────────┬──────────────┬─────────────┬─────────────┐
│  ACCELEROMETER  │  GYROSCOPE  │   LIGHT      │ PROXIMITY   │MAGNETOMETER │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ Measures linear │ Measures    │ Measures     │ Detects     │ Measures    │
│ acceleration in │ rotational  │ ambient      │ objects     │ Earth's     │
│ 3 axes          │ velocity    │ illumination │ near device │ magnetic    │
│ X, Y, Z (m/s²)  │ X, Y, Z (r/s)│ (Lux)       │ (cm)        │ field (µT)  │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ Student: 1      │ Student: 2  │ Student: 3   │ Student: 4  │ Student: 5  │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ ✅ Shake detect │ ✅ Rotation │ ✅ 5 light   │ ✅ Debounce │ ✅ Compass  │
│ ✅ Recording    │ ✅ Gestures │ ✅ Auto-    │ ✅ Near/Far │ ✅ 8 direct │
│    trigger      │ ✅ Video    │    brightness│ ✅ Auto-    │ ✅ Sensor   │
│ ✅ Scene switch │    stab     │ ✅ Flash    │    focus    │    fusion   │
│                 │             │    control  │             │             │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ Threshold:      │ Threshold:  │ Categories: │ Threshold:  │ Needs:      │
│ 15 m/s²         │ 100 deg/s   │ 0,10,50,500 │ 3 cm        │ Both mag+   │
│ Debounce:       │ Debounce:   │ 10000 lux   │ Debounce:   │ accel       │
│ 500 ms          │ 300 ms      │             │ 300 ms      │             │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ Update Rate:    │ Update Rate:│ Update Rate:│ Update Rate:│ Update Rate:│
│ SENSOR_DELAY_   │ SENSOR_DELAY│ SENSOR_DELAY│ SENSOR_DELAY│ SENSOR_DELAY│
│ GAME (~20ms)    │ _GAME       │ _NORMAL     │ _NORMAL     │ _UI         │
│ ~50 updates/s   │ (~20ms)     │ (~200ms)    │ (~200ms)    │ (~67ms)     │
│                 │ ~50/s       │ ~5/s        │ ~5/s        │ ~15/s       │
├─────────────────┼─────────────┼──────────────┼─────────────┼─────────────┤
│ Availability:   │ Availability│ Availability│ Availability│ Availability│
│ ~100%           │ ~95%        │ ~70%        │ ~80%        │ ~100%       │
│                 │             │             │             │ (if accel)  │
└─────────────────┴─────────────┴──────────────┴─────────────┴─────────────┘
```

---

## 🎯 Student 1: Accelerometer

### What It Does
Detects motion and shaking in 3 dimensions. Triggers shake events and tracks device movement.

### Public Methods
| Method | Returns | Purpose |
|--------|---------|---------|
| `initialize()` | boolean | Check if available |
| `startListening()` | void | Start receiving data |
| `stopListening()` | void | Stop receiving data |
| `isAvailable()` | boolean | Is sensor present? |
| `setOnDataChangedListener()` | void | Register data callback |
| `setOnShakeListener()` | void | Register shake callback |

### Callbacks
```java
// Called ~50 times/second
onDataChanged(float x, float y, float z, float magnitude)

// Called when acceleration > 15 m/s²
onShake(float intensity)
```

### Key Values
- X, Y, Z: -30 to +30 m/s²
- Magnitude: 0 to 50 m/s²
- Stationary: magnitude ≈ 9.8 m/s² (gravity)

### Common Actions
- ✅ Start/stop recording on shake
- ✅ Switch scenes based on motion
- ✅ Gesture-based scene control
- ✅ Motion-triggered focus

---

## 🔄 Student 2: Gyroscope

### What It Does
Detects device rotation speed. Identifies fast spins for scene navigation.

### Public Methods
| Method | Returns | Purpose |
|--------|---------|---------|
| `initialize()` | boolean | Check if available |
| `startListening()` | void | Start receiving data |
| `stopListening()` | void | Stop receiving data |
| `isAvailable()` | boolean | Is sensor present? |
| `setOnRotationListener()` | void | Register rotation callback |
| `setOnRotationGestureListener()` | void | Register gesture callback |
| `radiansToDegrees()` | float | Convert rad/s to deg/s |

### Callbacks
```java
// Called ~50 times/second
onRotation(float rotX, float rotY, float rotZ)  // in rad/s

// Called when |Z-rotation| > 100 deg/s
onFastRotation(float degreesPerSecond, boolean clockwise)
```

### Key Values
- X, Y, Z: -5 to +5 rad/s
- In degrees: -286 to +286 deg/s
- Fast rotation: > 100 deg/s (≈1.7 rad/s)

### Common Actions
- ✅ Scene navigation (clockwise/counter-clockwise)
- ✅ Video stabilization warnings
- ✅ Screen rotation control
- ✅ Portrait/landscape detection

---

## 💡 Student 3: Light Sensor

### What It Does
Measures ambient brightness. Categorizes light into 5 levels for camera control.

### Public Methods
| Method | Returns | Purpose |
|--------|---------|---------|
| `initialize()` | boolean | Check if available |
| `startListening()` | void | Start receiving data |
| `stopListening()` | void | Stop receiving data |
| `isAvailable()` | boolean | Is sensor present? |
| `setOnLightChangedListener()` | void | Register callback |
| `getCameraRecommendation()` | String | Get camera advice |
| `categorizeLightLevel()` | LightCategory | Classify lux value |

### Callbacks
```java
// Called ~5 times/second when light changes
onLightChanged(float lux, LightCategory category)
```

### Light Categories
| Category | Range | Real-World Example |
|----------|-------|-------------------|
| VERY_DARK | 0-10 lux | Dark room |
| DARK | 10-50 lux | Candle light |
| NORMAL | 50-500 lux | Office |
| BRIGHT | 500-10k lux | Sunny room |
| VERY_BRIGHT | 10k+ lux | Direct sun |

### Camera Recommendations
```
VERY_DARK  → "Enable night mode"
DARK       → "Increase ISO"
NORMAL     → "Good indoor lighting"
BRIGHT     → "Optimal conditions"
VERY_BRIGHT → "Reduce exposure"
```

### Common Actions
- ✅ Auto-brightness adjustment
- ✅ Camera ISO control
- ✅ Flash on/off control
- ✅ Screen dimming

---

## 📍 Student 4: Proximity Sensor

### What It Does
Detects objects near the device front. Useful for auto-focus and obstruction detection.

### Public Methods
| Method | Returns | Purpose |
|--------|---------|---------|
| `initialize()` | boolean | Check if available |
| `startListening()` | void | Start receiving data |
| `stopListening()` | void | Stop receiving data |
| `isAvailable()` | boolean | Is sensor present? |
| `isObjectNear()` | boolean | Current state |
| `getMaxRange()` | float | Max detectable distance |
| `setOnProximityChangedListener()` | void | Register change callback |
| `setOnNearListener()` | void | Register near-only callback |
| `setOnFarListener()` | void | Register far-only callback |

### Callbacks
```java
// Called when state changes (debounced 300ms)
onProximityChanged(float distance, boolean isNear)

// Called only when object becomes NEAR
onNear()

// Called only when object becomes FAR
onFar()
```

### Key Values
- Distance: 0-5 cm (typically)
- Near Threshold: < 3 cm
- Max Range: 5 cm (varies by device)

### Common Actions
- ✅ Auto-focus trigger
- ✅ Recording pause on obstruction
- ✅ Screen lock on proximity
- ✅ Hand detection warnings

---

## 🧭 Student 5: Magnetometer

### What It Does
Creates a digital compass by combining magnetometer + accelerometer data.

### Public Methods
| Method | Returns | Purpose |
|--------|---------|---------|
| `initialize()` | boolean | Check if available (both sensors) |
| `startListening()` | void | Start receiving data |
| `stopListening()` | void | Stop receiving data |
| `isAvailable()` | boolean | Are both sensors present? |
| `getCurrentDirection()` | CompassDirection | Current compass direction |
| `setOnCompassChangeListener()` | void | Register compass callback |
| `setOnDirectionChangeListener()` | void | Register direction change callback |

### Callbacks
```java
// Called ~15 times/second
onCompassChange(float azimuth, CompassDirection direction)

// Called only when direction changes (8 possible)
onDirectionChange(CompassDirection direction)
```

### Compass Directions
| Direction | Azimuth | Abbrev |
|-----------|---------|--------|
| North | 0° | N |
| North-East | 45° | NE |
| East | 90° | E |
| South-East | 135° | SE |
| South | 180° | S |
| South-West | 225° | SW |
| West | 270° | W |
| North-West | 315° | NW |

### Key Values
- Azimuth: 0-360°
- Direction: 8 cardinal directions
- ⚠️ Requires calibration (figure-8 motion) if inaccurate

### Common Actions
- ✅ Compass display
- ✅ Direction-based scene selection
- ✅ Navigation guidance
- ✅ Video orientation hints

---

## 📋 Basic Implementation Template

Use this template for each sensor:

```java
// 1. Declare
private [Sensor]Sensor sensorName;

// 2. Create (onCreate)
sensorName = new [Sensor]Sensor(this);

// 3. Check Availability (onCreate)
if (!sensorName.initialize()) {
    Log.w("Sensor", "[Sensor] not available");
    return;
}

// 4. Set Callbacks (onCreate)
sensorName.setOn[Event]Listener((params) -> {
    // Handle event
});

// 5. Start (onResume)
sensorName.startListening();

// 6. Stop (onPause)
sensorName.stopListening();
```

---

## 🔌 Integration with SensorDataStreamer

All sensor data is automatically sent to Python every 100ms:

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

## 💾 Persistence with SensorPreferences

Save/load sensor enabled states:

```java
// Save
SensorPreferences.setAccelerometerEnabled(context, true);

// Load
boolean isEnabled = SensorPreferences.isAccelerometerEnabled(context);

// Get count
int count = SensorPreferences.getEnabledSensorCount(context);
```

---

## ⚡ Battery Impact (High to Low)

1. **Accelerometer** (HIGH) - 50 updates/sec
2. **Gyroscope** (HIGH) - 50 updates/sec
3. **Magnetometer** (MEDIUM) - 15 updates/sec
4. **Light** (LOW) - 5 updates/sec
5. **Proximity** (LOW) - 5 updates/sec

**Critical:** Always stop sensors in onPause() to save battery!

---

## 🐛 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Sensor always null | Device doesn't have sensor | Check `isAvailable()` |
| No data from sensor | Didn't call `startListening()` | Call in onResume() |
| Battery drains fast | Sensors left running in background | Call `stopListening()` in onPause() |
| Jittery motion | Sensor noise | Add low-pass filter or debounce |
| Compass spins wildly | Magnetic interference | Show calibration message |
| Proximity keeps flickering | Debouncing too aggressive | Adjust DEBOUNCE_DELAY |
| Light never changes | Sensor delay too long | Check SENSOR_DELAY_NORMAL |

---

## 📊 File Locations

```
SensorImplementations:
├── AccelerometerSensor.java (101 lines)
├── GyroscopeSensor.java (105 lines)
├── LightSensor.java (120 lines)
├── ProximitySensor.java (213 lines)
├── MagnetometerSensor.java (293 lines)
└── SensorPreferences.java (90 lines)

Location: /app/src/main/java/com/obs/mobile/sensors/
Location: /app/src/main/java/com/obs/mobile/utils/
```

---

## 🎯 What Each Student Needs to Do

### Student 1 (Accelerometer)
- [ ] Understand shake detection algorithm
- [ ] Implement record-on-shake feature
- [ ] Add shake-intensity visualization
- [ ] Create scene-switching on different shake patterns
- [ ] Test with real device shaking

### Student 2 (Gyroscope)
- [ ] Understand rotation detection
- [ ] Implement fast-spin scene navigation
- [ ] Add rotation smoothing
- [ ] Create video-stabilization warnings
- [ ] Test with device rotation gestures

### Student 3 (Light Sensor)
- [ ] Understand light categorization
- [ ] Implement auto-brightness control
- [ ] Add camera exposure adjustment
- [ ] Create flash-on/off logic
- [ ] Test in various lighting conditions

### Student 4 (Proximity)
- [ ] Understand debouncing technique
- [ ] Implement auto-focus trigger
- [ ] Add obstruction detection
- [ ] Create recording pause-on-block
- [ ] Test with hand near camera

### Student 5 (Magnetometer)
- [ ] Understand sensor fusion (mag + accel)
- [ ] Implement compass display
- [ ] Add direction-based scene selection
- [ ] Create calibration guidance
- [ ] Test in different locations

---

## ✅ Verification Checklist

For each sensor implementation:

- [ ] Sensor initializes successfully
- [ ] Callbacks are invoked
- [ ] Data is displayed on screen
- [ ] State persists after app restart
- [ ] Feature works as intended
- [ ] Battery is not excessively drained
- [ ] Device orientation doesn't break it
- [ ] Works without internet connection
- [ ] Graceful degradation if sensor missing
- [ ] No crashes or exceptions

---

## 📚 Related Files

**Main Documentation:**
- `SENSORS_DETAILED_REPORT.md` - Complete technical reference
- `SENSORS_IMPLEMENTATION_GUIDE.md` - Copy-paste code examples
- `README.md` - Project overview

**Integration Points:**
- `CameraActivity.java` - Where sensors are used
- `SensorsActivity.java` - Sensor UI and settings
- `SensorDataStreamer.java` - Python integration

---

**Quick Reference Version:** 1.0  
**Last Updated:** January 2, 2026  
**Print This Page for Quick Lookup!**

