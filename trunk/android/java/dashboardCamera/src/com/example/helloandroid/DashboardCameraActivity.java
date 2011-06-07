/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.helloandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class DashboardCameraActivity extends Activity { 
    
	public static final String PREFS_NAME = "DashboardCameraPrefsFile";
	public static final String PREF_VIDEO_LENGTH = "videoLength";
	public static final String PREF_ACCIDENT_DELAY = "accidentDelay";
	public static final String PREF_ACCIDENT_THRESHOLD = "accidentThreshold";
	
	private DashboardCamera camera;
	private PreviewSurfaceView preview;
	private ToggleThread thread;
	private AccelerometerHandler accelerometerHandler;
	
    /** Called when the activity is first created. */ 
    public void onCreate(Bundle savedInstanceState) { 
         super.onCreate(savedInstanceState); 
         requestWindowFeature(Window.FEATURE_NO_TITLE); 
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
         
         setContentView(R.layout.preview);
         preview = (PreviewSurfaceView) findViewById(R.id.new_preview);
         camera = preview.getCamera();
         // Thread to toggle the video buffers.
         thread = new ToggleThread(camera, this);
         thread.start();
         
         // Handler to handle changes in the accelerometer.
         accelerometerHandler = new AccelerometerHandler(this, camera);
         
         // Restore the settings.
         SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
         restoreVideoLength(settings);
         restoreAccidentDelay(settings);
         restoreAccidentThreshold(settings);
    }
    
    public void onResume() {
    	super.onResume();
    	Log.i("HelloAndroid", "Resumining operations!");
    	thread.enable();
    	accelerometerHandler.enable();
    }
    
    public void onPause() {
    	super.onPause();
    	Log.i("HelloAndroid", "Pausing operations!");
    	thread.disable();
    	accelerometerHandler.disable();
    	// Include an extra disable here in case the application gets paused before the PreviewSurfaceView
    	// gets destroyed.
    	camera.disableRecording();
    }
    
    private void restoreVideoLength(SharedPreferences settings) {
    	int videoLength = settings.getInt(PREF_VIDEO_LENGTH, 30);
        Log.i("HelloAndroid", "Restoring videoLength to: " + videoLength);
        thread.setVideoLength(videoLength);
    }
    
    private void restoreVideoLengthMenu(Menu menu, SharedPreferences settings) {
        int videoLength = settings.getInt(PREF_VIDEO_LENGTH, 30);
        // TODO(tgnourse): This is ugly and brittle. The option objects need to be more closely
        // linked to the actual values.
        if (videoLength == 15) {
        	menu.findItem(R.id.fifteen_seconds).setChecked(true);
        } else if (videoLength == 30) {
        	menu.findItem(R.id.thirty_seconds).setChecked(true);
        } else if (videoLength == 60) {
        	menu.findItem(R.id.one_minute).setChecked(true);
        } else if (videoLength == 120) {
        	menu.findItem(R.id.two_minutes).setChecked(true);
        } else {
        	// This is here in case some value other than the 4 prescribed got set somehow.
        	Log.w("HelloAndroid", "videoLength is an unknown value: " + videoLength);
        }
    }
    
    private void restoreAccidentDelay(SharedPreferences settings) {
    	int accidentDelay = settings.getInt(PREF_ACCIDENT_DELAY, 15);
        Log.i("HelloAndroid", "Restoring accidentDelay to: " + accidentDelay);
        accelerometerHandler.setDelay(accidentDelay);
    }
    
    private void restoreAccidentDelayMenu(Menu menu, SharedPreferences settings) {
        int accidentDelay = settings.getInt(PREF_ACCIDENT_DELAY, 15);
        // TODO(tgnourse): This is ugly and brittle. The option objects need to be more closely
        // linked to the actual values.
        if (accidentDelay == 0) {
        	menu.findItem(R.id.accident_delay_zero_seconds).setChecked(true);
        } else if (accidentDelay == 5) {
        	menu.findItem(R.id.accident_delay_five_seconds).setChecked(true);
        } else if (accidentDelay == 10) {
        	menu.findItem(R.id.accident_delay_ten_seconds).setChecked(true);
        } else if (accidentDelay == 15) {
        	menu.findItem(R.id.accident_delay_fifteen_seconds).setChecked(true);
        } else {
        	// This is here in case some value other than the 4 prescribed got set somehow.
        	Log.w("HelloAndroid", "accidentDelay is an unknown value: " + accidentDelay);
        }
    }
    
    private void restoreAccidentThreshold(SharedPreferences settings) {
    	int accidentThreshold = settings.getInt(PREF_ACCIDENT_THRESHOLD, 2000);
        Log.i("HelloAndroid", "Restoring accidentThreshold to: " + accidentThreshold);
        accelerometerHandler.setThreshold(accidentThreshold);
    }
    
    private void restoreAccidentThresholdMenu(Menu menu, SharedPreferences settings) {
        int accidentThreshold = settings.getInt(PREF_ACCIDENT_THRESHOLD, 2000);
        // TODO(tgnourse): This is ugly and brittle. The option objects need to be more closely
        // linked to the actual values.
        if (accidentThreshold == 1500) {
        	menu.findItem(R.id.accident_threshold_one_point_five).setChecked(true);
        } else if (accidentThreshold == 2000) {
        	menu.findItem(R.id.accident_threshold_two).setChecked(true);
        } else if (accidentThreshold == 3000) {
        	menu.findItem(R.id.accident_threshold_three).setChecked(true);
        } else if (accidentThreshold == Integer.MAX_VALUE) {
        	menu.findItem(R.id.accident_threshold_disabled).setChecked(true);
        } else {
        	// This is here in case some value other than the 4 prescribed got set somehow.
        	Log.w("HelloAndroid", "accidentDelay is an unknown value: " + accidentThreshold);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        restoreVideoLengthMenu(menu, settings);
        restoreAccidentDelayMenu(menu, settings);
        restoreAccidentThresholdMenu(menu, settings);
        
        return true;
    }

    private void setVideoLength(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.fifteen_seconds:
    		Log.i("HelloAndroid", "Setting the video length to 15 seconds.");
    		if (!item.isChecked()) {
    			thread.setVideoLength(15);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.thirty_seconds:
    		Log.i("HelloAndroid", "Setting the video length to 30 seconds.");
    		if (!item.isChecked()) {
    			thread.setVideoLength(30);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.one_minute:
    		Log.i("HelloAndroid", "Setting the video length to 1 minute.");
    		if (!item.isChecked()) {
    			thread.setVideoLength(60);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.two_minutes:
    		Log.i("HelloAndroid", "Setting the video length to 2 minutes.");
    		if (!item.isChecked()) {
    			thread.setVideoLength(120);
    		}
    		item.setChecked(true);
    		break;
    	}

    	// Save the changes to the preferences.
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(PREF_VIDEO_LENGTH, thread.getVideoLength());
    	editor.commit();
    }
    
    private void setAccidentDelay(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.accident_delay_zero_seconds:
    		Log.i("HelloAndroid", "Setting the accident delay to 0 seconds.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setDelay(0);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_delay_five_seconds:
    		Log.i("HelloAndroid", "Setting the accident delay to 5 seconds.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setDelay(5);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_delay_ten_seconds:
    		Log.i("HelloAndroid", "Setting the accident delay to 10 seconds.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setDelay(10);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_delay_fifteen_seconds:
    		Log.i("HelloAndroid", "Setting the accident delay to 15 seconds.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setDelay(15);
    		}
    		item.setChecked(true);
    		break;
    	}

    	// Save the changes to the preferences.
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(PREF_ACCIDENT_DELAY, accelerometerHandler.getDelay());
    	editor.commit();
    }
    
    private void setAccidentThreshold(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.accident_threshold_one_point_five:
    		Log.i("HelloAndroid", "Setting the accident threshold to 1.5G.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setThreshold(1500);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_threshold_two:
    		Log.i("HelloAndroid", "Setting the accident threshold to 2G.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setThreshold(2000);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_threshold_three:
    		Log.i("HelloAndroid", "Setting the accident threshold to 3G.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setThreshold(3000);
    		}
    		item.setChecked(true);
    		break;
    	case R.id.accident_threshold_disabled:
    		Log.i("HelloAndroid", "Setting the accident threshold to disabld.");
    		if (!item.isChecked()) {
    			accelerometerHandler.setThreshold(Integer.MAX_VALUE);
    		}
    		item.setChecked(true);
    		break;
    	}

    	// Save the changes to the preferences.
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(PREF_ACCIDENT_THRESHOLD, accelerometerHandler.getThreshold());
    	editor.commit();
    }
    
    private void showHelp() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Tap the screen to record an incident. An incident will automatically be recorded after a delay if the phone detects an accident.")
    	       .setCancelable(false)
    	       .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	setVideoLength(item);
    	setAccidentDelay(item);
    	setAccidentThreshold(item);
    	
    	if (item.getItemId() == R.id.help) {
    		showHelp();
    	}
        
        return true;
    }
}


