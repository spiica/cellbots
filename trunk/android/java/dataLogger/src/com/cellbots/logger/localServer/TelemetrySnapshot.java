
package com.cellbots.logger.localServer;

import android.hardware.SensorEvent;
import android.location.Location;
import android.util.Base64;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Class for keeping a snapshot of the current telemetry information.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class TelemetrySnapshot {

    private double mLat;
    private double mLon;
    private double mAlt;
    private HashMap<String, SensorEvent> mSensors;

    public TelemetrySnapshot() {
        mLat = 0;
        mLon = 0;
        mAlt = 0;
        mSensors = new HashMap<String, SensorEvent>();
    }

    public void updateLocation(double latitude, double longitude, double altitude) {
        mLat = latitude;
        mLon = longitude;
        mAlt = altitude;
    }

    public void updateSensor(SensorEvent event) {
        mSensors.put(event.sensor.getName(), event);
    }

    private Telemetry.Sensor getSensor(String sensorName) {
        SensorEvent event = mSensors.get(sensorName);
        Telemetry.Sensor.SensorType sensorType = null;
        switch (event.sensor.getType()) {
            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                sensorType = Telemetry.Sensor.SensorType.AMBIENT_TEMPERATURE;
                break;
            case android.hardware.Sensor.TYPE_LIGHT:
                sensorType = Telemetry.Sensor.SensorType.LIGHT;
                break;
            case android.hardware.Sensor.TYPE_PRESSURE:
                sensorType = Telemetry.Sensor.SensorType.PRESSURE;
                break;
            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                sensorType = Telemetry.Sensor.SensorType.RELATIVE_HUMIDITY;
                break;
            default:
                Log.e("Telemetry Error", "Unknown sensor type:" + sensorName);
                return null;
        }
        return Telemetry.Sensor.newBuilder()
                .setSensorType(sensorType).setValue(event.values[0]).build();
    }

    private Telemetry.ThreeAxisSensor getThreeAxisSensor(String sensorName) {
        SensorEvent event = mSensors.get(sensorName);
        Telemetry.ThreeAxisSensor.SensorType sensorType = null;
        switch (event.sensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.ACCELEROMETER;
                break;
            case android.hardware.Sensor.TYPE_GRAVITY:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.GRAVITY;
                break;
            case android.hardware.Sensor.TYPE_GYROSCOPE:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.GYROSCOPE;
                break;
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.LINEAR_ACCELERATION;
                break;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.MAGNETIC_FIELD;
                break;
            case android.hardware.Sensor.TYPE_ORIENTATION:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.ORIENTATION;
                break;
            case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                sensorType = Telemetry.ThreeAxisSensor.SensorType.ROTATION_VECTOR;
                break;
            default:
                Log.e("Telemetry Error", "Unknown sensor type:" + sensorName);
                return null;
        }
        return Telemetry.ThreeAxisSensor.newBuilder().setSensorType(sensorType)
                .setX(event.values[0]).setY(event.values[1]).setZ(event.values[2]).build();
    }

    private Telemetry.Position getPosition() {
        return Telemetry.Position.newBuilder()
                .setLatitude((float) mLat)
                .setLongitude((float) mLon)
                .setAltitude((float) mAlt).build();
    }

    public String getBase64EncodedProtobufDataPacket() {
        Telemetry.DataPacket.Builder dataPacket = Telemetry.DataPacket.newBuilder();
        dataPacket.setTimestamp(System.currentTimeMillis());
        dataPacket.setPosition(getPosition());
        Iterator<String> sensorsIt = mSensors.keySet().iterator();
        while (sensorsIt.hasNext()) {
            String sensorName = sensorsIt.next();
            SensorEvent event = mSensors.get(sensorName);
            if (event.values.length == 1) {
                Telemetry.Sensor s = getSensor(sensorName);
                if (s != null) {
                    dataPacket.addSensor(s);
                }
            } else if (event.values.length == 3) {
                Telemetry.ThreeAxisSensor tas = getThreeAxisSensor(sensorName);
                if (tas != null) {
                    dataPacket.addThreeAxisSensor(tas);
                }
            } else {
                Log.e("Telemetry Error", "Unknown sensor type:" + sensorName);
                return null;
            }
        }
        byte[] dataBytes = dataPacket.build().toByteArray();
        return Base64.encodeToString(dataBytes, Base64.DEFAULT);
    }

}
