package com.tgnourse.aprs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class APRSEncoderService extends Service {
	
	private static final String TAG = "APRSEncoderService";
	
	/**
	 * Our thread that transmits the data over APRS.
	 */
	private SensorDataRunnable transmitRunnable;
	
	/**
	 * Tracks the most recent readings from all of the sensors.
	 */
	private SensorDataCollector data;

	private void statusToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onCreate() {
		Util.log("onCreate()");
		super.onCreate();
		
        // Create the data collector.
        data = new SensorDataCollector(
        		(LocationManager) getSystemService(LOCATION_SERVICE), 
        		(SensorManager) getSystemService(Context.SENSOR_SERVICE));
        
        // Create the transmit thread.
        /*TextView frame = (TextView) findViewById(R.id.frame);
        TextView time = (TextView) findViewById(R.id.time);
        TextView aprs = (TextView) findViewById(R.id.aprs);*/
        transmitRunnable = new SensorDataRunnable(new Handler(), null, null, null, data);
	}

	@Override
	public void onDestroy() {
		Util.log("onDestroy()");
		super.onDestroy();
		stopTransmitting();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Util.log("onStartCommand()");
		startTransmitting();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void startTransmitting() {
		statusToast("Started transmitting!");
		
    	// Start listening for sensor updates.
    	data.registerListeners();
		
    	// Start the transmitter.
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(transmitRunnable);
	}
	
	private void stopTransmitting() {
		statusToast("Stopped transmitting!");
    	// Stop transmitting data.
    	if (transmitRunnable.playing()) {
    		transmitRunnable.stop();
        }
    	
    	// Stop listening for sensor updates.
    	data.removeListeners();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Util.log("onUnbind()");
		return super.onUnbind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Util.log("onBind()");
		return null;
	}

}
