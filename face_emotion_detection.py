#!/usr/bin/env python3
"""
OBS Mobile - Face and Emotion Detection with Sensor Display
Detects faces and emotions in real-time camera feed
Displays all sensor data including: Accelerometer, Gyroscope, Light, Proximity, Magnetometer
Uses OpenCV for face detection and DeepFace for emotion recognition
"""

import cv2
import numpy as np
import time
from datetime import datetime
from collections import deque
import threading
import socket
import json
import os

# Disable Qt platform plugin warning for headless environments
os.environ['QT_QPA_PLATFORM'] = 'offscreen'

try:
    from deepface import DeepFace
    DEEPFACE_AVAILABLE = True
except ImportError:
    DEEPFACE_AVAILABLE = False
    print("⚠️  DeepFace not installed. Install with: pip install deepface")

# Sensor data storage (for demo purposes)
class SensorData:
    def __init__(self):
        self.accelerometer = {"x": 0.0, "y": 0.0, "z": 0.0, "magnitude": 0.0}
        self.gyroscope = {"x": 0.0, "y": 0.0, "z": 0.0}
        self.light = {"lux": 0, "category": "Normal"}
        self.proximity = {"distance": 0.0, "is_near": False}
        self.magnetometer = {"azimuth": 0.0, "direction": "N"}
        self.last_update = time.time()

    def update_random_demo(self):
        """Update with simulated sensor data (for demo when no Android device)"""
        import random
        self.accelerometer = {
            "x": round(random.uniform(-10, 10), 2),
            "y": round(random.uniform(-10, 10), 2),
            "z": round(random.uniform(-10, 10), 2),
            "magnitude": round(random.uniform(5, 15), 2)
        }
        self.gyroscope = {
            "x": round(random.uniform(-50, 50), 2),
            "y": round(random.uniform(-50, 50), 2),
            "z": round(random.uniform(-50, 50), 2)
        }
        lux_value = round(random.uniform(10, 50000), 2)
        self.light = {
            "lux": lux_value,
            "category": self._categorize_light(lux_value)
        }
        self.proximity = {
            "distance": round(random.uniform(0, 20), 2),
            "is_near": random.choice([True, False])
        }
        self.magnetometer = {
            "azimuth": round(random.uniform(0, 360), 2),
            "direction": self._get_direction(random.uniform(0, 360))
        }
        self.last_update = time.time()

    @staticmethod
    def _categorize_light(lux):
        """Categorize light level"""
        if lux < 10:
            return "Very Dark"
        elif lux < 50:
            return "Dark"
        elif lux < 500:
            return "Normal"
        elif lux < 10000:
            return "Bright"
        else:
            return "Very Bright"

    @staticmethod
    def _get_direction(azimuth):
        """Get direction from azimuth"""
        directions = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                     "S", "SSW", "SW", "WSW", "W", "WSW", "NW", "NNW"]
        index = int((azimuth + 11.25) / 22.5) % 16
        return directions[index]


class FaceEmotionDetector:
    def __init__(self, sensor_data=None, use_demo=False):
        """
        Initialize face and emotion detector

        Args:
            sensor_data: SensorData object to store sensor readings
            use_demo: If True, use simulated sensor data
        """
        self.sensor_data = sensor_data or SensorData()
        self.use_demo = use_demo

        # Load Haar Cascade for face detection
        self.face_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        )

        # Initialize video capture
        self.cap = cv2.VideoCapture(0)
        if not self.cap.isOpened():
            raise RuntimeError("Cannot open camera")

        # Set camera properties
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
        self.cap.set(cv2.CAP_PROP_FPS, 30)

        # Face tracking
        self.face_history = deque(maxlen=5)
        self.emotion_history = deque(maxlen=10)

        # Performance tracking
        self.fps = 0
        self.frame_count = 0
        self.last_fps_time = time.time()
        self.emotion_confidence = 0.0

        # Colors
        self.COLOR_GREEN = (0, 255, 0)
        self.COLOR_RED = (0, 0, 255)
        self.COLOR_BLUE = (255, 0, 0)
        self.COLOR_YELLOW = (0, 255, 255)
        self.COLOR_WHITE = (255, 255, 255)
        self.COLOR_BLACK = (0, 0, 0)

        # Emotion to color mapping
        self.emotion_colors = {
            'happy': (0, 255, 0),      # Green
            'sad': (255, 0, 0),         # Blue
            'angry': (0, 0, 255),       # Red
            'neutral': (0, 255, 255),   # Yellow
            'surprise': (0, 128, 255),  # Orange
            'fear': (128, 0, 255),      # Purple
            'disgust': (0, 165, 255)    # Orange
        }

    def detect_emotion(self, face_roi):
        """
        Detect emotion in face region

        Args:
            face_roi: Face region of interest (numpy array)

        Returns:
            emotion: Detected emotion string
            confidence: Confidence score (0-100)
        """
        if not DEEPFACE_AVAILABLE:
            return "N/A", 0.0

        try:
            # Prepare face for analysis
            if face_roi.shape[0] < 48 or face_roi.shape[1] < 48:
                return "Too Small", 0.0

            # Detect emotion using DeepFace
            result = DeepFace.analyze(face_roi, actions=['emotion'], enforce_detection=False)

            if result and len(result) > 0:
                emotions = result[0]['emotion']
                detected_emotion = max(emotions, key=emotions.get)
                confidence = emotions[detected_emotion]
                return detected_emotion, confidence

            return "Unknown", 0.0

        except Exception as e:
            return "Error", 0.0

    def draw_sensor_panel(self, frame, x, y, width=300):
        """
        Draw sensor data panel on frame

        Args:
            frame: Video frame (numpy array)
            x, y: Top-left position of panel
            width: Width of panel
        """
        # Background panel
        overlay = frame.copy()
        cv2.rectangle(overlay, (x, y), (x + width, y + 400), (0, 0, 0), -1)
        cv2.addWeighted(overlay, 0.7, frame, 0.3, 0, frame)

        # Border
        cv2.rectangle(frame, (x, y), (x + width, y + 400), self.COLOR_GREEN, 2)

        # Title
        cv2.putText(frame, "SENSOR DATA", (x + 10, y + 25),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.6, self.COLOR_GREEN, 2)

        line_y = y + 50
        line_height = 35

        # Accelerometer
        cv2.putText(frame, "ACCELEROMETER:", (x + 10, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_YELLOW, 1)
        line_y += line_height
        accel = self.sensor_data.accelerometer
        text = f"X:{accel['x']:6.2f} Y:{accel['y']:6.2f} Z:{accel['z']:6.2f}"
        cv2.putText(frame, text, (x + 15, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.4, self.COLOR_WHITE, 1)
        line_y += 20
        cv2.putText(frame, f"Mag: {accel['magnitude']:.2f} m/s2",
                   (x + 15, line_y), cv2.FONT_HERSHEY_SIMPLEX, 0.4,
                   self.COLOR_WHITE, 1)
        line_y += line_height

        # Gyroscope
        cv2.putText(frame, "GYROSCOPE:", (x + 10, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_YELLOW, 1)
        line_y += line_height
        gyro = self.sensor_data.gyroscope
        text = f"X:{gyro['x']:6.2f} Y:{gyro['y']:6.2f} Z:{gyro['z']:6.2f}"
        cv2.putText(frame, text, (x + 15, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.4, self.COLOR_WHITE, 1)
        line_y += line_height

        # Light Sensor
        cv2.putText(frame, "LIGHT SENSOR:", (x + 10, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_YELLOW, 1)
        line_y += line_height
        light = self.sensor_data.light
        cv2.putText(frame, f"Lux: {light['lux']} - {light['category']}",
                   (x + 15, line_y), cv2.FONT_HERSHEY_SIMPLEX, 0.4,
                   self.COLOR_WHITE, 1)
        line_y += line_height

        # Proximity
        cv2.putText(frame, "PROXIMITY:", (x + 10, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_YELLOW, 1)
        line_y += line_height
        prox = self.sensor_data.proximity
        state = "NEAR" if prox['is_near'] else "FAR"
        color = self.COLOR_RED if prox['is_near'] else self.COLOR_GREEN
        text = f"Dist: {prox['distance']:.1f}cm [{state}]"
        cv2.putText(frame, text, (x + 15, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.4, color, 1)
        line_y += line_height

        # Magnetometer
        cv2.putText(frame, "MAGNETOMETER:", (x + 10, line_y),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_YELLOW, 1)
        line_y += line_height
        mag = self.sensor_data.magnetometer
        cv2.putText(frame, f"Dir: {mag['direction']} ({mag['azimuth']:.0f}°)",
                   (x + 15, line_y), cv2.FONT_HERSHEY_SIMPLEX, 0.4,
                   self.COLOR_WHITE, 1)

    def draw_face_info(self, frame, x, y, w, h, emotion, confidence):
        """
        Draw face detection and emotion info

        Args:
            frame: Video frame
            x, y, w, h: Face bounding box
            emotion: Detected emotion
            confidence: Emotion confidence score
        """
        # Draw bounding box
        color = self.emotion_colors.get(emotion, self.COLOR_GREEN)
        cv2.rectangle(frame, (x, y), (x + w, y + h), color, 3)

        # Draw emotion label
        label = f"{emotion.upper()}: {confidence:.1f}%"
        label_size = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)[0]

        # Background for label
        cv2.rectangle(frame, (x, y - 30), (x + label_size[0] + 10, y), color, -1)
        cv2.putText(frame, label, (x + 5, y - 8),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, self.COLOR_WHITE, 2)

        # Draw face center and distance indicator
        center_x = x + w // 2
        center_y = y + h // 2
        cv2.circle(frame, (center_x, center_y), 5, color, -1)

        # Draw proximity indicator (simulated based on face size)
        face_area = w * h
        if face_area > 50000:
            proximity_text = "VERY NEAR"
            prox_color = self.COLOR_RED
        elif face_area > 30000:
            proximity_text = "NEAR"
            prox_color = self.COLOR_YELLOW
        else:
            proximity_text = "FAR"
            prox_color = self.COLOR_GREEN

        cv2.putText(frame, proximity_text, (x + 5, y + h + 25),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.6, prox_color, 2)

    def draw_stats(self, frame):
        """Draw FPS and statistics on frame"""
        # Update FPS
        self.frame_count += 1
        current_time = time.time()
        if current_time - self.last_fps_time >= 1.0:
            self.fps = self.frame_count
            self.frame_count = 0
            self.last_fps_time = current_time

        # Draw FPS
        fps_text = f"FPS: {self.fps}"
        cv2.putText(frame, fps_text, (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.8, self.COLOR_GREEN, 2)

        # Draw timestamp
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cv2.putText(frame, timestamp, (10, 65),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.6, self.COLOR_GREEN, 1)

        # Draw mode indicator
        mode_text = "DEMO MODE" if self.use_demo else "LIVE MODE"
        mode_color = self.COLOR_YELLOW if self.use_demo else self.COLOR_GREEN
        cv2.putText(frame, mode_text, (frame.shape[1] - 200, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, mode_color, 2)

    def process_frame(self, frame):
        """
        Process single frame for face and emotion detection

        Args:
            frame: Input frame from video capture

        Returns:
            Processed frame with detections and sensor data
        """
        # Update sensor data
        if self.use_demo:
            self.sensor_data.update_random_demo()

        # Convert to grayscale for face detection
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        # Detect faces
        faces = self.face_cascade.detectMultiScale(gray, 1.1, 4, minSize=(50, 50))

        # Process detected faces
        for (x, y, w, h) in faces:
            # Extract face region
            face_roi = frame[y:y+h, x:x+w]

            # Detect emotion
            emotion, confidence = self.detect_emotion(face_roi)

            # Draw face info
            self.draw_face_info(frame, x, y, w, h, emotion, confidence)

            # Update history
            self.emotion_history.append(emotion)

        # Draw sensor panel
        self.draw_sensor_panel(frame, frame.shape[1] - 320, 80)

        # Draw statistics
        self.draw_stats(frame)

        # Draw instructions
        cv2.putText(frame, "Press 'Q' to quit, 'S' to save screenshot",
                   (10, frame.shape[0] - 20),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLOR_WHITE, 1)

        return frame

    def run(self):
        """Main loop for face and emotion detection"""
        print("🎥 Starting Face and Emotion Detection with Sensor Display")
        print("📊 Press 'Q' to quit, 'S' to save screenshot")
        print()

        if self.use_demo:
            print("⚠️  Running in DEMO MODE with simulated sensor data")
        else:
            print("✅ Running in LIVE MODE with camera feed")

        if not DEEPFACE_AVAILABLE:
            print("⚠️  DeepFace not available - emotion detection disabled")
            print("   Install: pip install deepface tensorflow")

        print()

        frame_count = 0
        start_time = time.time()

        try:
            while True:
                ret, frame = self.cap.read()
                if not ret:
                    print("Error reading frame")
                    break

                # Process frame
                frame = self.process_frame(frame)

                # Display frame
                cv2.imshow('Face & Emotion Detection with Sensor Data', frame)

                # Keyboard controls
                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    print("\n✅ Quitting...")
                    break
                elif key == ord('s'):
                    filename = f"screenshot_{int(time.time())}.png"
                    cv2.imwrite(filename, frame)
                    print(f"💾 Screenshot saved: {filename}")

                frame_count += 1

                # Print stats every 30 frames
                if frame_count % 30 == 0:
                    elapsed = time.time() - start_time
                    actual_fps = frame_count / elapsed
                    print(f"Frame: {frame_count} | Actual FPS: {actual_fps:.1f}")

        finally:
            self.cap.release()
            cv2.destroyAllWindows()
            print("✅ Detection stopped")

    def __del__(self):
        """Cleanup"""
        if hasattr(self, 'cap'):
            self.cap.release()


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description='Face and Emotion Detection with Sensor Display'
    )
    parser.add_argument('--demo', action='store_true',
                       help='Run in demo mode with simulated sensor data')
    parser.add_argument('--camera', type=int, default=0,
                       help='Camera device index (default: 0)')

    args = parser.parse_args()

    try:
        detector = FaceEmotionDetector(use_demo=args.demo)
        detector.run()
    except RuntimeError as e:
        print(f"Error: {e}")
        return 1
    except KeyboardInterrupt:
        print("\n✅ Interrupted by user")
        return 0

    return 0


if __name__ == "__main__":
    exit(main())

