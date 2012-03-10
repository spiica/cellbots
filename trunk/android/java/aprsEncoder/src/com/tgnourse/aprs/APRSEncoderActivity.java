package com.tgnourse.aprs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.TextView;

public class APRSEncoderActivity extends Activity {
	
	/**
	 * Our thread that transmits the data over APRS.
	 */
	private SensorDataRunnable transmitRunnable;
	
	/**
	 * Screen lock.
	 */
	private PowerManager.WakeLock screenLock;
	
	/**
	 * Tracks the most recent readings from all of the sensors.
	 */
	private SensorDataCollector data;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.log("onCreate()");
        setContentView(R.layout.main);
        
        // Create the data collector.
        data = new SensorDataCollector(
        		(LocationManager) getSystemService(LOCATION_SERVICE), 
        		(SensorManager) getSystemService(Context.SENSOR_SERVICE));
        
        // Create the transmit thread.
        TextView frame = (TextView) findViewById(R.id.frame);
        TextView time = (TextView) findViewById(R.id.time);
        TextView aprs = (TextView) findViewById(R.id.aprs);
        transmitRunnable = new SensorDataRunnable(new Handler(), frame, time, aprs, data);
    }
    
    public void onResume() {
    	super.onResume();
    	Util.log("onResume()");
    	
    	// Start listening for sensor updates.
    	data.registerListeners();
        
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        screenLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DiveTracker");
        screenLock.acquire();
    	
    	// Start the transmitter.
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(transmitRunnable);
    }
    
    public void onPause() {
    	super.onPause();
    	Util.log("onPause()");
    	
    	// Stop transmitting data.
    	if (transmitRunnable.playing()) {
    		transmitRunnable.stop();
        }
    	
    	// Stop listening for sensor updates.
    	data.removeListeners();
    	
    	// Release the screen lock.
    	screenLock.release();
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	Util.log("onDestroy()");
    }
    

}