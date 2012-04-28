package com.tgnourse.aprs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class APRSEncoderActivity extends Activity {
	
	private static final String TAG = "APRSEncoderActivity";
	
	/**
	 * Screen lock.
	 */
	private PowerManager.WakeLock screenLock;
	
	/**
	 * The intent we use to start and stop our service.
	 */
	private Intent serviceIntent;
	
	private void startTransmitting() {
		Util.log("startTransmitting()");
		serviceIntent = new Intent(this, APRSEncoderService.class);
		if (startService(serviceIntent) == null) {
			Util.log("Service cannot be found!");
		}
	}
	
	private void stopTransmitting() {
		Util.log("stopTransmitting()");
		stopService(serviceIntent);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.log("onCreate()");
        setContentView(R.layout.main);
        
        // Set up the toggle button.
        final ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener () {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startTransmitting();
				} else {
					stopTransmitting();
				}
				// TODO(tgnourse): Need to store this state to disk or look up the state of the service on start up.
			}
        });
    }
    
    public void onResume() {
    	super.onResume();
    	Util.log("onResume()");
        
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        screenLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "APRSEncoder");
        screenLock.acquire();
    }
    
    public void onPause() {
    	super.onPause();
    	Util.log("onPause()");
    	
    	// Release the screen lock.
    	screenLock.release();
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	Util.log("onDestroy()");
    }
    

}