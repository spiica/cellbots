// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;



/**
 * A wrapper for an Android sensor.
 * This enables us to keep a most recent value with a timestamp so reading
 * sensors does not need to rely on waiting for a sample to come in. It also
 * enables use of a SensorWrapperFactory so the same codebase can be used
 * for different Android versions.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class SensorWrapper {

  /** The Android sensor we are encapsulating */
  public Sensor sensor;
  /** Current time interval recorded. */
  public long timestamp;
  /** Last time interval recorded. */
  public long lastTimestamp;
  /** Elapsed time since the mLastTime interval. */
  public long elapsedTime;

  /**
   * 
   */
  public SensorWrapper() {
    super();
  }

  /**
   * A time-independent integration of acceleration into velocity.
   * This is factored this apart into a lot of small methods so it is easy
   * to write a new subclass that overrides part of this computation.
   * @param event to extract data from
   */
  public void update(SensorEvent event) {
    updateTimestamps(event);
    saveOldSensorData();
    updateSensorData(event);
    updateDerivedValues();
    smoothAccumulators();
  }

  /** Capture previous values; done here so updateSensorData remains simple. */
  protected void saveOldSensorData() {
    // pass
  }

  /**
   * Update sensor values (may be different for different sensors).
   * @param event to extract data from
   */
  protected void updateSensorData(SensorEvent event) {
    // pass
  }

  /** Update values derived from the sensor data. */
  protected void updateDerivedValues() {
    // pass
  }

  /**
   * Compute a smoothed version of the extracted values. 
   */
  protected void smoothAccumulators() {
    // pass
  }

  /**
   * Update the timestamps based on the event.
   * @param event to extract data from
   */
  private void updateTimestamps(SensorEvent event) {
    lastTimestamp = timestamp;
    timestamp = event.timestamp;
    elapsedTime = timestamp - lastTimestamp;
  }

}
