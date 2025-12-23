#!/usr/bin/env python3
"""
OBS Mobile - Live Sensor Integration
Receives real sensor data from Android device via network socket
Integrates with face/emotion detection for complete monitoring
"""

import socket
import json
import threading
import time
import queue
from datetime import datetime

class SensorReceiver:
    def __init__(self, host='192.168.1.100', port=5000):
        """
        Initialize sensor data receiver

        Args:
            host: IP address to listen on
            port: Port to listen on
        """
        self.host = host
        self.port = port
        self.socket = None
        self.running = False
        self.data_queue = queue.Queue()
        self.clients = []
        self.lock = threading.Lock()

        # Current sensor values
        self.sensor_data = {
            'accelerometer': {'x': 0.0, 'y': 0.0, 'z': 0.0, 'magnitude': 0.0},
            'gyroscope': {'x': 0.0, 'y': 0.0, 'z': 0.0},
            'light': {'lux': 0, 'category': 'Normal'},
            'proximity': {'distance': 0.0, 'is_near': False},
            'magnetometer': {'azimuth': 0.0, 'direction': 'N'},
            'timestamp': datetime.now().isoformat()
        }

    def start_server(self):
        """Start UDP/TCP server to receive sensor data"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.socket.bind((self.host, self.port))
            self.running = True

            print(f"✅ Sensor server listening on {self.host}:{self.port}")
            print("   Configure Android app to send data to this address")

            # Start receiver thread
            receiver_thread = threading.Thread(target=self._receive_loop, daemon=True)
            receiver_thread.start()

        except Exception as e:
            print(f"❌ Error starting server: {e}")
            self.running = False

    def _receive_loop(self):
        """Receive sensor data in background thread"""
        while self.running:
            try:
                data, addr = self.socket.recvfrom(1024)
                message = data.decode('utf-8')

                try:
                    json_data = json.loads(message)

                    with self.lock:
                        self.sensor_data.update(json_data)
                        self.sensor_data['timestamp'] = datetime.now().isoformat()

                    self.data_queue.put(json_data)
                    print(f"📡 Received sensor data from {addr}")

                except json.JSONDecodeError:
                    print(f"⚠️  Invalid JSON received: {message}")

            except Exception as e:
                if self.running:
                    print(f"❌ Error receiving data: {e}")

    def get_latest_data(self):
        """Get latest sensor data"""
        with self.lock:
            return self.sensor_data.copy()

    def stop_server(self):
        """Stop the sensor server"""
        self.running = False
        if self.socket:
            self.socket.close()
        print("✅ Sensor server stopped")


class SensorDataFormatter:
    """Format sensor data for display"""

    @staticmethod
    def format_accelerometer(data):
        """Format accelerometer data"""
        return (
            f"X: {data['x']:7.2f} m/s²\n"
            f"Y: {data['y']:7.2f} m/s²\n"
            f"Z: {data['z']:7.2f} m/s²\n"
            f"Magnitude: {data['magnitude']:6.2f} m/s²"
        )

    @staticmethod
    def format_gyroscope(data):
        """Format gyroscope data"""
        return (
            f"X: {data['x']:7.2f} °/s\n"
            f"Y: {data['y']:7.2f} °/s\n"
            f"Z: {data['z']:7.2f} °/s"
        )

    @staticmethod
    def format_light(data):
        """Format light sensor data"""
        return (
            f"Lux: {data['lux']} lux\n"
            f"Category: {data['category']}"
        )

    @staticmethod
    def format_proximity(data):
        """Format proximity sensor data"""
        state = "NEAR ⚠️" if data['is_near'] else "FAR ✓"
        return (
            f"Distance: {data['distance']:.1f} cm\n"
            f"State: {state}"
        )

    @staticmethod
    def format_magnetometer(data):
        """Format magnetometer data"""
        return (
            f"Azimuth: {data['azimuth']:.1f}°\n"
            f"Direction: {data['direction']}"
        )

    @staticmethod
    def format_all(sensor_data):
        """Format all sensor data for terminal display"""
        output = []
        output.append("=" * 60)
        output.append("📊 REAL-TIME SENSOR DATA")
        output.append("=" * 60)
        output.append(f"Timestamp: {sensor_data['timestamp']}\n")

        output.append("🔴 ACCELEROMETER (Linear Acceleration)")
        output.append("-" * 60)
        output.append(SensorDataFormatter.format_accelerometer(sensor_data['accelerometer']))
        output.append("")

        output.append("🌀 GYROSCOPE (Angular Velocity)")
        output.append("-" * 60)
        output.append(SensorDataFormatter.format_gyroscope(sensor_data['gyroscope']))
        output.append("")

        output.append("💡 LIGHT SENSOR (Ambient Light)")
        output.append("-" * 60)
        output.append(SensorDataFormatter.format_light(sensor_data['light']))
        output.append("")

        output.append("📍 PROXIMITY SENSOR (Object Detection)")
        output.append("-" * 60)
        output.append(SensorDataFormatter.format_proximity(sensor_data['proximity']))
        output.append("")

        output.append("🧭 MAGNETOMETER (Compass)")
        output.append("-" * 60)
        output.append(SensorDataFormatter.format_magnetometer(sensor_data['magnetometer']))
        output.append("")
        output.append("=" * 60)

        return "\n".join(output)


def setup_android_app():
    """Print setup instructions for Android app"""
    instructions = """
╔══════════════════════════════════════════════════════════════╗
║  OBS MOBILE - SENSOR DATA STREAMING SETUP                   ║
╚══════════════════════════════════════════════════════════════╝

To send real sensor data from your Android device to this script:

1. CREATE A NEW BROADCAST RECEIVER IN YOUR ANDROID APP:

   File: app/src/main/java/com/obs/mobile/SensorStreamBroadcaster.java

   Purpose: Send sensor data to Python script via UDP socket

2. UPDATE ACTIVITY FILES TO USE THE BROADCASTER:
   - CameraActivity.java
   - SensorsActivity.java

3. CONFIGURE NETWORK SETTINGS:
   - Update 'host' with your computer's IP address
   - Ensure firewall allows UDP on port 5000
   - Device and computer must be on same network

4. EXAMPLE SENSOR DATA FORMAT (JSON):
   {
     "accelerometer": {
       "x": -0.52,
       "y": 0.81,
       "z": 9.87,
       "magnitude": 10.15
     },
     "gyroscope": {
       "x": 0.05,
       "y": -0.02,
       "z": 0.08
     },
     "light": {
       "lux": 450,
       "category": "Normal"
     },
     "proximity": {
       "distance": 3.2,
       "is_near": true
     },
     "magnetometer": {
       "azimuth": 42.5,
       "direction": "NE"
     }
   }

5. RUN THIS SCRIPT:
   python3 sensor_receiver.py --host <YOUR_IP> --port 5000

6. START SENSORS IN YOUR APP:
   - Open Sensors Settings
   - Enable desired sensors
   - Sensor data will stream to this script

═══════════════════════════════════════════════════════════════
"""
    print(instructions)


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description='OBS Mobile Sensor Data Receiver'
    )
    parser.add_argument('--host', default='0.0.0.0',
                       help='Host to listen on (default: 0.0.0.0)')
    parser.add_argument('--port', type=int, default=5000,
                       help='Port to listen on (default: 5000)')
    parser.add_argument('--setup', action='store_true',
                       help='Show setup instructions')

    args = parser.parse_args()

    if args.setup:
        setup_android_app()
        return 0

    # Start receiver
    receiver = SensorReceiver(host=args.host, port=args.port)
    receiver.start_server()

    # Get local IP for instructions
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        print(f"💻 Your computer IP: {local_ip}")
    except:
        print("⚠️  Could not determine IP address")

    try:
        last_display = 0

        while True:
            # Display data every 5 seconds
            current_time = time.time()
            if current_time - last_display >= 5:
                data = receiver.get_latest_data()
                print("\033[2J\033[H")  # Clear screen
                print(SensorDataFormatter.format_all(data))
                last_display = current_time

            time.sleep(0.1)

    except KeyboardInterrupt:
        print("\n✅ Shutting down...")
        receiver.stop_server()
        return 0

    return 0


if __name__ == "__main__":
    exit(main())

