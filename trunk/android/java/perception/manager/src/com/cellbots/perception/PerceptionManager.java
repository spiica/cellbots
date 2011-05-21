// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.cellbots.perception.percepts.Percept;
import com.cellbots.perception.sensors.AccelSensor;
import com.cellbots.perception.sensors.ElevatorSensor;
import com.cellbots.perception.sensors.OrientationSensor;
import com.cellbots.perception.sensors.ProximitySensor;
import com.cellbots.perception.sensors.SensorWrapper;
import com.cellbots.perception.sensors.SensorWrapperFactory;
import com.cellbots.perception.sensors.VectorSensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates access to the hardware sensors on the Android device and makes
 * them available as higher-order features like "shaking" or "acceleration
 * minus * gravity" or "staleness of observations".
 *
 * @author centaur@google.com (Anthony Francis)
 */
public class PerceptionManager implements SensorEventListener {
  /** Tag that indicates debug messages are coming from this class. */
  private static final String DEBUG_TAG = "PerceptionManager";


  // -------------------------------------------------------------------
  // Classes and constants to be used in triggering
  // -------------------------------------------------------------------
  /** Interface for actions that this perceiver can execute in callbacks. */
  public abstract interface Callback {
    /** Perform an arbitrary action - go nuts. */
    public abstract void execute();
  }

  /** Classes of states the perceptual system can detect. */
  public enum State {
    HANDLED, SHAKING, HELD, SIDEWAYS, UPSIDE_DOWN
  }

  /** Classes of transitions the perceptual system can detect */
  public enum Event {
    ANY_EVENT, NO_EVENT, // TOFU and NIL :-)s
    SENSOR_UPDATE, // Generic sensor updates
    HANDLED_START,
    HANDLED_STOP, // Transition to/from being handled
    SHAKING_START,
    SHAKING_STOP, // Transition to/from shaking
    PICKED_UP,
    PUT_DOWN, // Transition to being held
    SIDEWAYS_START,
    SIDEWAYS_STOP, // Transition to/from sideways
    UPSIDEDOWN_START,
    UPSIDEDOWN_STOP, // Transition to/from upside down
  }

  // -------------------------------------------------------------------
  // Perceptual constants
  // -------------------------------------------------------------------
  // Timing interval constants
  /** Rough temporal scale of sensor measurements in nanoseconds. */ 
  public static final double STANDARD_INTERVAL = 19834618.01; // sampled

  /** How much to weight new input values being mixed with old values. */
  public static final double NEW_INPUT_WEIGHT = .1; // good empirical value

  /** One second in nanoseconds, used for convenience. */
  public static final long SECOND = 1000000000; // in units of System.nanoTime

  /** One microsecond in nanoseconds, used for convenience. */
  public static final long MICROSECOND = 1000000; // again System.nanoTime

  /** The desired update frequency - 100 times a second. */
  private static final long UPDATE_FREQUENCY = 100 * MICROSECOND;

  /**
   * The sensor delay, in microseconds.
   * The API will also allow constant values from the SensorManager, e.g.:
   * SensorManager.SENSOR_DELAY_UI for example.
   */
  private static final int SENSOR_DELAY = 100; // scaled to microseconds

  /**
   * Value of the raw handled signal that counts as "handled" - scaled to 1.0.
   */
  private static final double HANDLE_THRESHOLD = 1.0; // adjusted value
  /** How long a "handle" event should be considered to last. */
  private static final double HANDLE_DURATION = 3.0 * SECOND; // 3 seconds

  /**
   * Value of the raw shaking signal that counts as "shaking".
   * Shake threshold is a bit empirical ... 7.0 too many !+, 10.0 few !+'s.
   */
  private static final double SHAKE_THRESHOLD = 7.0; // adjusted value
  /** How long a "shake" event should be considered to last. */
  private static final double SHAKE_DURATION = 3.0 * SECOND; // 3 seconds

  /**
   * Value of the raw sideways signal that counts as "sideways" - 45 degrees.
   */
  private static final double SIDEWAYS_THRESHOLD = 45.0; // count at 45deg
  /** How long a "sideways" event should be considered to last. */
  private static final double SIDEWAYS_DURATION = 3.0 * SECOND; // 3 seconds

  /**
   * Value of the raw upside down signal that counts - 180 degrees.
   */
  private static final double UPSIDEDOWN_THRESHOLD = 180.0; // count at 180deg
  /** How long an "upside down" event should be considered to last. */
  private static final double UPSIDEDOWN_DURATION = 3.0 * SECOND; // 3 seconds

  /**
   * Value of the raw held signal that counts as "held" - scaled to 1.0.
   */
  private static final double HELD_THRESHOLD = 1.0;  // summed value
  /** How long an "held" event should be considered to last. */
  private static final double HELD_DURATION = 3.0 * SECOND; // 3 seconds

  /** Stickiness of old values. */
  private static final double VALUE_INERTIA = 0.9;  // stickiness of old values
  /** Amount of time that a response is considered to last. */
  private static final double RESPONSE_TIMEOUT = 5.0 * SECOND; // 5 seconds

  /** Damping (of velocity) for motion sensor. */
  public static final float MOTION_DAMPING = 0.95f;
  /** Flattening (of distance) for motion sensor. */
  public static final float POSITION_FLATTENING = 0.99f;

  /** Damping (of velocity) for elevator sensor. */
  private static final float VERTICAL_DAMPING = 0.90f;
  /** Flattening (of distance) for elevator sensor. */
  private static final float VERTICAL_FLATTENING = 0.80f;

  // -------------------------------------------------------------------
  // Parameterization
  // -------------------------------------------------------------------
  /** How frequently sensors are polled. */
  private int sensorDelay;
  /** How long it takes for a response to timeout. */
  private double responseTimeout;
  /** Weight to assign to new input values when being combined. */
  private double inputWeight;
  /** Weight that old values have when being combined. */
  private double valueInertia;

  // Logging and configuration
  /** Whether logging is turned on. */
  private boolean logging;
  /** Whether the gyroscope is enabled - it can oversample on NexusSen. */
  private boolean gyroscopeEnabled;
  /** Whether sensing is active. */
  private boolean sensing;

  // -------------------------------------------------------------------
  // Hardware sensor handles
  // -------------------------------------------------------------------
  /** Handles for the hardware sensors */
  private SensorWrapperFactory sensorFactory;

  // Encapsulated sensors
  /** Absolute Acceleration sensor. */
  private VectorSensor absAcc;
  /** Gravitational Acceleration sensor. */
  private VectorSensor graAcc;
  /** Linear Acceleration sensor. */
  private AccelSensor linAcc;
  /** Gyroscopic Acceleration sensor. */
  private VectorSensor gyrAcc;
  /** Orientation sensor. */
  private OrientationSensor orient;
  /** Proximity sensor. */
  private ProximitySensor proxi;
  /** "Elevator" sensor. */
  private ElevatorSensor motion;

  /** List of sensors which have been sensed on this cycle. */
  private HashSet<Integer> sensed;

  // -------------------------------------------------------------------
  // Derived state computed from the sensors
  // -------------------------------------------------------------------
  /** Whether the perception system has been initialized. */
  private boolean isPerceptionInitialized;
  /** Absolute time in nanoseconds of the last update. */
  private long lastUpdate;
  /** Length of time since the last update. */ 
  private long updateInterval;

  /** Time moving sum of absolute acceleration magnitude minus gravity. */
  private double sumAbsAccMagMinusG;
  /** Instantaneous absolute acceleration magnitude minus gravity. */
  private double absAccMagMinusG;

  // -------------------------------------------------------------------
  // High level sensory variables
  // -------------------------------------------------------------------
  /** "Shaking" percept - a hard shake. */
  private Percept shakingPercept;
  /** "Handled" percept - is someone messing with it. */
  private Percept handledPercept;
  /** Percept for on its side. */
  private Percept sidewaysPercept;
  /** Percept for upside down. */
  private Percept upsidedownPercept;
  /** Percept for picked up off the table. */
  private Percept pickedupPercept;

  // -------------------------------------------------------------------
  // Responses to actions
  // -------------------------------------------------------------------
  /**
   * A class which encapsulates a callback and a text label for retrieval.
   */
  private class LabeledCallback {
    String label;
    Callback callback;
    LabeledCallback(String label, Callback callback) {
      this.label = label;
      this.callback = callback;
    }
  }
  /** Map events to a list of labeled callbacks. */
  private HashMap<Event, List<LabeledCallback>> callbacks;


  // -------------------------------------------------------------------
  // MovementPerceiver Initialization
  // -------------------------------------------------------------------
  /**
   * Create the PerceptionManager, with ref to hardware sensors.
   * @param sensorManager
   */
  public PerceptionManager(SensorManager sensorManager) {
    sensorFactory = new SensorWrapperFactory(sensorManager);
    acquireSensors();
    configureResponseSystem();
    setDefaultParameters();
  }

  /** Acquire all the sensor handles we'll be using. */
  private void acquireSensors() {
    absAcc = sensorFactory.getAccelerometerSensor();
    graAcc = sensorFactory.getGravitySensor();
    linAcc = sensorFactory.getAccelSensor(MOTION_DAMPING, POSITION_FLATTENING);
    gyrAcc = sensorFactory.getGyroscopeSensor();
    orient = sensorFactory.getOrientationSensor();
    proxi = sensorFactory.getProximitySensor();
    motion = sensorFactory.getElevatorSensor(
        linAcc, graAcc, VERTICAL_DAMPING, VERTICAL_FLATTENING);
    sensed = new HashSet<Integer>();
  }

  /** Set up the lists used to store responses. */
  private void configureResponseSystem() {
    callbacks = new HashMap<Event, List<LabeledCallback>>();
  }

  /** Set params to defaults. Clients may reset these before sensing begins. */
  private void setDefaultParameters() {
    sensorDelay = SENSOR_DELAY;
    inputWeight = NEW_INPUT_WEIGHT;
    handledPercept = new Percept("Handled",
        HANDLE_THRESHOLD, HANDLE_DURATION, RESPONSE_TIMEOUT);
    shakingPercept = new Percept("Shaking",
        SHAKE_THRESHOLD, SHAKE_DURATION, RESPONSE_TIMEOUT);
    sidewaysPercept = new Percept("Sideways",
        SIDEWAYS_THRESHOLD, SIDEWAYS_DURATION, RESPONSE_TIMEOUT);
    upsidedownPercept = new Percept("Upsidedown",
        UPSIDEDOWN_THRESHOLD, UPSIDEDOWN_DURATION, RESPONSE_TIMEOUT);
    pickedupPercept = new Percept("Held",
        HELD_THRESHOLD, HELD_DURATION, RESPONSE_TIMEOUT);
    responseTimeout = RESPONSE_TIMEOUT;
    valueInertia = VALUE_INERTIA;
    updateInterval = SECOND / UPDATE_FREQUENCY;
    isPerceptionInitialized = false;
  }

  /** Start sensing, resetting some values if need be. */
  public void startSensing() {
    // Register listeners on events. We set the accelerometer rate to
    // slower than normal as a low pass filter, also reducing power and CPU.
    registerListener(absAcc, sensorDelay);
    registerListener(graAcc, sensorDelay);
    registerListener(linAcc, sensorDelay);
    if (isGyroscopeEnabled()) {
      registerListener(gyrAcc, sensorDelay);
    }
    registerListener(orient, sensorDelay);
    registerListener(proxi, sensorDelay);

    // Set up the perceptual state ... we want to start this just once so
    // that end-user apps don't completely reset state when they start/stop
    // the sensing process.
    if (!isPerceptionInitialized) {
      initializePerceptualState();
    }
    sensing = true;
  }
  
  /**
   * Starts listening on the given sensor.
   * Fails safely if the sensor does not exist.
   * @param wrapper
   * @param sensorDelay
   */
  public void registerListener(SensorWrapper wrapper, int sensorDelay) {
    if (wrapper != null && wrapper.sensor != null) {
      sensorFactory.getSensorManager().registerListener(
          this, wrapper.sensor, sensorDelay);
    }
  }

  /** Clears out the perceptual state. */
  public void initializePerceptualState() {
    isPerceptionInitialized = true;
    lastUpdate = System.nanoTime(); // Initial timestamp for reference.
    sumAbsAccMagMinusG = 0; // Start off with no acceleration.
    shakingPercept.transition(false, lastUpdate);
    handledPercept.transition(false, lastUpdate);
    sidewaysPercept.transition(false, lastUpdate);
    upsidedownPercept.transition(false, lastUpdate);
  }

  /** Stop sensing and remove listeners. */
  public void stopSensing() {
    sensorFactory.getSensorManager().unregisterListener(this);
    sensing = false;
  }

  /**
   * Add a response to the triggered actions. 
   * @param name Name of the action (by which it can be deleted).
   * @param event Event to trigger on.
   * @param response Callback to fire.
   */
  public void addCallback(String name, Event event, Callback response) {
    if (!callbacks.containsKey(Event.SENSOR_UPDATE)) {
      callbacks.put(event, new ArrayList<LabeledCallback>());
    }
    callbacks.get(event).add(new LabeledCallback(name, response));
  }

  /**
   * Remove a specific callback from wherever it is found based on identity.
   * @param callback to be removed
   */
  public void removeCallback(Callback callback) {
    for (Event event : callbacks.keySet()) {
      for (Iterator<LabeledCallback> it = callbacks.get(event).iterator();
      it.hasNext();) {
        if (callback.equals(it.next().callback)) {
          it.remove();
        }
      }
    }
  }

  /**
   * Remove a specific callback from wherever it is found based on name.
   * @param name of trigger to be removed.
   */
  public void removeCallback(String name) {
    for (Event event : callbacks.keySet()) {
      for (Iterator<LabeledCallback> it = callbacks.get(event).iterator();
      it.hasNext();) {
        if (name.equals(it.next().label)) {
          it.remove();
        }
      }
    }
  }

  /**
   * Remove all the callbacks for a given event type.
   * @param event type to clear triggers from.
   */
  public void removeCallback(Event event) {
    if (callbacks.containsKey(event)) {
      callbacks.get(event).clear();
    }
  }

  /** Execute the response actions for a specific event. */
  public void executeResponses(Event event) {
    if (callbacks.containsKey(event)) {
      for (LabeledCallback response : callbacks.get(event)) {
        response.callback.execute();
      }
    }
  }

  /**
   * Update our perception of our acceleration.
   *
   * @param event Assumes an acceleration event, for now.
   */
  void updatePerceptionOfMovement(SensorEvent event) {
    logPerceptionValues();
    updatePerceptionOfShaking(event.timestamp);
    updatePerceptionOfHandle(event.timestamp);
    updatePerceptionOfSideways(event.timestamp);
    updatePerceptionOfUpsidedown(event.timestamp);
    updatePerceptionOfHeld(event.timestamp);
    executeResponses(Event.SENSOR_UPDATE);
  }

  /** Get the weight for a sensor based on how often it is updated. */
  private double getPerceptualWeight(SensorWrapper sensor) {
    double scaledInterval = sensor.elapsedTime / STANDARD_INTERVAL;
    return Math.min(1.0, Math.max(0.0, inputWeight * scaledInterval));
  }

  /**
   * A time-independent integration of absolute acceleration into velocity.
   * Other summed variables are built up here too.
   *
   * @param event
   */
  private void updateAbsoluteAcceleration(SensorEvent event) {
    absAcc.update(event);
    absAccMagMinusG = Math.abs(absAcc.data.mag - SensorManager.GRAVITY_EARTH);
    sumAbsAccMagMinusG =
      (1.0 - getPerceptualWeight(absAcc))
      * sumAbsAccMagMinusG + getPerceptualWeight(absAcc)
      * absAccMagMinusG;
  }

  /**
   * Gravitational acceleration.
   * @param event
   */
  private void updateGravitationalAcceleration(SensorEvent event) {
    graAcc.update(event);
  }

  /**
   * A time-independent integration of linear acceleration into velocity. Other
   * summed variables are built up here too.
   * @param event
   */
  private void updateLinearAcceleration(SensorEvent event) {
    linAcc.update(event);
    motion.update();
  }

  /**
   * A time-independent integration of gyroscopic acceleration into velocity.
   * @param event
   */
  private void updateGyroscopicAcceleration(SensorEvent event) {
    gyrAcc.update(event);
  }

  /**
   * Overloading the use of the vector sensor to handle orientation.
   * @param event
   */
  private void updateOrientation(SensorEvent event) {
    orient.update(event);
  }

  /**
   * Overloading the use of the vector sensor to handle orientation.
   * @param event
   */
  private void updateProximity(SensorEvent event) {
    proxi.update(event);
  }

  /**
   * Compute handling based on time passed and instantaneous acceleration.
   * @param timestamp
   */
  private void updatePerceptionOfHandle(long timestamp) {
    double rawValue =
      linAcc.smoothZeroCrossings * 5.0
      + linAcc.smoothMag
      + gyrAcc.smoothMag
      + (1.0f - proxi.data.x);
    updatePercept(handledPercept, rawValue, timestamp, Event.HANDLED_START);
  }

  /**
   * Compute shaking based on time passed and instantaneous acceleration.
   * @param timestamp
   */
  private void updatePerceptionOfShaking(long timestamp) {
    double rawValue = (linAcc.smoothMag + gyrAcc.smoothMag) / 2.0;
    updatePercept(shakingPercept, rawValue, timestamp, Event.SHAKING_START);
  }

  /**
   * Compute sideways based on orientation of Z axis.
   * TODO(centaur): Handle tablets and other devices with different axes.
   * @param timestamp
   */
  private void updatePerceptionOfSideways(long timestamp) {
    double rawValue = accumulate(sidewaysPercept, Math.abs(orient.data.y));
    updatePercept(sidewaysPercept, rawValue, timestamp, Event.SIDEWAYS_START);
  }

  /**
   * Compute upsidedown based on orientation of X axis.
   * TODO(centaur): Handle tablets and other devices with different axes.
   * @param timestamp
   */
  private void updatePerceptionOfUpsidedown(long timestamp) {
    double rawValue = accumulate(upsidedownPercept,
        orient.data.x * 0.5 - Math.max(absAcc.data.z, 0));
    updatePercept(
        upsidedownPercept, rawValue, timestamp, Event.UPSIDEDOWN_START);
  }

  /**
   * Compute held based on handled plus vertical motion.
   * TODO(centaur): Handle tablets and other devices with different axes.
   * @param timestamp
   */
  private void updatePerceptionOfHeld(long timestamp) {
    double rawValue = accumulate(pickedupPercept,
        linAcc.smoothMag * 0.1
        + Math.max(motion.upperBound * 30.0 - 0.05f, 0.0f));
    updatePercept(pickedupPercept, rawValue, timestamp, Event.PICKED_UP);
  }

  /** Utility function to update a percept and fire the events. */
  private void updatePercept(
      Percept percept, double value, long timestamp, Event event) {
    if (percept.update(value, timestamp)) {
      if (percept.isNewOnset()) {
        log(percept.getName() + " onset detected - executing responses.");
      } else {
        log(percept.getName() + " going on a bit - executing responses.");
      }
      executeResponses(event);
    }
  }

  /** Utility function to make it easy to have a slow-onset percept. */
  private double accumulate(Percept percept, double value) {
    return (percept.getValue() * valueInertia
        + value * (1.0 - valueInertia));
  }

  /**
   * Log a message only if logging is turned on.
   * @param message the message to log
   */
  private void log(String message) {
    if (logging) {
      Log.v(DEBUG_TAG, message);
    }
  }

  /** Log function (extracted just not to clutter the logic). */
  private void logPerceptionValues() {
    log("\tELAPSED\t" + absAcc.elapsedTime
        + logFormat(absAcc, "ABSAC")
        + logFormat(linAcc, "LINAC")
        + logFormat(gyrAcc, "GYRAC")
        + logFormat(orient, "ORIEN")
        + logFormat(proxi,  "PROXI")
        + logFormat(motion, "VERT")
        + logFormat(handledPercept, "RAW_HND", "HANDLED", "NOHANDL")
        + logFormat(shakingPercept, "RAW_SHK", "SHAKING", "NOSHAKE")
        + logFormat(sidewaysPercept, "RAW_SID", "SIDEWAY", "NOSIDES")
        + logFormat(upsidedownPercept, "RAW_UPS", "UPSIDED", "NOUPSID")
        + logFormat(pickedupPercept, "RAW_PCK", "PICKUP", "RESTING"));
  }

  /**
   * Format a percept for tab-separated log output.
   * @param percept encapsulated object
   * @param tag for the raw value
   * @param on for when the percept is detected
   * @param off for when it is not
   * @return tab separated log value
   */
  public static String logFormat(
      Percept percept, String tag, String on, String off) {
    return (logFormat(tag, percept.getValue())
        + "\t"
        + (percept.isDetected() ? on : off));
  }

  /**
   * Format double value for log output.
   * @param label
   * @param value
   * @return tab-separated log value
   */
  public static String logFormat(String label, double value) {
    return String.format("\t%s\t%3.2f", label, value);
  }

  /**
   * Format the double with vector element, tag and double value.
   * @param letter
   * @param label
   * @param value
   * @return tab-separated log value
   */
  public static String logFormat(String letter, String label, double value) {
    return String.format("\t%s_%s\t%3.2f", letter, label, value);
  }

  /**
   * Format a VectorSensor named by the 5-letter tag for log output.
   * @param sensor
   * @param tag
   * @return tab-separated log value
   */
  public String logFormat(VectorSensor sensor, String tag) {
    return (logFormat("X%s", tag, sensor.data.x)
        + logFormat("Y", tag, sensor.data.y)
        + logFormat("Z", tag, sensor.data.z)
        + logFormat("M", tag, sensor.data.mag)
        + logFormat("0", tag, sensor.zeroCrossings));
  }

  /**
   * Format a VectorSensor named by the 5-letter tag for log output.
   * @param sensor
   * @param tag
   * @return tab-separated log value
   */
  public String logFormat(ElevatorSensor sensor, String tag) {
    return (logFormat("RZ%s", tag, sensor.data.z)
        + logFormat("UB", tag, sensor.upperBound)
        + logFormat("UM", tag, sensor.upperMotion)
        + logFormat("LB", tag, sensor.lowerBound)
        + logFormat("LV", tag, sensor.lowerMotion));
  }

  // private String logInt(String label, int value) {
  // return String.format("\t%s\t%d", label, value);
  // }

  /** Detect changes to the logged sensors. */
  @Override
  public void onSensorChanged(SensorEvent event) {
    handleSensors(event);
    updatePerception(event);
  }

  /**
   * Capture a sample from a sensor if not logged during this time frame.
   * @param event
   */
  private void handleSensors(SensorEvent event) {
    // We have to make sure we get one of each type of sensor's samples
    // during each update period or fast-responding sensors like the 
    // gyroscope will starve them out.
    if (!sensed.contains(event.sensor.getType())) {
      sensed.add(event.sensor.getType());
      // Switch on the event type to handle it correctly
      switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
          updateAbsoluteAcceleration(event);
          break;

        case Sensor.TYPE_GRAVITY:
          updateGravitationalAcceleration(event);
          break;

        case Sensor.TYPE_LINEAR_ACCELERATION:
          updateLinearAcceleration(event);
          break;

        case Sensor.TYPE_GYROSCOPE:
          updateGyroscopicAcceleration(event);
          break;

        case Sensor.TYPE_ORIENTATION:
          updateOrientation(event);
          break;

        case Sensor.TYPE_PROXIMITY:
          updateProximity(event);
          break;

        default:
          // pass; in the test harness we handle other sensor types.
      }
    }
  }

  /**
   * Call the perceptual updater if and only if enough time has passed.
   * @param event
   */
  private void updatePerception(SensorEvent event) {
    if ((event.timestamp - lastUpdate) > updateInterval) {
      lastUpdate = event.timestamp;
      sensed.clear();
      updatePerceptionOfMovement(event);
    }
  }

  /** This is basically deprecated but we still need to implement it. */
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // TODO(centaur): this claimed to be deprecated, but in practice is not.
    // Investigate whether we need to perform any tasks here.
  }



  // -------------------------------------------------------------------
  // Samples of accessors we might provide to external programs.
  // -------------------------------------------------------------------
  /**
   * Encapsulates absolute acceleration.
   * @return the current summed absolute acceleration model
   */
  public VectorSensor getAbsAcc() {
    return absAcc;
  }

  /**
   * Encapsulates linear acceleration.
   * @return the current summed linear acceleration model
   */
  public AccelSensor getLinAcc() {
    return linAcc;
  }

  /**
   * Encapsulates the gyroscope.
   * @return the current summed gyroscopic acceleration model
   */
  public VectorSensor getGyro() {
    return gyrAcc;
  }

  /**
   * Encapsulates the orientation sensor.
   * @return the current orientation model
   */
  public VectorSensor getOrient() {
    return orient;
  }

  /**
   * Encapsulates the proximity sensor.
   * @return the current proximity sensor.
   */
  public ProximitySensor getProxi() {
    return proxi;
  }


  /**
   * A sensor that detects vertical motion.
   * @return the current motion sensor.
   */
  public ElevatorSensor getMotion() {
    return motion;
  }

  /**
   * Percept that represents a hard shake to the phone.
   * @return the current object that represents our perception of shaking.
   */
  public Percept getShaking() {
    return shakingPercept;
  }

  /**
   * Percept that represents someone handling the phone.
   * @return the current object that represents our perception of handled.
   */
  public Percept getHandled() {
    return handledPercept;
  }

  /**
   * Percept that represents the phone on its side.
   * @return the current object that represents our perception of sideways.
   */
  public Percept getSideways() {
    return sidewaysPercept;
  }

  /**
   * Percept that represents phone upside down.
   * @return the current object that represents our perception of upside down.
   */
  public Percept getUpsidedown() {
    return upsidedownPercept;
  }

  /**
   * Percept that represents lifted off of a surface.
   * @return the current object that represents our perception of picked up.
   */
  public Percept getPickedup() {
    return pickedupPercept;
  }

  /**
   * Absolute difference of acceleration from gravity.
   * @return the absAccelMinusG
   */
  public double getAbsAccelMinusG() {
    return absAccMagMinusG;
  }

  /**
   * Absolute difference of acceleration from gravity.
   * @return the sumAbsAccelMinusG
   */
  public double getSumAbsAccMag() {
    return sumAbsAccMagMinusG;
  }

  // -------------------------------------------------------------------
  // Samples of parameters an external program might configure
  // -------------------------------------------------------------------
  /**
   * Sets the sensor delay and update interval to the specified hertz. 
   * @param hertz the delay to use
   */
  public void setSensorRate(int hertz) {
    setSensorDelay(1000 / hertz);
    setUpdateInterval(SECOND / hertz);
  }
  
  /**
   * Delay used to poll the sensors. Should be one of
   * SensorManager.SENSOR_DELAY_{FASTEST|GAME|UI|NORMAL}
   * or a delay in microseconds.
   * @param sensorDelay the delay to use
   */
  public void setSensorDelay(int sensorDelay) {
    // TODO(centaur): should set the STANDARD_INTERVAL based on this.
    this.sensorDelay = sensorDelay;
  }
  
  /**
   * Should be one of SensorManager.SENSOR_DELAY_{FASTEST|GAME|UI|NORMAL}
   * or a delay in microseconds.
   * @return the current sensor delay
   */
  public int getSensorDelay() {
    return sensorDelay;
  }

  /**
   * Time interval used to update the output perception, in nanoseconds.
   * @param updateInterval the delay to use
   */
  public void setUpdateInterval(long updateInterval) {
    this.updateInterval = updateInterval;
  }

  /**
   * Time interval used to update the output perception, in nanoseconds.
   * @return the current sensor delay
   */
  public long getUpdateInterval() {
    return updateInterval;
  }

  /**
   * Sets the weight to give new sample inputs (compared to the old average).
   * The lower this value, the longer it takes for a sensed event to fire.
   * @param inputWeight new weight value
   */
  public void setInputWeight(double inputWeight) {
    this.inputWeight = inputWeight;
  }

  /**
   * The weight of the sample inputs.
   * @return current inputWeight
   */
  public double getInputWeight() {
    return inputWeight;
  }

  /**
   * Sets timeout for a second response to an ongoing input in nanoseconds.
   * @param responseTimeout the timeout to set
   */
  public void setResponseTimeout(double responseTimeout) {
    this.responseTimeout = responseTimeout;
  }

  /**
   * What our current response timeout is.
   * @return current response timeout
   */
  public double getResponseTimeout() {
    return responseTimeout;
  }

  /**
   * Turn logging to adb logcat on or off.
   * @param logging desired logging state.
   */
  public void setLogging(boolean logging) {
    this.logging = logging;
  }

  /**
   * Returns whether debug messages are being logged to adb logcat.
   * @return the current logging status
   */
  public boolean isLogging() {
    return logging;
  }

  /**
   * Try to set the gyroscope enabled field - only allowed when not sensing.
   * The actual activation of the gyroscope occurs in the startSensing method
   * so if we're already sensing the variable is not set and the function
   * returns true if and only if the desired state is already the actual state.
   * @param gyroscopeEnabled the mGyroscopeActive to set
   * @return whether the value attempted to be set actually stuck
   */
  public boolean trySetGyroscopeEnabled(boolean gyroscopeEnabled) {
    if (!isSensing()) {
      this.gyroscopeEnabled = gyroscopeEnabled;
    }
    return this.gyroscopeEnabled == gyroscopeEnabled;
  }

  /**
   * Is the gyroscope enabled?
   * @return the gyroscopeActive
   */
  public boolean isGyroscopeEnabled() {
    return gyroscopeEnabled;
  }

  /**
   * Is the sensory system activated?
   * @return the sensing value
   */
  public boolean isSensing() {
    return sensing;
  }

  /**
   * How much accumulated values take from their previous value.
   * @param valueInertia the mValueInertia to set
   */
  public void setValueInertia(double valueInertia) {
    this.valueInertia = valueInertia;
  }

  /**
   * How much accumulated values take from their previous value.
   * @return the value inertia
   */
  public double getValueInertia() {
    return valueInertia;
  }
}
