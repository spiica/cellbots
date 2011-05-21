// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.percepts;

/**
 * Encapsulates a "percept" that can be detected or not based on a raw signal.
 * The "value" is the raw signal, and "detected" is the boolean computed
 * from it. A detection event has a duration that lasts before being re-set, 
 * and it also has a timeout where it re fires even if the raw value has
 * been continuously over the threshold.
 * @author  centaur@google.com (Anthony Francis)
 */
public class Percept {
  /** Display name of this percept (for logging). */
  private String name;
  
  /** Level the value has to reach to trigger this percept. */
  private double threshold;
  
  /** Duration a percept is considered to last once triggered. */
  private double duration;
  
  /** Time after which a continiously firing percept "re-fires". */
  private double timeout;
  
  /** Continuous value used as a threshold for detecting a percept. */
  private double value;
  
  /** Boolean trigger that we've detected this percept. */
  private boolean detected;
  
  /** Last time this percept was considered to trigger. */
  private long lastOnset;
  
  /** Is this a new onset of a percept or has it "gone on too long"? */
  private boolean newOnset;

  /**
   * Create a percept with an initial parameterization.
   * @param name
   * @param threshold
   * @param duration
   * @param timeout
   */
  public Percept(
      String name, double threshold, double duration, double timeout) {
    this.name = name;
    this.threshold = threshold;
    this.duration = duration;
    this.timeout = timeout;
    lastOnset = -1;           // signal value for neverShaken.
  }

  /** Update the raw value and then the boolean value. */
  public boolean update(double rawValue, long timestamp) {
    setValue(rawValue);
    return update(timestamp);
  }

  /**
   * Update the detected state based on the value + threshold.
   * The "on" state is sticky for the time length of duration, computed from
   * the last onset time (which is set on the on transition).  The "on"
   * state can re-trigger if the duration since last onset exceeds the timeout.
   * @param timestamp
   */
  public boolean update(long timestamp) {
    if (value > threshold) {
      long timeSinceLastOnset = timestamp - lastOnset;
      if (!detected) {
        transition(true, timestamp);
        if (neverDetected() || timeSinceLastOnset > duration) {
          newOnset = false;
          return true;
        }
      } else if (timeSinceLastOnset > timeout) {
        lastOnset = timestamp;
        newOnset = true;
        return true;
      }
    } else {
      transition(false, timestamp);
    }
    newOnset = false;
    return false;
  }

  /**
   * Updates the onsets and detected state respecting the duration and timeout.
   * @param detected is the robot shaking?
   * @param timestamp what time we're doing this at.
   */
  public void transition(boolean detected, long timestamp) {
    // If we're transitioning to true AND enough time has passed from
    // the last sideways onset to count as a new event, reset the onset.
    if (detected) {
      if (!this.detected) {
        this.detected = true;
        if (neverDetected()) {
          lastOnset = timestamp;
        } else if (timestamp - lastOnset > timeout) {
          lastOnset = timestamp;
        } // otherwise, don't update the timestamp ... too soon.
      }
    } else {  // we're not shaking right now
      if (this.detected) {  // if we were shaking ...
        if (timestamp - lastOnset > duration) {
          this.detected = false;  // ... update only if past response timeout
        }  // otherwise shaking is "sticky" for 
      }  // otherwise we're already not shaking
    }
  }

  /**
   * Set the name that will appear in logging.
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the name that will appear in logging.
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the threshold of the value that will trigger detected.
   * @param threshold the threshold to set
   */
  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  /**
   * Get the threshold of the value that will trigger detected.
   * @return the threshold
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * Get the threshold of the value that will trigger detected.
   * @param duration the duration to set
   */
  public void setDuration(double duration) {
    this.duration = duration;
  }

  /**
   * Set the duration that detected is presumed to last.
   * @return the duration
   */
  public double getDuration() {
    return duration;
  }

  /**
   * Set the timeout of a detection for when it re-fires.
   * @param timeout the timeout to set
   */
  public void setTimeout(double timeout) {
    this.timeout = timeout;
  }

  /**
   * Get the timeout of a detection for when it re-fires.
   * @return the timeout
   */
  public double getTimeout() {
    return timeout;
  }

  /**
   * The raw value behind the boolean sensor.
   * @param value
   */
  public void setValue(double value) {
    this.value = value; 
  }
  
  /**
   * Summed variable used to represent the value of the sensor.
   * @return the handled accumulator
   */
  public double getValue() {
    return value;
  }

  /**
   * Has this percept been detected?
   * This cannot be set externally; it's computed by the percept.
   * @return the current percept state.
   */
  public boolean isDetected() {
    return detected;
  }

  /**
   * What was the last onset of detected?
   * This cannot be set externally; it's computed by the percept.
   * @return the lastOnset
   */
  public long getDetectedOnset() {
    return lastOnset;
  }
  
  /**
   * Is this onset of the percept a new one?
   * This cannot be set externally; it's computed by the percept.
   * @return newOnset
   */
  public boolean isNewOnset() {
    return newOnset;
  }

  /**
   * Has this percept ever been detected?
   * This cannot be set externally; it's computed by the percept.
   * @return true only if we're at the initial signal value.
   */
  public boolean neverDetected() {
    return lastOnset == -1;
  }
}