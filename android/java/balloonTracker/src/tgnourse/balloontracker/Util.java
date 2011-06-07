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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.util.Log;
import android.widget.Toast;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class Util {

	public static final float FEET_INA_METER = (float) 3.2808399;
	public static final float MPH_INA_MPS = (float) 2.23693629;
	
	/**
	 * Print a info message to the logs.
	 * @param message The message to be printed
	 */
	public static void log(String message) {
		Log.i("DiveTracker", message);
	}

	/**
	 * Print an error message to the logs.
	 * @param message The message to be printed
	 */
	public static void error(String message) {
		Log.e("DiveTracker", message);
	}

	public static String getDirectionFromHeading(int heading) {		
		if (heading >=0 && heading < 90)
			return getDirectionFromQuadrant(heading, "N", "E");
		if (heading >=90 && heading < 180)
			return getDirectionFromQuadrant(heading - 90, "E", "S");
		if (heading >=180 && heading < 270)
			return getDirectionFromQuadrant(heading - 180, "S", "W");
		if (heading >=270 && heading < 360)
			return getDirectionFromQuadrant(heading - 270, "W", "N");
		return "XX";
	}
	
	private static String getDirectionFromQuadrant(int heading, String first, String second) {
		if (heading >= 0 && heading < 10) return first;
		if (heading >= 10 && heading < 35) return first + "-" + first + second;
		if (heading >= 35 && heading < 55) return first + second;
		if (heading >= 55 && heading < 80) return second + "-" + first + second;
		if (heading >= 80 && heading < 90) return second;
		
		return "XX";
	}

	public static String getUnitForDistance(double distance) {		
		if (distance < 300) return "ft";
		if (distance < 440 * 3) return " yards";
		return " miles";
	}
	
	public static String getHumanReadableDistance(double distance) {
		if (distance < 300) return String.valueOf(Math.round(distance));
		if (distance < 440 * 3) return String.valueOf(Math.round(distance / 3));
		
		DecimalFormat format = new DecimalFormat("#.#");
		return String.valueOf(Double.valueOf(format.format(distance / 5280)));
	}

	public static String getUnitForDuration(long millis) {
		long seconds = millis / 1000;
		if (seconds < 120) return " sec";
		if (seconds < 3600) return " min";
		if (seconds < 86400) return " hrs";
		return " days"; 
	}
	
	public static String getHumanReadableDuration(long millis) {
		long seconds = millis / 1000;
		if (seconds < 120) return String.valueOf(seconds);
		if (seconds < 3600) return String.valueOf(seconds / 60);
		if (seconds < 86400) return String.valueOf(seconds / 3600);
		return String.valueOf(seconds / 86400);
	}
	
	/**
	 * @param num
	 * @return the latitude or longitude to an accuracy of 11.1 meters.
	 */
	public static String getHumanReadableLatLong(double num) {
		return formatNumber(num, 4);
	}
	
	public static String formatNumber(double number, int decimals) {
		double multiplier = Math.pow(10, decimals);
		number = number * multiplier;
		long inumber = Math.round(number);
		DecimalFormat df = new DecimalFormat();
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setGroupingSeparator(',');
		df.setDecimalFormatSymbols(dfs);
		df.setMaximumFractionDigits(2);
		return df.format(inumber / multiplier);
	}
}
