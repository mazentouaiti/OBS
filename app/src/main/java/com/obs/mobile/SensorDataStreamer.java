package com.obs.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * SensorDataStreamer - Sends sensor data to Python script
 *
 * Streams all sensor readings to a Python application for real-time
 * processing and display. Uses UDP for low-latency communication.
 */
public class SensorDataStreamer {

    private static final String TAG = "SensorDataStreamer";

    // Network settings
    private String serverHost = "192.168.1.100";  // Update with your computer IP
    private int serverPort = 5000;

    // Socket and threading
    private DatagramSocket socket;
    private HandlerThread senderThread;
    private Handler senderHandler;
    private boolean isRunning = false;

    // Sensor data
    private SensorValues currentSensorValues = new SensorValues();

    // Send rate (ms)
    private static final int SEND_INTERVAL_MS = 100;  // Send every 100ms

    /**
     * Container for all sensor values
     */
    public static class SensorValues {
        public float accelX = 0.0f;
        public float accelY = 0.0f;
        public float accelZ = 0.0f;
        public float accelMagnitude = 0.0f;

        public float gyroX = 0.0f;
        public float gyroY = 0.0f;
        public float gyroZ = 0.0f;

        public float lightLux = 0.0f;
        public String lightCategory = "Normal";

        public float proximityDistance = 0.0f;
        public boolean proximityIsNear = false;

        public float magnetometerAzimuth = 0.0f;
        public String magnetometerDirection = "N";

        /**
         * Convert to JSON for transmission
         */
        public JSONObject toJSON() throws Exception {
            JSONObject json = new JSONObject();

            JSONObject accel = new JSONObject();
            accel.put("x", accelX);
            accel.put("y", accelY);
            accel.put("z", accelZ);
            accel.put("magnitude", accelMagnitude);
            json.put("accelerometer", accel);

            JSONObject gyro = new JSONObject();
            gyro.put("x", gyroX);
            gyro.put("y", gyroY);
            gyro.put("z", gyroZ);
            json.put("gyroscope", gyro);

            JSONObject light = new JSONObject();
            light.put("lux", lightLux);
            light.put("category", lightCategory);
            json.put("light", light);

            JSONObject proximity = new JSONObject();
            proximity.put("distance", proximityDistance);
            proximity.put("is_near", proximityIsNear);
            json.put("proximity", proximity);

            JSONObject magnetometer = new JSONObject();
            magnetometer.put("azimuth", magnetometerAzimuth);
            magnetometer.put("direction", magnetometerDirection);
            json.put("magnetometer", magnetometer);

            return json;
        }
    }

    /**
     * Constructor
     */
    public SensorDataStreamer(Context context) {
        this.senderThread = new HandlerThread("SensorDataStreamer");
    }

    /**
     * Initialize the streamer
     */
    public void initialize(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;

        try {
            socket = new DatagramSocket();
            Log.d(TAG, "DatagramSocket created");
        } catch (Exception e) {
            Log.e(TAG, "Error creating socket: " + e.getMessage());
        }
    }

    /**
     * Start streaming sensor data
     */
    public void start() {
        if (isRunning) {
            Log.w(TAG, "Streamer already running");
            return;
        }

        if (!senderThread.isAlive()) {
            senderThread.start();
        }

        senderHandler = new Handler(senderThread.getLooper());
        isRunning = true;

        // Start periodic sending
        sendSensorData();

        Log.d(TAG, "Sensor data streaming started");
        Log.d(TAG, "Sending to " + serverHost + ":" + serverPort);
    }

    /**
     * Stop streaming sensor data
     */
    public void stop() {
        isRunning = false;

        if (senderHandler != null) {
            senderHandler.removeCallbacksAndMessages(null);
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        Log.d(TAG, "Sensor data streaming stopped");
    }

    /**
     * Update accelerometer data
     */
    public void updateAccelerometer(float x, float y, float z, float magnitude) {
        currentSensorValues.accelX = x;
        currentSensorValues.accelY = y;
        currentSensorValues.accelZ = z;
        currentSensorValues.accelMagnitude = magnitude;
    }

    /**
     * Update gyroscope data
     */
    public void updateGyroscope(float x, float y, float z) {
        currentSensorValues.gyroX = x;
        currentSensorValues.gyroY = y;
        currentSensorValues.gyroZ = z;
    }

    /**
     * Update light sensor data
     */
    public void updateLight(float lux, String category) {
        currentSensorValues.lightLux = lux;
        currentSensorValues.lightCategory = category;
    }

    /**
     * Update proximity sensor data
     */
    public void updateProximity(float distance, boolean isNear) {
        currentSensorValues.proximityDistance = distance;
        currentSensorValues.proximityIsNear = isNear;
    }

    /**
     * Update magnetometer data
     */
    public void updateMagnetometer(float azimuth, String direction) {
        currentSensorValues.magnetometerAzimuth = azimuth;
        currentSensorValues.magnetometerDirection = direction;
    }

    /**
     * Send sensor data to Python script
     */
    private void sendSensorData() {
        if (!isRunning || senderHandler == null) {
            return;
        }

        // Send current values
        try {
            JSONObject data = currentSensorValues.toJSON();
            byte[] buffer = data.toString().getBytes();

            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, address, serverPort
            );

            if (socket != null && !socket.isClosed()) {
                socket.send(packet);
                Log.d(TAG, "Sensor data sent");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending data: " + e.getMessage());
        }

        // Schedule next send
        senderHandler.postDelayed(this::sendSensorData, SEND_INTERVAL_MS);
    }

    /**
     * Set network address
     */
    public void setServerAddress(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        Log.d(TAG, "Server address updated: " + host + ":" + port);
    }

    /**
     * Check if streaming is active
     */
    public boolean isStreaming() {
        return isRunning;
    }

    /**
     * Get current sensor values
     */
    public SensorValues getCurrentValues() {
        return currentSensorValues;
    }
}

