/*
 * Copyright (C) 2011 Google Inc.
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

package com.example.helloandroid;

import android.app.Activity;
import android.util.Log;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class ToggleThread extends PausableThread {
	private DashboardCamera camera;
	private Activity activity;
	private int videoLength = 30000;
	
	ToggleThread(DashboardCamera camera, Activity activity) {
		this.camera = camera;
		this.activity = activity;
	}
	
	/**
	 * Set the video length. Will cause the camera to skip to the next buffer immediately.
	 * @param videoLength Length of the video in seconds.
	 */
	public void setVideoLength(int videoLength) {
		this.videoLength = videoLength * 1000;
		// We interrupt ourselves to reset the sleep loop in run().
		interrupt();
	}
	
	public int getVideoLength() {
		return videoLength / 1000;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(videoLength);
			} catch (InterruptedException e) {
				
			}
			Log.i("HelloAndroid", "Attempting to initiate toggling of video. Thread id: " + getId());
			activity.runOnUiThread(new ToggleAction());
			pauseIfNecessary();
		}
	}
	
	private class ToggleAction implements Runnable {
		public void run() {
			Log.i("HelloAndroid", "Attempting to toggle video buffers. Thread id: " + getId());
			camera.toggleBuffers();
		}
	}
}
