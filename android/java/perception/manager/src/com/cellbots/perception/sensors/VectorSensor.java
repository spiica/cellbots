// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import com.cellbots.perception.PerceptionManager;
import com.cellbots.perception.math.Vector;

/**
 * Encapsulates the variables we extract from an x,y,z sensor.
 * Enables us to easily add gyroscopes, gravity, etc.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class VectorSensor extends SensorWrapper {
  //-------------------------------------------------------------------
  // Vector values
  //-------------------------------------------------------------------
  /** Current value of the vector. */
  public Vector data;
  /** Value on the previous timestamp. */
  public Vector lastData;
  
  
  //-------------------------------------------------------------------
  // Derived values
  //-------------------------------------------------------------------
  /**
   * Zero crossings of x, y, or z in last time interval.
   * This is actually scaled by the magnitude of change so that a large change
   * is counted more than tiny wiggles about the axis.
   */
  public double zeroCrossings;
  /** Previous value of the zero crossing count */
  public double lastZeroCrossings;

  /** Smoothed values of magnitude. */
  public double smoothMag;
  /** Smoothed value of zero crossings. */
  public double smoothZeroCrossings;


  //-------------------------------------------------------------------
  // Constructors and accessors
  //-------------------------------------------------------------------
  /**
   * Create a new VectorSensor wrapping the given Android Sensor.
   * @param sensor to encapsulate
   */
  public VectorSensor(Sensor sensor) {
    this.sensor = sensor;
    this.data = new Vector();
    this.lastData = new Vector();
  }

  /**
   * Create a new VectorSensor that extracts the default type of sensor.
   * @param sensorManager sensor manager that provides the sensor
   * @param type what kind to extract
   */
  public VectorSensor(SensorManager sensorManager, int type) {
    this(sensorManager.getDefaultSensor(type));
  }

  @Override
  protected void saveOldSensorData() {
    lastData.update(data);
  }

  @Override
  protected void updateSensorData(SensorEvent event) {
    data.update(event.values[0], event.values[1], event.values[2]);
  }

  @Override
  protected void updateDerivedValues() {
    // Compute zero crossings
    lastZeroCrossings = zeroCrossings;
    zeroCrossings = 
      crosses(data.x, lastData.x) 
      + crosses(data.y, lastData.y)
      + crosses(data.z, lastData.z);
  }

  @Override
  protected void smoothAccumulators() {
    smoothMag = getWeightedSum(smoothMag, data.mag);
    smoothZeroCrossings = getWeightedSum(smoothZeroCrossings, zeroCrossings);
  }

  /**
   * If a zero crossing has happened, return absolute value of change.
   * @param value the current value
   * @param last the last value
   * @return the amount by which we crossed the origin
   */
  public double crosses(double value, double last) {
    if ((value > 0 && last < 0) || (value < 0 && last > 0)) {
      return Math.abs(value - last);
    } else {
      return 0;
    }
  }

  /**
   * Weights new percepts and mixes them in to the old value.
   * @param oldValue accumulated older value
   * @param newValue new value to add in
   * @return a blend of the two values based on the elapsed time
   */
  private double getWeightedSum(double oldValue, double newValue) {
    double scaledInterval = elapsedTime / PerceptionManager.STANDARD_INTERVAL;
    double perceptualWeight = Math.min(1.0, Math.max(0.0, 
        PerceptionManager.NEW_INPUT_WEIGHT * scaledInterval));
    return (1.0 - perceptualWeight) * oldValue + perceptualWeight * newValue;
  }
}