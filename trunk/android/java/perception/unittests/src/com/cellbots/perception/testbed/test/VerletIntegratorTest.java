// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed.test;

import com.cellbots.perception.PerceptionManager;
import com.cellbots.perception.math.Integrator;
import com.cellbots.perception.math.Vector;
import com.cellbots.perception.math.VerletIntegrator;

/**
 * Test harness for the Verlet Integrator object.
 * @author centaur@google.com (Anthony Francis)
 */
public class VerletIntegratorTest extends VectorTestCase {
  /** Integrator used in the test. */
  Integrator integrator;

  @Override
  protected void setUp() throws Exception {
      super.setUp();
      integrator = new VerletIntegrator();
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationZeroAcceleration() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(1, 0, 0);
    Vector accNew = new Vector(0, 0, 0);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(1, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(2, 0, 0), newPos);  // constant velocity movement 
    assertEquals(new Vector(0, 0, 0), accNew);  // accel should not be touched 
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationConstantAccelerationX() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(1, 0, 0);
    Vector accNew = new Vector(1, 0, 0);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(1, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(3, 0, 0), newPos);  // increased velocity movement 
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched 
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationOpposedAccelerationX() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(1, 0, 0);
    Vector accNew = new Vector(-2, 0, 0);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(1, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(0, 0, 0), newPos);  // should return to old pos 
    assertEquals(new Vector(-2, 0, 0), accNew);  // accel should not be touched 
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationConstantAccelerationY() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(0, 1, 0);
    Vector accNew = new Vector(0, 1, 0);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0, 1, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(0, 3, 0), newPos);  // increased velocity movement 
    assertEquals(new Vector(0, 1, 0), accNew);  // accel should not be touched 
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationConstantAccelerationZ() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(0, 0, 1);
    Vector accNew = new Vector(0, 0, 1);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0, 0, 1), oldPos);  // original new -> old position
    assertEquals(new Vector(0, 0, 3), newPos);  // increased velocity movement 
    assertEquals(new Vector(0, 0, 1), accNew);  // accel should not be touched 
  }

  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationHalfSecond() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(0, 0, 0);
    Vector accNew = new Vector(1, 0, 0);
    long elapsed = PerceptionManager.SECOND / 2;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(0.25f, 0, 0), newPos);  // smaller change in pos
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0.25f, 0, 0), oldPos);  // original new -> old pos
    assertEquals(new Vector(0.75f, 0, 0), newPos);  // acceleration kicks in
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0.75f, 0, 0), oldPos);  // original new -> old pos
    assertEquals(new Vector(1.50f, 0, 0), newPos);  // smaller change in pos
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(1.50f, 0, 0), oldPos);  // original new -> old pos
    assertEquals(new Vector(2.50f, 0, 0), newPos);  // acceleration kicks in
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
  }
  
  /**
   * Test method for {@link
   * com.cellbots.perception.math.VerletIntegrator#integrate(
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * com.cellbots.perception.math.Vector,
   * long)}.
   */
  public void testComputeVerletIntegrationSequence() {
    Vector oldPos = new Vector(0, 0, 0);
    Vector newPos = new Vector(0, 0, 0);
    Vector accNew = new Vector(0, 0, 0);
    long elapsed = PerceptionManager.SECOND;
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(0, 0, 0), newPos);  // no change in position
    assertEquals(new Vector(0, 0, 0), accNew);  // accel should not be touched
    accNew.update(1, 0, 0);  // accelerate
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(0, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(1, 0, 0), newPos);  // acceleration kicks in
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(1, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(3, 0, 0), newPos);  // acceleration kicks in
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(3, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(6, 0, 0), newPos);  // acceleration kicks in
    assertEquals(new Vector(1, 0, 0), accNew);  // accel should not be touched
    accNew.update(0, 0, 0);  // coast
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(6, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(9, 0, 0), newPos);  // constant velocity
    assertEquals(new Vector(0, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(9, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(12, 0, 0), newPos);  // STILL constant velocity
    assertEquals(new Vector(0, 0, 0), accNew);  // accel should not be touched
    accNew.update(-1, 0, 0);  // decelerate
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(12, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(14, 0, 0), newPos);  // slowing down
    assertEquals(new Vector(-1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(14, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(15, 0, 0), newPos);  // still slowing down
    assertEquals(new Vector(-1, 0, 0), accNew);  // accel should not be touched
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(15, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(15, 0, 0), newPos);  // come to a stop
    assertEquals(new Vector(-1, 0, 0), accNew);  // accel should not be touched
    accNew.update(0, 0, 0);  // stop the burn
    integrator.integrate(oldPos, newPos, accNew, elapsed);
    assertEquals(new Vector(15, 0, 0), oldPos);  // original new -> old position
    assertEquals(new Vector(15, 0, 0), newPos);  // come to a stop
    assertEquals(new Vector(0, 0, 0), accNew);  // accel should not be touched
  }
}
