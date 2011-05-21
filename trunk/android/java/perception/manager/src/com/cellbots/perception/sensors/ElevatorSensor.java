// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import com.cellbots.perception.math.FlatteningIntegrator;
import com.cellbots.perception.math.Integrator;
import com.cellbots.perception.math.Vector;


/**
 * A two-sensor joiner on linear acc + gravity to get vertical position.
 * @author centaur@google.com (Anthony Francis)
 */
public class ElevatorSensor implements PositionSensor {
  /** A linear acceleration sensor. */
  private VectorSensor linear;
  /** A gravity acceleration sensor. */
  private VectorSensor gravity;
  /** An integrator, preferably a FlatteningIntegrator. */
  private Integrator integrator;
  /** The integrated vertical position. */
  public Vector data;
  /** Current position. */
  public Vector pos;
  /** Last known position. */
  public Vector lastPos;
  /** Upper bound of recently recorded positions. */
  public float upperBound;
  /** Lower bound of recently recorded positions. */
  public float lowerBound;
  /** Averaged upper motion (upper bound averaged with current position. */
  public float upperMotion;
  /** Averaged lower motion (lower bound averaged with current position. */
  public float lowerMotion;
  
  /**
   * Create a new AccelSensor wrapping the two given Android Sensors.
   * @param linear a linear acceleration sensor
   * @param gravity a gravity sensor
   * @param damping how much to damp velocity
   * @param flattening how much to flatten position
   */
  public ElevatorSensor(
      VectorSensor linear,
      VectorSensor gravity,
      float damping,
      float flattening) {
    this.linear = linear;
    this.gravity = gravity;
    integrator = new FlatteningIntegrator(damping, flattening);
    data = new Vector();
    pos = new Vector();
    lastPos = new Vector();
  }

  /** Update the position based on the data from the encapsulated sensors. */
  public void update() {
    data.update(0, 0, Vector.dotproduct(linear.data, gravity.data.unit()));
    updatePosition();
  }
  
  /**
   * Perform Verlet integration of the position based on the accel vector.
   * This updates each position based on the current position, the current
   * velocity as estimated by the distance between old and new position,
   * and the current acceleration times the square of the timestep.
   * http://en.wikipedia.org/wiki/Verlet_integration
   */
  public void updatePosition() {
    // Integrate the position and velocity (damped)
    integrator.integrate(pos, lastPos, data, linear.elapsedTime);
    
    // Accumulate an upper and lower bound of the vertical motion
    upperBound = flattenSlowly(Math.max(Math.max(pos.z, upperBound), 0.0f));
    lowerBound = flattenSlowly(Math.min(Math.min(pos.z, lowerBound), 0.0f));
    
    // Accumulate an estimate of upper / lower motion, averaged with the bound
    upperMotion = (upperBound + pos.z) / 2.0f;
    lowerMotion = (lowerBound + pos.z) / 2.0f;
    
    // Overload the vector's other values to record the upper/lower bound
    pos.x = upperMotion;
    pos.y = lowerMotion;
  }
  
  /**
   * Flatten a value slowly towards a zero value.
   * @param value to flatten
   * @return flattened value
   */
  public float flattenSlowly(float value) {
    float flattening = ((FlatteningIntegrator) integrator).getFlattening();
    float difference = 1.0f - flattening;
    float slowFlat = 1.0f - (difference / 2.0f);
    return value * slowFlat;
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
