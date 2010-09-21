package com.cellbots.sensors;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.cellbots.CellbotProtos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
  public class SensorListenerImpl implements SensorListener {
    private CellbotProtos.PhoneState.Builder      state;
    private WifiManager                           wifi;
    //public SensorManager getSensorManager();
    
    public SensorListenerImpl()
    {
      state = CellbotProtos.PhoneState.newBuilder();
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
      state.setAccelerometer(b);

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
      state.setCompass(b);
    }

    
   

    public void onOrientationChanged(float azimuth, float pitch, float roll)
    {
      CellbotProtos.PhoneState.Orientation.Builder b = CellbotProtos.PhoneState.Orientation.newBuilder();

      b.setAzimuth(azimuth);
      b.setPitch(pitch);
      b.setRoll(roll);

      state.setOrientation(b);

    }


  public BroadcastReceiver mBatInfoReceiver  = new BroadcastReceiver()
                                             {
                                               public void onReceive(Context arg0, Intent intent)
                                               {
                                                  state.setPhoneBatteryLevel(intent.getIntExtra("level", 0));
                                                  state.setPhoneBatteryTemp(intent.getIntExtra("temperature", 0));
                                               }
                                             };

  public BroadcastReceiver mWifiInfoReceiver = new BroadcastReceiver()
                                             {



                                              @Override
                                               public void onReceive(Context context, Intent intent)
                                               {
                                                 WifiInfo info = wifi.getConnectionInfo();

                                                 //state.setWifiStrength(info.getRssi());

                                                 //state.setWifiSpeed(info.getLinkSpeed());

                                               }

                                             };

  public void onLightLevelChanged(float level)
  {
      state.setLightLevel(level);
  }
  
  public CellbotProtos.PhoneState getPhoneState()
  {
    return state.build();
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
}
