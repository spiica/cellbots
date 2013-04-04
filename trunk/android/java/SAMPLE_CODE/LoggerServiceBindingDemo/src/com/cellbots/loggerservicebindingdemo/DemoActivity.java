package com.cellbots.loggerservicebindingdemo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.cellbots.logger.localServer.ILoggingService;

/**
 * Simple example showing how to bind to the Logging Service.
 * This will connect to the logging service and generate one trivial entry of
 * random data.
 */
public class DemoActivity extends Activity {
    
    private ILoggingService mLoggingService;
    private ServiceConnection mServiceConnection = null;
    private boolean mKeepGoing;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mKeepGoing = true;
        connectToService();
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        mKeepGoing = false;
        unbindService(mServiceConnection);
    }
    
    
    public void connectToService(){
        if (mLoggingService == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mLoggingService = ILoggingService.Stub.asInterface(service);
                    try {
                        mLoggingService.addLogEntry("My Random Sensor", Math.random() + "");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mLoggingService = null;
                }
            };
            final Intent serviceIntent = new Intent("com.cellbots.logger.USE_LOGGING_SERVICE");
            serviceIntent.addCategory("android.intent.category.LOGGING_SERVICE");
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

}
