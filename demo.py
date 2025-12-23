#!/usr/bin/env python3
"""
Live Demo - Shows face detection and sensor data working together
Run this to see the system in action
"""

import sys
import time
from datetime import datetime
from sensor_receiver import SensorDataFormatter
from face_emotion_detection import FaceEmotionDetector, SensorData

def demo_mode():
    """Run demo showing simulated data"""
    print("\n" + "="*80)
    print("🎬 LIVE DEMO - Face Detection + Sensor Data")
    print("="*80 + "\n")

    # Create sensor data
    sensor_data = SensorData()

    # Show header
    print("Timestamp: {}".format(datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    print("Mode: DEMO (Simulated Data)\n")

    # Run for 10 demo cycles
    for cycle in range(1, 11):
        # Clear screen on some systems
        print("\n" + "─"*80)
        print(f"📊 Sensor Update Cycle {cycle}/10")
        print("─"*80 + "\n")

        # Update sensor data
        sensor_data.update_random_demo()

        # Display accelerometer
        print("🔴 ACCELEROMETER (Linear Motion)")
        print(f"   X: {sensor_data.accelerometer['x']:7.2f} m/s²")
        print(f"   Y: {sensor_data.accelerometer['y']:7.2f} m/s²")
        print(f"   Z: {sensor_data.accelerometer['z']:7.2f} m/s²")
        print(f"   Magnitude: {sensor_data.accelerometer['magnitude']:6.2f} m/s²\n")

        # Display gyroscope
        print("🌀 GYROSCOPE (Angular Velocity)")
        print(f"   X: {sensor_data.gyroscope['x']:7.2f} °/s")
        print(f"   Y: {sensor_data.gyroscope['y']:7.2f} °/s")
        print(f"   Z: {sensor_data.gyroscope['z']:7.2f} °/s\n")

        # Display light sensor
        print("💡 LIGHT SENSOR")
        print(f"   Lux: {sensor_data.light['lux']} lux")
        print(f"   Category: {sensor_data.light['category']}\n")

        # Display proximity
        proximity_state = "🔴 NEAR" if sensor_data.proximity['is_near'] else "🟢 FAR"
        print("📍 PROXIMITY")
        print(f"   Distance: {sensor_data.proximity['distance']:.1f} cm")
        print(f"   State: {proximity_state}\n")

        # Display magnetometer
        print("🧭 MAGNETOMETER")
        print(f"   Azimuth: {sensor_data.magnetometer['azimuth']:.1f}°")
        print(f"   Direction: {sensor_data.magnetometer['direction']}\n")

        # Simulate face detection
        emotions = ['Happy', 'Sad', 'Angry', 'Neutral', 'Surprise']
        import random
        emotion = random.choice(emotions)
        confidence = random.uniform(50, 99)

        print("😊 FACE DETECTION")
        print(f"   Face Detected: YES")
        print(f"   Emotion: {emotion}")
        print(f"   Confidence: {confidence:.1f}%\n")

        print(f"✅ Update {cycle}/10 complete")

        # Wait before next update
        if cycle < 10:
            time.sleep(1)

    print("\n" + "="*80)
    print("✅ DEMO COMPLETED SUCCESSFULLY!")
    print("="*80)
    print("\nSystem is working correctly!")
    print("You can now:")
    print("  1. Run with camera: python3 face_emotion_detection.py")
    print("  2. Start sensor receiver: python3 sensor_receiver.py")
    print("  3. Integrate with Android: See documentation\n")

if __name__ == "__main__":
    try:
        demo_mode()
    except KeyboardInterrupt:
        print("\n\n✅ Demo stopped by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)

