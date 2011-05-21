// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;


/**
 * A vector sensor that processes Android proximity output.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class ProximitySensor extends VectorSensor {
  /**
   * Create a new ProximitySensor wrapping the given Android Sensor.
   * @param sensor
   */
  public ProximitySensor(Sensor sensor) {
    super(sensor);
  }

  /** The proximity sensor only has one value; return 0 on other axes. */
  @Override
  protected void updateSensorData(SensorEvent event) {
    data.update(event.values[0] / sensor.getMaximumRange(),
                event.values[0],
                sensor.getMaximumRange()); 
  }
}
