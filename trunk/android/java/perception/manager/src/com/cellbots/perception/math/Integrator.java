// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;


/**
 * Abstract integration class suitable to be embedded in another object.
 * The interface takes an acceleration, a previous position, and the elapsed
 * time and produces the current position, suitable for Verlet Integration.
 * @author centaur@google.com (Your Name Here)
 *
 */
public interface Integrator {
  /** Integrate new pos based on accel vector, last pos and elapsed time. */
  public abstract void integrate(
      Vector oldPos, Vector newPos, Vector accel, long nanosec);
}
