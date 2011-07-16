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

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple Activity for choosing which mode to launch the data logger in.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class LauncherActivity extends Activity {
    private CheckBox useZipCheckbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        useZipCheckbox = (CheckBox) findViewById(R.id.useZip);

        final Activity self = this;
        Button launchVideoFrontButton = (Button) findViewById(R.id.launchVideoFront);
        launchVideoFrontButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchLoggingActivity(LoggerActivity.MODE_VIDEO_FRONT, useZipCheckbox.isChecked());
            }
        });
        Button launchVideoBackButton = (Button) findViewById(R.id.launchVideoBack);
        launchVideoBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchLoggingActivity(LoggerActivity.MODE_VIDEO_BACK, useZipCheckbox.isChecked());
            }
        });
        final EditText pictureDelayEditText = (EditText) findViewById(R.id.pictureDelay);
        Button launchPictureButton = (Button) findViewById(R.id.launchPicture);
        launchPictureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(self, LoggerActivity.class);
                i.putExtra(LoggerActivity.EXTRA_MODE, LoggerActivity.MODE_PICTURES);
                i.putExtra(LoggerActivity.EXTRA_USE_ZIP, useZipCheckbox.isChecked());
                int delay = 30;
                try {
                    delay = Integer.parseInt(pictureDelayEditText.getText().toString());
                } catch (Exception e) {
                    Toast.makeText(self,
                            "Error parsing picture delay time. Using default delay of 30 seconds.",
                            1).show();
                }
                i.putExtra(LoggerActivity.EXTRA_PICTURE_DELAY, delay);
                startActivity(i);
                finish();
            }
        });
        // The code we are using for taking video through the front camera
        // relies on APIs added in SDK 9. Don't offer the front video option to
        // users on devices older than that OR to devices who have only one
        // camera. Currently assume that if only one camera is present, it is
        // the back camera.
        if (Build.VERSION.SDK_INT < 9 || Camera.getNumberOfCameras() == 1) {
            launchVideoFrontButton.setVisibility(View.GONE);
        }
    }

    private void launchLoggingActivity(int mode, boolean useZip) {
        Intent i = new Intent(LauncherActivity.this, LoggerActivity.class);
        i.putExtra(LoggerActivity.EXTRA_MODE, mode);
        i.putExtra(LoggerActivity.EXTRA_USE_ZIP, useZip);
        startActivity(i);
        finish();
    }
}
