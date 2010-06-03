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

import com.cellbots.R;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ServoTester extends Activity implements OnSeekBarChangeListener
{

  private LinearLayout main;

  PulseGenerator       noise;

  GestureDetector      nGestures;

  SeekBar              lPulseBar;

  SeekBar              rPulseBar;

  TextView             rPulseText;

  Thread               noiseThread;

  TextView             lPulseText;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);
    
    // set up the noise thread
    noise = new PulseGenerator();
    noiseThread = new Thread(noise);

    lPulseBar = (SeekBar) findViewById(R.id.LeftServo);
    lPulseBar.setProgress(noise.getLeftPulsePercent()); 
    lPulseBar.setOnSeekBarChangeListener(this);
    lPulseText = (TextView) findViewById(R.id.LeftServoValue);
    lPulseText.setText("Left Pulse width =" + noise.getLeftPulsePercent());
    
    rPulseBar = (SeekBar) findViewById(R.id.RightServo);
    rPulseBar.setProgress(noise.getRightPulsePercent()); 
    rPulseBar.setOnSeekBarChangeListener(this);
    rPulseText = (TextView) findViewById(R.id.RightServoValue);
    rPulseText.setText("Right Pulse width =" + noise.getRightPulsePercent());

  }

  @Override
  protected void onStart()
  {
    noiseThread.start();
    super.onStart();
  }

  @Override
  protected void onPause()
  {
    noise.stop();
    // TODO Auto-generated method stub
    super.onPause();
  }

  public void onToggleSound(View v)
  {
    noise.togglePlayback();
  }

  public void onToggleInvert(View v)
  {
    noise.toggleInverted();
  }

  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
  {
    if (seekBar.getId() == lPulseBar.getId())
    {
      noise.setLeftPulsePercent(progress);
      lPulseText.setText("Left Pulse width = " + noise.getLeftPulseMs() + "ms (" + noise.getLeftPulseSamples() +" samples)");
    }
    
    if (seekBar.getId() == rPulseBar.getId())
    {
      noise.setRightPulsePercent(progress);
      rPulseText.setText("Right Pulse width = " + noise.getRightPulseMs() + "ms (" + noise.getRightPulseSamples() +" samples)");
    }

  }

  public void onStartTrackingTouch(SeekBar seekBar)
  {
    // mTrackingText.setText(getString(R.string.seekbar_tracking_on));
  }

  public void onStopTrackingTouch(SeekBar seekBar)
  {
    // mTrackingText.setText(getString(R.string.seekbar_tracking_off));
  }

}