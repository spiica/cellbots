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

import android.util.Log;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class PausableThread extends Thread {
	private boolean isEnabled = true;

	/**
	 * Request that this thread stop doing anything intensive at its earliest convenience.
	 */
	public void disable() {
		isEnabled = false;
		Log.i("HelloAndroid", "PausableThread has been disabled. Thread id: " + getId());
	}
	
	/**
	 * Request that this thread continue its task immediately.
	 */
	public void enable() {
		// Wed don't want to interrupt if the thread is already enabled.
		if (!isEnabled) {
			isEnabled = true;
			interrupt();
			Log.i("HelloAndroid", "PausableThread has been enabled. Thread id: " + getId());
		}
	}
	
	/**
	 * This method must be called by the implementing class at the appropriate place to
	 * pause.
	 */
	protected void pauseIfNecessary() {
		while (!isEnabled) {
			try {
				// Sleep for 1 hour or until we're waken up.
				Thread.sleep(1000 * 60 * 60 * 60);
			} catch (InterruptedException e) {
				// Do nothing. We were expecting this at some point.
			}
		}
	}
	
}
