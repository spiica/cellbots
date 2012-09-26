/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.altitudeannouncer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * This activity will launch the background TTS service that checks
 * the server and announces the altitude + it also opens up a web page from
 * the server that contains the link to the live Google Earth KML file.
 *
 * Note: The server URL should be the base URL. The "alt/" will be added
 * automatically by this app.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class AltitudeAnnouncerActivity extends Activity {

	  /** Called when the activity is first created. */
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    setContentView(R.layout.main);
	    
	    final Activity self = this;
	    
	    final Button startButton = (Button) findViewById(R.id.start);
	    final Button stopButton = (Button) findViewById(R.id.stop);
	    
	    startButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				startButton.setEnabled(false);
				EditText serverUrl = (EditText) findViewById(R.id.url);
				String url = serverUrl.getText().toString() + "alt/;
				serverUrl.setEnabled(false);

				EditText initialThresholdEditText = (EditText) findViewById(R.id.initialThreshold);
				int initialThreshold = Integer.parseInt(initialThresholdEditText.getText().toString());
				initialThresholdEditText.setEnabled(false);
				
				EditText deltaThresholdEditText = (EditText) findViewById(R.id.delta);
				int deltaThreshold = Integer.parseInt(deltaThresholdEditText.getText().toString());
				deltaThresholdEditText.setEnabled(false);	
						
						
			    Intent serviceIntent = new Intent();
			    serviceIntent.setClass(self, AltitudeAnnouncerService.class);
			    serviceIntent.putExtra("ACTION", AltitudeAnnouncerService.ACTION_START);
			    serviceIntent.putExtra("URL", url);
			    serviceIntent.putExtra("INITIAL_THRESHOLD", initialThreshold);
			    serviceIntent.putExtra("DELTA", deltaThreshold);
			    startService(serviceIntent);

				stopButton.setEnabled(true);

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(serverUrl.getText().toString()));
                startActivity(i);
			}	    	
	    });
	    
	    stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stopButton.setEnabled(false);
			    Intent serviceIntent = new Intent();
			    serviceIntent.setClass(self, AltitudeAnnouncerService.class);
			    serviceIntent.putExtra("ACTION", AltitudeAnnouncerService.ACTION_STOP);
			    startService(serviceIntent);
				finish();
			}	    	
	    });
	  }
}
