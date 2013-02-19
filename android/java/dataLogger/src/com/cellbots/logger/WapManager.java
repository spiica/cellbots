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

package com.cellbots.logger;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.TreeMap;

public class WapManager {
	public static final String TAG = "WapManager";
	
	@SuppressWarnings("serial")
	public static class SsidResults extends TreeMap<String, Integer> { };
	
	@SuppressWarnings("serial")
	public static class ScanResults extends TreeMap<String, SsidResults> { 
		public SsidResults getSSID(String ssid) {
			if (!containsKey(ssid))
				put(ssid, new SsidResults());
			return get(ssid);
		}
	}
	
	public interface WapManagerListener {
		void onScanResults(long timestamp, ScanResults results);
	}
	
	private final Context mContext;
	private WapManagerListener mListener = null;
	
	private WifiManager mManager;
	
	public WapManager(Context context, WapManagerListener listener) {
		mContext = context;
		mListener = listener;
		mManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		
		Log.i(TAG, "WapManager created");
	}
	
	public void setWapManagerListener(WapManagerListener listener) {
		mListener = listener;
	}
	
	public void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		
		mContext.registerReceiver(mWifiReceiver, filter);
		
		// Ensure wifi is enabled for scanning
		if (!mManager.isWifiEnabled())
			mManager.setWifiEnabled(true);
	}
	
	public void unregisterReceiver() {
		mContext.unregisterReceiver(mWifiReceiver);
	}
	
	public void onScanCompleted(long timestamp, List<ScanResult> results) {
		if (results == null)
			return;
		
		ScanResults newResults = new ScanResults();

		for (ScanResult r : results) {
			SsidResults bssids = newResults.getSSID(r.SSID);
			bssids.put(r.BSSID, r.level);
		}
		
		if (mListener != null) {
			mListener.onScanResults(timestamp, newResults);
		}
	}
	
	public void onSupplicantConnected() {
		/*
		mManager.disconnect();
		for (WifiConfiguration c : mManager.getConfiguredNetworks()) {
			mManager.removeNetwork(c.networkId);
		}
		mManager.saveConfiguration();
		*/
	}
	
	private Handler mHandler = new Handler();
	private final Runnable mScanStarter = new Runnable() {
		public void run() {
			if (!mManager.startScan()) {
				Log.e(TAG, "Unable to start scan... trying again in a second");
				mHandler.postDelayed(mScanStarter, 1000);
			} else {
				Log.i(TAG, "Started scan successfully");
			}
		}
	};
	
	private final Runnable mSupplicantConnected = new Runnable() {
		public void run() {
			onSupplicantConnected();
		}
	};
	
	private final BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				Log.i(TAG, "Scan Completed");
				onScanCompleted(System.currentTimeMillis(), mManager.getScanResults());
				mHandler.postDelayed(mScanStarter, 1000);
			} else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
				Log.i(TAG, "Supplicant connection intent.");
				boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
				if (connected) {
					mHandler.post(mSupplicantConnected);
				}
			} else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				Log.i(TAG, "Wifi State changed: " + state);

				if (state == WifiManager.WIFI_STATE_ENABLED)
					mHandler.post(mScanStarter);
			}
		}
	};
}
