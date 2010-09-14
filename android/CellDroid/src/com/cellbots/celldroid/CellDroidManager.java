/*
 * Copyright (C) 2010 Google Inc.
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

package com.cellbots.celldroid;

import com.cellbots.celldroid.ICellDroidService;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Helps manage connection to the CellDriod service.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CellDroidManager {
    
    private static final String TAG = "CellDroidManager";
    
    private ServiceConnection mServiceConnection = null;
    
    private final ComponentName mServiceComponent;
    
    private ICellDroidService mCellDroidService = null;
    
    private Context mContext;
    
    private final Lock serviceStartLock = new ReentrantLock();

    private final Condition serviceStarted  = serviceStartLock.newCondition();
    
    public CellDroidManager(Context ct) {
        this(ct, null);
    }
    
    public CellDroidManager(Context ct, ComponentName component) {
        mContext = ct;
        mServiceComponent = component;
        startCellDroidService();
    }
    
    private void startCellDroidService() {
        if (mCellDroidService == null) {
            mServiceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mCellDroidService = ICellDroidService.Stub.asInterface(service);
                    serviceStartLock.lock();
                    serviceStarted.signal();
                    serviceStartLock.unlock();
                }

                public void onServiceDisconnected(ComponentName name) {
                    mCellDroidService = null;
                    Log.d(TAG, "Service disconnected...");
                }
            };
            Intent serviceIntent = new Intent("android.intent.action.USE_CELLDROID_SERVICE");
            serviceIntent.addCategory("android.intent.category.CELLDROID_SERVICE");

            // Use explicit component when requested
            if (mCellDroidService != null) {
                serviceIntent.setComponent(mServiceComponent);
            }
            mContext.bindService(serviceIntent, mServiceConnection, Service.BIND_AUTO_CREATE);
        }
    }
    
    /**
     * Connects to the service by the specified username and password. Call startCellDroidService()
     * before this.
     * 
     * @param username
     * @param password
     */
    public void connect(final String username, final String password, final String cmdUrl,
            final String robotBt) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Wait to connect to the service for 5 seconds.
                for (int i = 0; i < 50; i++) {
                    try {
                        if (mCellDroidService != null) {
                            mCellDroidService.connect(username, password, cmdUrl, robotBt);
                            break;
                        } else
                            Thread.sleep(100);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error connecting to CellDroid service: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Connection to CellDroid service interrupted: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    
    public void disconnect() {
        try {
            if (mCellDroidService != null)
                mCellDroidService.disconnect();
        } catch (RemoteException e) {
            Log.e(TAG, "Error disconnecting from CellDroid service: " + e.getMessage());
        }
    }

    /**
     * Stops the CellDroid service. Make sure to call this in your Activity's onDestroy().
     */
    public void stopCellDroidService() {
        disconnect();
        mContext.unbindService(mServiceConnection);
    }
}
