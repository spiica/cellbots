package com.allthingsgeek.celljoust;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.ControllerState;
import com.cellbots.CellbotProtos.PhoneState;
import com.cellbots.sensors.CompassManager;
import com.cellbots.sensors.LightSensorManager;
import com.cellbots.sensors.OrientationManager;
import com.cellbots.sensors.SensorListener;
import com.google.protobuf.ByteString;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/*
 * This is a class to store the state of the robot.
 */
public class RobotStateHandler implements SensorListener
{
  private BTCommThread                          bTcomThread;

  private WifiManager                           wifi;

  private Handler                               uiHandler;

  private static RobotStateHandler              instance      = null;

  public boolean                                listening     = false;

  public static String                          TAG           = "RobotStateHandler";

  public static String                          ROBOT_ID      = "Pokey";

  private CellbotProtos.PhoneState.Builder      state;

  private SensorSender                          sensorSender;

  private Movement                              mover;

  HttpClient                                    httpclient;

  InetSocketAddress                             clientAddress = null;

  private CellbotProtos.AudioVideoFrame.Builder avFrame;

  private RobotStateHandler(Handler h) throws IOException
  {
    uiHandler = h;

    state = CellbotProtos.PhoneState.newBuilder();
    state.setTimestamp(System.currentTimeMillis());
    avFrame = CellbotProtos.AudioVideoFrame.newBuilder();
    avFrame.setFrameNumber(0);
    Random generator = new Random(System.currentTimeMillis());
    ROBOT_ID = Integer.toHexString(generator.nextInt()).toUpperCase();

    mover = Movement.getInstance();
  }

  public static RobotStateHandler getInstance(Handler h) throws IOException
  {
    if (instance == null)
    {
      instance = new RobotStateHandler(h);
    }
    return instance;
  }

  private Handler handler = new Handler()
                          {

                            @Override
                            public void handleMessage(Message msg)
                            {
                              state = state.mergeFrom((com.google.protobuf.Message) msg.obj);
                            }

                          };

  public CellbotProtos.AudioVideoFrame getVideoFrame()
  {

    CellbotProtos.AudioVideoFrame av = avFrame.build();
    // state = CellbotProtos.PhoneState.newBuilder();
    return av;
  }

  public void setVideoFrame(ByteString frame)
  {
    avFrame.setData(frame);
    avFrame.setTimestamp(System.currentTimeMillis());
    avFrame.setEncoding(CellbotProtos.AudioVideoFrame.Encoding.JPEG);
    avFrame.setFrameNumber(avFrame.getFrameNumber() + 1);
  }

  synchronized public CellbotProtos.PhoneState getStateAndReset()
  {
    state.setTimestamp(System.currentTimeMillis());
    CellbotProtos.PhoneState s = state.build();
    state = CellbotProtos.PhoneState.newBuilder();
    return s;
  }

  public void onBtDataRecive(String data)
  {
    /*
     * Log.i(TAG, "got bt data:" + data);
     * 
     * //state.blueToothConnected = true; Date date = new Date();
     * //state.lastBtTimestamp = date.getTime();
     * 
     * if(data.startsWith("L")) { //state.message += data; } else {
     * 
     * String[] botData = data.split(" ");
     * 
     * try {
     * 
     * state.botBatteryLevel = Integer.parseInt(botData[0]); state.damage =
     * Integer.parseInt(botData[1]); state.servoSpeed =
     * Integer.parseInt(botData[2]); state.strideOffset =
     * Integer.parseInt(botData[3]); state.turretAzimuth =
     * Integer.parseInt(botData[4]); state.turretElevation =
     * Integer.parseInt(botData[5]); state.sonarDistance =
     * Integer.parseInt(botData[6]); state.irDistance =
     * Integer.parseInt(botData[7]); state.lampOn = Integer.parseInt(botData[8])
     * == 1; state.laserOn = Integer.parseInt(botData[9]) == 1; state.rGunOn =
     * Integer.parseInt(botData[10]) == 1; state.lGunOn =
     * Integer.parseInt(botData[11]) == 1; state.moving =
     * Integer.parseInt(botData[12]) == 1;
     * 
     * } catch(Exception e) { Log.e(TAG, "Error parsing robot data: " + data +
     * " e:",e); } }
     */
  }

  public void onBtDataError()
  {
    /*
     * if(state.blueToothConnected) { Message say = uiHandler.obtainMessage();
     * say.obj = "Danger! Danger! Bluetooth error!"; say.sendToTarget(); }
     * state.blueToothConnected = false;
     */
  }

  public BroadcastReceiver mBatInfoReceiver  = new BroadcastReceiver()
                                             {
                                               public void onReceive(Context arg0, Intent intent)
                                               {
                                                 //state.setPhoneBatteryLevel(intent.getIntExtra("level", 0));
                                                 //state.setPhoneBatteryTemp(intent.getIntExtra("temperature", 0));
                                               }
                                             };

  public BroadcastReceiver mWifiInfoReceiver = new BroadcastReceiver()
                                             {

                                               @Override
                                               public void onReceive(Context context, Intent intent)
                                               {
                                                 WifiInfo info = wifi.getConnectionInfo();

                                                 // state.setWifiStrength(info.getRssi());

                                                 // state.setWifiSpeed(info.getLinkSpeed());

                                               }

                                             };

  /**
   * onShake callback
   */
  public void onShake(float force)
  {
    // Toast.makeText(this, "Phone shaked : " + force, 1000).show();
  }

  /**
   * onAccelerationChanged callback
   */
  public void onAccelerationChanged(float x, float y, float z)
  {

    CellbotProtos.PhoneState.Accelerometer.Builder b = CellbotProtos.PhoneState.Accelerometer.newBuilder();

    b.setX(x);
    b.setY(y);
    b.setZ(z);

  }

  /**
   * onCompassChanged callback
   */
  public void onCompassChanged(float x, float y, float z)
  {
    CellbotProtos.PhoneState.Compass.Builder b = CellbotProtos.PhoneState.Compass.newBuilder();

    b.setX(x);
    b.setY(y);
    b.setZ(z);

  }

  /**
   * onLightLevelChanged callback
   */
  public void onLightLevelChanged(float level)
  {
    state.setLightLevel(level);
  }

  public void onOrientationChanged(float azimuth, float pitch, float roll)
  {

    CellbotProtos.PhoneState.Orientation.Builder b = CellbotProtos.PhoneState.Orientation.newBuilder();

    b.setAzimuth(azimuth);
    b.setPitch(pitch);
    b.setRoll(roll);

    state.setOrientation(b);

  }

  public void onBottomUp()
  {
    // Toast.makeText(this, "Bottom UP", 1000).show();
  }

  public void onLeftUp()
  {
    // Toast.makeText(this, "Left UP", 1000).show();
  }

  public void onRightUp()
  {
    // / Toast.makeText(this, "Right UP", 1000).show();
  }

  public void onTopUp()
  {
    // / Toast.makeText(this, "Top UP", 1000).show();
  }

  public String getLocalIpAddress()
  {
    try
    {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
      {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
        {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress())
          {
            return inetAddress.getHostAddress().toString();
          }
        }
      }
    }
    catch (SocketException ex)
    {
      // Log.e(LOG_TAG, ex.toString());
    }
    return null;
  }

  public synchronized void startListening(ProgressDialog btDialog, WifiManager wifi)
  {
    // Log.d("RobotStateHandler","startListening");

    this.wifi = wifi;

    httpclient = new DefaultHttpClient();
    if (!listening)
    {

      this.listening = true;

      this.sensorSender = new SensorSender();
      
      sensorSender.start();

      try
      {
        // this.start();

        if (OrientationManager.isSupported())
        {
          OrientationManager.startListening(this);
        }

        if (LightSensorManager.isSupported())
        {
          LightSensorManager.startListening(this);
        }

        if (CompassManager.isSupported())
        {
          CompassManager.startListening(this);
        }

        // server.start();
        /*
         * 
         * if(bTcomThread == null) { bTcomThread = new
         * BTCommThread(BluetoothAdapter.getDefaultAdapter(), this);
         * bTcomThread.start(); }
         */
      }
      catch (java.lang.IllegalThreadStateException e)
      {
        Log.e(TAG, "Robot state handler thead start error", e);
      }

    }

  }

  public synchronized void stopListening()
  {

    Log.e(TAG, "Robot state handler STOPING ALL LISTINERS");

    /*
     * if (OrientationManager.isListening()) {
     * OrientationManager.stopListening(); }
     * 
     * if (CompassManager.isListening()) { CompassManager.stopListening(); }
     * 
     * if (LightSensorManager.isListening()) {
     * LightSensorManager.stopListening(); }
     * 
     * 
     * //watch out for double start
     * 
     * if (bTcomThread != null) { try { bTcomThread.handler.getLooper().quit();
     * } catch(Exception e) {}
     * 
     * bTcomThread.disconnect(); }
     */
    this.listening = false;

  }

  class SensorSender extends Thread
  {

    private boolean listening = true;

    public void run()
    {
      setName("Robot State Handler");

      while (this.listening)
      {
        try
        {
          HttpPost post = new HttpPost(MainActivity.putUrl + "/robotState");

          state.setTimestamp(System.currentTimeMillis());

          post.setEntity(new ByteArrayEntity(state.build().toByteArray()));
          
          state = PhoneState.newBuilder();

          HttpResponse resp = httpclient.execute(post);

          InputStream resStream = resp.getEntity().getContent();

          ControllerState cs = ControllerState.parseFrom(resStream);

          mover.processControllerStateEvent(cs);
        }
        catch (UnsupportedEncodingException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        catch (IllegalStateException e)
        {
          e.printStackTrace();
        }
        catch (com.google.protobuf.InvalidProtocolBufferException e)
        {
          // e.printStackTrace();
          // resetConnection();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }

        /*
         * if (clientConnection != null) {
         * 
         * if (bTcomThread != null && controllerState != null &&
         * controllerState.timestamp != lastControllerTimeStamp) {
         * if(controllerState.R3) { //toggle autoaim state.autoAimOn = !
         * state.autoAimOn; }
         * 
         * lastControllerTimeStamp = controllerState.timestamp; Message btMsg =
         * bTcomThread.handler.obtainMessage(); btMsg.obj = controllerState;
         * btMsg.sendToTarget(); } //else //{ // Message btMsg =
         * bTcomThread.handler.obtainMessage(); //btMsg.obj = "K";
         * //btMsg.sendToTarget(); //} clientConnection.sendTCP(state);
         * state.message = "";
         * 
         * if (targetBlob != null && targetBlob.timestamp !=
         * lastTargetBlobTimeStamp) { lastTargetBlobTimeStamp =
         * controllerState.timestamp;
         * 
         * targetBlob.calculateAimpoints(targetSettings);
         * 
         * clientConnection.sendTCP(targetBlob);
         * 
         * //autoaim the head if needed if(bTcomThread != null &&
         * state.autoAimOn) { Message btMsg =
         * bTcomThread.handler.obtainMessage(); btMsg.obj = targetBlob;
         * btMsg.sendToTarget(); }
         * 
         * targetBlob = null; } }
         */
        try
        {
          // Log.d(TAG, "Sleeping");
          Thread.sleep(50);
        }

        catch (InterruptedException e)
        {
          // TODO Auto-generated catch block
          // listening = false;
        }
      }
    }
  }
}
