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

/**
 * Information about the difference between the current location and the target location
 * including accuracy information about the current location.
 * 
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class Difference {
	// Information about the difference.
	private double distance;
	private double groundDistance;
	private float azimuth;
	private double altitude;
	
	// Information from the locations.
	private long currentLocationAge;
	private long targetAge;
	private float targetSpeed;
	private double targetElevation;
	
	public Difference(double distance, double groundDistance, float azimuth, double altitude,
			long currentLocationAge, long targetAge,
			float targetSpeed, double targetElevation) {
		this.distance = distance;
		this.groundDistance = groundDistance;
		this.azimuth = azimuth;
		this.altitude = altitude;
		this.currentLocationAge = currentLocationAge;
		this.targetAge = targetAge;
		this.targetSpeed = targetSpeed;
		this.targetElevation = targetElevation;
	}
	
	/**
	 * @return distance in feet to the target
	 */
	public double getDistance() {
		return distance;
	}
	
	/**
	 * @return distance in feet to the target as projected on the ground
	 */
	public double getGroundDistance() {
		return groundDistance;
	}
	
	/**
	 * @return the azimuth of the target in degrees from north clockwise
	 */
	public float getAzimuth() {
		return azimuth;
	}
	
	/**
	 * @return the altitude of inclination to the target in degrees
	 */
	public double getAltitude() {
		return altitude;
	}
	
	/**
	 * @return age in seconds of the current location
	 */
	public long getCurrentLocationAge() {
		return currentLocationAge;
	}
	
	/**
	 * @return age in ms of the target location
	 */
	public long getTargetAge() {
		return targetAge;
	}
	
	/**
	 * @return speed of the target in mph
	 */
	public float getTargetSpeed() {
		return targetSpeed;
	}
	
	/**
	 * @return elevation of the target in feet
	 */
	public double getTargetElevation() {
		return targetElevation;
	}
	
	public String toString() {
		return "distance: " + distance + " ft | " +
			   "ground distance: " + groundDistance + " ft | " +
			   "azimuth: " + azimuth + " degrees | " +
			   "altitude: " + altitude + " degrees | " +
			   "current age: " + currentLocationAge + " seconds | " +
			   "target age: " + targetAge + " ms | " +
			   "target speed: " + targetSpeed + " mph | " +
			   "target elevation: " + targetElevation + " ft";
 	}
}