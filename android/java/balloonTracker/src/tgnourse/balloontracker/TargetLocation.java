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

package tgnourse.balloontracker;

import android.location.Location;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class TargetLocation extends Location {
	
	private long lastTime;
	private long time;
	private boolean hasTime;
	
	public TargetLocation(double latitude, double longitude, float speed, double altitude) {
		super("TargetLocation");
		
		super.setLatitude(latitude);
		super.setLongitude(longitude);
		super.setSpeed(speed);
		super.setAltitude(altitude);
		
		this.lastTime = System.currentTimeMillis();
		this.time = System.currentTimeMillis();
		hasTime = false;
	}
	
	public TargetLocation(double latitude, double longitude, float speed, double altitude, long time) {
		super("TargetLocation");
		
		super.setLatitude(latitude);
		super.setLongitude(longitude);
		super.setSpeed(speed);
		super.setAltitude(altitude);
		super.setTime(time);
		
		this.lastTime = System.currentTimeMillis();
		this.time = time * 1000;
		hasTime = true;
	}
	
	public String toString() {
		return "Lat:" + getLatitude() + " Lon:" + getLongitude() + " Speed:" + getSpeed()
		    + " Alt:" + getAltitude();
	}
	
	public boolean hasTime() {
		return hasTime;
	}
	
	public long getAge() {
		// return System.currentTimeMillis() - lastTime;
		return time;
	}
}
