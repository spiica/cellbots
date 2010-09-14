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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * This Activity binds to the CellDroid service with a specified username and password. 
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CellDroidActivity extends Activity {

	private static final String TAG = "CellDroidActivity";
	
    private EditText mUsernameText, mPasswdText, mBluetoothText;
    
    private MenuItem mPrefMenuitem;
    
    private Button connectButton, disconnectButton;
    
    private CellDroidManager celldroid;
    
    private SharedPreferences prefs;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i("XMPPClient", "onCreate called");
        setContentView(R.layout.main);
        
        mUsernameText = (EditText) findViewById(R.id.username_text);
        mPasswdText = (EditText) findViewById(R.id.password_text);
        mBluetoothText = (EditText) findViewById(R.id.bluetooth_text);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUsernameText.setText(prefs.getString("ROBOT_ACCOUNT",
                getResources().getString(R.string.robot_name)));
        mPasswdText.setText(prefs.getString("ROBOT_PASS",
                getResources().getString(R.string.robot_pass)));
        mBluetoothText.setText(prefs.getString("ROBOT_BT",
                getResources().getString(R.string.robot_bt)));
        
        celldroid = new CellDroidManager(CellDroidActivity.this);
        
        connectButton = (Button) this.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ROBOT_ACCOUNT", mUsernameText.getText().toString());
                editor.putString("ROBOT_PASS", mPasswdText.getText().toString());
                editor.putString("ROBOT_BT", mBluetoothText.getText().toString());
                editor.commit();
                celldroid.connect(mUsernameText.getText().toString(),
                        mPasswdText.getText().toString(),
                        prefs.getString("REMOTE_COMMAND_URL", ""),
                        mBluetoothText.getText().toString());
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            }
        });
        disconnectButton = (Button) this.findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                celldroid.disconnect();
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        celldroid.stopCellDroidService();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mPrefMenuitem = menu.add(R.string.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        if (item == mPrefMenuitem) {
            intent = new Intent(this, PrefsActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return false;
    }
}
