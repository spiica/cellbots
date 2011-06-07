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
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class Target {
	private String callsign;
	private int color;
	private TargetLocation location;
	
	public Target(String callsign, int color) {
		this.callsign = callsign;
		this.color = color;
		this.location = null;
	}
	
	public String getCallsign() {
		return callsign;
	}
	
	public int getColor() {
		return color;
	}
	
	public boolean hasLocation() {
		return location == null;
	}
	
	public void clearLocation() {
		location = null;
	}
	
	public TargetLocation getLocation() {
		return location;
	}

	public void setLocation(TargetLocation newLocation) {
		location = newLocation;		
	}
	
	public String toString() {
		return callsign;
	}
}
