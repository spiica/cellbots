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
package com.googlecode.cellbots.nxt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Wrapper library to simplify the use of the NxtConnectionService.
 *
 * @author Charles L. Chen (clchen@google.com)
 */
public class NxtConnection {
    public interface OnNxtConnectionReadyListener {
        public void OnNxtConnectionReady();
    }

    private static final String TAG = "NxtConnection";

    public static final int SUCCESS = 0;

    public static final int FAILURE = 1;

    private ServiceConnection mServiceConnection;

    private OnNxtConnectionReadyListener mOnNxtConnectionReadyListener;

    private Context mContext;

    private String mPackageName = "";

    private INxtConnection mINxtConnection;

    private boolean mStarted = false;

    private final Object mStartLock = new Object();

    public NxtConnection(Context ctx, OnNxtConnectionReadyListener callback) {
        mContext = ctx;
        mOnNxtConnectionReadyListener = callback;
        mPackageName = ctx.getPackageName();
        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                synchronized (mStartLock) {
                    mINxtConnection = INxtConnection.Stub.asInterface(service);
                    mStarted = true;
                    if (mOnNxtConnectionReadyListener != null) {
                        mOnNxtConnectionReadyListener.OnNxtConnectionReady();
                    }
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                synchronized (mStartLock) {
                    try {
                        mStarted = false;
                        mINxtConnection.shutdown(mPackageName);
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException in onServiceDisconnected", e);
                    }
                    mINxtConnection = null;
                }
            }
        };

        Intent intent = new Intent("com.googlecode.cellbots.nxt.intent.action.NxtConnection_SERVICE");
        intent.addCategory("com.googlecode.cellbots.nxt.intent.category.NxtConnection");
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public int connect() {
        if (!mStarted) {
            return NxtConnection.FAILURE;
        }
        try {
            return mINxtConnection.connect(mPackageName);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return NxtConnection.FAILURE;
        }
    }

    public int setMotor(int motor, int power) {
        if (!mStarted) {
            return NxtConnection.FAILURE;
        }
        try {
            return mINxtConnection.setMotor(mPackageName, motor, power);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return NxtConnection.FAILURE;
        }
    }

    public int setUltraSonicSensor(int port) {
        if (!mStarted) {
            return NxtConnection.FAILURE;
        }
        try {
            return mINxtConnection.setUltraSonicSensor(mPackageName, port);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return NxtConnection.FAILURE;
        }
    }

    public int readUltraSonicSensor(int port) {
        if (!mStarted) {
            return NxtConnection.FAILURE;
        }
        try {
            return mINxtConnection.readUltraSonicSensor(mPackageName, port);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return NxtConnection.FAILURE;
        }
    }

    public int getVersion() {
        if (!mStarted) {
            return -1;
        }
        try {
            return mINxtConnection.getVersion();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }

    public void shutdown() {
        if (mStarted) {
            try {
                mINxtConnection.shutdown(mPackageName);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
