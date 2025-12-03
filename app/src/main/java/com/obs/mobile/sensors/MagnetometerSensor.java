package com.obs.mobile.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * MagnetometerSensor - Independent sensor class for compass/magnetometer
 *
 * ============================================================
 * COMPLETED - Magnetometer Implementation
 * ============================================================
 *
 * OBJECTIVE: Implement compass functionality for direction detection
 *
 * WHAT IS A MAGNETOMETER?
 * - Measures Earth's magnetic field in 3 axes
 * - Combined with accelerometer to create compass
 * - Values in microteslas (µT)
 *
 * COMPASS DIRECTIONS:
 * - 0° (360°) = North
 * - 90° = East
 * - 180° = South
 * - 270° = West
 *
 * IMPORTANT: Requires BOTH magnetometer AND accelerometer!
 *
 * USAGE IN ACTIVITIES:
 * - Create instance: magnetometerSensor = new MagnetometerSensor(this);
 * - Set callback: magnetometerSensor.setOnCompassChangeListener((azimuth, direction) -> { ... });
 * - Initialize: magnetometerSensor.initialize();
 * - Start: magnetometerSensor.startListening(); (in onResume)
 * - Stop: magnetometerSensor.stopListening(); (in onPause)
 */
public class MagnetometerSensor {

    private Context context;
    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;
    private SensorEventListener magnetometerListener;
    private SensorEventListener accelerometerListener;

    // Callbacks
    private OnCompassChangeListener onCompassChangeListener;
    private OnDirectionChangeListener onDirectionChangeListener;

    // Sensor data arrays
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // State tracking
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;
    private CompassDirection currentDirection = null;

    /**
     * Compass direction enum
     */
    public enum CompassDirection {
        NORTH("North", 0, "N"),
        NORTH_EAST("North-East", 45, "NE"),
        EAST("East", 90, "E"),
        SOUTH_EAST("South-East", 135, "SE"),
        SOUTH("South", 180, "S"),
        SOUTH_WEST("South-West", 225, "SW"),
        WEST("West", 270, "W"),
        NORTH_WEST("North-West", 315, "NW");

        private final String name;
        private final int degrees;
        private final String abbreviation;

        CompassDirection(String name, int degrees, String abbreviation) {
            this.name = name;
            this.degrees = degrees;
            this.abbreviation = abbreviation;
        }

        public String getName() { return name; }
        public int getDegrees() { return degrees; }
        public String getAbbreviation() { return abbreviation; }
    }

    /**
     * Constructor
     */
    public MagnetometerSensor(Context context) {
        this.context = context;
    }

    /**
     * COMPLETED: Sensor initialization
     *
     * Initializes both magnetometer and accelerometer sensors
     * Both are required for compass functionality
     */
    public boolean initialize() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        return magnetometer != null && accelerometer != null;
    }

    /**
     * COMPLETED: Sensor listeners implementation
     *
     * Creates and registers listeners for both magnetometer and accelerometer
     * Calculates orientation when both sensors have data
     */
    public void startListening() {
        magnetometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                geomagnetic[0] = event.values[0];
                geomagnetic[1] = event.values[1];
                geomagnetic[2] = event.values[2];
                hasGeomagnetic = true;
                calculateOrientation();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    // Recommend calibration (figure-8 motion)
                    // You can add a callback here if needed to notify the UI
                }
            }
        };

        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                gravity[0] = event.values[0];
                gravity[1] = event.values[1];
                gravity[2] = event.values[2];
                hasGravity = true;
                calculateOrientation();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        if (sensorManager != null) {
            if (magnetometer != null) {
                sensorManager.registerListener(magnetometerListener, magnetometer,
                        SensorManager.SENSOR_DELAY_UI);
            }
            if (accelerometer != null) {
                sensorManager.registerListener(accelerometerListener, accelerometer,
                        SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    /**
     * COMPLETED: Calculate compass orientation
     *
     * Combines magnetometer and accelerometer data to calculate device orientation
     * Converts to degrees and determines compass direction
     */
    private void calculateOrientation() {
        if (!hasGravity || !hasGeomagnetic) {
            return;
        }

        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null,
                gravity, geomagnetic);

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Azimuth is rotation around Z axis in radians
            float azimuthRad = orientation[0];

            // Convert to degrees
            float azimuthDeg = (float) Math.toDegrees(azimuthRad);

            // Normalize to 0-360
            azimuthDeg = (azimuthDeg + 360) % 360;

            // Get direction
            CompassDirection direction = getDirectionFromAzimuth(azimuthDeg);

            // Notify compass listener
            if (onCompassChangeListener != null) {
                onCompassChangeListener.onCompassChange(azimuthDeg, direction);
            }

            // Detect direction change
            if (direction != currentDirection) {
                currentDirection = direction;
                if (onDirectionChangeListener != null) {
                    onDirectionChangeListener.onDirectionChange(direction);
                }
            }
        }
    }

    /**
     * COMPLETED: Convert azimuth to compass direction
     *
     * Maps degree values to 8 compass directions
     */
    private CompassDirection getDirectionFromAzimuth(float azimuth) {
        int rounded = Math.round(azimuth);

        if ((rounded >= 0 && rounded < 22) || (rounded >= 338 && rounded <= 360)) {
            return CompassDirection.NORTH;
        } else if (rounded >= 22 && rounded < 67) {
            return CompassDirection.NORTH_EAST;
        } else if (rounded >= 67 && rounded < 112) {
            return CompassDirection.EAST;
        } else if (rounded >= 112 && rounded < 157) {
            return CompassDirection.SOUTH_EAST;
        } else if (rounded >= 157 && rounded < 202) {
            return CompassDirection.SOUTH;
        } else if (rounded >= 202 && rounded < 247) {
            return CompassDirection.SOUTH_WEST;
        } else if (rounded >= 247 && rounded < 292) {
            return CompassDirection.WEST;
        } else {
            return CompassDirection.NORTH_WEST;
        }
    }

    /**
     * COMPLETED: Unregister sensor listeners
     *
     * Cleans up listeners and resets state flags
     */
    public void stopListening() {
        if (sensorManager != null) {
            if (magnetometerListener != null) {
                sensorManager.unregisterListener(magnetometerListener);
            }
            if (accelerometerListener != null) {
                sensorManager.unregisterListener(accelerometerListener);
            }
        }

        hasGravity = false;
        hasGeomagnetic = false;
    }

    /**
     * Check if sensors are available
     */
    public boolean isAvailable() {
        return magnetometer != null && accelerometer != null;
    }

    /**
     * Get current compass direction
     */
    public CompassDirection getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Set compass change listener
     */
    public void setOnCompassChangeListener(OnCompassChangeListener listener) {
        this.onCompassChangeListener = listener;
    }

    /**
     * Set direction change listener
     */
    public void setOnDirectionChangeListener(OnDirectionChangeListener listener) {
        this.onDirectionChangeListener = listener;
    }

    /**
     * Callback interface for compass changes
     */
    public interface OnCompassChangeListener {
        void onCompassChange(float azimuth, CompassDirection direction);
    }

    /**
     * Callback interface for direction changes
     */
    public interface OnDirectionChangeListener {
        void onDirectionChange(CompassDirection direction);
    }
}