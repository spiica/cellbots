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

import java.text.DecimalFormat;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class AccelerometerHandler implements SensorEventListener {
	Context context;
	DashboardCamera camera;
	int threshold = 2000;  // milli g forces.
	int delay = 15;  // seconds.
	
	/**
	 * Sets the force threshold in any direction for accelerator triggered
	 * video capture.
	 * @param threshold The threshold for capture in units of milli g forces.
	 */
	public void setThreshold(int threshold) {
		this.threshold = threshold;
		Log.i("HelloAndroid", "Accident threshold is not " + threshold + " milli g forces");
	}
	
	/**
	 * Gets the current force threshold for capture.
	 * @return threshold The threshold for capture in units of milli g forces.
	 */
	public int getThreshold() {
		return threshold;
	}
	
	/**
	 * Sets how long we should record after an accident is detected.
	 * @param delay The number of seconds to continue to record.
	 */
	public void setDelay(int delay) {
		this.delay = delay;
		Log.i("HelloAndroid", "Accident delay is now " + delay + " seconds.");
	}
	
	/**
	 * Gets how long we should record after an accident is detected.
	 * @return The number of seconds that we will continue to record.
	 */
	public int getDelay() {
		return delay;
	}
	
	AccelerometerHandler(Context context, DashboardCamera camera) {
		this.context = context;
		this.camera = camera;
	}
	
	private SensorManager getSensorManager() {
		return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	// Can return null.
	private Sensor getAccelerometer(SensorManager manager) {
		return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	public void disable() {
		getSensorManager().unregisterListener(this);
		Log.i("HelloAndroid", "Accelerometer listener disabled.");
	}
	
	public void enable() {
		Log.i("HelloAndroid", "Enabling the accelerometer listener.");
		// Find the accelerometer and register this object as a listener if it exists.
		SensorManager manager = getSensorManager();
		Sensor accelerometer = getAccelerometer(manager);
		if (accelerometer != null) {
			// SENSOR_DELAY_UI was chosen over SENSOR_DELAY_NORMAL because it was possible to
			// miss events simulated in hand with the slower rate.
			if (!manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)) {
				Log.e("HelloAndroid", "Could not register a listener for the accelerometer.");
				issueLongToast("Could not find and accelerometer.");
			}
		} else {
			Log.e("HelloAndroid", "Could not get a default accelerometer sensor!");
			issueLongToast("Could not find and accelerometer.");
		}
		Log.i("HelloAndroid", "Accelerometer listener enabled!");
	}
	
	private void issueShortToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
	
	private void issueLongToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do nothing.
	}

	private double getMagnitude(float x, float y, float z) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
	}
	
	private double toGForce(double newtons) {
		return newtons / SensorManager.GRAVITY_EARTH;
	}
	
	private double toGForce(int milliGForce) {
		return (milliGForce * 1.0) / 1000;
	}
	
	private int toMilliGForce(double newtons) {
		return (int) (toGForce(newtons) * 1000);
	}
	
	private double toNewtons(int milliGForce) {
		return toNewtons(toGForce(milliGForce));
	}
	
	private double toNewtons(double gForce) {
		return gForce * SensorManager.GRAVITY_EARTH;
	}
	
	public void onSensorChanged(SensorEvent event) {
		if (event.values.length == 3) {
			double force = getMagnitude(event.values[0], event.values[1], event.values[2]);
			// Log.i("HelloAndroid", "Accelerometer Reading: " + force + "N ");
			
			if (toGForce(force) >= toGForce(threshold)){
				// Disable the sensor so it doesn't fire any more events.
				disable();
				
				Log.i("HelloAndroid", "Accident " + toGForce(force) + " >= " + toGForce(threshold));
				
				// TODO(tgnourse): Before this dialog appears we should get a lock on the camera object.
				// This disable() / enable() crap is just an approximation of that. Right now we can get
				// into wonky states if say a touch and an acceleration trigger happen in very close
				// proximity.
				
				// Begin the count down until the recording should be captured. 
				SaveVideoInitiator initiator = new SaveVideoInitiator(force);
				initiator.execute();
			}
		} else {
			Log.e("HelloAndroid", "Got different (" + event.values.length + ") " +
					"than 3 values from the acceleromter!! " +
					"The phone may have been transported to another universe");
		}
	}
	
	private class SaveVideoInitiator extends AsyncTask<Void, Integer, Void> {
		private ProgressDialog dialog = new ProgressDialog(context);
		private double force;
		
		SaveVideoInitiator(double force) {
			this.force = force;
		}
		
		protected void onPreExecute() {
			dialog.setCancelable(false);
		}
		
		protected Void doInBackground(Void... unused) {	
			try {
				// Sleep for recordingDelay seconds to get the aftermath.
				for (int second = delay; second > 0; second--) {
					publishProgress(second);
					Thread.sleep(1000);
				}
				
			} catch (InterruptedException e) {
				// Do nothing. This shouldn't happen.
			}
			
			return null;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			if (progress.length == 1) {
				dialog.setMessage("Accident detected! Measured "
						+ (new DecimalFormat("#.#")).format(toGForce(force))
						+ "G which is over the threshold of "
						+ (new DecimalFormat("#.#")).format(toGForce(threshold))
						+ "G. Recording for " + progress[0] + " more seconds.");
				dialog.show();
			} else {
				Log.e("HelloAndroid", "Progress params should have a single Integer!");
			}
	    }
		
		protected void onPostExecute(Void unused) {
			dialog.dismiss();
			// Now stop and save the recording.
			camera.stopAndSaveRecording();
			
			// Re-enable the sensor.
			// TODO(tgnourse): It would be better to re-enable the sensor after the video has been
			// saved so we don't accidentally trigger another recording. The other option is to
			// clean up the DashboardCamera class to just ignore certain additional events when it
			// is already doing something (like saving a file).
			enable();
		}
	}

}
