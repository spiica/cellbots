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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * Service that runs in the background and checks the server for the altitude.
 * The server is expected to give the altitude in meters, and AltitudeAnnouncerService
 * will automatically convert it to feet before announcing it.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class AltitudeAnnouncerService extends Service {
	public static final int ACTION_STOP = 0;
	public static final int ACTION_START = 1;

	private TextToSpeech mTts;
	private String mHttpTargetUrl = "http://www.google.com";
	private boolean keepGoing = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO(clchen): Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		keepGoing = false;
		if (mTts != null) {
			mTts.shutdown();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		if (intent == null){
			keepGoing = false;
			this.stopSelf();
			return;
		}
		
		if (intent.getIntExtra("ACTION", ACTION_STOP) == ACTION_STOP) {
			keepGoing = false;
			this.stopSelf();
			return;
		}

		mHttpTargetUrl = intent.getStringExtra("URL");
		altDelta = intent.getIntExtra("DELTA", 1666);
		initialThreshold = intent.getIntExtra("INITIAL_THRESHOLD", 3333);
		mTts = new TextToSpeech(this, new OnInitListener() {

			@Override
			public void onInit(int status) {
				mTts.speak("Android Altitude Announcer ready", 0, null);

			}
		});
		Log.e("server", "starting server");
		keepGoing = true;
		new Thread(incomingDataProcessor).start();
	}

	public String getPage() {
		Log.e("Android Altitude Announcer", "Fetching content from:"
				+ mHttpTargetUrl);
		try {
			// Download the HTML content
			URL sourceURL = new URL(mHttpTargetUrl);
			// obtain the connection
			HttpURLConnection sourceConnection = (HttpURLConnection) sourceURL
					.openConnection();
			// add parameters to the connection
			HttpURLConnection.setFollowRedirects(true);
			// allow both GZip and Deflate (ZLib) encodings
			sourceConnection.setRequestProperty("Accept-Encoding", "gzip");

			// establish connection, get response headers
			sourceConnection.connect();

			// obtain the encoding returned by the server
			String encoding = sourceConnection.getContentEncoding();

			InputStream stream = null;
			// create the appropriate stream wrapper based on
			// the encoding type
			if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
				stream = new GZIPInputStream(sourceConnection.getInputStream());
			} else {
				stream = sourceConnection.getInputStream();
			}

			StringBuffer htmlContent = new StringBuffer();

			byte buf[] = new byte[128000];
			do {
				int numread = stream.read(buf);
				if (numread <= 0) {
					break;
				}
				htmlContent.append(new String(buf, 0, numread));
			} while (true);
			String result = htmlContent.toString();
			Log.e("Android Altitude Announcer", "Received:" + result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private int getAltitude() {
		int answer = -1;
		String pageContents = getPage();
		try {
			if (pageContents.length() > 0) {
				Float alt = Float.parseFloat(pageContents);
				answer = Math.round(alt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return answer;
		}

	}

	private int lastAlt = 0; // in meters
	private int altDelta = 1666; // in meters
	private int initialThreshold = 3333; // in meters
	private int timesFailedToIncrease = 0;
	private int highestKnownAltitude = 0;
	private long lastSpokenTime = 0;
	
	private float mToFeetFactor = 3.2808399f;

	private Runnable incomingDataProcessor = new Runnable() {
		@Override
		public void run() {
			try {

				while (keepGoing) {
					int alt = getAltitude();
					if (alt >= initialThreshold) {
						if (alt >= (lastAlt + altDelta)) {
							int altInFeet = Math.round(alt * mToFeetFactor);
							String message = altInFeet + " feet";
							if (lastAlt == 0) {
								message = "To infinity and beyond! We just reached "
										+ message + "!";
							}
							mTts.speak(message, 0, null);
							lastAlt = alt;
							
						}
						if (alt > highestKnownAltitude){
							highestKnownAltitude = alt;
							timesFailedToIncrease = 0;
						} else if (alt <= highestKnownAltitude){
							timesFailedToIncrease++;
						}
						if (timesFailedToIncrease > 9){
							int highestKnownAltitudeInFeet = Math.round(highestKnownAltitude * mToFeetFactor);
							mTts.speak("Highest known altitude was " + highestKnownAltitudeInFeet + " feet.", 0, null);
							timesFailedToIncrease = 0;
						}
					}
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

}
