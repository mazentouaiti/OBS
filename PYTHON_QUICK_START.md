# ⚡ Quick Start - Face & Emotion Detection + Sensors

## 🚀 5-Minute Quick Start

### Option 1: Demo Mode (No Setup Required)

```bash
cd /home/mazen/StudioProjects/OBS
python3 face_emotion_detection.py --demo
```

✅ **Instantly shows:**
- Face detection (if you show face to camera)
- Simulated sensor data on screen
- No Android device needed

**Press Q to quit**

---

### Option 2: With Your Computer Camera

```bash
cd /home/mazen/StudioProjects/OBS
python3 face_emotion_detection.py
```

✅ **Shows:**
- Real face detection from camera
- Emotion recognition
- Simulated sensor data

**Requirements:** Webcam + DeepFace installed

---

### Option 3: With Real Android Sensor Data

**Terminal 1 - Start sensor receiver:**
```bash
python3 sensor_receiver.py
```

**Terminal 2 - Start face detection:**
```bash
python3 face_emotion_detection.py
```

✅ **Shows:**
- Face & emotion detection
- REAL sensor data from Android phone
- Complete integrated system

---

## 📋 Before You Start

### 1. Install Python Packages

```bash
cd /home/mazen/StudioProjects/OBS
pip3 install -r requirements_python.txt
```

Or use the launcher:
```bash
bash run_python_scripts.sh
# Select option 6
```

### 2. Check Your Setup

```bash
python3 -c "import cv2, numpy; print('✅ Ready')"
```

---

## 🎯 What You'll See

### Face Detection Window

```
FPS: 28
2025-12-23 14:35:42
                    LIVE MODE

┌─────────────────────────────────────┐
│                                     │  Camera feed
│   ╔═════════════════════╗           │
│   ║    HAPPY: 89.5%     ║  Emotion  │
│   ║    [Face box]       ║  label    │
│   ║    VERY NEAR        ║  & state  │
│   ╚═════════════════════╝           │
│                                     │
├─────────────────────────────────────┤
│ SENSOR DATA                         │
│ ACCELEROMETER:                      │ Sensor panel
│ X: -0.52  Y: 0.81  Z: 9.87        │
│ Mag: 10.15 m/s²                    │
│ GYROSCOPE:                          │
│ X: 0.05  Y: -0.02  Z: 0.08        │
│ LIGHT SENSOR:                       │
│ Lux: 450 - Normal                  │
│ PROXIMITY:                          │
│ Dist: 3.2cm [NEAR]                 │
│ MAGNETOMETER:                       │
│ Dir: NE (42°)                       │
└─────────────────────────────────────┘
Press 'Q' to quit, 'S' to save screenshot
```

---

## 🎨 Color Meanings

### Emotions (Face Box Color)
- 🟢 **Green** = Happy
- 🔴 **Red** = Angry
- 🔵 **Blue** = Sad
- 🟡 **Yellow** = Neutral
- 🟠 **Orange** = Surprise

### Proximity State
- 🔴 **RED** = VERY NEAR (danger!)
- 🟡 **YELLOW** = NEAR (close)
- 🟢 **GREEN** = FAR (far away)

---

## 📊 Sensor Data Explained

| Sensor | What It Shows | Normal Range |
|--------|---------------|--------------|
| **Accelerometer** | Phone motion/tilt | -10 to +10 m/s² |
| **Gyroscope** | Phone rotation | -360 to +360 °/s |
| **Light** | Room brightness | 0-50000 lux |
| **Proximity** | Distance to face | 0-200 cm |
| **Magnetometer** | Compass direction | N, NE, E, etc |

---

## ⌨️ Keyboard Controls

| Key | Action |
|-----|--------|
| **Q** | Quit program |
| **S** | Save screenshot |

---

## 🔗 Integration with Android

### To Send Real Sensor Data:

1. Find `192.168.1.X` (your computer IP)
   ```bash
   hostname -I
   ```

2. Add this to CameraActivity.java:
   ```java
   private SensorDataStreamer streamer;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       streamer = new SensorDataStreamer(this);
       streamer.initialize("192.168.1.X", 5000);  // Use your IP
       streamer.start();
   }
   ```

3. Build and run Android app
4. Enable sensors in app
5. Python will receive real data!

---

## 🐛 Common Issues

### "Camera not found"
```bash
# Run in demo mode instead
python3 face_emotion_detection.py --demo
```

### "DeepFace not found"
```bash
# Install it
pip3 install deepface tensorflow
```

### "No sensor data received"
```bash
# Check your computer IP matches
hostname -I
# Update the IP in Android code
```

### "Low FPS / Slow"
- Close other apps
- Use demo mode (--demo flag)
- No emotion detection needed? Use simple face detection

---

## 📱 Easy Launcher

```bash
bash run_python_scripts.sh
```

Then pick option from menu!

---

## 🎓 File Guide

| File | Purpose |
|------|---------|
| **face_emotion_detection.py** | Main face/emotion detector |
| **sensor_receiver.py** | Receive Android sensor data |
| **SensorDataStreamer.java** | Send data from Android |
| **requirements_python.txt** | Python packages list |
| **run_python_scripts.sh** | Easy launcher script |

---

## ✅ Checklist

- [ ] Python 3 installed (`python3 --version`)
- [ ] Dependencies installed (`pip3 install -r requirements_python.txt`)
- [ ] Camera working (if not using demo mode)
- [ ] Can run `python3 face_emotion_detection.py --demo`

**That's it! You're ready.** 🎉

---

## 🚀 Try It Now

```bash
# The absolute quickest way
cd /home/mazen/StudioProjects/OBS
python3 face_emotion_detection.py --demo
```

Press **Q** when done!


