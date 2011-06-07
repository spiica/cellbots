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

import android.hardware.GeomagneticField;
import android.location.Location;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class CurrentLocation {
	// The most recent location we've seen.
	private Location currentLocation;
	// The last system time that we saw that location.
	private long lastTime;
	
	public CurrentLocation() {
		currentLocation = null;
	}
	
	synchronized public void locationChanged(Location location) {
		currentLocation = location;
		lastTime = System.currentTimeMillis();
	}
	
	/**
	 * @return The age of this CurrentLocation in ms.
	 */
	public long getAge() {
		return System.currentTimeMillis() - lastTime;
	}
	
	public float getDeclination() {
		GeomagneticField field = new GeomagneticField(
				(float) currentLocation.getLatitude(),
				(float) currentLocation.getLongitude(),
				(float) currentLocation.getAltitude(),
				currentLocation.getTime());
		return field.getDeclination();
	}
	
	synchronized public Difference getDifference(TargetLocation target) {
		double adjacent = currentLocation.distanceTo(target) * Util.FEET_INA_METER;
		double opposite = target.getAltitude() * Util.FEET_INA_METER;
		double hypotenuse = Math.sqrt(opposite*opposite + adjacent*adjacent);
		float azimuth = (currentLocation.bearingTo(target) + 360) % 360;
		double altitude = Math.atan(opposite / adjacent) * 57.2957795;
		float speed = target.getSpeed() * Util.MPH_INA_MPS; 
		long age = System.currentTimeMillis() - (lastTime - (currentLocation.getTime() - target.getTime()));
		
		// Note that this calculation breaks down when the earth isn't flat.
		
		return new Difference(hypotenuse, adjacent, azimuth, altitude,
							  getAge(), age, speed, opposite);
	}
	
	synchronized public boolean hasLocation() {
		return currentLocation != null;
	}
}
