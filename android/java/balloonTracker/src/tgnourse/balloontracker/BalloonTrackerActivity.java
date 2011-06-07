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

package tgnourse.balloontracker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import tgnourse.balloontracker.AprsReader.AprsException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class BalloonTrackerActivity extends Activity {
	
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
	private List<Target> targets;
	
	/**
	 * Set up the UI and grab the LocationManager.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Create the current location object.
        currentLocation = new CurrentLocation();
        targets = new ArrayList<Target>();

        
        // Set up the UI.
        setContentView(R.layout.main);
        
        // Set up the various location listeners so we get GPS updates.
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsStatusListener = new MyGpsStatusListener();
        nmeaListener = new MyNmeaListener();
        locationListener = new MyLocationListener();
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
    		case R.id.edit_locations:
    			showDialog(DIALOG_EDIT_LOCATIONS);
    			break;
    		case R.id.refresh_locations:
    			updateLocations();
    			break;
    		case R.id.edit_callsigns:
    			showDialog(DIALOG_EDIT_CALLSIGNS);
    			break;
    	}
        
        return true;
    }

    private void updateLocations() {
    	issueShortToast("Refreshing locations ...");
    	final List<String> stationNames = getStationNames();
		
    	new AsyncTask<Void, Void, Map<String, TargetLocation>>() {
    		protected Map<String, TargetLocation> doInBackground(Void... unused) {
    			try {
        			return new AprsReader().getLocation(stationNames);
    			} catch (AprsException e) {
    				Util.log(e.toString());
    				return null;
    			}
    		}

    		protected void onPostExecute(Map<String, TargetLocation> locations) {
    			if (locations == null) {
    				issueShortToast("Failed to refresh locations");
    				return;
    			}
    			int count = 0;
    			for (Target target : targets) {
    				String callsign = target.getCallsign();
					if (locations.containsKey(callsign)) {
    					target.setLocation(locations.get(callsign));
    					count++;
    					Util.log("Updated location for station " + callsign);
    				}
    			}
    	    	issueShortToast(count + " New Locations Received");
    			updateUI();    	
    		}
    	}.execute();
    }

	private List<String> getStationNames() {
		final List<String> stationNames = new ArrayList<String>();
		for (Target target : targets) {
			stationNames.add(target.getCallsign());
		}
		return stationNames;
	}
    
	static final int DIALOG_EDIT_LOCATIONS = 0;
	static final int DIALOG_EDIT_CALLSIGNS = 1;
	static final int DIALOG_EDIT_LOCATION = 2;
	
	static final String DIALOG_EDIT_LOCATION_CALLSIGN = "callsign";
	
	protected Dialog onCreateDialog(int id, Bundle args) {
		Util.log("onCreateDialog(id,args)");
	    final Dialog dialog;
	    
	    switch(id) {
	    case DIALOG_EDIT_CALLSIGNS:
	    	Util.log("Create callsigns dialog");
	    	dialog = createEditCallsignsDialog();
	    	break;
	    case DIALOG_EDIT_LOCATIONS:
	    	Util.log("Create locations dialog");
	    	dialog = createEditLocationsDialog();
	        break;
	    case DIALOG_EDIT_LOCATION:
	    	Util.log("Create location dialog");
	    	dialog = createEditLocationDialog(args.getString(DIALOG_EDIT_LOCATION_CALLSIGN));
	    	break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		Util.log("onPrepareDialog(id, dialog, args)");
	    switch(id) {
	    case DIALOG_EDIT_CALLSIGNS:
	    	Util.log("Edit callsigns dialog");
	    	updateEditCallsignsDialog(dialog);
	    	break;
	    case DIALOG_EDIT_LOCATIONS:
	    	Util.log("Edit locations dialog");
	    	updateEditLocationsDialog(dialog);
	        break;
	    case DIALOG_EDIT_LOCATION:
	    	Util.log("Edit location dialog");
	    	updateEditLocationDialog(dialog, args.getString(DIALOG_EDIT_LOCATION_CALLSIGN)); 
	    	break;
	    }
	}
	
	private void updateEditLocationsDialog(Dialog dialog) {
		Util.log("updateEditLocationsDialog");
		
		final Button callsign1 = (Button) dialog.findViewById(R.id.callsign1_button);
    	callsign1.setText(targets.size() >= 1 ? targets.get(0).getCallsign() : "");
    	final Button callsign2 = (Button) dialog.findViewById(R.id.callsign2_button);
    	callsign2.setText(targets.size() >= 2 ? targets.get(1).getCallsign() : "");
    	final Button callsign3 = (Button) dialog.findViewById(R.id.callsign3_button);
    	callsign3.setText(targets.size() >= 3 ? targets.get(2).getCallsign() : "");
    	final TextView warning = (TextView) dialog.findViewById(R.id.warning);
    	
    	Util.log("There are " + targets.size() + " targets");
    	if (targets.size() == 0) {
    		Util.log("0");
    		warning.setVisibility(View.VISIBLE);
    		callsign1.setVisibility(View.GONE);
    		callsign2.setVisibility(View.GONE);
    		callsign3.setVisibility(View.GONE);
    	} else if (targets.size() == 1) {
    		Util.log("1");
    		warning.setVisibility(View.GONE);
    		callsign1.setVisibility(View.VISIBLE);
    		callsign2.setVisibility(View.GONE);
    		callsign3.setVisibility(View.GONE);
    	} else if (targets.size() == 2) {
    		Util.log("2");
    		warning.setVisibility(View.GONE);
    		callsign1.setVisibility(View.VISIBLE);
    		callsign2.setVisibility(View.VISIBLE);
    		callsign3.setVisibility(View.GONE);
    	} else if (targets.size() >= 3) {
    		Util.log("3");
    		warning.setVisibility(View.GONE);
    		callsign1.setVisibility(View.VISIBLE);
    		callsign2.setVisibility(View.VISIBLE);
    		callsign3.setVisibility(View.VISIBLE);
    	}
	}
	
	private Dialog createEditLocationsDialog() {
    	Context mContext = getApplicationContext();
    	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    	View layout = inflater.inflate(R.layout.locations_dialog, (ViewGroup) findViewById(R.id.layout_root));
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Edit callsign locations");
    	
    	builder.setView(layout)
        .setCancelable(true)
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	Util.log("OKAY!");          	 
                dialog.dismiss();
                updateUI();
            }
        });
    	final Dialog dialog = builder.create();
    	
    	final Button callsign1 = (Button) layout.findViewById(R.id.callsign1_button);
    	callsign1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Bundle args = new Bundle();
            	args.putString(DIALOG_EDIT_LOCATION_CALLSIGN, getTextViewValue(dialog, R.id.callsign1_button));
            	showDialog(DIALOG_EDIT_LOCATION, args);
            }
        });
    	final Button callsign2 = (Button) layout.findViewById(R.id.callsign2_button);
    	callsign2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Bundle args = new Bundle();
            	args.putString(DIALOG_EDIT_LOCATION_CALLSIGN, getTextViewValue(dialog, R.id.callsign2_button));
            	showDialog(DIALOG_EDIT_LOCATION, args);
            }
        });
    	final Button callsign3 = (Button) layout.findViewById(R.id.callsign3_button);
    	callsign3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Bundle args = new Bundle();
            	args.putString(DIALOG_EDIT_LOCATION_CALLSIGN, getTextViewValue(dialog, R.id.callsign3_button));
            	showDialog(DIALOG_EDIT_LOCATION, args);
            }
        });
         
         return dialog;
	}
	
	private String getTextViewValue(Dialog layout, int id) {
		return ((TextView) layout.findViewById(id)).getText().toString();
	}
	
	private void updateEditCallsignsDialog(Dialog dialog) {
		Util.log("updateEditCallsignsDialog");
    	final TextView callsign1 = (TextView) dialog.findViewById(R.id.edit_callsign1);
    	callsign1.setText(targets.size() >= 1 ? targets.get(0).getCallsign() : "");
    	final TextView callsign2 = (TextView) dialog.findViewById(R.id.edit_callsign2);
    	callsign2.setText(targets.size() >= 2 ? targets.get(1).getCallsign() : "");
    	final TextView callsign3 = (TextView) dialog.findViewById(R.id.edit_callsign3);
    	callsign3.setText(targets.size() >= 3 ? targets.get(2).getCallsign() : "");
	}
	
	private Dialog createEditCallsignsDialog() {
    	Context mContext = getApplicationContext();
    	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    	final View layout = inflater.inflate(R.layout.callsign_dialog,
    	                               (ViewGroup) findViewById(R.id.layout_root));
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Edit callsigns");
    	
    	builder.setView(layout)
    	.setCancelable(false)
        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	 Util.log("OKAY!");
           	 
	           	 String one, two, three;
	           	 one = getTextViewValue((Dialog) dialog, R.id.edit_callsign1).toUpperCase().trim();
	           	 two = getTextViewValue((Dialog) dialog, R.id.edit_callsign2).toUpperCase().trim();
	           	 three = getTextViewValue((Dialog) dialog, R.id.edit_callsign3).toUpperCase().trim();
	           	 Util.log("Done!");
	           	 
	           	 setTargetLocations("".equals(one) ? null : one, "".equals(two) ? null : two, "".equals(three) ? null : three);
	           	 
                dialog.dismiss();
                updateUI();
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	Util.log("Cancel, not changing any callsigns!");
                dialog.cancel();
            }
        });
    	final Dialog dialog = builder.create();
         
        return dialog;
	}
	
	private void updateEditLocationDialog(Dialog dialog, String callsign) {
		Util.log("updateEditLocationsDialog");
		
    	TextView callsignview = (TextView) dialog.findViewById(R.id.callsign);
    	callsignview.setText("Update location for " + callsign);
    	
    	TargetLocation location = null;
    	for (Target target : targets) {
    		Util.log(target.getCallsign());
    		if (target.getCallsign().equals(callsign)) {
    			location = target.getLocation();
    		}
    	}
    	
		if (location != null) {
	    	TextView latitude = (TextView) dialog.findViewById(R.id.edit_latitude);
	    	latitude.setText(String.valueOf(location.getLatitude()));
	    	TextView longitude = (TextView) dialog.findViewById(R.id.edit_longitude);
	    	longitude.setText(String.valueOf(location.getLongitude()));
	    	TextView altitude = (TextView) dialog.findViewById(R.id.edit_altitude);
	    	altitude.setText(String.valueOf(location.getAltitude()));
		} else {
	    	TextView latitude = (TextView) dialog.findViewById(R.id.edit_latitude);
	    	latitude.setText("");
	    	TextView longitude = (TextView) dialog.findViewById(R.id.edit_longitude);
	    	longitude.setText("");
	    	TextView altitude = (TextView) dialog.findViewById(R.id.edit_altitude);
	    	altitude.setText("");
		}
	}
	
	private Dialog createEditLocationDialog(final String callsign) {
    	Context mContext = getApplicationContext();
    	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    	View layout = inflater.inflate(R.layout.location_dialog,
    	                               (ViewGroup) findViewById(R.id.layout_root));
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Set a location for " + callsign);
    	
    	builder.setView(layout)
    	.setCancelable(false)
        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
        	   Util.log("OKAY! Setting new location for " + callsign + "!");
        	   try {
        		   double latitude = Double.parseDouble(getTextViewValue((Dialog) dialog, R.id.edit_latitude));
        		   double longitude = Double.parseDouble(getTextViewValue((Dialog) dialog, R.id.edit_longitude));
        		   double altitude = Double.parseDouble(getTextViewValue((Dialog) dialog, R.id.edit_altitude));
        		   
            	   for (Target target : targets) {
            		   if (callsign.equals(target.getCallsign())) {
            			   target.setLocation(new TargetLocation(latitude, longitude, 0, altitude));
            		   }
            	   }
            	   
            	   issueShortToast("Updated location for " + callsign);
                   dialog.dismiss();
                   updateUI();
        	   } catch (NumberFormatException e) {
        		   issueShortToast("Numbers are not formatted correctly. Please try again.");
        	   }
           }
       })
       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
        	   Util.log("Cancel, not changing any callsigns!");
               dialog.cancel();
           }
       });
    	
    	final Dialog dialog = builder.create();
         
         return dialog;
	}
    
    private void setTargetLocations(String one, String two, String three) {
    	targets.clear();
    	if (one != null) targets.add(new Target(one, Color.rgb(255, 0, 255)));
    	if (two != null) targets.add(new Target(two, Color.rgb(0, 0, 255)));
    	if (three != null) targets.add(new Target(three, Color.rgb(255, 255, 0)));
    	
    	Util.log("1.'" + one + "' 2.'" + two + "' 3.'" + three + "'");
    	
    	updateUI(targets);
    }
    
    private void updateStatus() {
    	if (targets.size() > 0) {
	    	if (currentLocation.hasLocation()) {
	    		updateStatus("Your location is " + 
	    				Util.getHumanReadableDuration(currentLocation.getAge()) +
	    				Util.getUnitForDuration(currentLocation.getAge()) + " old at " +
	    				Math.round(currentLocation.getDeclination()) + "¡ declination");
	    	} else {
	    		updateStatus("Waiting for your location ...");
	    	}
    	} else {
    		updateStatus("Please set some callsigns...");
    	}
    }
    
    private void updateStatus(String message) {
    	TextView status = (TextView) findViewById(R.id.status);
    	status.setText(message);
    }
    
    private void updateUI() {
    	updateUI(targets);
    }
    
    private void updateAgeSpeedElevation(View layout, long ageValue, float speedValue, double elevationValue) {
    	TextView age = (TextView) findViewByIdAndReleaseId(layout, R.id.age);
    	TextView speed = (TextView) findViewByIdAndReleaseId(layout, R.id.speed);
    	TextView elevation = (TextView) findViewByIdAndReleaseId(layout, R.id.elevation);
    	// age.setText(Util.getHumanReadableDuration(ageValue) + " " + Util.getUnitForDuration(ageValue));
    	// Age wasn't being calculated right so switch this to time of last fix
    	String time = new java.text.SimpleDateFormat("M/d HH:mm").format(new java.util.Date(ageValue));
    	age.setText(time);
		speed.setText(Util.formatNumber(speedValue * Util.MPH_INA_MPS, 0) + " mph");
		elevation.setText(Util.formatNumber(elevationValue * Util.FEET_INA_METER, 0) + " ft");
    }
    
    private void updateUI(List<Target> targets) {
    	Util.log("Updating the UI.");
    	
    	updateStatus();
    	
    	LayoutInflater inflater = getLayoutInflater();
    	
    	TableLayout mainTable = (TableLayout) findViewById(R.id.main_table);
    	// HACK: assumes there's only 1 permanent row in the table
    	mainTable.removeViews(1, mainTable.getChildCount() - 1);
    	
    	for (Target target : targets) {
    		TableLayout layout = (TableLayout) inflater.inflate(
    				R.layout.target_table, (ViewGroup) findViewById(R.id.main_table), true);

    		// HACK: Since we are merging new rows in multiple times, we would have ID
    		// collisions. To avoid this, we blow away the ID of each View as we access it.
    		// That way it won't cause conflicts when we merge in another set of rows.
    		TextView callsign = (TextView) findViewByIdAndReleaseId(layout, R.id.callsign);
	    	TextView altitude = (TextView) findViewByIdAndReleaseId(layout, R.id.altitude);			    	
	    	TextView azimuth = (TextView) findViewByIdAndReleaseId(layout, R.id.azimuth);
	    	TextView distance = (TextView) findViewByIdAndReleaseId(layout, R.id.distance);

	    	callsign.setText(target.getCallsign());
	    	TargetLocation targetLocation = target.getLocation();
	    	if (targetLocation == null) {
	    		callsign.setTextColor(Color.rgb(255, 0, 0));
	    	} else {
	    		callsign.setTextColor(Color.rgb(0, 255, 0));
	    	}
	    	
	    	if (!currentLocation.hasLocation() && targetLocation != null) {
	    		Util.log("Waiting for GPS signal ...");
	    		altitude.setText("-");
	    		azimuth.setText("-");
	    		distance.setText("-");
	    		updateAgeSpeedElevation(layout, targetLocation.getAge(), targetLocation.getSpeed(), targetLocation.getAltitude());
	    	} else if (!currentLocation.hasLocation() || targetLocation == null) {
	    		altitude.setText("-");
	    		azimuth.setText("-");
	    		distance.setText("-");
	    		
	        	TextView age = (TextView) findViewByIdAndReleaseId(layout, R.id.age);
	        	TextView speed = (TextView) findViewByIdAndReleaseId(layout, R.id.speed);
	        	TextView elevation = (TextView) findViewByIdAndReleaseId(layout, R.id.elevation);
	    		age.setText("-");
	    		speed.setText("-");
	    		elevation.setText("-");
	    	} else {
	    		Difference difference = currentLocation.getDifference(targetLocation);
	    		Util.log(difference.toString());
	    		altitude.setText(Util.formatNumber(difference.getAltitude(), 1) + "¡");
	    		azimuth.setText(Util.formatNumber(difference.getAzimuth(), 1) + "¡ " + Util.getDirectionFromHeading(Math.round(difference.getAzimuth())));
	    		distance.setText(Util.getHumanReadableDistance(difference.getDistance()) + " " + Util.getUnitForDistance(difference.getDistance()));
	    		
	    		long age;
	    		// TODO(tgnourse): Get actual time working.
	    		/*if (targetLocation.hasTime()) {
	    			age = difference.getTargetAge();
	    		} else {*/
	    			age = targetLocation.getAge();
	    		// }
	    		
	    		updateAgeSpeedElevation(layout, age, targetLocation.getSpeed(), targetLocation.getAltitude());
	    	}
    	}    	
    }

    /**
     * calls findViewById() and then changes the view's ID to -1.
     */
	private View findViewByIdAndReleaseId(View view, int id) {
		View out = view.findViewById(id);
		out.setId(-1);
		return out;
	}
    
    public static final String PREFS_NAME = "BalloonTrackerPrefsFile";
	public static final String PREF_TARGET_1 = "target1";
	public static final String PREF_TARGET_2 = "target2";
	public static final String PREF_TARGET_3 = "target3";
	
	public static final String PREF_HAS_LOCATION_PREFIX = "has_location_";
	public static final String PREF_LATITUDE_PREFIX = "latitude_";
	public static final String PREF_LONGITUDE_PREFIX = "longitude_";
	public static final String PREF_SPEED_PREFIX = "speed_";
	public static final String PREF_ALTITUDE_PREFIX = "altitude_";
	public static final String PREF_TIME_PREFIX = "time_";
    
    private double getPreferencesDouble(SharedPreferences preferences, String key) {
    	String str = preferences.getString(key, "");
    	try {
    		double num = Double.parseDouble(str);
    		return num;
    	} catch (NumberFormatException e) {
    		return (double) -1;
    	}
    }
	
	private void loadTargetLocation(SharedPreferences settings, String callsign) {
		double latitude = getPreferencesDouble(settings, PREF_LATITUDE_PREFIX + callsign);
		double longitude = getPreferencesDouble(settings, PREF_LONGITUDE_PREFIX + callsign);
		double altitude = getPreferencesDouble(settings, PREF_ALTITUDE_PREFIX + callsign);
		float speed = settings.getFloat(PREF_SPEED_PREFIX + callsign, (float) -1);
		long time = settings.getLong(PREF_TIME_PREFIX + callsign, (long) -1);
		boolean hasLocation = settings.getBoolean(PREF_HAS_LOCATION_PREFIX + callsign, false);
		
		if (hasLocation) {
			for (Target target : targets) {
				if (callsign.equals(target.getCallsign())) {
					target.setLocation(new TargetLocation(latitude, longitude, speed, altitude, time));
				}	
			}
		}
	}
	
    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        String one = settings.getString(PREF_TARGET_1, null);
        String two = settings.getString(PREF_TARGET_2, null);
        String three = settings.getString(PREF_TARGET_3, null);
        
        setTargetLocations(one, two, three);
        
        loadTargetLocation(settings, one);
        loadTargetLocation(settings, two);
        loadTargetLocation(settings, three);
    }
    
    private void putPreferencesDouble(SharedPreferences.Editor editor, String key, double num) {
    	editor.putString(key, String.valueOf(num));
    }
    
    private void saveTargetLocation(SharedPreferences.Editor editor, Target target) {
    	TargetLocation location = target.getLocation();
    	if (location != null) {
    		String callsign = target.getCallsign();
	    	putPreferencesDouble(editor, PREF_LATITUDE_PREFIX + callsign, location.getLatitude());
	    	putPreferencesDouble(editor, PREF_LONGITUDE_PREFIX + callsign, location.getLongitude());
	    	putPreferencesDouble(editor, PREF_ALTITUDE_PREFIX + callsign, location.getAltitude());
	    	editor.putFloat(PREF_SPEED_PREFIX + callsign, location.getSpeed());
	    	editor.putLong(PREF_TIME_PREFIX + callsign, location.getTime());
	    	editor.putBoolean(PREF_HAS_LOCATION_PREFIX + callsign, true);
    	}
    }
    
    private void savePreferences() {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.remove(PREF_TARGET_1);
    	editor.remove(PREF_TARGET_2);
    	editor.remove(PREF_TARGET_3);
    	
    	if (targets.size() >= 1) {
    		editor.putString(PREF_TARGET_1, targets.get(0).getCallsign());
    		saveTargetLocation(editor, targets.get(0));
    	}
    	if (targets.size() >= 2) {
    		editor.putString(PREF_TARGET_2, targets.get(1).getCallsign());
    		saveTargetLocation(editor, targets.get(1));
    	}
    	if (targets.size() >= 3) {
    		editor.putString(PREF_TARGET_3, targets.get(2).getCallsign());
    		saveTargetLocation(editor, targets.get(2));
    	}
    	editor.commit();
    }
    
    /**
     * Start the location tracking.
     */
    @Override
    protected void onResume() {
    	super.onResume();
    	Util.log("onResume()");
        
    	// Re-register all of the listeners.
        if (!locationManager.addGpsStatusListener(gpsStatusListener)) {
        	Util.error("Couldn't add Gps Status Listener!");
        }
        
        if (!locationManager.addNmeaListener(nmeaListener)) {
        	Util.error("Couldn't add Nmea Listener!");
        }
        
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        
        loadPreferences();
        
        // If we have saved values, we should update the UI.
        updateUI();
        
        startTimer();
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
    	
    	// Remover the listeners.
    	locationManager.removeGpsStatusListener(gpsStatusListener);
    	locationManager.removeNmeaListener(nmeaListener);
    	locationManager.removeUpdates(locationListener);
    	
    	savePreferences();
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
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
			GpsStatus status = locationManager.getGpsStatus(null);
			//Util.log("Max Satellites: " + status.getMaxSatellites());
			//Util.log("Time to First Fix: " + status.getTimeToFirstFix());
		}
    }
    
    private class MyNmeaListener implements GpsStatus.NmeaListener {
		public void onNmeaReceived(long timestamp, String nmea) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(timestamp);
			// Util.log(calendar.getTime().toString() + "] " + nmea);
		}
    }
    
	private void issueShortToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}	
}