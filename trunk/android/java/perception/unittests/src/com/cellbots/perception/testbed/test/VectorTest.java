// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed.test;

import com.cellbots.perception.math.Vector;

/**
 * Test harness for the 3D Vector object.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class VectorTest extends VectorTestCase {
  /**
   * Test method for {@link com.cellbots.perception.math.Vector#Vector()}.
   */
  public void testVectorConstructor() {
    assertEquals(0.0f, 0.0f, 0.0f, 0.0f, new Vector());
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#Vector(
   * float, float, float)}.
   */
  public void testVectorFloatFloatFloat() {
    assertEquals(0.0f, 0.0f, 0.0f, 0.0f, new Vector(0, 0, 0));
    assertEquals(8.0f, 6.0f, 0.0f, 10.f, new Vector(8, 6, 0));
    assertEquals(6.0f, 0.0f, 8.0f, 10.f, new Vector(6, 0, 8));
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#Vector(
   * com.cellbots.perception.math.Vector)}.
   */
  public void testVectorVector() {
    Vector temp = new Vector(8, 6, 0);
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, temp);
    Vector v = new Vector(temp);
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, v);
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#unit()}.
   */
  public void testUnit() {
    assertEquals(1.0f, 0.0f, 0.0f, 1.0f, (new Vector(1, 0, 0)).unit());
    assertEquals(0.0f, 1.0f, 0.0f, 1.0f, (new Vector(0, 1, 0)).unit());
    assertEquals(0.0f, 0.0f, 1.0f, 1.0f, (new Vector(0, 0, 1)).unit());
    assertEquals(1.0f, 0.0f, 0.0f, 1.0f, (new Vector(3, 0, 0)).unit());
    assertEquals(0.0f, 1.0f, 0.0f, 1.0f, (new Vector(0, 4, 0)).unit());
    assertEquals(0.0f, 0.0f, 1.0f, 1.0f, (new Vector(0, 0, 5)).unit());
    assertEquals(0.8f, 0.6f, 0.0f, 1.0f, (new Vector(8, 6, 0)).unit());
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#update()}.
   */
  public void testUpdate() {
    // Update should be a no-op if we use the API.
    Vector v = new Vector(8, 6, 0);
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, v);
    v.update();
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, v);

    // Manual update "should" put us in a bad state!
    v.x = 0.0f;
    v.y = 0.0f;
    v.z = 0.0f;
    assertEquals(0.0f, 0.0f, 0.0f, 10.0f, v);
    
    // Update should resync the vector.
    v.update();
    assertEquals(0.0f, 0.0f, 0.0f, 0.0f, v);
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#update(
   * float, float, float)}.
   */
  public void testUpdateFloatFloatFloat() {
    Vector v = new Vector();
    assertEquals(0.0f, 0.0f, 0.0f, 0.0f, v);
    v.update(8.0f, 6.0f, 0.0f);
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, v);
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#update(
   * com.cellbots.perception.math.Vector)}.
   */
  public void testUpdateVector() {
    Vector temp = new Vector(8, 6, 0);
    assertEquals(8.0f, 6.0f, 0.0f, 10.0f, temp);
    Vector v = new Vector();
    assertEquals(0.0f, 0.0f, 0.0f, 0.0f, v);
    v.update(temp);
    assertEquals(8, 6, 0, 10, v);
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#magnitude(
   * float, float, float)}.
   */
  public void testMagnitudeFloatFloatFloat() {
    assertEquals(0.0, Vector.magnitude(0, 0, 0));
    assertEquals(10.0, Vector.magnitude(8, 6, 0));
    assertEquals(10.0, Vector.magnitude(0, 6, 8));
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#magnitude(
   * com.cellbots.perception.math.Vector)}.
   */
  public void testMagnitudeVector() {
    assertEquals(0.0, Vector.magnitude(new Vector()));
    assertEquals(10.0, Vector.magnitude(new Vector(8, 6, 0)));
  }

  /**
   * Test method for {@link com.cellbots.perception.math.Vector#dotproduct(
   * com.cellbots.perception.math.Vector, com.cellbots.perception.math.Vector)}.
   */
  public void testDotproductVectorVector() {
    assertEquals(0.0f, Vector.dotproduct(new Vector(), new Vector()));
    assertEquals(1.0f, Vector.dotproduct(new Vector(1, 0, 0), new Vector(1, 0, 0)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(1, 0, 0), new Vector(0, 1, 0)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(1, 0, 0), new Vector(0, 0, 1)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(0, 1, 0), new Vector(1, 0, 0)));
    assertEquals(1.0f, Vector.dotproduct(new Vector(0, 1, 0), new Vector(0, 1, 0)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(0, 1, 0), new Vector(0, 0, 1)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(0, 0, 1), new Vector(1, 0, 0)));
    assertEquals(0.0f, Vector.dotproduct(new Vector(0, 0, 1), new Vector(0, 1, 0)));
    assertEquals(1.0f, Vector.dotproduct(new Vector(0, 0, 1), new Vector(0, 0, 1)));
    assertEquals(3.0f, Vector.dotproduct(new Vector(1, 1, 1), new Vector(1, 1, 1)));
    assertEquals(12.0f, Vector.dotproduct(new Vector(2, 2, 2), new Vector(2, 2, 2)));
  }
}
