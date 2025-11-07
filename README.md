# OBS Mobile - Android Application

## 📱 Project Overview

**OBS Mobile** is a sensor-controlled recording application template built with **Java** for Android. This project serves as a foundation for 5 engineering students to implement sensor-based features for camera control and scene management.

---

## 🎯 Project Details

- **Project Name:** OBS Mobile
- **Package:** `com.obs.mobile`
- **Language:** Java
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36 (Latest Stable)
- **Build System:** Gradle
- **Architecture:** Classic Android (Activities + XML Layouts)

---

## 📂 Project Structure

```
app/src/main/java/com/obs/mobile/
├── SplashActivity.java          # Splash screen (2.5 seconds)
├── MainMenuActivity.java        # Main menu with navigation buttons
├── CameraActivity.java          # Camera preview and recording
├── SensorsActivity.java         # Sensor settings and monitoring
└── ScenesActivity.java          # Scene management

app/src/main/res/layout/
├── activity_splash.xml
├── activity_main_menu.xml
├── activity_camera.xml
├── activity_sensors.xml
└── activity_scenes.xml
```

---

## 🚀 Features

### ✅ Implemented (Template)

1. **Splash Screen** - Shows app logo for 2.5 seconds then navigates to main menu
2. **Main Menu** - Navigation hub with 4 buttons:
   - Start Camera/Recording
   - Sensor Settings
   - Scenes
   - Exit (with confirmation dialog)
3. **Camera Preview** - Placeholder with SurfaceView for future camera implementation
4. **Sensor Settings** - UI cards for each sensor with on/off switches
5. **Scenes Screen** - Placeholder for scene management

### 🔨 To Be Implemented by Students

Each student will implement one sensor:

---

## 👥 Student Assignments

### **Student 1 - Accelerometer** 🏃

**Responsibilities:**
- Detect shake gestures
- Trigger recording start/stop on shake
- Implement shake intensity detection
- Switch scenes based on shake patterns

**Files to Edit:**
- `CameraActivity.java` - Add accelerometer listener in `initializeSensors()`
- `SensorsActivity.java` - Implement accelerometer switch functionality
- `ScenesActivity.java` - Add shake-to-switch-scene

**Key Code Locations:**
```java
// CameraActivity.java - Line ~120
/* TODO (Student 1 - Accelerometer): */

// SensorsActivity.java - Line ~100
/* TODO (Student 1 - Accelerometer): */
```

**Resources:**
- Sensor Type: `Sensor.TYPE_ACCELEROMETER`
- Values: `event.values[0]` (X), `event.values[1]` (Y), `event.values[2]` (Z)
- Shake detection: Calculate magnitude `sqrt(x² + y² + z²)` > threshold

---

### **Student 2 - Gyroscope** 🔄

**Responsibilities:**
- Detect device rotation
- Implement rotation-based gestures
- Video stabilization hints
- Rotate UI elements based on device orientation

**Files to Edit:**
- `CameraActivity.java` - Add gyroscope listener
- `SensorsActivity.java` - Display rotation rates
- `ScenesActivity.java` - Rotation-based scene switching

**Key Code Locations:**
```java
// CameraActivity.java - Line ~140
/* TODO (Student 2 - Gyroscope): */
```

**Resources:**
- Sensor Type: `Sensor.TYPE_GYROSCOPE`
- Values: Rotation rates in rad/s around X, Y, Z axes
- Convert to degrees: `degrees = radians * 180 / π`

---

### **Student 3 - Light Sensor** 💡

**Responsibilities:**
- Measure ambient light level
- Auto-switch to night/day scenes
- Adjust camera settings based on lighting
- Display light level warnings

**Files to Edit:**
- `CameraActivity.java` - Add light sensor listener
- `SensorsActivity.java` - Display lux values and categories
- `ScenesActivity.java` - Auto scene switching based on light

**Key Code Locations:**
```java
// CameraActivity.java - Line ~160
/* TODO (Student 3 - Light Sensor): */
```

**Resources:**
- Sensor Type: `Sensor.TYPE_LIGHT`
- Values: Illuminance in lux (`event.values[0]`)
- Typical ranges:
  - 0-50 lux: Dark (night mode)
  - 50-500 lux: Normal indoor
  - 500-10000 lux: Bright
  - 10000+ lux: Very bright (outdoor)

---

### **Student 4 - Proximity Sensor** 📏

**Responsibilities:**
- Detect object proximity
- Auto-pause recording when phone is covered
- Privacy feature (pause when in pocket)
- Wave gesture detection

**Files to Edit:**
- `CameraActivity.java` - Add proximity sensor listener
- `SensorsActivity.java` - Display near/far status
- `ScenesActivity.java` - Proximity-based actions

**Key Code Locations:**
```java
// CameraActivity.java - Line ~180
/* TODO (Student 4 - Proximity Sensor): */
```

**Resources:**
- Sensor Type: `Sensor.TYPE_PROXIMITY`
- Values: Distance in cm (`event.values[0]`)
- Most sensors are binary: 0 (near) or max range (far)
- Add debouncing to avoid rapid triggers

---

### **Student 5 - Magnetometer** 🧭

**Responsibilities:**
- Implement compass functionality
- Calculate azimuth (compass direction)
- Direction-based scene switching (North/South/East/West)
- Display compass overlay on camera

**Files to Edit:**
- `CameraActivity.java` - Add magnetometer + accelerometer listeners
- `SensorsActivity.java` - Display compass direction
- `ScenesActivity.java` - Direction-based scene switching

**Key Code Locations:**
```java
// CameraActivity.java - Line ~200
/* TODO (Student 5 - Magnetometer): */
```

**Resources:**
- Sensor Type: `Sensor.TYPE_MAGNETIC_FIELD`
- **Important:** Also need `Sensor.TYPE_ACCELEROMETER` for accurate compass
- Use `SensorManager.getRotationMatrix()` and `getOrientation()`
- Azimuth calculation: Convert radians to degrees (0-360°)
- Directions: 0°=North, 90°=East, 180°=South, 270°=West

---

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio (Latest version)
- JDK 11 or higher
- Android SDK 26+
- Physical device or emulator with sensors

### Installation

1. **Clone/Open the project:**
   ```bash
   cd "/home/mazen/Documents/mazen touaiti/OBS"
   ```

2. **Sync Gradle:**
   - Open Android Studio
   - File → Open → Select OBS folder
   - Wait for Gradle sync to complete

3. **Build the project:**
   ```bash
   ./gradlew clean build
   ```

4. **Run on device/emulator:**
   - Connect Android device or start emulator
   - Click Run ▶️ button in Android Studio
   - Or use: `./gradlew installDebug`

---

## 📋 Permissions

The following permissions are already declared in `AndroidManifest.xml`:

- `CAMERA` - For camera preview and recording
- `RECORD_AUDIO` - For audio recording with video
- `WRITE_EXTERNAL_STORAGE` - For saving recordings (API < 29)

**Note:** Camera permission is requested at runtime in `CameraActivity.java`

---

## 🎓 Development Guidelines for Students

### ⚠️ Important Rules

1. **Always unregister sensor listeners** in `onPause()` and `onDestroy()`
   - Prevents battery drain
   - Avoids memory leaks

2. **Choose appropriate sensor delay:**
   - `SENSOR_DELAY_NORMAL` - Light sensor, proximity (slow updates)
   - `SENSOR_DELAY_UI` - Compass, general UI updates
   - `SENSOR_DELAY_GAME` - Gyroscope, accelerometer (fast updates)
   - `SENSOR_DELAY_FASTEST` - Only if absolutely necessary

3. **Check if sensor exists:**
   ```java
   if (sensor == null) {
       // Sensor not available on this device
       Toast.makeText(this, "Sensor not available", Toast.LENGTH_SHORT).show();
   }
   ```

4. **Add proper error handling** for all sensor operations

5. **Test on real devices** - Emulator sensors are not accurate

---

## 📝 Implementation Checklist

### For Each Student:

- [ ] Initialize SensorManager in onCreate()
- [ ] Get your specific sensor
- [ ] Create SensorEventListener with onSensorChanged() and onAccuracyChanged()
- [ ] Register listener in onResume()
- [ ] Unregister listener in onPause()
- [ ] Implement logic in CameraActivity
- [ ] Add UI controls in SensorsActivity
- [ ] Add scene switching logic in ScenesActivity (optional)
- [ ] Test on real device
- [ ] Add comments explaining your code
- [ ] Handle cases where sensor is not available

---

## 🧪 Testing

### Manual Testing

1. **Splash Screen:**
   - Launch app → Should show splash for 2.5s → Navigate to Main Menu

2. **Main Menu:**
   - Test all 4 buttons
   - Exit button should show confirmation dialog
   - Back button should show exit dialog

3. **Camera Activity:**
   - Grant camera permission
   - Should see placeholder text
   - Record button should toggle text

4. **Sensors Activity:**
   - All 5 sensor cards should display
   - Switches should be functional (placeholder)

5. **Scenes Activity:**
   - Should display placeholder text

### Sensor Testing

Each student should test their sensor:
- Enable sensor in Sensors Activity
- Verify data updates in real-time
- Test trigger actions in Camera Activity
- Verify proper cleanup (no leaks)

---

## 🐛 Common Issues & Solutions

### Issue: App crashes on start
**Solution:** Check AndroidManifest.xml - ensure all activities are declared

### Issue: Sensor not working
**Solutions:**
- Check if sensor exists on device
- Verify listener is registered in onResume()
- Ensure proper sensor type is used
- Test on real device (emulator sensors are limited)

### Issue: Battery drains quickly
**Solution:** Make sure listeners are unregistered in onPause()

### Issue: Camera permission denied
**Solution:** Go to Settings → Apps → OBS Mobile → Permissions → Enable Camera

---

## 📚 Resources

### Android Sensor Documentation
- [Sensor Overview](https://developer.android.com/guide/topics/sensors/sensors_overview)
- [Motion Sensors](https://developer.android.com/guide/topics/sensors/sensors_motion)
- [Position Sensors](https://developer.android.com/guide/topics/sensors/sensors_position)
- [Environment Sensors](https://developer.android.com/guide/topics/sensors/sensors_environment)

### Example Code
All TODO comments in the code provide specific implementation hints

---

## 🔄 Git Workflow (Recommended)

1. Each student creates their own branch:
   ```bash
   git checkout -b student1-accelerometer
   ```

2. Implement your sensor feature

3. Test thoroughly

4. Create pull request for review

5. Merge to main after approval

---

## 📞 Support

If you encounter issues:
1. Check TODO comments in the code
2. Review Android Sensor documentation
3. Test on a real device
4. Consult with team members
5. Ask instructor for guidance

---

## 📄 License

Educational project for engineering students.

---

## ✅ Project Status

- [x] Project structure created
- [x] All activities implemented (UI only)
- [x] All layouts created
- [x] Navigation working
- [x] TODO comments added for students
- [ ] Student 1: Accelerometer (To be implemented)
- [ ] Student 2: Gyroscope (To be implemented)
- [ ] Student 3: Light Sensor (To be implemented)
- [ ] Student 4: Proximity Sensor (To be implemented)
- [ ] Student 5: Magnetometer (To be implemented)
- [ ] Camera recording implementation
- [ ] Scene management implementation

---

**Good luck with your implementation! 🚀**

