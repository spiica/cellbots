// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed;

import com.google.robots.senses.testbed.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.cellbots.perception.PerceptionManager;
import com.cellbots.perception.PerceptionManager.Event;
import com.cellbots.perception.math.Functions;
import com.cellbots.perception.sensors.ElevatorSensor;
import com.cellbots.perception.sensors.PositionSensor;
import com.cellbots.perception.sensors.ProximitySensor;
import com.cellbots.perception.sensors.VectorSensor;
import com.cellbots.perception.ui.SeismographView;
import com.cellbots.perception.ui.SeismographWrapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * A basic test app that encapsulates the Google PerceptionManager class
 * and fires off basic intents in response to detected conditions.
 * A series of "seismographs" displays the recent history of sensors.
 * 
 * @author centaur@google.com (Anthony Francis)
 *
 */
public class PerceptionTestbedActivity extends Activity 
implements OnInitListener, OnUtteranceCompletedListener {
  /** Tag that indicates debug messages are coming from this class. */
  private static final String DEBUG_TAG = "PerceptionTestbedApp";

  //-------------------------------------------------------------------
  // Color scheme for the display
  //-------------------------------------------------------------------
  // TODO(centaur): switch to use of constants file
  /** Blue value from the UX defined palette set. */
  public static final int SEISMO_BLUE = Color.parseColor("#133CAC");
  /** Light blue value from the UX defined palette set. */
  public static final int SEISMO_LTBLUE = Color.parseColor("#599CFF");
  /** Dark blue value from the UX defined palette set. */
  public static final int SEISMO_DTBLUE = Color.parseColor("#80062270");
  /** Light gray value from the UX defined palette set. */
  public static final int SEISMO_LTGRAY = Color.rgb(128, 128, 128);
  /** Red value from the UX defined palette set. */
  public static final int SEISMO_RED = Color.parseColor("#FD0006");
  /** Yellow value from the UX defined palette set. */
  public static final int SEISMO_YELLOW = Color.parseColor("#FFFA40");



  //-------------------------------------------------------------------
  // Configuration Constants
  //-------------------------------------------------------------------
  // TODO(centaur): move into a constants file, these are too hardcoded
  /** Maximum acceleration to display */
  private static final float MAX_DISPLAY_ACC = 20.f;
  /** Maximum display along the axes of a vector measurement. */
  private static final float[] MAX_DISPLAY_VEC = new float[] { 
    MAX_DISPLAY_ACC,
    MAX_DISPLAY_ACC,
    MAX_DISPLAY_ACC,
    MAX_DISPLAY_ACC,
    MAX_DISPLAY_ACC,
  };  
  /** Maximum display along the axes of a orientation measurement. */
  private static final float[] MAX_DISPLAY_ORI = new float[] { 
    1000000000,  // squeeze these down to nothing
    1000000000,  // squeeze these down to nothing
    180,         // degrees
    90,         // degrees
    360,         // degrees
  };  
  /** Maximum display along the axes of movement in space. */
  private static final float[] MAX_DISPLAY_SPA = new float[] { 
    1000000000,  // squeeze these down to nothing
    1000000000,  // squeeze these down to nothing
    1.0f,        // 1m
    1.0f,        // 1m
    1.0f,        // 1m
  };    
  /** Maximum display along the axes of the elevator sensor. */
  private static final float[] MAX_DISPLAY_SP2 = new float[] { 
    1000000000,  // squeeze these down to nothing
    1000000000,  // squeeze these down to nothing
    0.1f,        // 1m
    0.1f,        // 1m
    0.1f,        // 1m
  };    
  /** Maximum display along the axes of the proximity sensor. */
  private static final float[] MAX_DISPLAY_PRO = new float[] { 
    1.0f,        // 0..1
    10.0f,       // 10cm
    10.0f,       // 10cm
  };    
  /** Rate choices for the rate selector, in hertz. */
  private static final int[] RATE_CHOICES = new int[] {
    1, 10, 15, 30, 60, 100
  };


  //-------------------------------------------------------------------
  // User Interface Handles
  //-------------------------------------------------------------------
  // Handles to Android management APIs
  /** The power management system needed for the wake lock. */
  private PowerManager powerManager;
  /** Lets us grab the screen so it doesn't shut off. */
  private WakeLock wakeLock;
  /** The sensor manager we're integrating. */
  private SensorManager sensorManager;
  /** The perception manager doing the integrating. */
  private PerceptionManager perceptionManager;

  // User Interface for Speech Responses
  /** Handle to the button that tests the speech system. */
  private Button testResponse;
  /** Whether to use the text to speech API. */
  private CheckBox vocalizeResponse;
  /** The text to speech API handle. */
  private TextToSpeech textToSpeech;
  /** Whether we're currently talking. */
  private boolean talking;
  /** Timestamp for the last utterance sent to tts. */
  private long lastUtteranceQueued;
  /** key used to verify the message in a callback. */
  private static final int SPEECH_DATA_CHECK_CODE = 999;
  /** key used to verify the message in a callback. */
  private static final String SPEECH_QUEUED_CODE = "PTAA speech queued";
  /** How many seconds to wait before forcing speech to restart. */
  private static final long SPEECH_RESET_DELAY = 5 * PerceptionManager.SECOND;


  // --- Seismograph Displays -----------------------------------------
  /** Seismograph for absolute acceleration including gravity. */
  private SeismographWrapper absAccDisplay;
  /** Seismograph for linear acceleration minus gravity. */
  private SeismographWrapper linAccDisplay;
  /** Seismograph for movement in space. */
  private SeismographWrapper movSpaDisplay;
  /** Seismograph for gyroscopic acceleration. */
  private SeismographWrapper gyroDisplay;
  /** Seismograph for orientation. */
  private SeismographWrapper orientDisplay;
  /** Seismograph for proximity sensor. */
  private SeismographWrapper proxiDisplay; 
  /** Seismograph for vertical motion (integrated). */
  private SeismographWrapper elevatorDisplay; 

  /** Seismograph for handled. */
  private SeismographWrapper handledDisplay;
  /** Seismograph for hard shaking. */
  private SeismographWrapper shakingDisplay;
  /** Seismograph for sideways turning. */
  private SeismographWrapper sidewaysDisplay;
  /** Seismograph for turned upside down. */
  private SeismographWrapper upsidednDisplay;
  /** Seismograph for lifted off the table. */
  private SeismographWrapper pickedupDisplay;


  //-------------------------------------------------------------------
  // Internal State
  //-------------------------------------------------------------------
  /** So we can do randomized text strings. */
  private Random randomGenerator;
  /** When the session started. */
  private long startOfSession;



  //-------------------------------------------------------------------
  // Overrides of extended / implemented classes
  //-------------------------------------------------------------------
  /** Acquire the screen lock and start sensing. */
  @Override
  protected void onResume() {
    super.onResume();
    wakeLock.acquire();                // keep screen on so user can gesture
    perceptionManager.startSensing();  // activate the sensor module
    log("Perception Manager: sensing activated");
  }

  /** Stop sensing and release the screen lock. */
  @Override
  protected void onPause() {
    super.onPause();
    log("Perception Manager: sensing deactivated");
    perceptionManager.stopSensing();  // release the sensor resources
    wakeLock.release();               // release the screen
  }

  /** Acquires the SensorManager, Display and WakeLock. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    log("Creating PerceptionTestbedApp");
    super.onCreate(savedInstanceState);
    acquireWakeLock();
    createPerceptionManager();
    acquireSpeechInterface();
    createUserInterface();
    log("Initialized PerceptionTestbedApp");
  }

  /** Make sure the screen doesn't shut down while the user is working. */
  private void acquireWakeLock() {
    powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
        getClass().getName());
  }

  /** Create the perception manager and its support variables. */
  private void createPerceptionManager() {
    // Sensor module configuration proper
    initializePerceptionManager();
    configurePerceptionManager();
    addTriggeredResponses();

    // Other housekeeping
    randomGenerator = new Random();
    startOfSession = System.nanoTime();
  }

  /** Create the PerceptionManager and wire it up to Android services. */
  private void initializePerceptionManager() {
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    perceptionManager = new PerceptionManager(sensorManager);
  }

  /** Set the PerceptionManager's sensitivity. */
  private void configurePerceptionManager() {
    // Update sensory configuration
    perceptionManager.setSensorRate(30);  // in hertz
    perceptionManager.setLogging(true);
    perceptionManager.trySetGyroscopeEnabled(false);

    // Shaking sensor - reconfigure this to make it sensitive, but long onset.
    perceptionManager.setInputWeight(0.05);  // takes a while to build up
    perceptionManager.getHandled().setThreshold(1.5);  // actual value is low
    perceptionManager.getHandled().setDuration(3.0 * PerceptionManager.SECOND);
    perceptionManager.getShaking().setThreshold(7.0);  // actual value is low
    perceptionManager.getShaking().setDuration(PerceptionManager.SECOND);
    perceptionManager.getSideways().setThreshold(50.0);  // count at 40deg
    perceptionManager.getSideways().setDuration(
        3 * PerceptionManager.SECOND);
    perceptionManager.getUpsidedown().setThreshold(10.0);  // >10deg upsidedn
    perceptionManager.getUpsidedown().setDuration(
        3 * PerceptionManager.SECOND);
    perceptionManager.getPickedup().setThreshold(1.0);  // summed value over 1
    perceptionManager.getPickedup().setDuration(
        3 * PerceptionManager.SECOND);

    // Applies to all triggered behaviors
    perceptionManager.setResponseTimeout(3 * PerceptionManager.SECOND);
  }

  /** Set up responses to perceptual events. */
  private void addTriggeredResponses() {
    addCallback(
        "DEFAULT", Event.SHAKING_START, new PerceptionManager.Callback() {
          @Override
          public void execute() {
            shakingResponse();
          }
        });
    addCallbackMessage(
        "DEFAULT", Event.UPSIDEDOWN_START, "Don't turn me upside down.");
    addCallbackMessage(
        "DEFAULT", Event.SIDEWAYS_START, "Don't tilt me bro.");
    addCallbackMessage(
        "DEFAULT", Event.PICKED_UP, "Put me down!");
    addCallback(
        "DEFAULT", Event.SENSOR_UPDATE, new PerceptionManager.Callback() {
          @Override
          public void execute() {
            updateSeismographs();
          }
        });
  }

  /**
   * Add a callback that performs an action.
   * @param tag the tag that can be used to wipe this callback
   * @param event The event to trigger on
   * @param callback the action to execute.
   */
  private void addCallback(
      String tag, Event event, PerceptionManager.Callback callback) {
    perceptionManager.addCallback(tag, event, callback);
  }

  /**
   * Add a callback that sends a speech message.
   * @param tag the tag that can be used to wipe this callback
   * @param event The event to trigger on
   * @param message The speech string to say.
   */
  private void addCallbackMessage(
      String tag, Event event, final String message) {
    addCallback(tag, event, new PerceptionManager.Callback() {
      @Override
      public void execute() {
        say(message);
      }
    });
  }

  /** Starts creation of speech UI and creates callback to handle response. */
  private void acquireSpeechInterface() {
    talking = false;
    lastUtteranceQueued = System.nanoTime();
    Intent checkIntent = new Intent();
    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    startActivityForResult(checkIntent, SPEECH_DATA_CHECK_CODE);
  }

  /** Verifies that we can use the text to speech service. */
  @Override
  protected void onActivityResult(
      int requestCode,
      int resultCode,
      Intent data) {
    if (requestCode == SPEECH_DATA_CHECK_CODE) {
      if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
        // success, create the TTS instance
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
      } else {
        // missing data, install it
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        startActivity(installIntent);
      }
    }
  }

  /** Implementation for OnInitListener */
  @Override
  public void onInit(int status) {
    if (status == TextToSpeech.SUCCESS) {
      log("TextToSpeech.SUCCESS on setup");
    } else {
      log("TextToSpeech.FAILURE on setup");
    }
  }

  /** Implementation for OnUtteranceCompletedListener */
  @Override
  public void onUtteranceCompleted(String uttId) {
    if (SPEECH_QUEUED_CODE.equals(uttId)) {
      talking = false;
    }
  }

  /** Initializes the content view and creates/acquires interface elements. */
  private void createUserInterface() {
    setContentView(R.layout.main);
    createTestingInterface();
    acquireSeismoInterface();
    log("UI Configured");   
  }

  /** Creates the seismographs that we will be displaying sensor data in. */
  private void acquireSeismoInterface() {
    log("Getting Seismos");   
    getVectorSeismos();
    getOrientSeismos();   
    getProxiSeismos();
    getElevatorSeismos();
    getPerceptSeismos();
  }

  /** Creates the testing interface code to use speech interface. */
  private void createTestingInterface() {
    // Attach the test interface button
    testResponse = (Button) findViewById(R.id.test_response);
    log("mTestResponse" + testResponse);
    testResponse.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        testingResponse();
      }
    });

    // Attach the checkbox to turn on vocalization
    vocalizeResponse = (CheckBox) findViewById(R.id.vocalize_response);
    log("mVocalizeResponse" + vocalizeResponse);
  }

  /** Create seismograph with a label on the left. */
  private SeismographWrapper createLabeledSeismograph(
      String labelName, int seismoHeight, int parentLayout) {
    // Compute the height of the label and the seismo
    int height = getDips(seismoHeight);

    // Create the container for the label and the seismo
    LinearLayout container = new LinearLayout(this);
    container.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    container.setGravity(Gravity.CENTER);
    container.setPadding(3, 3, 3, 3);

    // Create the label
    TextView labelView = new TextView(this);
    labelView.setText(labelName);
    labelView.setLayoutParams(new LayoutParams(getDips(75), height));
    labelView.setGravity(Gravity.CENTER);
    labelView.setPadding(5, 5, 5, 5);
    container.addView(labelView);

    // Create the seismo
    SeismographView seismoView = new SeismographView(this);
    seismoView.setLayoutParams(
        new LayoutParams(LayoutParams.FILL_PARENT, height, 1));
    container.addView(seismoView);

    // Add the seismo to the view and return the handler that wraps it.
    ((LinearLayout) findViewById(parentLayout)).addView(container);
    return new SeismographWrapper(labelView, seismoView);
  }

  /**
   * Convert pixels to device independent pixels.
   * @param pixels
   * @return the dips we need to make what we want to display
   */
  private int getDips(int pixels) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
        pixels, getResources().getDisplayMetrics());
  }

  /** Get all the seismographs for normal linear acceleration. */
  private void getVectorSeismos() {
    absAccDisplay = getVectorAndMagSeismo("Absolute Accel.");
    linAccDisplay = getVectorAndMagSeismo("Linear Accel.");
    movSpaDisplay = getVectorSeismo("Movemt. in Space");
    gyroDisplay = getVectorAndMagSeismo("Gyro. Accel.");
  }

  /** Set up seismograph for generic vector seismo. */
  private SeismographWrapper getVectorAndMagSeismo(String label) {
    SeismographWrapper seismo =
      createLabeledSeismograph(label, 60, R.id.sensor_layout);
    seismo.configureVectorAndMagSeismo();
    return seismo;
  }

  /** Set up seismograph and display label for a generic vector seismograph. */
  private SeismographWrapper getVectorSeismo(String label) {
    SeismographWrapper seismo =
      createLabeledSeismograph(label, 60, R.id.sensor_layout);
    seismo.configureVectorSeismo();
    return seismo;
  }

  /** Set up seismograph and display label for orientation. */
  private void getOrientSeismos() {
    orientDisplay = getOrientSeismo("Orient. in Space");
  }

  /** Set up seismograph and display label for orientation. */
  private SeismographWrapper getOrientSeismo(String label) {
    SeismographWrapper handler =
      createLabeledSeismograph(label, 60, R.id.sensor_layout);
    handler.seismo.addSeries(SEISMO_LTGRAY, 0.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);
    handler.seismo.addSeries(SEISMO_DTBLUE, 0.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);

    // Add X,Y,Z components in RGB
    handler.seismo.addSeries(SEISMO_RED);
    handler.seismo.addSeries(SEISMO_LTBLUE);
    handler.seismo.addSeries(SEISMO_YELLOW, 0.0f, 1.0f);

    // Clear the seismograph
    handler.seismo.invalidate();
    return handler;
  }

  /** Acquire the proximity sensor and its custom time series. */
  private void getProxiSeismos() {
    proxiDisplay =
      createLabeledSeismograph("Proximity Sensor", 40, R.id.sensor_layout);
    proxiDisplay.seismo.addSeries(SEISMO_LTGRAY, 0.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);
    proxiDisplay.seismo.addSeries(SEISMO_RED, 0.0f, 1.0f);
    proxiDisplay.seismo.addSeries(SEISMO_LTBLUE, 0.0f, 1.0f);
    proxiDisplay.seismo.invalidate();
  }

  /** Acquire the "elevator" (vertical motion sensor). */
  private void getElevatorSeismos() {
    elevatorDisplay =
      createLabeledSeismograph("Vertical Motion", 40, R.id.sensor_layout);
    elevatorDisplay.seismo.addSeries(SEISMO_DTBLUE, -1.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);
    elevatorDisplay.seismo.addSeries(SEISMO_LTGRAY, -1.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);
    elevatorDisplay.seismo.addSeries(SEISMO_LTBLUE);
    elevatorDisplay.seismo.addSeries(SEISMO_RED);
    elevatorDisplay.seismo.addSeries(SEISMO_YELLOW);

    // Clear the seismograph
    elevatorDisplay.seismo.invalidate();
  }

  /** Get the seismos for percepts. */
  private void getPerceptSeismos() {
    handledDisplay = getPerceptSeismo("Handled", 40, SEISMO_BLUE);
    shakingDisplay = getPerceptSeismo("Shaking", 40, SEISMO_RED);
    sidewaysDisplay = getPerceptSeismo("Sideways", 40, SEISMO_BLUE);
    upsidednDisplay = getPerceptSeismo("Upsi_dn", 40, SEISMO_RED);
    pickedupDisplay = getPerceptSeismo("Picked Up", 40, SEISMO_BLUE);
  }

  /** Get a percept seismo handler including its labels. */
  private SeismographWrapper getPerceptSeismo(
      String label, int seismo, int color) {
    SeismographWrapper handler = 
      createLabeledSeismograph(label, 40, R.id.perception_layout);
    // Yellow is the color for the "raw" value; all other values piped in.
    handler.addPerceptSeries(color, SEISMO_YELLOW);
    return handler; 
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.percept_view:
        setPerceptView();
        return true;
      case R.id.sensor_view:
        setSensorView();
        return true;
      case R.id.toggle_percepts:
        togglePercepts();
        return true;
      case R.id.toggle_sensors:
        toggleSensors();
        return true;
      case R.id.toggle_responses:
        toggleResponses();
        return true;
      case R.id.toggle_controls:
        toggleControls();
        return true;
      case R.id.toggle_gyroscope:
        toggleGyroscope();
        return true;
      case R.id.toggle_speech:
        toggleVocalizations();
        return true;
      case R.id.set_sample_rate:
        showSetSampleRateDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /** Show a dialog which enables user to set the sample rate. */
  private void showSetSampleRateDialog() {
    // Create the text for the current set of allowed rate choices
    CharSequence[] rateChoices = new CharSequence[RATE_CHOICES.length];
    for (int i = 0; i < RATE_CHOICES.length; i++) {
      rateChoices[i] = new String(RATE_CHOICES[i] + " hertz");
    }

    // Fire off the sensor rate picker
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Set Sample Rate");
    builder.setItems(rateChoices, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int item) {
        setSensorRate(RATE_CHOICES[item]);
      }
    });
    builder.create().show();
  }
  
  /**
   * Set the sensor sample rate to the specified value in hertz.
   * @param hertz Desired sensor rate in hertz.
   */
  private void setSensorRate(int hertz) {
    boolean sensing = perceptionManager.isSensing();
    if (sensing) {
      perceptionManager.stopSensing();
    }
    perceptionManager.setSensorRate(hertz);
    if (sensing) {
      perceptionManager.startSensing();
    }
  }
  
  /** Turn the percept display on or off. */
  private void setPerceptView() {
    setVisibility(R.id.perception_layout, true);
    setVisibility(R.id.response_layout, true);
    setVisibility(R.id.sensor_layout, true);
    setVisibility(R.id.control_layout, true);
  }

  /** Turn the sensor display on or off. */
  private void setSensorView() {
    setVisibility(R.id.perception_layout, false);
    setVisibility(R.id.response_layout, false);
    setVisibility(R.id.sensor_layout, true);
    setVisibility(R.id.control_layout, false);
  }

  /** Turn the percept display on or off. */
  private void togglePercepts() {
    toggleVisibility(R.id.perception_layout);
  }

  /**
   * Turn the response display on or off.
   */
  private void toggleResponses() {
    toggleVisibility(R.id.response_layout);
  }

  /**
   * Turn the sensor display on or off.
   */
  private void toggleSensors() {
    toggleVisibility(R.id.sensor_layout);
  }

  /**
   * Turn the control display on or off.
   */
  private void toggleControls() {
    toggleVisibility(R.id.control_layout);
  }

  /**
   * Turn the gyroscope on or off.
   */
  private void toggleGyroscope() {
    boolean sensing = perceptionManager.isSensing();
    if (sensing) {
      perceptionManager.stopSensing();
    }
    perceptionManager.trySetGyroscopeEnabled(
        !perceptionManager.isGyroscopeEnabled());
    if (sensing) {
      perceptionManager.startSensing();
    }
  }

  /**
   * Turn the speech on or off.
   */
  private void toggleVocalizations() {
    vocalizeResponse.setChecked(!vocalizeResponse.isChecked());
  }

  /**
   * Toggle the visibility of a view from visible to gone.
   * @param id
   */
  private void toggleVisibility(int id) {
    setVisibility(id, findViewById(id).getVisibility() == View.GONE);
  }

  /**
   * Set a view to either visible (true) or gone (false).
   * @param id
   */
  private void setVisibility(int id, boolean visible) {
    findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  //-------------------------------------------------------------------
  // Response Facilities
  //-------------------------------------------------------------------
  /**
   * Log message - fires to both adb logcat and the screen log message.
   * If the screen log message is not available yet, it skips it.
   * @param message
   */
  public void log(String message) {
    Log.v(DEBUG_TAG, message);
    displayMessage(R.id.log_message, message);
  }

  /**
   * Generic message logged to a visible text element if possible.
   * @param id
   * @param message
   */
  public void displayMessage(int id, String message) {
    TextView view = (TextView) findViewById(id);
    if (view != null) {
      view.setText(message);
    }
  }

  /**
   * Say something to the user.
   * @param text
   */
  protected void say(String text) {
    say(text, text);
  }

  /**
   * Say something to the user with a more informative status message.
   * @param status
   * @param text
   */
  protected void say(String status, String text) {
    displayMessage(R.id.response_message, status);
    if (vocalizeResponse.isChecked()) {
      if (textToSpeech == null) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
        builder.create();
      } else {
        if (!talking 
            || System.nanoTime() - lastUtteranceQueued > SPEECH_RESET_DELAY) {
          HashMap<String, String> speechParams = new HashMap<String, String>();
          speechParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
              SPEECH_QUEUED_CODE);
          textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, speechParams);
          lastUtteranceQueued = System.nanoTime();
          talking = true;
        }
      }
    }
  }

  /** Default response when shaking detected. */
  protected void shakingResponse() {
    long time = secondsSinceStartOfSession(); 
    log("Perception Manager: shaking detected at " + time + " seconds");
    String response = getRandomResponse(getResources().getStringArray(
        R.array.shaking_exclamations));
    say("SHAKING_START detected @ " + time + "s", response);
  }

  /** Update the view of the seismographs based on the data samples. */
  protected void updateSeismographs() {
    long timestamp = System.nanoTime(); // TODO(centaur): use event timestamp
    updateSensorSeismographs(timestamp);
    updateHandledSeismograph(timestamp);
    updateShakingSeismograph(timestamp);
    updateSidewaysSeismograph(timestamp);
    updateUpsidedownSeismograph(timestamp);
    updatePickedupSeismograph(timestamp);
  }

  /**
   * Updates the seismographs that sum up vector accelerations.
   * @param timestamp
   */
  private void updateSensorSeismographs(long timestamp) {
    updateVectorSeismograph(
        absAccDisplay.seismo,
        perceptionManager.getAbsAcc(),
        timestamp);
    updateVectorSeismograph(
        linAccDisplay.seismo,
        perceptionManager.getLinAcc(),
        timestamp);
    updateMovementSeismograph(
        movSpaDisplay.seismo,
        perceptionManager.getLinAcc(),
        timestamp,
        MAX_DISPLAY_SPA);
    updateVectorSeismograph(
        gyroDisplay.seismo,
        perceptionManager.getGyro(),
        timestamp);
    updateScaledSeismograph(
        orientDisplay.seismo,
        perceptionManager.getOrient(),
        timestamp,
        MAX_DISPLAY_ORI);
    updateProximitySeismograph(
        proxiDisplay.seismo,
        perceptionManager.getProxi(),
        timestamp,
        MAX_DISPLAY_PRO);
    updateElevatorSeismograph(
        elevatorDisplay.seismo,
        perceptionManager.getMotion(),
        timestamp,
        MAX_DISPLAY_SP2);
  }

  /**
   * Updates the seismographs that display handling.
   * @param timestamp
   */
  private void updateHandledSeismograph(long timestamp) {
    updateSeismograph(
        handledDisplay.seismo,
        new float[] {
            perceptionManager.getHandled().isDetected() ? 0.9f : 0.0f,
                2.0f * (float) perceptionManager.getHandled().getValue()
                / MAX_DISPLAY_ACC,
        },
        timestamp);
    handledDisplay.label.setBackgroundColor(
        perceptionManager.getHandled().isDetected()
        ? SEISMO_BLUE : Color.BLACK);
  }

  /**
   * Updates the seismographs that display shaking.
   * @param timestamp
   */
  private void updateShakingSeismograph(long timestamp) {
    updateSeismograph(
        shakingDisplay.seismo,
        new float[] {
            perceptionManager.getShaking().isDetected() ? 0.9f : 0.0f,
                2.0f * (float) perceptionManager.getShaking().getValue()
                / MAX_DISPLAY_ACC,
        },
        timestamp);
    shakingDisplay.label.setBackgroundColor(
        perceptionManager.getShaking().isDetected()
        ? SEISMO_RED : Color.BLACK);
  }


  /**
   * Updates the seismographs that display sideways.
   * @param timestamp
   */
  private void updateSidewaysSeismograph(long timestamp) {
    updateSeismograph(
        sidewaysDisplay.seismo,
        new float[] {
            perceptionManager.getSideways().isDetected() ? 0.9f : 0.0f,
                (float) perceptionManager.getSideways().getValue() / 90.0f,
        },
        timestamp);
    sidewaysDisplay.label.setBackgroundColor(
        perceptionManager.getSideways().isDetected() 
        ? SEISMO_BLUE : Color.BLACK);
  }

  /**
   * Updates the seismographs that display upside down.
   * @param timestamp
   */
  private void updateUpsidedownSeismograph(long timestamp) {
    updateSeismograph(
        upsidednDisplay.seismo,
        new float[] {
            perceptionManager.getUpsidedown().isDetected() ? 0.9f : 0.0f,
                (float) perceptionManager.getUpsidedown().getValue() / 360.0f,
        },
        timestamp);
    upsidednDisplay.label.setBackgroundColor(
        perceptionManager.getUpsidedown().isDetected() 
        ? SEISMO_RED : Color.BLACK);
  }

  /**
   * Updates the seismographs that display being lifted.
   * @param timestamp
   */
  private void updatePickedupSeismograph(long timestamp) {
    updateSeismograph(
        pickedupDisplay.seismo,
        new float[] {
            perceptionManager.getPickedup().isDetected() ? 0.9f : 0.0f,
                (float) perceptionManager.getPickedup().getValue() / 2.0f,
        },
        timestamp);
    pickedupDisplay.label.setBackgroundColor(
        perceptionManager.getPickedup().isDetected() 
        ? SEISMO_RED : Color.BLACK);
  }

  /**
   * Displays X,Y,Z and Magnitude,ZeroCrossings in a single(!) seismograph.
   * @param timestamp
   */
  private void updateVectorSeismograph(
      SeismographView seismo,
      VectorSensor sensor,
      long timestamp) {
    updateScaledSeismograph(seismo, sensor, timestamp, MAX_DISPLAY_VEC);
  }

  /**
   * Displays X,Y,Z and Magnitude,ZeroCrossings in a single(!) seismograph.
   * @param timestamp
   */
  private void updateScaledSeismograph(
      SeismographView seismo,
      VectorSensor sensor,
      long timestamp,
      float[] scale) {
    updateSeismograph(
        seismo,
        new float[] {
            (float) sensor.data.mag / scale[0],
            (float) sensor.zeroCrossings / scale[1],
            sensor.data.x / scale[2],
            sensor.data.y / scale[3],
            sensor.data.z / scale[4] },
            timestamp);
  }

  /**
   * Displays Z, upper/lower bound and upper/lower avg motion.
   * @param timestamp
   */
  private void updateElevatorSeismograph(
      SeismographView seismoXyz,
      ElevatorSensor sensor,
      long timestamp,
      float[] scale) {
    updateSeismograph(
        seismoXyz,
        new float[] {
            Functions.clamp(sensor.lowerBound / scale[2], -0.9f, 0.9f),
            Functions.clamp(sensor.upperBound / scale[2], -0.9f, 0.9f),
            Functions.clamp(sensor.lowerMotion / scale[2], -0.9f, 0.9f),
            Functions.clamp(sensor.upperMotion / scale[2], -0.9f, 0.9f),
            Functions.clamp(sensor.getPos().z / scale[2], -0.9f, 0.9f),
        },
        timestamp);
  }

  /**
   * Displays X,Y,Z and Magnitude,ZeroCrossings in a single(!) seismograph.
   * @param timestamp
   */
  private void updateMovementSeismograph(
      SeismographView seismoXyz,
      PositionSensor sensor,
      long timestamp,
      float[] scale) {
    updateSeismograph(
        seismoXyz,
        new float[] {
            Functions.squeeze(sensor.getPos().x / scale[2], -0.9f, 0.9f),
            Functions.squeeze(sensor.getPos().y / scale[3], -0.9f, 0.9f),
            Functions.squeeze(sensor.getPos().z / scale[4], -0.9f, 0.9f) },
            timestamp);
  }

  /**
   * Displays the proximity sensor data scaled appropriately.
   * @param timestamp
   */
  private void updateProximitySeismograph(
      SeismographView seismo,
      ProximitySensor sensor,
      long timestamp,
      float[] scale) {
    updateSeismograph(
        seismo,
        new float[] {
            sensor.data.x / scale[0],
            sensor.data.y / scale[1],
            sensor.data.z / scale[2],
        },
        timestamp);
  }

  /**
   * Utility method to SAFELY update a single seismograph view.
   * @param seismograph
   * @param values Must be from -1.0 to +1.0 for accurate readings.
   * @param timestamp
   */
  protected void updateSeismograph(
      SeismographView seismograph,
      float[] values,
      long timestamp) {
    if (seismograph != null) {
      seismograph.addSamples(values, timestamp);
      seismograph.invalidate();
    }
  }

  /**
   * Default response to the testing button.
   */
  private void testingResponse() {
    log("Perception Testbed: testing response at "
        + secondsSinceStartOfSession() + " seconds");
    say("testing response code");
  }

  /**
   * Time since start of session, in seconds (duh).
   * @return seconds
   */
  private long secondsSinceStartOfSession() {
    return ((System.nanoTime() - startOfSession) / PerceptionManager.SECOND);
  }

  /**
   * Utility method to get a random string.
   * @param responses the responses to choose from.
   * @return a random response.
   */
  protected String getRandomResponse(String[] responses) {
    return responses[randomGenerator.nextInt(responses.length)];
  }
}