package com.cellbots.sensors;

import android.hardware.SensorManager;

  public interface SensorListener {
    
    //public SensorManager getSensorManager();
    
    public void onOrientationChanged(float azimuth, 
            float pitch, float roll);
 
    public void onCompassChanged(float x, 
        float y, float z);
    

    public void onLightLevelChanged(float level);
 
    public void onAccelerationChanged(float x, float y, float z);

    public void onShake(float force);
}
