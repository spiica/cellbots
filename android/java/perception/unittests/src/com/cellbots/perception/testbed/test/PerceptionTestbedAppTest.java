// Copyright 2011 Google Inc. All Rights Reserved.

package com.cellbots.perception.testbed.test;


import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

import com.cellbots.perception.testbed.PerceptionTestbedActivity;

/**
 * Test harness for the PerceptionTestbedAppActivity.
 * This is nearly a no-op test simply done so we can make sure the unit test
 * framework is working, which in turn will be used for detailed testing of
 * classes like the VerletIntegrator which has a lot of subtlety to it.
 * 
 * @author centaur@google.com (Anthony Francis)
 */
public class PerceptionTestbedAppTest extends ActivityInstrumentationTestCase2<
    PerceptionTestbedActivity> {
  /** A copy of the activity being tested. */
  private PerceptionTestbedActivity activity;
  
  /** Make sure the view exists. */
  private TextView view;
  
  /** a resource string that we expect to find in the UI. */
  private String resourceString;
  
  /** Create the test. */
  public PerceptionTestbedAppTest() {
    super("com.google.robots.senses.testbed",
        PerceptionTestbedActivity.class);
  }
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      activity = this.getActivity();
      view = (TextView) activity.findViewById(
          com.google.robots.senses.testbed.R.id.response_message);
      resourceString = activity.getString(
          com.google.robots.senses.testbed.R.string.initial_response);
  }
  
  /** Make sure a view is created. */
  public void testPreconditions() {
    assertNotNull(view);
  }
  
  /** Make sure the view has the expected text. */
  public void testText() {
    assertEquals(resourceString, (String) view.getText());
  }
  
  // TODO(centaur): Flesh out this test
  // Most functions here have been moved out to support classes.
}
