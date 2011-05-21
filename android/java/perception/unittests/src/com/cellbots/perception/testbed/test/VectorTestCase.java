// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed.test;

import com.cellbots.perception.math.Vector;

import junit.framework.TestCase;

/**
 * Superclass for test case for a Vector.
 * Provides testing methods that can be used to match Vectors.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public abstract class VectorTestCase extends TestCase {

  /** Super plumbing. s*/
  public VectorTestCase() {
    super();
  }

  /**
   * Super plumbing.
   * @param name
   */
  public VectorTestCase(String name) {
    super(name);
  }

  /**
   * Checks a set of expected values against the fields of a vector.
   * @param x expected x
   * @param y expected y
   * @param z expected z
   * @param mag expected magnitude
   * @param v actual vectors
   */
  protected void assertEquals(float x, float y, float z, float mag, Vector v) {
    assertEquals(x, v.x, 0.01f);
    assertEquals(y, v.y, 0.01f);
    assertEquals(z, v.z, 0.01f);
    assertEquals(mag, v.mag, 0.01f);
  }

  /**
   * Checks an expected vector against a new vector.
   * @param expected vector
   * @param actual vector
   */
  protected void assertEquals(Vector expected, Vector actual) {
    assertEquals(expected.x, actual.x, 0.01f);
    assertEquals(expected.y, actual.y, 0.01f);
    assertEquals(expected.z, actual.z, 0.01f);
    assertEquals(expected.mag, actual.mag, 0.01f);
  }
}
