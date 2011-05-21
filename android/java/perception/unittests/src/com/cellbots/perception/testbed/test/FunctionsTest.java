// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed.test;

import com.cellbots.perception.math.Functions;

import junit.framework.TestCase;

/**
 * Test harness for the Functions object.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class FunctionsTest extends TestCase {
  
  /** Test the rollover function. */
  public void testRollover() {
    assertEquals(0f, Functions.rollover(0f, -1.0f, 1.0f), 0.01);
    assertEquals(1.0f, Functions.rollover(1.0f, -1.0f, 1.0f), 0.01);
    assertEquals(-1.0f, Functions.rollover(-1.0f, -1.0f, 1.0f), 0.01);
    assertEquals(-0.9f, Functions.rollover(1.1f, -1.0f, 1.0f), 0.01);
    assertEquals(-0.5f, Functions.rollover(1.5f, -1.0f, 1.0f), 0.01);
    assertEquals(-0.5f, Functions.rollover(1.5f, -1.0f, 1.0f), 0.01);
    assertEquals(0.9f, Functions.rollover(-1.1f, -1.0f, 1.0f), 0.01);
    assertEquals(0.5f, Functions.rollover(-1.5f, -1.0f, 1.0f), 0.01);
    assertEquals(0.0f, Functions.rollover(-2.0f, -1.0f, 1.0f), 0.01);
    assertEquals(-0.5f, Functions.rollover(-2.5f, -1.0f, 1.0f), 0.01);
    assertEquals(1.0f, Functions.rollover(-3.0f, -1.0f, 1.0f), 0.01);
  }

}
