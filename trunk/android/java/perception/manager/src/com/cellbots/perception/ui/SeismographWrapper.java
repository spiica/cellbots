// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.ui;

import android.app.Activity;
import android.graphics.Paint;
import android.widget.TextView;


/**
 * Encapsulates a label and seismograph together into a single object.
 * Provides utility methods to add appropriate sets of time series.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class SeismographWrapper {
  /** The label which gets displayed next to the seismograph. */
  public TextView label;

  /** Data values displayed as a seismograph. */
  public SeismographView seismo;

  /**
   * Encapsulate this label and seismo into a single object.
   * @param label the text view which is being encapsulated
   * @param seismo the seismograph being encapsulated
   */
  public SeismographWrapper(TextView label, SeismographView seismo) {
    this.label = label;
    this.seismo = seismo;
  }

  /**
   * Find and encapsulate the named label and seismo into a single object.
   * @param activity parent of the label and seismo
   * @param labelId resource id of the label
   * @param seismoId resource id of the seismo
   */
  public SeismographWrapper(Activity activity, int labelId, int seismoId) {
    label = (TextView) activity.findViewById(labelId);
    seismo = (SeismographView) activity.findViewById(seismoId);
  }

  /**
   * Create a seismo handler with a "percept" configuration.
   * @param activity parent of the label and seismo
   * @param labelId resource id of the label
   * @param seismoId resource id of the seismo
   * @param booleanColor color for the on/off value of the percept
   * @param continuousColor color for the continuous value of the percept
   */
  public SeismographWrapper(
      Activity activity,
      int labelId,
      int seismoId,
      int booleanColor,
      int continuousColor) {
    this(activity, labelId, seismoId);
    addPerceptSeries(booleanColor, continuousColor);
  }

  /**
   * Setup for a "percept" with a line raw and filled boolean value.
   * @param booleanColor color for the on/off value of the percept
   * @param continuousColor color for the continuous value of the percept
   */
  public void addPerceptSeries(int booleanColor, int continuousColor) {
    seismo.addSeries(booleanColor, 0.0f, 1.0f, Paint.Style.FILL_AND_STROKE);
    seismo.addSeries(continuousColor, 0.0f, 1.0f);
    seismo.invalidate();
  }

  /** Set up seismograph for generic vector seismo with magnitudes. */
  public void configureVectorAndMagSeismo() {
    // Add magnitude of acceleration (gray) and zero crossings (blue trans).
    seismo.addSeries(SeismographView.SEISMO_LTGRAY, 0.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);
    seismo.addSeries(SeismographView.SEISMO_DTBLUE, 0.0f, 1.0f,
        Paint.Style.FILL_AND_STROKE);

    // Add vector series
    configureVectorSeismo();
  }

  /** Set up seismograph for generic vector seismo only. */
  public void configureVectorSeismo() {
    // Add X,Y,Z components in RGB
    seismo.addSeries(SeismographView.SEISMO_RED);
    seismo.addSeries(SeismographView.SEISMO_LTBLUE);
    seismo.addSeries(SeismographView.SEISMO_YELLOW);

    // Clear the seismograph
    seismo.invalidate();
  }
}