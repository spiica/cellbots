// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;

/**
 * Displays a seismograph-like view of a sequence of floats.
 * You need to add "series" of data with a given color and optionally
 * min and max values and a drawing style (filled or not).
 * @author centaur@google.com (Anthony Francis)
 */
public class SeismographView extends View {
  /** Max samples displayed in a graph. */
  public static final int MAX_SAMPLES = 100;

  /** Default fill/stroke style for a time series. */
  private static final Style DEFAULT_STYLE = Paint.Style.STROKE;

  /** Encapsulates the color, min, max and style of a data series. */
  class Series {
    /**
     * Create a new data series to be displayed in the seismo.
     * @param color Color to draw.
     * @param min Min expected value.
     * @param max Max expected value.
     * @param style Whether the values are filled or not.
     */
    Series(int color, float min, float max, Paint.Style style) {
      this.color = color;
      this.min = min;
      this.max = max;
      this.style = style;
      // Automatically draw an axis at zero if it falls between min and max.
      this.hasAxis = min <= 0 && 0 <= max;
    }
    int color;
    float min;
    float max;
    Paint.Style style;
    boolean hasAxis;
  }
  /** The time series displayed in this seismograph. */
  LinkedList<Series> dataSeries;

  /** Records a float sample at a given timestamp. */
  class Samples {
    /**
     * A list of values recorded at the given timestamp
     * @param values
     * @param timestamp
     */
    Samples(float[] values, long timestamp) {
      this.timestamp = timestamp;
      this.values = values;
    }
    float[] values;
    long timestamp;
  }
  /** List of data samples that will be displayed in the series. */
  LinkedList<Samples> dataSamples;

  /** Yellow value from the UX defined palette set. */
  public static final int SEISMO_YELLOW = Color.parseColor("#FFFA40");

  /** Red value from the UX defined palette set. */
  public static final int SEISMO_RED = Color.parseColor("#FD0006");

  /** Light gray value from the UX defined palette set. */
  public static final int SEISMO_LTGRAY = Color.rgb(128, 128, 128);

  /** Dark blue value from the UX defined palette set. */
  public static final int SEISMO_DTBLUE = Color.parseColor("#80062270");

  /** Light blue value from the UX defined palette set. */
  public static final int SEISMO_LTBLUE = Color.parseColor("#599CFF");

  //-------------------------------------------------------------------
  // Color scheme for the display
  //-------------------------------------------------------------------
  // TODO(centaur): switch to use of constants file
  /** Blue value from the UX defined palette set. */
  public static final int SEISMO_BLUE = Color.parseColor("#133CAC");

  /**
   * Needed so this control can be loaded in an XML view.
   * @param context
   */
  public SeismographView(Context context) {
    super(context);
    init();
  }

  /**
   * Needed so this control can be loaded in an XML view.
   * @param context
   * @param attrs
   */
  public SeismographView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  /**
   * Needed so this control can be loaded in an XML view.
   * @param context
   * @param attrs
   * @param style
   */
  public SeismographView(Context context, AttributeSet attrs, int style) {
    super(context, attrs, style);
    init();
  }

  /** Factor out initialization across the constructors */
  public void init() {
    dataSeries = new LinkedList<Series>();
    dataSamples = new LinkedList<Samples>();
  }

  /**
   * Add a fully configured time series.
   * @param color of the line to draw
   * @param min minimum value of the time series
   * @param max maximum value of the time series
   * @param style drawing style of the time series
   */
  public void addSeries(int color, float min, float max, Paint.Style style) {
    dataSeries.add(new Series(color, min, max, style));
  }

  /**
   * Add a time series with a default stroke. 
   * @param color of the line to draw
   * @param min minimum value of the time series
   * @param max maximum value of the time series
   */
  public void addSeries(int color, float min, float max) {
    addSeries(color, min, max, DEFAULT_STYLE);
  }

  /**
   * Add a time series between the default ranges of -1 and +1.
   * @param color of the line to draw
   */
  public void addSeries(int color) {
    addSeries(color, -1.0f, 1.0f);   
  }

  /** 
   * Add a sample to the list and drop previous samples.
   * TODO(centaur): use the timestamp to record a varying number of samples.
   * This feature would go with scaling line segments by timestamp values.
   * @param value value to be entered in the time series
   * @param timestamp timestamp the value was entered in
   */
  public void addSample(float value, long timestamp) {
    addSamples(new float[] {value}, timestamp);
  }

  /** 
   * Add a list of samples to the list and drop previous samples.
   * TODO(centaur): use the timestamp to record the right number of samples.
   * @param values values to be entered in the time series
   * @param timestamp timestamp the value was entered in
   */
  public void addSamples(float[] values, long timestamp) {
    dataSamples.add(new Samples(values, timestamp));
    if (dataSamples.size() > MAX_SAMPLES) {
      dataSamples.remove();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(Color.DKGRAY);
    for (int series = 0; series < dataSeries.size(); series++) {
      if (dataSeries.get(series).hasAxis) {
        drawAxis(canvas, dataSeries.get(series).min, dataSeries.get(series).max);
      }
      drawSamples(canvas,
          series,
          dataSeries.get(series).color,
          dataSeries.get(series).min,
          dataSeries.get(series).max,
          dataSeries.get(series).style);
    }
  }

  /**
   * Draw the samples in a seismograph like line.
   * TODO(centaur): scale line segments by the timestamp values.
   * @param canvas where we're drawing on
   * @param series which time series we are drawing
   * @param color of the line to draw
   * @param min minimum value of the time series
   * @param max maximum value of the time series
   * @param style drawing style of the time series
   */
  private void drawSamples(
      Canvas canvas,
      int series,
      int color,
      float min,
      float max,
      Paint.Style style) {
    // Create the paint object based on the canvas sampl
    Paint samplePaint = new Paint();
    samplePaint.setColor(color);
    samplePaint.setStyle(style);
    samplePaint.setStrokeWidth(style == DEFAULT_STYLE ? 2 : 1);

    // Create the path, compute it's "zero point" based on max & min, and
    // establish initial and final values for drawing the box if filled.
    Path samplePath = new Path();
    float scaledZero = scaleValue(Math.max(0.0f, min), min, max);
    float initialHeight = scaledZero;
    float finalHeight = scaledZero;

    // Draw the samples, using a straight line if no samples found 
    if (dataSamples.size() == 0) {
      initialHeight = scaledZero;
      samplePath.moveTo(0, initialHeight);
      samplePath.lineTo(getWidth(), initialHeight);

    } else {  // Draw the samples as far as we can reach.
      // Draw a "header" of zero samples until the samples catch up with the
      // sample size - this makes the samples come in from the right.
      int headerLength = Math.max(MAX_SAMPLES - dataSamples.size(), 0);
      if (headerLength > 0) {
        initialHeight = scaledZero;
        samplePath.moveTo(0, initialHeight);
        for (int i = 0; i < headerLength; i++) {
          samplePath.lineTo(scaleStep(i), initialHeight);
        }
      } else {
        initialHeight = scaleValue(getSample(0, series), min, max);
        samplePath.moveTo(0, initialHeight);
      }

      // Now draw the remainder of the samples
      for (int i = headerLength; i < MAX_SAMPLES; i++) {
        int index = i - headerLength;
        if (index < dataSamples.size()) {
          finalHeight = scaleValue(getSample(index, series), min, max); 
        } else {  // in case samples some how get out of sync
          finalHeight = scaledZero; 
        }
        samplePath.lineTo(scaleStep(i), finalHeight);
      }
    }

    // Finish the line to the end of the seismograph, then fill in the rest
    // of the box if we are drawing in a fill mode
    samplePath.lineTo(getWidth(), finalHeight);
    if (style == Paint.Style.FILL || style == Paint.Style.FILL_AND_STROKE) {
      samplePath.lineTo(getWidth(), scaledZero);
      samplePath.lineTo(0, scaledZero);
      samplePath.lineTo(0, initialHeight);
    }
    canvas.drawPath(samplePath, samplePaint);
  }

  /**
   * Scales a step between 0 and the max samples to fit in the width.
   * @param i
   * @return Step scaled to width.
   */
  private float scaleStep(int i) {
    return (1.0f / MAX_SAMPLES) * i * getWidth();
  }

  /**
   * Scales a value to fit between the min and max of the range.
   * @param value value to be displayed in the time series
   * @param min minimum value of the time series
   * @param max maximum value of the time series
   * @return Value scaled to height.
   */
  private float scaleValue(float value, float min, float max) {
    return Math.max(0, Math.min(getHeight(),
        getHeight() * (1.0f - (value - min) / (max - min))));
  }

  /**
   * Get a sample from the given series.
   * @param index within the time series
   * @param series to display
   */
  private float getSample(int index, int series) {
    return dataSamples.get(index).values[series];
  }

  /**
   * Draw just the linear axis.
   * @param canvas to draw on
   * @param min minimum value of the time series
   * @param max maximum value of the time series
   */
  private void drawAxis(Canvas canvas, float min, float max) {
    // Set up the paint to draw the axis
    Paint zeroPaint = new Paint();
    zeroPaint.setColor(Color.LTGRAY);
    zeroPaint.setStyle(DEFAULT_STYLE);
    zeroPaint.setStrokeWidth(1);

    // Now draw the path
    Path zeroPath = new Path();
    float axis = scaleValue(0, min, max);
    zeroPath.moveTo(0, axis);
    zeroPath.lineTo(getWidth(), axis);
    canvas.drawPath(zeroPath, zeroPaint);
  }
}
