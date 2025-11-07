#!/bin/bash

# Final Cleanup Script for OBS Mobile
# Removes all unnecessary files and old packages

echo "🧹 Final Cleanup - OBS Mobile Project"
echo "======================================"
echo ""

cd "/home/mazen/Documents/mazen touaiti/OBS"

echo "1️⃣ Removing old com.example package (Kotlin files + old structure)..."
rm -rf "app/src/main/java/com/example"
echo "   ✅ Removed: app/src/main/java/com/example/"
echo ""

echo "2️⃣ Keeping test files (already updated to com.obs.mobile)..."
echo "   ✓ Keeping: app/src/androidTest/java/com/example/obs/ (already updated)"
echo "   ✓ Keeping: app/src/test/java/com/example/obs/ (already updated)"
echo ""

echo "3️⃣ Cleanup documentation files..."
# Keep only essential docs
echo "   ✓ Keeping: README.md (main documentation)"
echo "   ✓ Keeping: SENSOR_CLASSES_GUIDE.md (sensor usage guide)"
echo "   ✓ Keeping: STUDENT_GUIDE.java (implementation examples)"
echo "   ℹ Optional: PROJECT_SUMMARY.md (can be removed if redundant)"
echo "   ℹ Optional: CLEANUP_REPORT.md (can be removed after cleanup)"
echo ""

echo "4️⃣ Removing cleanup script itself after execution..."
# Uncomment next line to self-delete after running
# rm -- "$0"
echo ""

echo "✅ Cleanup Complete!"
echo ""
echo "📊 Final Project Structure:"
echo "   com.obs.mobile/"
echo "   ├── SplashActivity.java"
echo "   ├── MainMenuActivity.java"
echo "   ├── CameraActivity.java"
echo "   ├── SensorsActivity.java"
echo "   ├── ScenesActivity.java"
echo "   └── sensors/"
echo "       ├── AccelerometerSensor.java (Student 1)"
echo "       ├── GyroscopeSensor.java (Student 2)"
echo "       ├── LightSensor.java (Student 3)"
echo "       ├── ProximitySensor.java (Student 4)"
echo "       └── MagnetometerSensor.java (Student 5)"
echo ""
echo "🎉 Project is clean and ready for students!"

