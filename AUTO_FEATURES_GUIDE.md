# Auto Camera & Screen Features Guide

## Overview
This guide explains the automatic features implemented using sensor data to enhance the OBS Mobile camera experience.

---

## 🎥 Auto Zoom & Focus on Proximity Detection

### Feature Description
The camera automatically adjusts focus when the proximity sensor detects an object approaching the device. This simulates face detection and auto-zoom behavior.

### How It Works
- **Proximity Sensor Input**: Detects distance to objects (typically faces)
- **Focus Trigger**: When object is near (< ~5cm), the camera triggers autofocus
- **Continuous Focus**: Maintains continuous autofocus when object is farther away

### Implementation Details

#### File: `CameraActivity.java`

**Field Added:**
```java
private boolean autoFocusOnProximityEnabled = false;
```

**Key Methods:**

1. **`setupProximitySensor()`** - Enhanced to trigger focus
   - Initializes proximity sensor
   - Sets up proximity change listener
   - Calls `adjustCameraFocusByProximity()` when proximity changes
   - Enables auto-focus when proximity sensor is enabled

2. **`adjustCameraFocusByProximity(float distanceCm, boolean isNear)`**
   - Triggers autofocus when object detected near (≤5cm)
   - Maintains continuous autofocus when object is far
   - Logs all focus adjustments for debugging
   - Handles camera exceptions gracefully

3. **`resetCameraFocus()`** - Reset focus to default
   - Cancels active focus trigger
   - Returns to continuous autofocus mode
   - Available for manual control if needed

#### Focus Logic:

**When Object is NEAR (isNear = true):**
```
1. Start focus trigger: CONTROL_AF_TRIGGER_START
2. Set to continuous autofocus: CONTROL_AF_MODE_CONTINUOUS_PICTURE
3. Capture frame with focus request
4. Reset trigger to idle: CONTROL_AF_TRIGGER_IDLE
5. Continue with repeating request
```

**When Object is FAR (isNear = false):**
```
1. Maintain continuous autofocus
2. Set AF mode: CONTROL_AF_MODE_CONTINUOUS_PICTURE
3. Continue with repeating request
```

#### Example Log Output:
```
Auto Focus: Object detected at 3.2 cm - focusing...
Auto Focus: Object far (8.5 cm) - continuous focus active
```

### Usage

**In Camera Activity:**
1. Open Camera app
2. Go to Sensors Settings
3. Enable "Proximity Sensor" toggle
4. When enabled:
   - Camera will automatically focus when you bring object near
   - Focus releases when object moves away
   - Continuous autofocus maintains focus while object is farther

**In Sensors Activity:**
1. Open Settings
2. Toggle "Proximity" on
3. Notice message: "⭐ Auto-focus enabled in Camera"
4. All proximity changes are logged and displayed
5. Disable to turn off auto-focus

---

## 💡 Auto Screen Brightness Control

### Feature Description
The screen brightness automatically adjusts based on ambient light level detected by the light sensor.

### How It Works
- **Light Sensor Input**: Measures ambient light in lux (0-50000+)
- **Brightness Mapping**: Converts lux to screen brightness (0.2-1.0)
- **Dynamic Adjustment**: Updates brightness in real-time as light changes

### Implementation Details

#### File: `CameraActivity.java` & `SensorsActivity.java`

**Field Added:**
```java
private boolean autoBrightnessEnabled = false;
```

**Key Methods:**

1. **`setupLightSensor()`** - Enhanced to control brightness
   - Initializes light sensor
   - Calls `adjustScreenBrightness()` on light changes
   - Enables brightness control when light sensor is enabled

2. **`adjustScreenBrightness(float lux)`**
   - Maps lux values to brightness level using logarithmic scale
   - Applies brightness to window immediately
   - Logs all brightness adjustments
   - Clamps brightness between 0.2-1.0 for safety

3. **`resetScreenBrightness()`**
   - Resets to system default brightness (value: -1.0f)
   - Called when light sensor is disabled

#### Brightness Mapping Scale:

| Lux Range | Category | Brightness | Use Case |
|-----------|----------|-----------|----------|
| 0 | Very Dark | 0.2 | Complete darkness |
| 1-100 | Dark | 0.2-0.5 | Low indoor light, night |
| 100-1000 | Normal | 0.5-0.8 | Regular indoor lighting |
| 1000-10000 | Bright | 0.8-1.0 | Outdoor, bright sunlight |
| 10000+ | Very Bright | 1.0 | Direct sunlight |

#### Interpolation Formula:
- **Between 0-100 lux**: `brightness = 0.2 + (lux/100) * 0.3`
- **Between 100-1000 lux**: `brightness = 0.5 + ((lux-100)/900) * 0.3`
- **Between 1000-10000 lux**: `brightness = 0.8 + ((lux-1000)/9000) * 0.2`

#### Example Log Output:
```
Auto Brightness: 45.0 lux -> 0.38 brightness
Auto Brightness: 2500.0 lux -> 0.83 brightness
Auto Brightness: 50000.0 lux -> 1.00 brightness
```

### Usage

**In Camera Activity:**
1. Open Camera app
2. Go to Sensors Settings
3. Enable "Light Sensor" toggle
4. Screen brightness automatically adjusts:
   - In dark room → Screen dims (0.2-0.5 brightness)
   - In normal light → Medium brightness (0.5-0.8)
   - In bright sunlight → Maximum brightness (0.8-1.0)
5. Disable to return to system default

**In Sensors Activity:**
1. Open Settings
2. Toggle "Light" on
3. Move device to different lighting:
   - Under blanket/dark corner → Low brightness
   - Normal room light → Medium brightness
   - Near window/sunlight → High brightness
4. Watch real-time updates in UI
5. Check logcat for exact lux→brightness conversion

---

## 📡 Integration with Sensors Settings

### Broadcast Communication
Both features use Android BroadcastReceiver for real-time state changes:

```java
Intent intent = new Intent("com.obs.mobile.SENSOR_STATE_CHANGED");
intent.putExtra("sensor_type", "proximity");  // or "light"
intent.putExtra("is_enabled", true);
sendBroadcast(intent);
```

### State Management
- `updateSensorState()` in CameraActivity receives broadcasts
- Updates `autoFocusOnProximityEnabled` or `autoBrightnessEnabled`
- Automatically starts/stops features based on sensor preferences

---

## 🔧 Technical Details

### Camera2 API Focus Mechanism

**Focus Modes Used:**
- `CONTROL_AF_MODE_CONTINUOUS_PICTURE`: Continuous autofocus for video/preview
- `CONTROL_AF_TRIGGER_START`: Trigger immediate focus lock
- `CONTROL_AF_TRIGGER_IDLE`: Return to continuous autofocus
- `CONTROL_AF_TRIGGER_CANCEL`: Cancel active focus

**Capture Session:**
```java
// Trigger autofocus
captureSession.capture(previewRequestBuilder.build(), null, backgroundHandler);

// Continuous autofocus
captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
```

### Window Brightness Control

**Using WindowManager.LayoutParams:**
```java
Window window = getWindow();
WindowManager.LayoutParams layoutParams = window.getAttributes();
layoutParams.screenBrightness = 0.5f;  // 0.0f to 1.0f
window.setAttributes(layoutParams);
```

**Special Values:**
- `0.0f` = Minimum brightness (darkest)
- `1.0f` = Maximum brightness (brightest)
- `-1.0f` = System default brightness

---

## 📊 Sensor Constraints

### Proximity Sensor
- **Detection Range**: Typically 0-5cm (varies by device)
- **Accuracy**: Binary (near/far) or continuous (some devices)
- **Response Time**: ~100-200ms per reading

### Light Sensor
- **Range**: 0 - 50,000+ lux
- **Accuracy**: ±15% typical
- **Response Time**: 100-500ms per reading
- **Update Rate**: 200ms normal

### Camera Focus
- **Hardware**: Requires autofocus camera (most modern devices)
- **Support**: All devices with Camera2 API
- **Latency**: 200-500ms for focus lock

---

## 🐛 Troubleshooting

### Auto-Focus Not Working

1. **Check Device Support**
   - Not all devices have proximity sensors
   - Check logs: `"Proximity sensor not available on this device"`

2. **Check Permissions**
   - Camera permission must be granted
   - Monitor: `logcat | grep CameraActivity`

3. **Check Settings**
   - Verify proximity sensor is enabled in Sensors Settings
   - Verify `autoFocusOnProximityEnabled = true` in code

4. **Debug Steps**
   ```
   adb logcat CameraActivity:D
   # Look for: "Auto Focus:" messages
   ```

### Brightness Not Adjusting

1. **Check Device Support**
   - Not all devices have light sensors
   - Check logs: `"Light sensor not available on this device"`

2. **Check Settings**
   - Verify light sensor is enabled in Sensors Settings
   - Verify `autoBrightnessEnabled = true` in code

3. **Check System Settings**
   - Device-wide brightness control might override
   - Try disabling system auto-brightness

4. **Debug Steps**
   ```
   adb logcat SensorsActivity:D CameraActivity:D | grep Brightness
   # Look for: "Auto Brightness:" messages
   ```

---

## 📚 Code References

### Modified Files:
1. **CameraActivity.java**
   - Added: `autoFocusOnProximityEnabled` field
   - Enhanced: `setupProximitySensor()`
   - Added: `adjustCameraFocusByProximity()`
   - Added: `resetCameraFocus()`
   - Added: `adjustScreenBrightness()`
   - Added: `resetScreenBrightness()`
   - Enhanced: `updateSensorState()` for proximity handling

2. **SensorsActivity.java**
   - Enhanced: Proximity sensor setup with auto-focus note
   - Added: `adjustScreenBrightness()` for testing
   - Added: `resetScreenBrightness()` for testing

### Related Constants:
```java
// Light sensor categories
0-10 lux: VERY_DARK
10-50 lux: DARK
50-500 lux: NORMAL
500-10000 lux: BRIGHT
10000+ lux: VERY_BRIGHT

// Proximity states
< 5cm: NEAR (isNear = true)
>= 5cm: FAR (isNear = false)
```

---

## 🚀 Future Enhancements

Possible improvements:
1. Add face detection for more accurate focus
2. Add zoom adjustment based on proximity distance
3. Add brightness smoothing/averaging to reduce flicker
4. Add user-configurable brightness curves
5. Add separate auto-brightness profiles (bright/dark mode)
6. Add focus peaking visualization
7. Add proximity-based exposure compensation

---

## 📝 Notes for Developers

- Both features are **non-blocking** and run on main UI thread
- All operations have **null checks** for safety
- All operations include **exception handling**
- Both features log extensively for debugging
- Features can be **toggled at runtime** without app restart
- Features **gracefully degrade** on unsupported devices


