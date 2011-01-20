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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Main activity for a data gathering tool. This tool enables recording video
 * and collecting data from sensors. Data is stored in:
 * /sdcard/cellbots_logger/.
 *
 * @author clchen@google.com (Charles L. Chen)
 */

public class LoggerActivity extends Activity {
    public final static String TAG = "CELLBOTS LOGGER";

    public String timeString;

    private FrontCamcorderPreview mCamcorderView;

    private SensorManager mSensorManager;

    private boolean mIsRecording;

    // Accelerometer
    private TextView mAccelXTextView;

    private TextView mAccelYTextView;

    private TextView mAccelZTextView;

    private int mAccelAccuracy;

    private float mAccelX = Float.NaN;

    private float mAccelY = Float.NaN;

    private float mAccelZ = Float.NaN;

    private BufferedWriter mAccelWriter;

    // Gyro
    private TextView mGyroXTextView;

    private TextView mGyroYTextView;

    private TextView mGyroZTextView;

    private float mGyroX = Float.NaN;

    private float mGyroY = Float.NaN;

    private float mGyroZ = Float.NaN;

    private BufferedWriter mGyroWriter;

    // Magnetic Field
    private TextView mMagXTextView;

    private TextView mMagYTextView;

    private TextView mMagZTextView;

    private int mMagAccuracy;

    private float mMagX = Float.NaN;

    private float mMagY = Float.NaN;

    private float mMagZ = Float.NaN;

    private BufferedWriter mMagWriter;

    // Battery temperature (in Kelvin)
    private int mBatteryTemp = 0;

    private TextView mBatteryTempTextView;

    private BufferedWriter mBatteryTempWriter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Keep the screen on to make sure the phone stays awake.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mIsRecording = false;
        Date date = new Date();
        timeString = date.toGMTString().replaceAll(" ", "_").replaceAll(":", "-");

        setContentView(R.layout.main);
        mCamcorderView = (FrontCamcorderPreview) findViewById(R.id.surface);
        final SurfaceHolder previewHolder = mCamcorderView.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        final Button stopQuitButton = (Button) findViewById(R.id.button_stopQuit);
        stopQuitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    mIsRecording = false;
                    // stop
                    mCamcorderView.stopRecording();
                    // release resources
                    mCamcorderView.releaseRecorder();
                    finish();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button recordButton = (Button) findViewById(R.id.button_record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    mIsRecording = true;

                    // initializes recording
                    mCamcorderView.initializeRecording();
                    // starts recording
                    mCamcorderView.startRecording();

                    recordButton.setVisibility(View.GONE);
                    stopQuitButton.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e("ls", "Recording has failed...", e);

                    Toast.makeText(getApplicationContext(),
                            "Recording is not possible at the moment: " + e.toString(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        setupSensors();
    }

    private void setupSensors() {
        // Prepare all the files for writing
        openFiles();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        initAccelerometer();
        initGyro();
        initMag();
        initBattery();
        // TODO: Add more sensors here!

        debugListSensors();
    }

    private void initAccelerometer() {
        // Setup the on screen display text views
        mAccelXTextView = (TextView) findViewById(R.id.accelerometerX_text);
        mAccelYTextView = (TextView) findViewById(R.id.accelerometerY_text);
        mAccelZTextView = (TextView) findViewById(R.id.accelerometerZ_text);

        mAccelAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                mAccelX = event.values[0];
                mAccelY = event.values[1];
                mAccelZ = event.values[2];

                int textColor = Color.GREEN;
                String prefix = "";
                switch (mAccelAccuracy) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                        textColor = Color.GREEN;
                        prefix = "";
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                        textColor = Color.YELLOW;
                        prefix = "*";
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                        textColor = Color.RED;
                        prefix = "**";
                        break;
                    case SensorManager.SENSOR_STATUS_UNRELIABLE:
                        textColor = Color.RED;
                        prefix = "***";
                        break;
                }

                mAccelXTextView.setTextColor(textColor);
                mAccelYTextView.setTextColor(textColor);
                mAccelZTextView.setTextColor(textColor);
                mAccelXTextView.setText(prefix + numberDisplayFormatter(mAccelX));
                mAccelYTextView.setText(prefix + numberDisplayFormatter(mAccelY));
                mAccelZTextView.setText(prefix + numberDisplayFormatter(mAccelZ));

                if (mIsRecording) {
                    try {
                        mAccelWriter.write(
                                event.timestamp + "," + mAccelAccuracy + "," + mAccelX + ","
                                        + mAccelY + "," + mAccelZ + "\n");
                        mAccelWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                mAccelAccuracy = accuracy;
            }
        }, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void initGyro() {
        // Setup the on screen display text views
        mGyroXTextView = (TextView) findViewById(R.id.gyroX_text);
        mGyroYTextView = (TextView) findViewById(R.id.gyroY_text);
        mGyroZTextView = (TextView) findViewById(R.id.gyroZ_text);

        Sensor gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                mGyroX = event.values[0];
                mGyroY = event.values[1];
                mGyroZ = event.values[2];

                int textColor = Color.GREEN;
                String prefix = "";

                mGyroXTextView.setTextColor(textColor);
                mGyroYTextView.setTextColor(textColor);
                mGyroZTextView.setTextColor(textColor);
                mGyroXTextView.setText(prefix + numberDisplayFormatter(mGyroX));
                mGyroYTextView.setText(prefix + numberDisplayFormatter(mGyroY));
                mGyroZTextView.setText(prefix + numberDisplayFormatter(mGyroZ));

                if (mIsRecording) {
                    try {
                        mGyroWriter.write(
                                event.timestamp + "," + SensorManager.SENSOR_STATUS_ACCURACY_HIGH
                                        + "," + mGyroX + "," + mGyroY + "," + mGyroZ + "\n");
                        mGyroWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Gyro is a special case; it doesn't really have a notion of
                // accuracy. For consistency (ie, to make sorting/filtering the
                // columns easier), we'll treat it as always being HIGH.
            }
        }, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void initMag() {
        // Setup the on screen display text views
        mMagXTextView = (TextView) findViewById(R.id.magneticFieldX_text);
        mMagYTextView = (TextView) findViewById(R.id.magneticFieldY_text);
        mMagZTextView = (TextView) findViewById(R.id.magneticFieldZ_text);

        mMagAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
        Sensor magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                mMagX = event.values[0];
                mMagY = event.values[1];
                mMagZ = event.values[2];

                int textColor = Color.GREEN;
                String prefix = "";
                switch (mMagAccuracy) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                        textColor = Color.GREEN;
                        prefix = "";
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                        textColor = Color.YELLOW;
                        prefix = "*";
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                        textColor = Color.RED;
                        prefix = "**";
                        break;
                    case SensorManager.SENSOR_STATUS_UNRELIABLE:
                        textColor = Color.RED;
                        prefix = "***";
                        break;
                }

                mMagXTextView.setTextColor(textColor);
                mMagYTextView.setTextColor(textColor);
                mMagZTextView.setTextColor(textColor);
                mMagXTextView.setText(prefix + numberDisplayFormatter(mMagX));
                mMagYTextView.setText(prefix + numberDisplayFormatter(mMagY));
                mMagZTextView.setText(prefix + numberDisplayFormatter(mMagZ));

                if (mIsRecording) {
                    try {
                        mMagWriter.write(
                                event.timestamp + "," + mAccelAccuracy + "," + mMagX + "," + mMagY
                                        + "," + mMagZ + "\n");
                        mMagWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                mMagAccuracy = accuracy;
            }
        }, magSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void initBattery() {
        // Battery isn't a regular sensor; instead we have to use a Broadcast
        // receiver.
        //
        // We always write this file since the battery changed event isn't
        // called that often; otherwise, we might miss the initial battery
        // reading.
        //
        // Note that we are reading the current time in MILLISECONDS for this,
        // as opposed to NANOSECONDS for regular sensors.
        mBatteryTempTextView = (TextView) findViewById(R.id.batteryTemp_text);
        IntentFilter batteryTemperatureFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                mBatteryTempTextView.setText(mBatteryTemp + "");
                try {
                    mBatteryTempWriter.write(
                            System.currentTimeMillis() + "," + mBatteryTemp + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, batteryTemperatureFilter);
    }

    private void debugListSensors() {
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 0; i < sensors.size(); i++) {
            Sensor s = sensors.get(i);
            Log.e("DEBUG", s.getName());
        }
    }

    private void openFiles() {
        String directoryName = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/cellbots_logger";
        File file;
        File directory = new File(directoryName);
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException(
                        "Path to file could not be created. " + directory.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Directory could not be created. " + e.toString());
            }
        }

        // Accelerometer
        String accelerometerFilename = directoryName + "/data-accelerometer-" + timeString + ".txt";
        file = new File(accelerometerFilename);
        try {
            mAccelWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Gyro
        String gyroFilename = directoryName + "/data-gyro-" + timeString + ".txt";
        file = new File(gyroFilename);
        try {
            mGyroWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Magnetic field
        String magFilename = directoryName + "/data-magneticfield-" + timeString + ".txt";
        file = new File(magFilename);
        try {
            mMagWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Magnetic field
        String batteryTempFilename = directoryName + "/data-batterytemp-" + timeString + ".txt";
        file = new File(batteryTempFilename);
        try {
            mBatteryTempWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add other sensor file writers here!
    }

    private void closeFiles() {
        try {
            mAccelWriter.close();
            mGyroWriter.close();
            mMagWriter.close();
            mBatteryTempWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String numberDisplayFormatter(float value) {
        String displayedText = Float.toString(value);
        if (value >= 0) {
            displayedText = " " + displayedText;
        }
        if (displayedText.length() > 8) {
            displayedText = displayedText.substring(0, 8);
        }
        while (displayedText.length() < 8) {
            displayedText = displayedText + " ";
        }
        return displayedText;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeFiles();
            // Make sure the video recording is stopped and all resources
            // released before quitting.
            if (mCamcorderView != null) {
                mCamcorderView.stopRecording();
                mCamcorderView.releaseRecorder();
                mCamcorderView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
        }
    }
}
