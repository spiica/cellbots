// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * @author centaur@google.com (Your Name Here)
 *
 */
public class SensorWrapperFactory {
  /** Handles for the hardware sensors */
  private SensorManager sensorManager;

  /**
   * Create a SensorWrapperFactory with the injected SensorManager.
   * @param newSensorManager
   */
  public SensorWrapperFactory(SensorManager newSensorManager) {
    setSensorManager(newSensorManager);
  }
  
  /**
   * Get a Sensor of the given type.
   * @param type The Sensor.TYPE_thatwewant.
   * @return a Sensor.
   */
  public Sensor getSensor(int type) {
    return getSensorManager().getDefaultSensor(type);
  }
  
  /**
   * Return a vector sensor wrapping the given sensor type.
   * @param type
   * @return A VectorSensor of the given sensor type.
   */
  public VectorSensor getVectorSensor(int type) {
    return new VectorSensor(getSensorManager(), type);
  }
  
  /**
   * Return a vector sensor wrapping the accelerometer.
   * @return A VectorSensor wrapping the accelerometer.
   */
  public VectorSensor getAccelerometerSensor() {
    return getVectorSensor(Sensor.TYPE_ACCELEROMETER);
  }

  /** Pre-Gingerbread, we don't have TYPE_GRAVITY. */
  class GravitySensorFroyo extends VectorSensor {
    GravitySensorFroyo() {
      super(null);
    }
  }

  /** Post-Gingerbread, we do have TYPE_GRAVITY. */
  class GravitySensorGingerbread extends VectorSensor {
    GravitySensorGingerbread() {
      super(getSensor(Sensor.TYPE_GRAVITY));
    }
  }

  /**
   * Return a VectorSensor wrapping gravity.
   * @return A vector sensor wrapping gravity.
   */
  public VectorSensor getGravitySensor() {
    try {
      return new GravitySensorGingerbread();
    } catch (Throwable e) {
      return new GravitySensorFroyo();
    }
  }

  /**
   * Return a vector sensor wrapping the gyroscope.
   * @return A VectorSensor wrapping the gyroscope.
   */
  public VectorSensor getGyroscopeSensor() {
    // TODO(centaur): fail if the sensor type is not known
    return getVectorSensor(Sensor.TYPE_GYROSCOPE);
  }


  /**
   * Return a AccelSensor with the given flattening and damping.
   * @param type The Sensor.TYPE_wewanthere.
   * @param damping Damping factor for velocity.
   * @param flattening Flattening factor for position.
   * @return An accel sensor wrapping the given sensor type.
   */
  public AccelSensor getAccelSensor(int type, float damping, float flattening) {
    return new AccelSensor(getSensor(type), damping, flattening);
  }
  
  /** Pre-Gingerbread, we don't have TYPE_LINEAR_ACCELERATION. */
  class AccelSensorFroyo extends AccelSensor {
    AccelSensorFroyo(float damping, float flattening) {
      super(null, damping, flattening);
    }
  }

  /** Post-Gingerbread, we do have TYPE_LINEAR_ACCELERATION. */
  class AccelSensorGingerbread extends AccelSensor {
    AccelSensorGingerbread(float damping, float flattening) {
      super(getSensor(Sensor.TYPE_LINEAR_ACCELERATION), damping, flattening);
    }
  }
  
  /**
   * Return a default AccelSensor with the given flattening and damping.
   * @param damping Damping factor for velocity.
   * @param flattening Flattening factor for position.
   * @return An accel sensor wrapping the given sensor type.
   */
  public AccelSensor getAccelSensor(float damping, float flattening) {
    try {
      return new AccelSensorGingerbread(damping, flattening);
    } catch (Throwable e) {
      return new AccelSensorFroyo(damping, flattening);
    }
  }

  /**
   * Return an OrientationSensor wrapping the given sensor type.
   * @param type The Sensor.TYPE_wewanthere.
   * @return An orientation sensor of the given sensor type.
   */
  public OrientationSensor getOrientationSensor(int type) {
    return new OrientationSensor(getSensorManager().getDefaultSensor(type));
  }

  /**
   * Return an OrientationSensor wrapping the standard orientation sensor.
   * @return An orientation sensor of the given sensor type.
   */
  public OrientationSensor getOrientationSensor() {
    return getOrientationSensor(Sensor.TYPE_ORIENTATION);
  }
  /**
   * Return a ProximitySensor wrapping the given sensor type.
   * @param type The Sensor.TYPE_wewanthere.
   * @return An proximity sensor of the given sensor type.
   */
  public ProximitySensor getProximitySensor(int type) {
    return new ProximitySensor(getSensorManager().getDefaultSensor(type));
  }

  /**
   * Return a ProximitySensor wrapping the standard proximity sensor.
   * @return A proximity sensor of the standard type.
   */
  public ProximitySensor getProximitySensor() {
    return getProximitySensor(Sensor.TYPE_PROXIMITY);
  }

  /**
   * Return a ProximitySensor wrapping the standard proximity sensor.
   * 
   * @param linear A VectorSensor wrapping linear acceleration.
   * @param gravity A VectorSensor wrapping gravitational acceleration.
   * @param damping Damping factor for vertical movement.
   * @param flattening Flattening factor for vertical position.
   * @return An elevator sensor wrapping the given vector sensors.
   */
  public ElevatorSensor getElevatorSensor(
      VectorSensor linear,
      VectorSensor gravity,
      float damping,
      float flattening) {
    return new ElevatorSensor(linear, gravity, damping, flattening);
  }
  
  /**
   * @param sensorManager the sensorManager to set
   */
  public void setSensorManager(SensorManager sensorManager) {
    this.sensorManager = sensorManager;
  }

  /**
   * @return the sensorManager
   */
  public SensorManager getSensorManager() {
    return sensorManager;
  }
}
