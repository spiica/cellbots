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
package com.googlecode.cellbots;

import com.googlecode.cellbots.nxt.NxtConnection;
import com.googlecode.cellbots.nxt.NxtConnection.OnNxtConnectionReadyListener;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Simple RC app to demonstrate the usage of the NxtConnection library.
 * 
 * There are a few assumptions made about the configuration of the robot.
 * 
 * Motors A and C are used, and there are 3 ultrasonic sensors hooked up
 * to ports 1, 3, and 4.
 * 
 * Note that Motor A maps to 0, B to 1, and C to 2.
 * Also, the ports are 0 indexed, so Port 1 is actually 0, Port 2 is 1, etc.
 *
 * @author Charles L. Chen (clchen@google.com)
 */
public class LegoRC extends Activity {    
	private LegoRC self; 
	private NxtConnection mNxtConnection;
	
	private boolean keepSensing = true;
	private int sensor1 = 255; 
    private int sensor3 = 255; 
    private int sensor4 = 255; 
	
	private Button forwardsButton;
	private Button backwardsButton;
	private Button leftButton;
	private Button rightButton;
    private Button stopButton;
    
    private TextView sensor1TextView;
    private TextView sensor3TextView;
    private TextView sensor4TextView;
	
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		self = this;
		
		mNxtConnection = new NxtConnection(this, new OnNxtConnectionReadyListener(){
            @Override
            public void OnNxtConnectionReady() {
                mNxtConnection.connect();
                mNxtConnection.setUltraSonicSensor(0);
                mNxtConnection.setUltraSonicSensor(2);
                mNxtConnection.setUltraSonicSensor(3);
                new Thread(new SensorUpdater()).start();
                Toast.makeText(self, "NXT connected!", 1).show();
            }		    
		});
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        setContentView(R.layout.controls);

        
        
        forwardsButton = (Button) findViewById(R.id.forwardsButton);
        forwardsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			    mNxtConnection.setMotor(0, 100);
                mNxtConnection.setMotor(2, 100);
			}
		});
        
        backwardsButton = (Button) findViewById(R.id.backwardsButton);
        backwardsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                mNxtConnection.setMotor(0, -100);
                mNxtConnection.setMotor(2, -100);
			}
		});
        
        leftButton = (Button) findViewById(R.id.leftButton);
        leftButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                mNxtConnection.setMotor(0, 100);
                mNxtConnection.setMotor(2, -100);
			}
		});
        
        rightButton = (Button) findViewById(R.id.rightButton);
        rightButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                mNxtConnection.setMotor(0, -100);
                mNxtConnection.setMotor(2, 100);
			}
		});
        
        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mNxtConnection.setMotor(0, 0);
                mNxtConnection.setMotor(2, 0);
            }
        });
        

        sensor1TextView = (TextView) findViewById(R.id.sensor1Text);
        sensor3TextView = (TextView) findViewById(R.id.sensor3Text);
        sensor4TextView = (TextView) findViewById(R.id.sensor4Text);
    }
	
	@Override
	public void onDestroy(){
        keepSensing = false;
	    mNxtConnection.shutdown();
	    super.onDestroy();
	}
	
	class SensorUpdater implements Runnable {
        @Override
        public void run() {
            while (keepSensing){
            sensor1 = mNxtConnection.readUltraSonicSensor(0);
            sensor3 = mNxtConnection.readUltraSonicSensor(2);
            sensor4 = mNxtConnection.readUltraSonicSensor(3);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            self.runOnUiThread(new UiUpdater());
            }
        }
	}
	
	class UiUpdater implements Runnable {
        @Override
        public void run() {
            sensor1TextView.setText(sensor1 + "");
            sensor3TextView.setText(sensor3 + "");
            sensor4TextView.setText(sensor4 + "");
        }	    
	}
    
}