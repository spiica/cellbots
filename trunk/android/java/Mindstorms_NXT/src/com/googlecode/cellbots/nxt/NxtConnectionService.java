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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing the lowlevel Bluetooth connection/control between
 * Android and the NXT brick.
 * 
 * @author Charles L. Chen (clchen@google.com)
 */
public class NxtConnectionService extends Service {
    private static final String TAG = "NxtConnectionService";

    private static final String NXT_NAME = "NXT";

    private BluetoothAdapter mBtAdapter;

    private BluetoothDevice mNxtBrick = null;

    private BluetoothSocket mBtSocket = null;

    private NxtConnectionService mSelf;

    public NxtConnectionService() {
        mSelf = this;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void startConnection() {
        mNxtBrick = null;

        Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice d : devices) {
            if (NXT_NAME.equals(d.getName())) {
                mNxtBrick = d;
                try {
                    // This specific UUID is needed for BT communication with
                    // the
                    // NXT brick to work! DO NOT CHANGE THIS!
                    mBtSocket = mNxtBrick.createRfcommSocketToServiceRecord(UUID
                            .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    mBtSocket.connect();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        if (mNxtBrick == null) {
            Log.e(TAG, "Unable to connect to NXT. Is it paired?");
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    private final INxtConnection.Stub mBinder = new INxtConnection.Stub() {

        @Override
        public int connect(String srcApp) {
            startConnection();
            return 0;
        }

        @Override
        public int getVersion() {
            PackageManager pm = mSelf.getPackageManager();
            try {
                return pm.getPackageInfo(mSelf.getPackageName(), 0).versionCode;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        public int readUltraSonicSensor(String srcApp, int port) {
            try {
                // Tell UltraSonic Sensor to read one byte
                byte[] writeLsCmd = new byte[8];
                writeLsCmd[0] = (byte) 0x80;
                writeLsCmd[1] = (byte) 0x0F;
                writeLsCmd[2] = (byte) port;
                writeLsCmd[3] = (byte) 0x02;
                writeLsCmd[4] = (byte) 0x01;
                writeLsCmd[5] = (byte) 0x02;
                writeLsCmd[6] = (byte) 0x42;
                mBtSocket.getOutputStream().write(writeLsCmd.length & 0xff);
                mBtSocket.getOutputStream().write((writeLsCmd.length >> 8) & 0xff);
                mBtSocket.getOutputStream().write(writeLsCmd);
                mBtSocket.getOutputStream().flush();

                // Read the result
                byte[] readLsCmd = new byte[3];
                readLsCmd[0] = (byte) 0x00;
                readLsCmd[1] = (byte) 0x10;
                readLsCmd[2] = (byte) port;
                mBtSocket.getOutputStream().write(readLsCmd.length & 0xff);
                mBtSocket.getOutputStream().write((readLsCmd.length >> 8) & 0xff);
                mBtSocket.getOutputStream().write(readLsCmd);
                mBtSocket.getOutputStream().flush();

                int bytesRead = -1;
                byte[] b = new byte[22];
                String message = "";
                bytesRead = mBtSocket.getInputStream().read(b);

                // This is the byte that holds the answer.
                // It is unsigned, so use & here to convert it.
                return (b[2 + 4]) & 0xFF;

            } catch (Exception e) {
                Log.e(TAG, "Error reading from ultrasonic sensor: " + e.getMessage());
            }
            return 0;
        }

        @Override
        public int setMotor(String srcApp, int motor, int power) {
            try {
                byte[] setCmd = new byte[13];
                setCmd[0] = (byte) 0x80;
                setCmd[1] = (byte) 0x04;
                setCmd[2] = (byte) motor;
                setCmd[3] = (byte) power;

                setCmd[4] = (byte) 0x01;

                setCmd[5] = (byte) 0x01;

                setCmd[6] = (byte) 0x00;

                setCmd[7] = (byte) 0x20;

                setCmd[8] = (byte) 0x00;
                setCmd[9] = (byte) 0x00;
                setCmd[10] = (byte) 0x00;
                setCmd[11] = (byte) 0x00;
                setCmd[12] = (byte) 0x00;

                mBtSocket.getOutputStream().write(setCmd.length & 0xff);
                mBtSocket.getOutputStream().write((setCmd.length >> 8) & 0xff);
                mBtSocket.getOutputStream().write(setCmd);
                mBtSocket.getOutputStream().flush();
            } catch (Exception e) {
                Log.e(TAG, "Error sending motor command: " + e.getMessage());
            }
            return 0;
        }

        @Override
        public int setUltraSonicSensor(String srcApp, int port) {
            try {
                // Ultrasonic sensor is considered a LOWSPEED_9V sensor
                byte[] setPortCmd = new byte[5];
                setPortCmd[0] = (byte) 0x80;
                setPortCmd[1] = (byte) 0x05;
                setPortCmd[2] = (byte) port;
                setPortCmd[3] = (byte) 0x0B;
                setPortCmd[4] = (byte) 0x00;
                mBtSocket.getOutputStream().write(setPortCmd.length & 0xff);
                mBtSocket.getOutputStream().write((setPortCmd.length >> 8) & 0xff);
                mBtSocket.getOutputStream().write(setPortCmd);
                mBtSocket.getOutputStream().flush();

                // Set ultrasonic sensor to continuous read mode
                byte[] writeLsCmd = new byte[8];
                writeLsCmd[0] = (byte) 0x80;
                writeLsCmd[1] = (byte) 0x0F;
                writeLsCmd[2] = (byte) port;
                writeLsCmd[3] = (byte) 0x03;
                writeLsCmd[4] = (byte) 0x00;
                writeLsCmd[5] = (byte) 0x02;
                writeLsCmd[6] = (byte) 0x41;
                writeLsCmd[7] = (byte) 0x02;
                mBtSocket.getOutputStream().write(writeLsCmd.length & 0xff);
                mBtSocket.getOutputStream().write((writeLsCmd.length >> 8) & 0xff);
                mBtSocket.getOutputStream().write(writeLsCmd);
                mBtSocket.getOutputStream().flush();
            } catch (Exception e) {
                Log.e(TAG, "Error setting ultrasonic sensor: " + e.getMessage());
            }
            return 0;
        }

        @Override
        public void shutdown(String srcApp) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    };

}
