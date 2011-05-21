// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;


/**
 * An integrator that damps velocity exponentially down to zero.
 * This enables us to apply an integrator to an acceleration sensor which
 * has some small amount of noise, damping down small velocity changes so
 * the phone or robot won't think it is bouncing around the room like an
 * idealized pool ball in a zero-gee billards tank.
 * @author centaur@google.com (Anthony Francis)
 */
public class DampingIntegrator extends VerletIntegrator {
  /** How much to slow the velocity by. */
  private float damping;

  /** Create the damping integrator with a given damping. */
  public DampingIntegrator(float damping) {
    this.damping = damping;
  }

  /** Overrides the top integrator with a damping factor. */
  @Override
  public void integrate(
      Vector oldPos, Vector newPos, Vector accel, long nanosec) {
    super.integrate(oldPos, newPos, accel, nanosec);
    dampVelocity(oldPos, newPos);
  }

  /**
   * Slow down the implied velocity by the damping amount. 
   * @param oldPos
   * @param newPos
   */
  public void dampVelocity(Vector oldPos, Vector newPos) {
    newPos.x = oldPos.x + (newPos.x - oldPos.x) * damping;
    newPos.y = oldPos.y + (newPos.y - oldPos.y) * damping;
    newPos.z = oldPos.z + (newPos.z - oldPos.z) * damping;
    newPos.update();
  }

  /**
   * How much to slow the velocity by.
   * @param damping the damping to set
   */
  public void setDamping(float damping) {
    this.damping = damping;
  }

  /**
   * How the velocity is slowed by.
   * @return the damping
   */
  public float getDamping() {
    return damping;
  }
}
