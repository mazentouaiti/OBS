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
 * TODO (Student 5 - Magnetometer):
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
     * TODO (Student 5): Implement sensor initialization
     *
     * Steps:
     * 1. Get SensorManager
     * 2. Get magnetometer (TYPE_MAGNETIC_FIELD)
     * 3. Get accelerometer (TYPE_ACCELEROMETER) - REQUIRED for compass!
     * 4. Check if BOTH sensors exist
     */
    public boolean initialize() {
        // TODO: Implement initialization
        // sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        // magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // return magnetometer != null && accelerometer != null;

        return false; // Replace with actual implementation
    }

    /**
     * TODO (Student 5): Implement sensor listeners
     *
     * Steps:
     * 1. Create magnetometer listener - store values in geomagnetic[]
     * 2. Create accelerometer listener - store values in gravity[]
     * 3. When both have data, call calculateOrientation()
     * 4. Register BOTH listeners
     */
    public void startListening() {
        // TODO: Implement listeners

        /* EXAMPLE CODE:
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
        */
    }

    /**
     * TODO (Student 5): Calculate compass orientation
     *
     * Steps:
     * 1. Check if we have both gravity and geomagnetic data
     * 2. Use SensorManager.getRotationMatrix()
     * 3. Use SensorManager.getOrientation()
     * 4. Extract azimuth (orientation[0])
     * 5. Convert radians to degrees
     * 6. Normalize to 0-360
     * 7. Determine direction
     * 8. Notify listeners
     */
    private void calculateOrientation() {
        // TODO: Implement orientation calculation

        /* EXAMPLE CODE:
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
        */
    }

    /**
     * TODO (Student 5): Convert azimuth to compass direction
     */
    private CompassDirection getDirectionFromAzimuth(float azimuth) {
        // TODO: Implement direction calculation
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
     * TODO (Student 5): Unregister BOTH sensor listeners
     */
    public void stopListening() {
        // TODO: Unregister BOTH listeners
        // if (sensorManager != null) {
        //     if (magnetometerListener != null) {
        //         sensorManager.unregisterListener(magnetometerListener);
        //     }
        //     if (accelerometerListener != null) {
        //         sensorManager.unregisterListener(accelerometerListener);
        //     }
        // }

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

