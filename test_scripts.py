#!/usr/bin/env python3
"""
Quick test to verify all components are working
"""

import sys
import os

print("╔════════════════════════════════════════════════════════════╗")
print("║  Testing OBS Mobile Python Scripts                        ║")
print("╚════════════════════════════════════════════════════════════╝\n")

# Test 1: Import OpenCV
print("1️⃣  Testing OpenCV...")
try:
    import cv2
    print(f"   ✅ OpenCV {cv2.__version__} loaded\n")
except ImportError as e:
    print(f"   ❌ OpenCV failed: {e}\n")
    sys.exit(1)

# Test 2: Import NumPy
print("2️⃣  Testing NumPy...")
try:
    import numpy as np
    print(f"   ✅ NumPy {np.__version__} loaded\n")
except ImportError as e:
    print(f"   ❌ NumPy failed: {e}\n")
    sys.exit(1)

# Test 3: Import sensor receiver
print("3️⃣  Testing SensorReceiver...")
try:
    from sensor_receiver import SensorReceiver, SensorDataFormatter
    print(f"   ✅ SensorReceiver loaded\n")
except ImportError as e:
    print(f"   ❌ SensorReceiver failed: {e}\n")
    sys.exit(1)

# Test 4: Import face detection
print("4️⃣  Testing FaceEmotionDetector...")
try:
    from face_emotion_detection import FaceEmotionDetector, SensorData
    print(f"   ✅ FaceEmotionDetector loaded\n")
except ImportError as e:
    print(f"   ⚠️  FaceEmotionDetector failed: {e}\n")
    print("   This is OK if you don't have a camera connected\n")

# Test 5: Create sensor data
print("5️⃣  Testing SensorData...")
try:
    sensor_data = SensorData()
    sensor_data.update_random_demo()
    print(f"   ✅ SensorData working")
    print(f"   - Accelerometer: X={sensor_data.accelerometer['x']}, Y={sensor_data.accelerometer['y']}")
    print(f"   - Light: {sensor_data.light['lux']} lux ({sensor_data.light['category']})")
    print(f"   - Proximity: {sensor_data.proximity['distance']}cm ({'NEAR' if sensor_data.proximity['is_near'] else 'FAR'})\n")
except Exception as e:
    print(f"   ❌ SensorData failed: {e}\n")
    sys.exit(1)

# Test 6: Test sensor receiver initialization
print("6️⃣  Testing SensorReceiver initialization...")
try:
    receiver = SensorReceiver(host='127.0.0.1', port=5000)
    print(f"   ✅ SensorReceiver initialized")
    print(f"   - Ready to listen on 127.0.0.1:5000\n")
except Exception as e:
    print(f"   ❌ SensorReceiver init failed: {e}\n")
    sys.exit(1)

# Test 7: Test data formatting
print("7️⃣  Testing SensorDataFormatter...")
try:
    formatted = SensorDataFormatter.format_all(receiver.get_latest_data())
    print(f"   ✅ SensorDataFormatter working\n")
except Exception as e:
    print(f"   ❌ SensorDataFormatter failed: {e}\n")
    sys.exit(1)

print("╔════════════════════════════════════════════════════════════╗")
print("║  ✅ ALL TESTS PASSED - READY TO USE!                      ║")
print("╚════════════════════════════════════════════════════════════╝\n")

print("NEXT STEPS:")
print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

print("Option 1: Run face detection in demo mode")
print("  $ python3 face_emotion_detection.py --demo\n")

print("Option 2: Start sensor receiver")
print("  $ python3 sensor_receiver.py\n")

print("Option 3: Use interactive launcher")
print("  $ bash run_python_scripts.sh\n")

print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

