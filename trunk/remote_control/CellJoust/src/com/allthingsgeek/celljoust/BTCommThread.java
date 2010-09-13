/*
 * Copyright 2010 Google Inc.
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
 * the License
 */

package com.allthingsgeek.celljoust;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

class BTCommThread extends Thread
{
  private BluetoothSocket  socket;

  private InputStream      istream;

  private OutputStream     ostream;

  private BluetoothAdapter adapter;

  StringBuffer             sb;

  byte[]                   prevBuffer;                         // buffer store
                                                                // for

  // the stream
  BluetoothDevice          device;

  int                      bytes;                              // bytes
                                                                // returned

  // from read()

  RobotStateHandler        state;

  public Handler           handler;

  public StringBuffer      readBuffer;

  public static String     TAG                = "BtCommThread";

  private Boolean          lastMsgWasAllZeros = false;

  public BTCommThread(BluetoothAdapter adapter, RobotStateHandler rState)
  {
    this.adapter = adapter;
    this.state = rState;
    setName("BlueTooth Com");

    sb = new StringBuffer();

    prevBuffer = new byte[128];

    readBuffer = new StringBuffer();

    if (adapter == null)
      return;

    Set<BluetoothDevice> devices = adapter.getBondedDevices();
    for (BluetoothDevice curDevice : devices)
    {
      if (curDevice.getName().matches(".*BlueRobot.*"))
      {
        device = curDevice;
        break;
      }
    }
    if (device == null)
      device = adapter.getRemoteDevice("00:06:66:03:A9:A2");

  }

  synchronized private void connect()
  {

    try
    {
      
      adapter.cancelDiscovery(); 
      socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
      socket.connect();
    }
    catch (IOException e)
    {
      socket = null;
      return;
    }

    InputStream tmpIn = null;
    OutputStream tmpOut = null;

    try
    {
      tmpIn = socket.getInputStream();
      tmpOut = socket.getOutputStream();
    }
    catch (IOException e)
    {
      disconnect();
      return;
      
    }

    istream = tmpIn;
    ostream = tmpOut;

  }

  public boolean connected()
  {
    return this.ostream != null;
  }

  public void run()
  {

    connect();

    Looper.prepare();

    handler = new Handler()
    {

      @Override
      public void handleMessage(Message msg)
      {

        if (connected())
        {
          
          /*

          if (msg.obj instanceof ControllerState)
          {
            ControllerState cs = ( (ControllerState) msg.obj );

              Log.d(TAG, "Handeling Message" + msg.obj);
              byte[] bytes = cs.toBytes();
              write(bytes);

          }

          if (msg.obj instanceof TargetBlob)
          {
            byte[] bytes = ( (TargetBlob) msg.obj ).toBytes();

            write(bytes);
          }
*/
          
          read();
        }
        else
        {
          connect();
        }
      }
    };

    Looper.loop();

    disconnect();

  }

  private void write(byte[] bytes)
  {
    if (ostream != null)
    {
      try
      {
        Log.d(TAG, "Writing bytes :" + bytes.length);
        ostream.write(bytes);
        prevBuffer = bytes;
      }
      catch (IOException e)
      {
        Log.e(TAG, "Error duing write", e);

        this.disconnect();

        state.onBtDataError();
        // java.io.IOException: Transport endpoint is not connected

      }
    }

  }

  private void read()
  {
    if (istream != null)
    {
      try
      {
        int inChar;
        while (istream.available() > 0)
        {

          inChar = istream.read();

          if (inChar != 13 && inChar != 10)// do not write carriage returns or
                                           // newlines to the buffer
          {
            readBuffer.append((char) inChar);
          }

          if (inChar == 10)// look for newlines
          {
            String tmp = readBuffer.toString();
            readBuffer.delete(0, readBuffer.length());
            state.onBtDataRecive(tmp);
          }
        }
      }

      catch (Exception e)
      {
        Log.e(TAG, "exception during read", e);
        this.disconnect();
        state.onBtDataError();
      }
    }
  }

  public void disconnect()
  {
    Log.i(TAG, "quit callled");

    try
    {
      istream.close();
    }
    catch (Exception e)
    {
    }

    try
    {
      ostream.close();
    }
    catch (Exception e)
    {
    }

    try
    {
      socket.close();
    }
    catch (Exception e)
    {
      // Log.e(TAG, "exception closing socket", e);
    }

    istream = null;
    ostream = null;
    socket = null;

  }

}
