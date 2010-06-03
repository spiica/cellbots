package com.cellbots;

/*
Robot control console.
Copyright (C) 2010 Darrell Taylor & Eric Hokanson

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class PulseGenerator implements Runnable
{
  public static int MIN_PULSE_WIDTH = 18;
  
  public static int MAX_PULSE_WIDTH = 48;

  
  private boolean playing = false;

  private int     lPulseWidth = 33;
  
  private int     rPulseWidth = 33;

  private int     pulseInterval = 441;

  private int        sampleRate = 22050;

  private AudioTrack noiseAudioTrack;

  private int        bufferlength;
  
  private boolean inverted = true;

  private short[]    audioBuffer;
  private short[]    leftChannelBuffer;
  private short[]    rightChannelBuffer;

  public PulseGenerator()
  {

    bufferlength = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);

    //bufferlength = sampleRate;

    noiseAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferlength, AudioTrack.MODE_STREAM);

    sampleRate = noiseAudioTrack.getSampleRate();

    Log.i("Noise Setup", "BufferLength = " + Integer.toString(bufferlength));
    Log.i("Noise Setup", "Sample Rate = " + Integer.toString(sampleRate));

    audioBuffer = new short[bufferlength];
    leftChannelBuffer = new short[bufferlength/2];
    rightChannelBuffer = new short[bufferlength/2];

    noiseAudioTrack.play();
  }

  
  private void generatePCM(int pulseWidth, int pulseInterval, short buffer[])
  {
    int inverter = 1;
    
    if (inverted)
    {
      inverter = -1;
    }

    for (int i = 0; i < buffer.length; i++)
    {
      int j = 0;
      while (j < pulseWidth && i < buffer.length)
      {
        //we have to modulate the signal a bit because the sound card freaks out if it goes dc
        buffer[i] = (short) ( (30000*inverter) + ( ( i % 2 ) * 100 ) );
        i++;
        j++;
      }
      while (j < pulseInterval && i < buffer.length)
      {
        buffer[i] = (short) ( (-30000*inverter) + ( ( i % 2 ) * 100 ) );
        i++;
        j++;
      }
      
    } 
  }
  
  
  public void run()
  {

    while (true)
    {
     
      if (playing)
      {
         generatePCM(lPulseWidth, pulseInterval, leftChannelBuffer);
         generatePCM(rPulseWidth, pulseInterval, rightChannelBuffer);
         for (int i = 0; i < bufferlength; i+=2)
         {
             audioBuffer[i] = leftChannelBuffer[i/2];
             audioBuffer[i+1] = rightChannelBuffer[i/2];
         }
         
      }
      else
      {
        for (int i = 0; i < bufferlength; i++)
        {
            audioBuffer[i] = (short) ( 0);
        }
      }
     
      noiseAudioTrack.write(audioBuffer, 0, bufferlength);
    }
  }

  public void stop()
  {
    noiseAudioTrack.stop();
    noiseAudioTrack.release();
  }

  public void togglePlayback()
  {
    playing = !playing;
  }

  public void toggleInverted()
  {
    inverted = !inverted;
  }
  
  public boolean isPlaying()
  {
    return playing;
  }

  public void setLeftPulsePercent(int percent)
  {
    this.lPulseWidth = MIN_PULSE_WIDTH + ((percent * (MAX_PULSE_WIDTH - MIN_PULSE_WIDTH))/ 100 );
  }
  
  public int getLeftPulsePercent()
  {
    return ((lPulseWidth - MIN_PULSE_WIDTH)/(MAX_PULSE_WIDTH-MIN_PULSE_WIDTH)) *100;
  }
  
  public float getLeftPulseMs()
  {
    return ((float)lPulseWidth / sampleRate) * 1000;
  }
  
  public int getRightPulseSamples()
  {
    return rPulseWidth;
  }
  
  
  public int getLeftPulseSamples()
  {
    return lPulseWidth;
  }
  
  public void setRightPulsePercent(int percent)
  {
    this.rPulseWidth = MIN_PULSE_WIDTH + ((percent * (MAX_PULSE_WIDTH - MIN_PULSE_WIDTH) )/ 100 );
  }

  public float getRightPulseMs()
  {
    return ((float)rPulseWidth / sampleRate) * 1000;
  }

  public int getRightPulsePercent()
  {
    return ((rPulseWidth - MIN_PULSE_WIDTH)/(MAX_PULSE_WIDTH-MIN_PULSE_WIDTH)) *100;
  }

}