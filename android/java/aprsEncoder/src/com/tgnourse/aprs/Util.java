package com.tgnourse.aprs;

import android.util.Log;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class Util {

	private static final String LOG_IDENTIFIER = "DiveGuide";
	
	/**
	 * Print a info message to the logs.
	 * @param message The message to be printed
	 */
	public static void log(String message) {
		Log.i(LOG_IDENTIFIER, message);
	}

	/**
	 * Print an error message to the logs.
	 * @param message The message to be printed
	 */
	public static void error(String message) {
		Log.e(LOG_IDENTIFIER, message);
	}
}
