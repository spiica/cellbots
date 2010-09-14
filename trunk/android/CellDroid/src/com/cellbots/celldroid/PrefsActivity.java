/*
 * Copyright (C) 2010 Google Inc.
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
package com.cellbots.celldroid;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

/**
 * Prefs activity for the remote control. Sets the accounts + Remote Eyes URL.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    private EditTextPreference robotAccountPref;
    private EditTextPreference robotPassPref;
    private EditTextPreference robotBluetoothPref;
    private EditTextPreference remoteCommandUrlPref;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        prefs = getPreferenceScreen().getSharedPreferences();
        robotAccountPref = (EditTextPreference) findPreference("ROBOT_ACCOUNT");
        robotPassPref = (EditTextPreference) findPreference("ROBOT_PASS");
        robotBluetoothPref = (EditTextPreference) findPreference("ROBOT_BT");
        remoteCommandUrlPref = (EditTextPreference) findPreference("REMOTE_COMMAND_URL");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        robotAccountPref.setSummary(prefs.getString("ROBOT_ACCOUNT", "")); 
        robotPassPref.setSummary(prefs.getString("ROBOT_PASS", "")); 
        robotBluetoothPref.setSummary(prefs.getString("ROBOT_BT", ""));
        remoteCommandUrlPref.setSummary(prefs.getString("REMOTE_COMMAND_URL", "")); 
        prefs.registerOnSharedPreferenceChangeListener(this);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("ROBOT_ACCOUNT")) {
            robotAccountPref.setSummary(sharedPreferences.getString(key, "")); 
        } else if (key.equals("ROBOT_PASS")) {
            robotPassPref.setSummary(sharedPreferences.getString(key, "")); 
        } else if (key.equals("ROBOT_BT")) {
            robotBluetoothPref.setSummary(sharedPreferences.getString(key, "")); 
        } else if (key.equals("REMOTE_COMMAND_URL")) {
            remoteCommandUrlPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }

}
