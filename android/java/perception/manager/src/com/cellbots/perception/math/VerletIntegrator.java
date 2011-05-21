// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;

import com.cellbots.perception.PerceptionManager;

/**
 * Verlet Integrator implementation to turn acceleration into position.
 * Perform Verlet integration of the position based on the accel vector.
 * This updates each position based on the current position, the current
 * velocity as estimated by the distance between old and new position,
 * and the current acceleration times the square of the timestep.
 * http://en.wikipedia.org/wiki/Verlet_integration
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class VerletIntegrator implements Integrator {
  @Override
  public void integrate(
      Vector oldPos, Vector newPos, Vector accel, long nanosec) {
    // Scale the time (in nanoseconds) down in a way that should hopefully
    // not completely eat the value inside the vagaries of numerical precision.
    double timesquared = ((nanosec * nanosec) * 1.0
        / (PerceptionManager.SECOND * PerceptionManager.SECOND));

    // Capture the current value for the verlet's funny half step
    Vector tempPos = new Vector(newPos);

    // Perform the updates using the verlet's funny half step
    newPos.x += (newPos.x - oldPos.x) + accel.x * timesquared;
    newPos.y += (newPos.y - oldPos.y) + accel.y * timesquared;
    newPos.z += (newPos.z - oldPos.z) + accel.z * timesquared;
    newPos.update();  // to make sure the magnitude is correct.

    // Now save the previous current value as the new old values
    oldPos.update(tempPos);
  }
}
