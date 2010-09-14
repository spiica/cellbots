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

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

/**
 * This class connects to the robot via Bluetooth and sends allows sending commands
 * to it.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 * 
 */
public class RobotBtController {
    
    private static final String TAG = "BluetoothClient";
        
    private String mDeviceName = "";
    
    private BluetoothAdapter mBtAdapter;

    private BluetoothDevice mBtDevice = null;

    private BluetoothSocket mBtSocket = null;
    
    /**
     * Creates an instance of RobotBtController which can be used to send commands to the
     * robot via Bluetooth.
     * @param robotName The name of the robot as its GMail account.
     * @param robotBt The name of the robot's BT device.
     */
    public RobotBtController(String robotName, String robotBt) {
        mDeviceName = robotBt;
        if (mDeviceName == null)
            return;
        Looper.prepare();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        startConnection();
    }
    
    /**
     * Appends newline to the specified command string and sends it to the robot via Bluetooth. 
     * @param cmd
     */
    public void write(String cmd) {
        if (mBtDevice == null) {
            Log.e(TAG, "Cannot write. Not connected to the robot.");
            return;
        }
        try {
            mBtSocket.getOutputStream().write((cmd + "\n").getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error sending data to BT device: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        try {
            if (mBtSocket != null) {
                mBtSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }    
    }
    
    public boolean isConnected() {
        return mBtDevice != null;
    }
    
    /**
     * Opens a connection with the Bluetooth device specified by |mDeviceName|. The phone should
     * first be paired with the Bluetooth device.
     */
    private void startConnection() {
        mBtDevice = null;
        Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice d : devices) {
            if (mDeviceName.equals(d.getName())) {
                mBtDevice = d;
                try {
                    mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(UUID
                            .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    mBtSocket.connect();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    mBtDevice = null;
                }
            }
        }
        if (mBtDevice == null) {
            Log.e(TAG, "Unable to connect to robot's Bluetooth. Is it paired?");
        }
    }
}
