// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import com.cellbots.perception.math.Vector;

/**
 * A class that abstracts a position sensor.
 * This enables us to use a variety of integrative sensors in the same way.
 *
 * @author centaur@google.com (Anthony Francis)
 */
public interface PositionSensor {

  /**
   * Set the observed position. 
   * @param pos the vector to set
   */
  public abstract void setPos(Vector pos);

  /**
   * Get the position.
   * @return current vector position.
   */
  public abstract Vector getPos();

  /**
   * Set the last observed position. 
   * @param lastPos the vector to set
   */
  public abstract void setLastPos(Vector lastPos);

  /**
   * Get the last observed position.
   * @return last vector position.
   */
  public abstract Vector getLastPos();

}