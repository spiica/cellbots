// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.math;

/**
 * Generic math functions we may reuse elsewhere.
 * @author centaur@google.com (Anthony Francis)
 */
public class Functions {

  /**
   * Takes a value and wraps it so that it appears between upper and lower.
   * Basically this is a modulus function with a different zero, but the
   * math to make it look right is a bit tricky.
   * @param value to be wrapped.
   * @param lower value that is allowed.
   * @param upper value that is allowed.
   * @return the wrapped value.
   */
  public static float rollover(float value, float lower, float upper) {
    if (value >= lower && value <= upper) {
      return value;
    } else if (value < lower) {
      return upper - (lower - value) % (upper - lower);
    } else {  // value > upper
      return lower + (value - lower) % (upper - lower);
    }
  }

  /**
   * Clamps a value between the lower and upper values.
   * @param value to be clamped.
   * @param lower value that is allowed.
   * @param upper value that is allowed.
   * @return the clamped value.
   */
  public static float clamp(float value, float lower, float upper) {
    if (value >= lower && value <= upper) {
      return value;
    } else if (value < lower) {
      return lower;
    } else {  // value > upper
      return upper;
    }
  }

  /**
   * Squeeze a value sigmoid style between the lower and upper values.
   * Unlike the clamp, this allows us to visualize values that grow far beyond
   * the desired range by showing them approach the bounds asymptotically.
   * @param value to be squeezed.
   * @param lower value that is allowed.
   * @param upper value that is allowed.
   * @return the clamped value.
   */
  public static float squeeze(float value, float lower, float upper) {
    // Yes this is very explicit but I trust the Java compiler to sort it out.
    float scale = upper - lower;
    float midpoint = lower + scale / 2;
    float distance = value - midpoint;
    float sigmoid = (float) Math.tanh(distance);
    float scaled = sigmoid * scale / 2;
    return midpoint + scaled;
  }

}
