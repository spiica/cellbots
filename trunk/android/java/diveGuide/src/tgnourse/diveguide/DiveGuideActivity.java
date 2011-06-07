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

package tgnourse.diveguide;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class DiveGuideActivity extends Activity {
	
	/**
	 * We need to keep track of all of these listeners so we can de-register them later.
	 */
	private GpsStatus.Listener gpsStatusListener;
	private GpsStatus.NmeaListener nmeaListener;
	private LocationListener locationListener;
	private LocationManager locationManager;
	
	/**
	 * Information about the current location.
	 */
	private CurrentLocation currentLocation;
	private List<TargetLocation> targetLocations;
	private int index = 0;
	
	/**
	 * We need to keep track of all the sensor listeners too.
	 */
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	/**
	 * Output file.
	 */
	ExternalStorageFile file;
	
	/**
	 * Screen lock.
	 */
	PowerManager.WakeLock screenLock;
	
	/**
	 * Set up the UI and grab the LocationManager.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Create the current location object.
        currentLocation = new CurrentLocation();
        targetLocations = new ArrayList<TargetLocation>();
        
        // Set up the UI.
        setContentView(R.layout.main);
        
        // Set up the various location listeners so we get GPS updates.
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsStatusListener = new MyGpsStatusListener();
        nmeaListener = new MyNmeaListener();
        locationListener = new MyLocationListener();
        
        // Set up the sensor listeners.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorListener = new MySensorListener();
        
        // Create the file object (note: we don't open it until onResume()).
        file = new ExternalStorageFile("divetracker", "csv");
    }
    
    private void setTargetLocations(CharSequence site) {
    	targetLocations.clear();
    	if (site.equals("Pt Lobos")) {
    		// Point Lobos
    		targetLocations.add(new TargetLocation("Boat Dock", Color.rgb(255, 0, 255), 36.520278, -121.940558));
    		targetLocations.add(new TargetLocation("Cannery Pt", Color.rgb(255, 0, 0), 36.521044,-121.939933));
    		targetLocations.add(new TargetLocation("N Middle Reef", Color.rgb(255, 255, 0), 36.522417, -121.939550));
    		targetLocations.add(new TargetLocation("Whale Bones", Color.rgb(0, 255, 255), 36.523200, -121.939333));
    		targetLocations.add(new TargetLocation("Granite Pt Wall?", Color.rgb(0, 255, 0), 36.522417, -121.939550));
    		
    	} else if (site.equals("Monastery North")) {
    		// Monastery North
    		targetLocations.add(new TargetLocation("North Beach", Color.rgb(255, 0, 255), 36.525696,-121.925223));
    		targetLocations.add(new TargetLocation("Kelp Tip", Color.rgb(255, 0, 0), 36.527372,-121.9279));
    	} else if (site.equals("Monastery South")) {
    		// Monastery South
    		targetLocations.add(new TargetLocation("South Beach", Color.rgb(255, 0, 255), 36.522587,-121.928909));
    		targetLocations.add(new TargetLocation("Kelp Finger Tip", Color.rgb(255, 0, 0), 36.525829,-121.930261));
    	} else if (site.equals("Breakwater")) {
	        // Breakwater
	        targetLocations.add(new TargetLocation("South Beach", Color.rgb(255, 0, 255), 36.609723, -121.894727));
	        targetLocations.add(new TargetLocation("The Barge", Color.rgb(255, 0, 0), 36.610633, -121.890150));
	        targetLocations.add(new TargetLocation("End of the Wall", Color.rgb(255, 255, 0), 36.608561, -121.889647));
	        targetLocations.add(new TargetLocation("Wall Elbow", Color.rgb(0, 255, 255), 36.609508, -121.892790));
	        // Old location for the Metridium Fields from the interwebs
	        // targetLocations.add(new TargetLocation("Metridium Fields", Color.rgb(0, 255, 0), 36.612400, -121.892817));
	        targetLocations.add(new TargetLocation("Metridium Fields", Color.rgb(0, 255, 0), 36.612703, -121.892886));
    	}
    	
    	if (currentLocation.hasLocation()) {
    		showNextTargetLocation();
    	}
    }
    
    private void updateUI(TargetLocation target) {
    	if (currentLocation.hasLocation()) {
    		TextView destination = (TextView) findViewById(R.id.destination);
    		destination.setText(target.getName());
    		destination.setTextColor(target.getColor());
    		
	    	TextView distanceValue = (TextView) findViewById(R.id.distance_value);
	    	TextView distanceUnits = (TextView) findViewById(R.id.distance_units);
	    	TextView headingValue = (TextView) findViewById(R.id.heading_value);
	    	TextView headingDirection = (TextView) findViewById(R.id.heading_direction);
	    	TextView ageValue = (TextView) findViewById(R.id.age_value);
	    	TextView ageUnits = (TextView) findViewById(R.id.age_units);
	    	
	    	CurrentLocation.Difference difference = currentLocation.getDifference(target);
	    	
	    	distanceValue.setText(Util.getHumanReadableDistance(difference.getDistance()));
	    	distanceUnits.setText(Util.getUnitForDistance(difference.getDistance()));
	    	headingValue.setText(String.valueOf(difference.getHeading()));
	    	headingDirection.setText(Util.getDirectionFromHeading(difference.getHeading()));
	    	ageValue.setText(Util.getHumanReadableDuration(difference.getAge()));
	    	ageUnits.setText(Util.getUnitForDuration(difference.getAge()));
    	} else {
    		updateMessage("Waiting for GPS ...");
    	}
    }
    
    private void updateMessage(String message) {
    	TextView status = (TextView) findViewById(R.id.destination);
    	status.setText(message);
    	status.setTextColor(Color.WHITE);
    }

    private void updateUI() {
    	if (targetLocations.size() > 0) {
    		updateUI(targetLocations.get(index));
		} else {
			updateMessage("Pick a dive site ...");
		}
    }
    
    private void showNextTargetLocation() {
    	// Reset the index if the dive site changed.
    	if (!(index < targetLocations.size())) {
    		index = 0;
    	}
    	
		if (targetLocations.size() > 0) {
			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
			updateUI(targetLocations.get(index));
			index = (index + 1) % targetLocations.size();
		} else {
			updateMessage("Pick a dive site ...");
			Toast.makeText(getApplicationContext(), "Please pick a dive site first.", Toast.LENGTH_SHORT).show();
		}
    }
    
    /**
     * Start the location tracking.
     */
    @Override
    protected void onResume() {
    	super.onResume();
    	Util.log("onResume()");
    	
    	file.open();
        
    	// Re-register all of the listeners.
        if (!locationManager.addGpsStatusListener(gpsStatusListener)) {
        	Util.error("Couldn't add Gps Status Listener!");
        }
        
        if (!locationManager.addNmeaListener(nmeaListener)) {
        	Util.error("Couldn't add Nmea Listener!");
        }
        
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        
        // Register to listen to all the sensors.
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
        	Util.log("Registering listener for " + sensor.getName());
        	sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        screenLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DiveTracker");
        screenLock.acquire();
        
        startTimer();
        
        // If there's no site currently selected, prompt the user to select one.
        if (targetLocations.size() == 0) {
        	showDialog(DIALOG_SITE_SELECTION);
        }
    }
    
    private Handler handler = new Handler();
    long startTime = 0L;
    private Runnable updateTimeTask = new Runnable() {
    	public void run() {
    		long seconds = (SystemClock.uptimeMillis() - startTime) / 1000;
    		long minutes = seconds / 60;
    		seconds = seconds % 60;

    		updateUI();

    		handler.postAtTime(this, startTime + (((minutes * 60) + seconds + 1) * 1000));
    	}
    };
    private void startTimer() {
    	Util.log("startTimer()");
    	startTime = System.currentTimeMillis();
    	handler.removeCallbacks(updateTimeTask);
    	handler.postDelayed(updateTimeTask, 100);
    }
    public void stopTimer() {
    	Util.log("stopTimer()");
    	handler.removeCallbacks(updateTimeTask);
    }
    
    /**
     * Stop the location tracking.
     */
    @Override
    protected void onPause() {
    	super.onPause();
    	Util.log("onPause()");
    	
    	stopTimer();
    	
    	screenLock.release();
    	
    	// Remover the listeners.
    	locationManager.removeGpsStatusListener(gpsStatusListener);
    	locationManager.removeNmeaListener(nmeaListener);
    	locationManager.removeUpdates(locationListener);
    	sensorManager.unregisterListener(sensorListener);
    	
    	file.close();
    }
    
    private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			// Util.log("New location: " + location);
			currentLocation.locationChanged(location);
			updateUI();
			String line = "GPS," + System.currentTimeMillis() + "," + 
				location.getAccuracy() + "," + location.getAltitude() + "," + 
				location.getBearing() + "," + location.getLatitude() + "," + 
				location.getLongitude() + "," + location.getSpeed() + "," + 
				location.getTime();
			// Util.log(line);
			file.write(line);
		}

		public void onProviderDisabled(String provider) {
			Util.log("Provider disabled: " + provider);
		}

		public void onProviderEnabled(String provider) {
			Util.log("Provider enabled: " + provider);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			Util.log("Provider status changed: " + provider);
			switch (status) {
				case LocationProvider.AVAILABLE:
					Util.log(provider + " is available.");
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					Util.log(provider + " is temporarily unavailable.");
				case LocationProvider.OUT_OF_SERVICE:
					Util.log(provider + " is out of service.");
			}
		}
    }
    
    private class MyGpsStatusListener implements GpsStatus.Listener {
		public void onGpsStatusChanged(int event) {
			//LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			switch (event) {
				case GpsStatus.GPS_EVENT_FIRST_FIX:
					//Util.log("First fix");
					break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					//Util.log("Satellite status");
					break;
				case GpsStatus.GPS_EVENT_STARTED:
					//Util.log("Started");
					break;
				case GpsStatus.GPS_EVENT_STOPPED:
					//Util.log("Stopped");
					break;
			}
			// Request a new GpsStatus object instead of having one filled in.
			//GpsStatus status = locationManager.getGpsStatus(null);
			//Util.log("Max Satellites: " + status.getMaxSatellites());
			//Util.log("Time to First Fix: " + status.getTimeToFirstFix());
		}
    }
    
    private class MyNmeaListener implements GpsStatus.NmeaListener {
		public void onNmeaReceived(long timestamp, String nmea) {
			//Calendar calendar = Calendar.getInstance();
			//calendar.setTimeInMillis(timestamp);
			//Util.log(calendar.getTime().toString() + "] " + nmea);
		}
    }
    
    private class MySensorListener implements SensorEventListener {
    	
    	long lastSwitchTime = 0;  // ms
    	
    	private double getMagnitude(float x, float y, float z) {
    		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    	}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Util.log("Sensor accuracy changed " + sensor.getName() + ", " + accuracy);
		}

		public void onSensorChanged(SensorEvent event) {
			StringBuffer values = new StringBuffer();
			for (float value : event.values) {
				values.append(value);
				values.append(':');
			}
			String line = "SNS," + System.currentTimeMillis() + "," +
				event.sensor.getType() + "," + "\"" + event.sensor.getName() + "\"," +
				event.timestamp + "," + event.accuracy + "," + values; 
			// Util.log(line);
			file.write(line);
			
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER &&
				event.values.length == 3) {
				double force = getMagnitude(event.values[0], event.values[1], event.values[2]);
				
				double forceThreshold = 25.0;  // N
				long timeThreshold = 800;  // ms
				long currentTime = System.currentTimeMillis();
				if (force > forceThreshold && currentTime - lastSwitchTime > timeThreshold) {
					Util.log("Detected a shake! " + force + " > " + forceThreshold);
					showNextTargetLocation();
					lastSwitchTime = currentTime;
				}
			}
		}	
    }
    
    /**
     * Android calls this when the user taps the menu button.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    /**
     * Android calls this when a user taps an option in the options menu.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.edit_dive_sites:
    			// showDialog(DIALOG_EDIT_DIVE_SITES);
    			break;
    		case R.id.warnings:
    			// showDialog(DIALOG_WARNINGS);
    			break;
    		case R.id.set_dive_site:
    			showDialog(DIALOG_SITE_SELECTION);
    			break;
    		case R.id.next_target:
    			showNextTargetLocation();
    			break;
    	}
        
        return true;
    }
    
    static final int DIALOG_SITE_SELECTION = 0;

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_SITE_SELECTION:
            // do the work to define the pause Dialog
            final CharSequence[] items = {"Breakwater", "Monastery North", "Monastery South", "Pt Lobos"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick a dive site");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getApplicationContext(), "Dive site set to " + items[item], Toast.LENGTH_SHORT).show();
                    setTargetLocations(items[item]);
                    showNextTargetLocation();
                }
            });
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }
}