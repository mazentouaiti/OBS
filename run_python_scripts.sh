#!/bin/bash

# OBS Mobile - Python Scripts Quick Launcher
# Easy way to run face detection and sensor monitoring

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  OBS MOBILE - PYTHON SCRIPTS LAUNCHER                       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed"
    echo "   Install with: sudo apt-get install python3 python3-pip"
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"
echo ""

# Menu
echo "Select an option:"
echo ""
echo "1) Run Face & Emotion Detection (DEMO MODE)"
echo "   - No camera or DeepFace required"
echo "   - Simulated sensor data"
echo ""
echo "2) Run Face & Emotion Detection (LIVE MODE)"
echo "   - Real camera feed"
echo "   - Real emotion detection"
echo ""
echo "3) Start Sensor Receiver Server"
echo "   - Listen for Android sensor data"
echo "   - Receive real sensor readings"
echo ""
echo "4) Show Setup Instructions"
echo "   - Network configuration guide"
echo "   - Android integration instructions"
echo ""
echo "5) Check Dependencies"
echo "   - Verify all packages installed"
echo ""
echo "6) Install Dependencies"
echo "   - Install/update all required packages"
echo ""
echo "0) Exit"
echo ""

read -p "Enter your choice (0-6): " choice

case $choice in
    1)
        echo ""
        echo "🎥 Starting Face & Emotion Detection (DEMO MODE)..."
        echo ""
        python3 face_emotion_detection.py --demo
        ;;
    2)
        echo ""
        echo "🎥 Starting Face & Emotion Detection (LIVE MODE)..."
        echo ""
        python3 face_emotion_detection.py
        ;;
    3)
        echo ""
        echo "📡 Starting Sensor Receiver Server..."
        echo ""
        python3 sensor_receiver.py
        ;;
    4)
        echo ""
        python3 sensor_receiver.py --setup
        ;;
    5)
        echo ""
        echo "🔍 Checking dependencies..."
        echo ""

        echo "Checking Python packages:"
        python3 -c "import cv2; print('✅ OpenCV:', cv2.__version__)" 2>/dev/null || echo "❌ OpenCV not found - install with: pip3 install opencv-python"
        python3 -c "import numpy; print('✅ NumPy:', numpy.__version__)" 2>/dev/null || echo "❌ NumPy not found - install with: pip3 install numpy"
        python3 -c "import deepface; print('✅ DeepFace available')" 2>/dev/null || echo "⚠️  DeepFace not found - install with: pip3 install deepface tensorflow"
        python3 -c "import PIL; print('✅ Pillow available')" 2>/dev/null || echo "❌ Pillow not found - install with: pip3 install pillow"

        echo ""
        echo "Checking system:"
        if command -v python3 &> /dev/null; then
            echo "✅ Python 3: $(python3 --version)"
        fi

        echo ""
        echo "To install all dependencies, run option 6"
        ;;
    6)
        echo ""
        echo "📦 Installing dependencies..."
        echo ""

        if [ -f "requirements_python.txt" ]; then
            pip3 install -r requirements_python.txt
            echo ""
            echo "✅ Dependencies installed"
        else
            echo "⚠️  requirements_python.txt not found"
            echo "   Installing packages manually..."
            pip3 install opencv-python numpy deepface tensorflow pillow
        fi
        ;;
    0)
        echo ""
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo ""
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo ""

