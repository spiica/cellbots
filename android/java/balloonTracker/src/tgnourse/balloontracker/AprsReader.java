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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Reads position info from aprs.fi
 * 
 * @author mivey@google.com (Mark Ivey)
 */
public class AprsReader {
	
	private static final int MAX_STATIONS_PER_CALL = 20;
	// TODO(tgnourse): We need a better way to access the API. This key is out in the open and
	// subject to getting blocked if too many people use the app.
	private static final String API_KEY = "19021.moPiBuGkvco4V";

	/**
	 * Uses HTTP to get location data from aprs.fi.
	 */
	public Map<String, TargetLocation> getLocation(List<String> stationNames) throws AprsException {
		Map<String, TargetLocation> locations = new HashMap<String, TargetLocation>();
		
		Util.log("Getting location data from aprs.fi for stations: " + stationNames);
		try {			
		    URL url = getLocationUrl(stationNames);
		    Util.log("  URL: " + url.toString());
			String json = downloadJson(url);
			Util.log("  JSON from aprs.fi:" + json);
			
			JSONObject object = (JSONObject) new JSONTokener(json).nextValue();	
			
			if (!object.getString("result").equals("ok")) {
				throw new AprsException("\"result\" != \"ok\". Full JSON: " + json);
			}

			JSONArray entries = object.getJSONArray("entries");
			for (int i = 0; i < entries.length(); i++) {
				JSONObject entry = entries.getJSONObject(i);
				String stationName = entry.getString("name");
				double latitude = Double.parseDouble(entry.getString("lat"));
				double longitude = Double.parseDouble(entry.getString("lng"));
				double altitude = Double.parseDouble(entry.getString("altitude"));
				// Convert from km/h to m/s
				float speed = (float) (Float.parseFloat(entry.getString("speed")) * 0.277777778);
				long time = Long.parseLong(entry.getString("time"));
				Util.log("Time " + time);
				TargetLocation location = new TargetLocation(latitude, longitude, speed, altitude, time);
				Util.log("  Location for station " + stationName + ": " + location);
				locations.put(stationName, location);
			}
			
			return locations;
		} catch (JSONException e) {
			throw new AprsException(e);
		} catch (MalformedURLException e) {
			throw new AprsException(e);
		} catch (IOException e) {
			throw new AprsException(e);
		}
	}

	private URL getLocationUrl(List<String> stationNames) throws MalformedURLException, AprsException {
		if (stationNames.size() > MAX_STATIONS_PER_CALL) {
			throw new AprsException("Too many station names (Have " + stationNames.size()
					+ " but limit is " + MAX_STATIONS_PER_CALL);
		}
		URL url = new URL("http://api.aprs.fi/api/get?name=" + join(stationNames) 
	    		+ "&what=loc&apikey=" + API_KEY + "&format=json");
		return url;
	}

	private String join(List<String> input) {
		StringBuilder builder = new StringBuilder();
		for (String item : input) {
			builder.append(item);
			builder.append(",");
		}
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1); // Chop off last comma
		}
		return builder.toString();
	}

	private String downloadJson(URL url) throws IOException {
		StringBuilder builder = new StringBuilder();
	    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
	    String line;
	    while ((line = in.readLine()) != null) {
	    	builder.append(line);
	    }
	    in.close();
		return builder.toString();
	}
	
	public static class AprsException extends Exception {

		public AprsException(String message) {
			super(message);
		}
		
		public AprsException(Exception e) {
			super(e);
		}		
	}
}
