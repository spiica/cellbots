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
import android.os.StatFs;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Main activity for a data gathering tool. This tool enables recording video
 * and collecting data from sensors. Data is stored in:
 * /sdcard/cellbots_logger/.
 *
 * @author clchen@google.com (Charles L. Chen)
 */

public class LoggerActivity extends Activity {
    public static final String TAG = "CELLBOTS LOGGER";

    private static final String ARROW = "←";

    private static final int UI_BAR_MAX_TOP_PADDING = 135;

    private static final float TEMPERATURE_MAX = 500;

    public String timeString;

    private FrontCamcorderPreview mCamcorderView;

    private SensorManager mSensorManager;

    private boolean mIsRecording;

    private SlidingDrawer mDataDrawer;

    private SlidingDrawer mDiagnosticsDrawer;

    // Accelerometer
    private TextView mAccelXTextView;

    private TextView mAccelYTextView;

    private TextView mAccelZTextView;

    // Gyro
    private TextView mGyroXTextView;

    private TextView mGyroYTextView;

    private TextView mGyroZTextView;

    // Magnetic Field
    private TextView mMagXTextView;

    private TextView mMagYTextView;

    private TextView mMagZTextView;

    // Battery temperature (in Kelvin)
    private int mBatteryTemp = 0;

    private TextView mBatteryTempTextView;

    private BufferedWriter mBatteryTempWriter;

    private long startRecTime = 0;

    private LinearLayout mFlashingRecGroup;

    private TextView mRecTimeTextView;

    private StatFs mStatFs;

    private int mFreeSpacePct;

    private TextView mStorageTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Setup the initial available space
        mStatFs = new StatFs(Environment.getExternalStorageDirectory().toString());
        float percentage = (float) mStatFs.getAvailableBlocks() / (float) mStatFs.getBlockCount();
        mFreeSpacePct = (int) (percentage * 100);
        mStorageTextView = (TextView) findViewById(R.id.storage_text);
        mStorageTextView.setText(ARROW + mFreeSpacePct);
        mStorageTextView.setPadding(mStorageTextView.getPaddingLeft(),
                (int) (percentage * UI_BAR_MAX_TOP_PADDING), mStorageTextView.getPaddingRight(),
                mStorageTextView.getPaddingBottom());

        mFlashingRecGroup = (LinearLayout) findViewById(R.id.flashingRecGroup);
        mRecTimeTextView = (TextView) findViewById(R.id.recTime);

        mDataDrawer = (SlidingDrawer) findViewById(R.id.dataDrawer);
        mDiagnosticsDrawer = (SlidingDrawer) findViewById(R.id.diagnosticsDrawer);

        final ImageButton recordButton = (ImageButton) findViewById(R.id.button_record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mIsRecording) {
                    try {
                        mIsRecording = true;
                        recordButton.setImageResource(R.drawable.rec_button_pressed);
                        // initializes recording
                        mCamcorderView.initializeRecording();
                        // starts recording
                        mCamcorderView.startRecording();
                        startRecTime = System.currentTimeMillis();
                        new Thread(updateRecTimeDisplay).start();
                    } catch (Exception e) {
                        Log.e("ls", "Recording has failed...", e);
                        Toast.makeText(getApplicationContext(),
                                "Recording is not possible at the moment: " + e.toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        mIsRecording = false;
                        // stop
                        mCamcorderView.stopRecording();
                        // release resources
                        mCamcorderView.releaseRecorder();
                        startRecTime = 0;
                        finish();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        final Button dataHandleButton = (Button) findViewById(R.id.dataHandleButton);
        final ImageButton dataButton = (ImageButton) findViewById(R.id.button_data);
        dataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDataDrawer.isOpened()) {
                    dataButton.setImageResource(R.drawable.data_button_up);
                    mDataDrawer.animateClose();
                } else {
                    dataButton.setImageResource(R.drawable.data_button_pressed);
                    mDataDrawer.animateOpen();
                }
            }
        });

        final Button diagnosticsHandleButton = (Button) findViewById(R.id.diagnosticsHandleButton);
        final ImageButton diagnosticsButton = (ImageButton) findViewById(R.id.button_diagnostics);
        diagnosticsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDiagnosticsDrawer.isOpened()) {
                    diagnosticsButton.setImageResource(R.drawable.diagnostics_button_up);
                    mDiagnosticsDrawer.animateClose();
                } else {
                    diagnosticsButton.setImageResource(R.drawable.diagnostics_button_pressed);
                    mDiagnosticsDrawer.animateOpen();
                }
            }
        });

        setupSensors();
    }

    private List<Sensor> sensors;

    private HashMap<String, BufferedWriter> sensorLogFileWriters;

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // Gyroscope doesn't really have a notion of accuracy.
                // Due to a bug in Android, the gyroscope incorrectly returns
                // its status as unreliable. This can be safely ignored and does
                // not impact the accuracy of the readings.
                event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            }
            updateSensorUi(sensor.getType(), event.accuracy, event.values);
            if (mIsRecording) {
                String valuesStr = "";
                for (int i = 0; i < event.values.length; i++) {
                    valuesStr = valuesStr + event.values[i] + ",";
                }
                BufferedWriter writer = sensorLogFileWriters.get(sensor.getName());
                try {
                    writer.write(event.timestamp + "," + event.accuracy + "," + valuesStr + "\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private Runnable updateRecTimeDisplay = new Runnable() {
        @Override
        public void run() {
            while (mIsRecording) {
                mStatFs = new StatFs(Environment.getExternalStorageDirectory().toString());
                float percentage = (float) mStatFs.getAvailableBlocks()
                        / (float) mStatFs.getBlockCount();
                final int paddingTop = (int) (percentage * UI_BAR_MAX_TOP_PADDING);
                mFreeSpacePct = (int) (percentage * 100);
                Log.e("DEBUG", mStatFs.getAvailableBlocks() + " / " + mStatFs.getBlockCount());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlashingRecGroup.getVisibility() == View.VISIBLE) {
                            mFlashingRecGroup.setVisibility(View.INVISIBLE);
                        } else {
                            mFlashingRecGroup.setVisibility(View.VISIBLE);
                        }
                        mRecTimeTextView.setText(DateUtils.formatElapsedTime(
                                (System.currentTimeMillis() - startRecTime) / 1000));
                        mStorageTextView = (TextView) findViewById(R.id.storage_text);
                        mStorageTextView.setText(ARROW + mFreeSpacePct);
                        mStorageTextView.setPadding(mStorageTextView.getPaddingLeft(), paddingTop,
                                mStorageTextView.getPaddingRight(),
                                mStorageTextView.getPaddingBottom());
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFlashingRecGroup.setVisibility(View.INVISIBLE);
                }
            });
        }
    };

    private void setupSensors() {
        initSensorUi();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // Setup the files
        sensorLogFileWriters = new HashMap<String, BufferedWriter>();
        String directoryName = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/cellbots_logger/" + timeString + "/data/";
        File directory = new File(directoryName);
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException(
                        "Path to file could not be created. " + directory.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Directory could not be created. " + e.toString());
            }
        }
        File file;
        for (int i = 0; i < sensors.size(); i++) {
            Sensor s = sensors.get(i);
            String sensorFilename = directoryName + s.getName().replaceAll(" ", "_") + "_"
                    + timeString + ".txt";
            file = new File(sensorFilename);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                sensorLogFileWriters.put(s.getName(), writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSensorManager.registerListener(
                    mSensorEventListener, s, SensorManager.SENSOR_DELAY_GAME);
        }

        // The battery is a special case since it is not a real sensor
        String batteryTempFilename = directoryName + "/BatteryTemp_" + timeString + ".txt";
        file = new File(batteryTempFilename);
        try {
            mBatteryTempWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        initBattery();

        debugListSensors();
    }

    private void initSensorUi() {
        mAccelXTextView = (TextView) findViewById(R.id.accelerometerX_text);
        mAccelYTextView = (TextView) findViewById(R.id.accelerometerY_text);
        mAccelZTextView = (TextView) findViewById(R.id.accelerometerZ_text);

        mGyroXTextView = (TextView) findViewById(R.id.gyroX_text);
        mGyroYTextView = (TextView) findViewById(R.id.gyroY_text);
        mGyroZTextView = (TextView) findViewById(R.id.gyroZ_text);

        mMagXTextView = (TextView) findViewById(R.id.magneticFieldX_text);
        mMagYTextView = (TextView) findViewById(R.id.magneticFieldY_text);
        mMagZTextView = (TextView) findViewById(R.id.magneticFieldZ_text);
    }

    private void updateSensorUi(int sensorType, int accuracy, float[] values) {
        // IMPORTANT: DO NOT UPDATE THE CONTENTS INSIDE A DRAWER IF IT IS BEING
        // ANIMATED VIA A CALL TO animateOpen/animateClose!!!
        // Updating anything inside will stop the animation from running.
        // Note that this does not seem to affect the animation if it had been
        // triggered by dragging the drawer instead of being called
        // programatically.
        if (mDataDrawer.isMoving()) {
            return;
        }
        TextView xTextView;
        TextView yTextView;
        TextView zTextView;
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            xTextView = mAccelXTextView;
            yTextView = mAccelYTextView;
            zTextView = mAccelZTextView;
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            xTextView = mGyroXTextView;
            yTextView = mGyroYTextView;
            zTextView = mGyroZTextView;
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            xTextView = mMagXTextView;
            yTextView = mMagYTextView;
            zTextView = mMagZTextView;
        } else {
            return;
        }

        int textColor = Color.GREEN;
        String prefix = "";
        switch (accuracy) {
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

        xTextView.setTextColor(textColor);
        yTextView.setTextColor(textColor);
        zTextView.setTextColor(textColor);
        xTextView.setText(prefix + numberDisplayFormatter(values[0]));
        yTextView.setText(prefix + numberDisplayFormatter(values[1]));
        zTextView.setText(prefix + numberDisplayFormatter(values[2]));

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
                float percentage = (float) mBatteryTemp / (float) TEMPERATURE_MAX;
                int paddingTop = (int) (percentage * UI_BAR_MAX_TOP_PADDING);
                mBatteryTempTextView.setText(ARROW + (mBatteryTemp / 10));
                mBatteryTempTextView.setPadding(mBatteryTempTextView.getPaddingLeft(), paddingTop,
                        mBatteryTempTextView.getPaddingRight(),
                        mBatteryTempTextView.getPaddingBottom());
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

    private void closeFiles() {
        try {
            Collection<BufferedWriter> writers = sensorLogFileWriters.values();
            BufferedWriter[] w = new BufferedWriter[0];
            w = writers.toArray(w);
            for (int i = 0; i < w.length; i++) {
                w.clone();
            }
            mBatteryTempWriter.close();
        } catch (IOException e) {
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
            e.printStackTrace();
        }
    }
}
