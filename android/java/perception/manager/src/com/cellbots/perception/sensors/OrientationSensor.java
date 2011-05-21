// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;


/**
 * A vector sensor that processes Android orientation output.
 * Basically, this just resorts the input values into something more useful
 * to our downstream processing.
 * @author centaur@google.com (Anthony Francis)
 */
public class OrientationSensor extends VectorSensor {
  /** Create a new OrientationSensor wrapping the given Android Sensor. s*/
  public OrientationSensor(Sensor sensor) {
    super(sensor);
  }

  /** The orientation sensor is not in x,y,z order; get the right order. */
  @Override
  protected void updateSensorData(SensorEvent event) {
    data.update(event.values[1], event.values[2], event.values[0]); 
  }
}
