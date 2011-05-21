// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;


/**
 * An integrator that pulls the integrated position back to zero.
 * Like the damping integrator, this enables us to integrate an acceleration
 * sensor which has noise, flattening perceived movements so that the robot's
 * perception of its position returns to a "zero point" after a few moments
 * where no acceleration is perceived. This embeds a damping integrator to
 * further reduce the effects of noise.
 * @author centaur@google.com (Anthony Francis)
 */
public class FlatteningIntegrator extends DampingIntegrator {
  /** How much to drag the position back to zero. */
  private float flattening;
  
  /** Create the flattening integrator with a given damping. */
  public FlatteningIntegrator(float damping, float flattening) {
    super(damping);
    this.flattening = flattening;
  }
  
  /** Overrides the top integrator with a damping factor. */
  @Override
  public void integrate(
      Vector oldPos, Vector newPos, Vector accel, long nanosec) {
    super.integrate(oldPos, newPos, accel, nanosec);
    flattenPosition(oldPos);
    flattenPosition(newPos);
  }

  /**
   * Pull the position slowly back towards zero. 
   * @param pos
   */
  public void flattenPosition(Vector pos) {
    pos.scale(flattening);
  }

  /**
   * How much to slow the velocity by.
   * @param flattening the flattening to set
   */
  public void setFlattening(float flattening) {
    this.flattening = flattening;
  }

  /**
   * How the velocity is slowed by.
   * @return the flattening
   */
  public float getFlattening() {
    return flattening;
  }
}
