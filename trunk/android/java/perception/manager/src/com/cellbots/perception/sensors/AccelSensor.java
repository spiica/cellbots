// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import com.cellbots.perception.math.FlatteningIntegrator;
import com.cellbots.perception.math.Integrator;
import com.cellbots.perception.math.Vector;

/**
 * A vector sensor that processes acceleration data and integrates position.
 * @author centaur@google.com (Anthony Francis)
 */
public class AccelSensor extends VectorSensor implements PositionSensor {
  /** The integrator used to record the integrated position. */
  private Integrator integrator;

  /** The current position. */
  private Vector pos;

  /** Previous position (required for Verlet integration). */
  private Vector lastPos;

  /** 
   * Create a new AccelSensor wrapping the given Android Sensor.
   * @param sensor the sensor wrapped by the superclass.
   * @param damping velocity damping with 1.0 is none.
   * @param flattening position flattening with 1.0 is none.
   */
  public AccelSensor(Sensor sensor, float damping, float flattening) {
    super(sensor);
    integrator = new FlatteningIntegrator(damping, flattening);
    pos = new Vector();
    lastPos = new Vector();
  }

  @Override
  public void update(SensorEvent event) {
    super.update(event);
    updatePosition();
  }

  /** Integrate the position based on the accel vector. */
  public void updatePosition() {
    integrator.integrate(getPos(), getLastPos(), data, elapsedTime);
  }

  @Override
  public void setPos(Vector pos) {
    this.pos = pos;
  }

  @Override
  public Vector getPos() {
    return pos;
  }

  @Override
  public void setLastPos(Vector lastPos) {
    this.lastPos = lastPos;
  }

  @Override
  public Vector getLastPos() {
    return lastPos;
  }
}
