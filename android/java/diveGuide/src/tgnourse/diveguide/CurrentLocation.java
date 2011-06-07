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

package tgnourse.diveguide;

import android.hardware.GeomagneticField;
import android.location.Location;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class CurrentLocation {
	// The most recent location we've seen.
	Location currentLocation;
	// The last system time that we saw that location.
	long lastTime;
	
	public CurrentLocation() {
		currentLocation = null;
	}
	
	synchronized public void locationChanged(Location location) {
		currentLocation = location;
		lastTime = System.currentTimeMillis();
	}
	
	synchronized public Difference getDifference(TargetLocation target) {
		return new Difference(currentLocation.distanceTo(target)  * Util.FEET_PER_METER,
							  Math.round(currentLocation.bearingTo(target) + getDeclination() + 360) % 360,
							  Math.round(currentLocation.getAccuracy() * Util.FEET_PER_METER),
							  System.currentTimeMillis() - lastTime);
	}
	
	synchronized public boolean hasLocation() {
		return currentLocation != null;
	}
	
	private float getDeclination() {
		GeomagneticField field = new GeomagneticField(
				(float) currentLocation.getLatitude(),
				(float) currentLocation.getLongitude(),
				(float) currentLocation.getAltitude(),
				currentLocation.getTime());
		return field.getDeclination();
	}
	
	/**
	 * Information about the difference between the current location and the target location
	 * including accuracy information about the current location.
	 * 
	 * @author tgnourse
	 */
	public class Difference {
		private double distance;
		private int heading;
		private int accuracy;
		private long age;
		
		private Difference(double distance, int heading, int accuracy, long age) {
			this.distance = distance;
			this.heading = heading;
			this.accuracy = accuracy;
			this.age = age;
		}
		
		/**
		 * @return distance in feet to the target
		 */
		public double getDistance() {
			return distance;
		}
		
		/**
		 * @return bearing in degrees clockwise from magnetic north to the target
		 */
		public int getHeading() {
			// TODO(tgnourse): We can calculate the accuracy of the bearing based on how far we are
			// from the target and the accuracy of the current location.
			return heading;
		}
		
		/**
		 * @return accuracy in feet of the current location
		 */
		public int getAccuracy() {
			return accuracy;
		}
		
		/**
		 * @return age in seconds of the current location
		 */
		public long getAge() {
			return age;
		}
	}
}
