// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;

/**
 * Encapsulates a 3-vector and its magnitude computation.
 * @author centaur@google.com (Anthony Francis)
 */
public class Vector {
  //-------------------------------------------------------------------
  // Vector state
  //-------------------------------------------------------------------
  /** Value on the X axis */
  public float x;
  /** Value on the Y axis */
  public float y;
  /** Value on the Z axis */
  public float z;
  /** Value of the vector magnitude (X^2 + Y^2 + Z^2)^0.5. */
  public double mag;


  //-------------------------------------------------------------------
  // Constructors
  //-------------------------------------------------------------------
  /** Create a new vector. */
  public Vector() {
  }

  /**
   * Create a new vector with a given set of values.
   * @param x Value on the X axis
   * @param y Value on the Y axis
   * @param z Value on the Z axis
   */
  public Vector(float x, float y, float z) {
    super();
    update(x, y, z);
  }

  /**
   * Create a new vector with a given set of values.
   * @param x Value on the X axis
   * @param y Value on the Y axis
   * @param z Value on the Z axis
   */
  public Vector(double x, double y, double z) {
    super();
    update((float) x, (float) y, (float) z);
  }

  /**
   * Create a new vector from an old vector. 
   * @param vector
   */
  public Vector(Vector vector) {
    this(vector.x, vector.y, vector.z);
  }

  //-------------------------------------------------------------------
  // Updaters
  //-------------------------------------------------------------------
  /**
   * Update the vector with its new values.
   * @param x Value on the X axis
   * @param y Value on the Y axis
   * @param z Value on the Z axis
   */
  public void update(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
    update();
  }

  /** Update the magnitude of the vector based on its values. */
  public void update() {
    this.mag = magnitude(this);
  }

  /**
   * Update a vector from another vector.
   * @param vector
   */
  public void update(Vector vector) {
    update(vector.x, vector.y, vector.z);
  }

  /**
   * Update a vector by a scaling factor.
   * @param scale
   */
  public void scale(float scale) {
    update(x * scale, y * scale, z * scale);
  }

  /**
   * Update a vector by adding another vector.
   * @param vector
   */
  public void add(Vector vector) {
    update(x + vector.x, y + vector.y, z + vector.z);
  }

  //-------------------------------------------------------------------
  // Factory methods
  //-------------------------------------------------------------------
  /** Return a unit vector based on this vector. */
  public Vector unit() {
    if (mag != 0) {
      return new Vector(x / mag, y / mag, z / mag);
    } else {
      return new Vector(0, 0, 0);
    }
  }

  //-------------------------------------------------------------------
  // Utility methods
  //-------------------------------------------------------------------
  @Override
  public String toString() {
    return String.format("[|%f, %f, %f| = %f]", x, y, z, mag);
  }

  //-------------------------------------------------------------------
  // Static utility methods
  //-------------------------------------------------------------------
  /**
   * Compute the Euclidean magnitude of three values.
   * @param x Value on the X axis
   * @param y Value on the Y axis
   * @param z Value on the Z axis
   */
  public static double magnitude(float x, float y, float z) {
    return Math.sqrt(x * x + y * y + z * z);
  }

  /**
   * Compute the Euclidean magnitude of a vector.
   * @param vector
   * @return the magnitude of the vector
   */
  public static double magnitude(Vector vector) {
    return magnitude(vector.x, vector.y, vector.z);
  }

  /**
   * Return the dot product of this vector and another vector.
   * @param a
   * @param b
   * @return the float dot product
   */
  public static float dotproduct(Vector a, Vector b) {
    return a.x * b.x + a.y * b.y + a.z * b.z;
  }
}