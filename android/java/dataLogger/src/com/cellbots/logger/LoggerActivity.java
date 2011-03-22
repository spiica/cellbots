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
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.os.Handler;
import android.os.StatFs;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.cellbots.logger.GpsManager.GpsManagerListener;

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
 * /sdcard/SmartphoneLoggerData/.
 *
 * @author clchen@google.com (Charles L. Chen)
 */

public class LoggerActivity extends Activity {
    /*
     * Constants
     */
    public static final String TAG = "CELLBOTS LOGGER";

    public static final String EXTRA_MODE = "MODE";

    public static final String EXTRA_PICTURE_DELAY = "PICTURE_DELAY";

    public static final int MODE_VIDEO_FRONT = 0;

    public static final int MODE_VIDEO_BACK = 1;

    public static final int MODE_PICTURES = 2;

    private static final int UI_BAR_MAX_TOP_PADDING = 206;

    private static final float TEMPERATURE_MAX = 500;

    // max file size. if this is set to zero, only 1 .zip file is created
    protected static final int MAX_OUTPUT_ZIP_CHUNK_SIZE = 50 * 1024 * 1024;

    private static final int PROGRESS_ID = 123122312;

    /*
     * App state
     */
    private volatile Boolean mIsRecording;

    private long startRecTime = 0;

    private Thread zipperThread;

    private long mDelay = 0;

    private LoggerApplication application;

    /*
     * UI Elements
     */
    private AbstractCamcorderPreview mCamcorderView;

    private CameraPreview mCameraView;

    private TextView mAccelXTextView;

    private TextView mAccelYTextView;

    private TextView mAccelZTextView;

    private TextView mGyroXTextView;

    private TextView mGyroYTextView;

    private TextView mGyroZTextView;

    private TextView mMagXTextView;

    private TextView mMagYTextView;

    private TextView mMagZTextView;

    private BarImageView mBatteryTempBarImageView;

    private TextView mBatteryTempTextView;

    private TextView mBatteryTempSpacerTextView;

    private LinearLayout mFlashingRecGroup;

    private TextView mRecTimeTextView;

    private BarImageView mStorageBarImageView;

    private TextView mStorageTextView;

    private TextView mStorageSpacerTextView;

    private SlidingDrawer mDiagnosticsDrawer;

    private SlidingDrawer mDataDrawer;

    private TextView mPictureCountView;

    private TextView mGpsLocationView;

    /*
     * Sensors
     */
    private List<Sensor> sensors;

    private SensorManager mSensorManager;

    private int mBatteryTemp = 0;

    private BufferedWriter mBatteryTempWriter;

    private HashMap<String, BufferedWriter> sensorLogFileWriters;

    private StatFs mStatFs;

    private int mFreeSpacePct;

    private BufferedWriter mGpsLocationWriter;

    private BufferedWriter mGpsStatusWriter;

    private BufferedWriter mGpsNmeaWriter;

    private GpsManager mGpsManager;

    /*
     * Event handlers
     */
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
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
            synchronized (mIsRecording) {
                if (mIsRecording) {
                    String valuesStr = "";
                    for (int i = 0; i < event.values.length; i++) {
                        valuesStr = valuesStr + event.values[i] + ",";
                    }
                    BufferedWriter writer = sensorLogFileWriters.get(sensor.getName());
                    try {
                        writer.write(
                              event.timestamp + "," + event.accuracy + "," + valuesStr + "\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float percentage = mBatteryTemp / TEMPERATURE_MAX;
            mBatteryTempBarImageView.setPercentage(percentage);
            int paddingTop = (int) ((1.0 - percentage) * UI_BAR_MAX_TOP_PADDING);
            mBatteryTempTextView.setText((mBatteryTemp / 10) + "°C");
            mBatteryTempSpacerTextView.setPadding(mBatteryTempSpacerTextView.getPaddingLeft(),
                    paddingTop, mBatteryTempSpacerTextView.getPaddingRight(),
                    mBatteryTempSpacerTextView.getPaddingBottom());
            try {
                mBatteryTempWriter.write(
                        System.currentTimeMillis() + "," + mBatteryTemp + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * Runnables
     */

    private Runnable updateRecTimeDisplay = new Runnable() {
        @Override
        public void run() {
            boolean isRecording;
            synchronized (mIsRecording) {
              isRecording = mIsRecording;
            }
            while (isRecording) {
                mStatFs = new StatFs(Environment.getExternalStorageDirectory().toString());
                final float percentage =
                        (float) (mStatFs.getBlockCount() - mStatFs.getAvailableBlocks())
                        / (float) mStatFs.getBlockCount();
                final int paddingTop = (int) ((1.0 - percentage) * UI_BAR_MAX_TOP_PADDING);
                mFreeSpacePct = (int) (percentage * 100);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlashingRecGroup != null) {
                            if (mFlashingRecGroup.getVisibility() == View.VISIBLE) {
                                mFlashingRecGroup.setVisibility(View.INVISIBLE);
                            } else {
                                mFlashingRecGroup.setVisibility(View.VISIBLE);
                            }
                        }
                        if (mRecTimeTextView != null) {
                            mRecTimeTextView.setText(DateUtils.formatElapsedTime(
                                    (System.currentTimeMillis() - startRecTime) / 1000));
                        }
                        if ((mPictureCountView != null) && (mCameraView != null)) {
                            mPictureCountView.setText(
                                    "Pictures taken: " + mCameraView.getPictureCount());
                        }
                        mStorageBarImageView.setPercentage(percentage);
                        mStorageTextView = (TextView) findViewById(R.id.storage_text);
                        mStorageTextView.setText(mFreeSpacePct + "%");
                        mStorageSpacerTextView.setPadding(mStorageSpacerTextView.getPaddingLeft(),
                                paddingTop, mStorageSpacerTextView.getPaddingRight(),
                                mStorageSpacerTextView.getPaddingBottom());
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (mIsRecording) {
                  isRecording = mIsRecording;
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mFlashingRecGroup != null) {
                        mFlashingRecGroup.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (LoggerApplication) getApplication();

        // Keep the screen on to make sure the phone stays awake.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mIsRecording = false;
        Date date = new Date();

        final int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_VIDEO_FRONT);

        if ((mode == MODE_VIDEO_FRONT) || (mode == MODE_VIDEO_BACK)) {
            if (mode == MODE_VIDEO_FRONT) {
                setContentView(R.layout.video_front_mode);
            } else {
                setContentView(R.layout.video_back_mode);
            }
            mCamcorderView = (AbstractCamcorderPreview) findViewById(R.id.surface);
            final SurfaceHolder previewHolder = mCamcorderView.getHolder();
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        } else {
            mDelay = getIntent().getIntExtra(EXTRA_PICTURE_DELAY, 30) * 1000;
            if (mDelay < 0) {
                mDelay = 0;
            }
            setContentView(R.layout.pictures_mode);
            mCameraView = (CameraPreview) findViewById(R.id.surface);
        }

        // Setup the initial available space
        mStatFs = new StatFs(Environment.getExternalStorageDirectory().toString());
        float percentage = (float) (mStatFs.getBlockCount() - mStatFs.getAvailableBlocks()) / (float) mStatFs.getBlockCount();
        mFreeSpacePct = (int) (percentage * 100);
        mStorageBarImageView = (BarImageView) findViewById(R.id.storage_barImageView);
        mStorageBarImageView.setPercentage(percentage);
        mStorageTextView = (TextView) findViewById(R.id.storage_text);
        mStorageSpacerTextView = (TextView) findViewById(R.id.storage_text_spacer);
        mStorageTextView.setText(mFreeSpacePct + "%");
        mStorageSpacerTextView.setPadding(mStorageSpacerTextView.getPaddingLeft(),
                (int) ((1 - percentage) * UI_BAR_MAX_TOP_PADDING),
                mStorageSpacerTextView.getPaddingRight(),
                mStorageSpacerTextView.getPaddingBottom());
        mFlashingRecGroup = (LinearLayout) findViewById(R.id.flashingRecGroup);
        mRecTimeTextView = (TextView) findViewById(R.id.recTime);
        mGpsLocationView = (TextView) findViewById(R.id.gpsLocation);
        mPictureCountView = (TextView) findViewById(R.id.pictureCount);

        mDataDrawer = (SlidingDrawer) findViewById(R.id.dataDrawer);
        mDiagnosticsDrawer = (SlidingDrawer) findViewById(R.id.diagnosticsDrawer);

        final ImageButton recordButton = (ImageButton) findViewById(R.id.button_record);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (mIsRecording) {
                    if ((mode == MODE_VIDEO_FRONT) || (mode == MODE_VIDEO_BACK)) {
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
                            cleanup();
                        }
                    } else {
                        if (!mIsRecording) {
                            try {
                                mIsRecording = true;
                                recordButton.setImageResource(R.drawable.rec_button_pressed);
                                mCameraView.takePictures(mDelay);
                                new Thread(updateRecTimeDisplay).start();
                            } catch (Exception e) {
                                Log.e("ls", "Taking pictures has failed...", e);
                                Toast.makeText(getApplicationContext(),
                                        "Taking pictures is not possible at the moment: "
                                                + e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            cleanup();
                        }
                    }
                }
            }
        });

        final Button dataHandleButton = (Button) findViewById(R.id.dataHandleButton);
        final ImageButton dataButton = (ImageButton) findViewById(R.id.button_data);
        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
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
            @Override
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

    @Override
    public void onPause() {
        super.onPause();
        cleanup();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id != PROGRESS_ID) {
            return super.onCreateDialog(id);
        }
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        // The setMessage call must be in both onCreateDialog and onPrepareDialog otherwise it will
        // fail to update the dialog in onPrepareDialog.
        progressDialog.setMessage("processing...");

        return progressDialog;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        super.onPrepareDialog(id, dialog, bundle);

        if (id != PROGRESS_ID) {
          return;
        }

        final ProgressDialog progressDialog = (ProgressDialog) dialog;
        progressDialog.setMessage("processing...");
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                int done = msg.getData().getInt("percentageDone");
                String status = msg.getData().getString("status");
                progressDialog.setProgress(done);
                progressDialog.setMessage(status);
            }
        };

        zipperThread = new Thread() {
            @Override
            public void run() {
                ZipItUpRequest request = new ZipItUpRequest();
                String directoryName = application.getLoggerPathPrefix();
                request.setInputFiles(
                        new FileListFetcher().getFilesAndDirectoriesInDir(directoryName));
                request.setOutputFile(directoryName + "/logged-data.zip");
                request.setMaxOutputFileSize(MAX_OUTPUT_ZIP_CHUNK_SIZE);
                request.setDeleteInputfiles(true);

                try {
                    new ZipItUpProcessor(request).zipIt(handler);
                } catch (IOException e) {
                    Log.e("Oh Crap!", "IoEx", e);
                }
                // closing dialog
                progressDialog.dismiss();
                application.generateNewFilePathUniqueIdentifier();

                // TODO: Need to deal with empty directories that are created if another recording
                // session is never started.
                initSensorLogFiles();
            }
        };
        zipperThread.start();
    }

    private void setupSensors() {
        initSensorUi();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        initSensorLogFiles();
        initBattery();
        initGps();

        printSensors();
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

    private void initSensorLogFiles() {
        sensorLogFileWriters = new HashMap<String, BufferedWriter>();

        String directoryName = application.getDataLoggerPath();
        File directory = new File(directoryName);
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException(
                        "Path to file could not be created. " + directory.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Directory could not be created. " + e.toString());
            }
        }

        for (int i = 0; i < sensors.size(); i++) {
            Sensor s = sensors.get(i);
            String sensorFilename = directoryName + s.getName().replaceAll(" ", "_") + "_"
                    + application.getFilePathUniqueIdentifier() + ".txt";
            File file = new File(sensorFilename);
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
        mBatteryTempWriter = createBufferedWriter("/BatteryTemp_", directoryName);

        // GPS is another special case since it is not a real sensor
        mGpsLocationWriter = createBufferedWriter("/GpsLocation_", directoryName);
        mGpsStatusWriter = createBufferedWriter("/GpsStatus_", directoryName);
        mGpsNmeaWriter = createBufferedWriter("/GpsNmea_", directoryName);
    }

    /**
     * Creates a new BufferedWriter.
     * @param prefix The prefix for the file that we're writing to.
     * @return A BufferedWriter for a file in the specified directory. Null if creation failed.
     */
    private BufferedWriter createBufferedWriter(String prefix, String directoryName) {
        String filename =
              directoryName + prefix + application.getFilePathUniqueIdentifier() + ".txt";
        File file = new File(filename);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bufferedWriter;
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

        int textColor = Color.WHITE;
        String prefix = "";
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                prefix = "  ";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                prefix = "  *";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                prefix = "  **";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                prefix = "  ***";
                break;
        }

        xTextView.setTextColor(textColor);
        yTextView.setTextColor(textColor);
        zTextView.setTextColor(textColor);
        xTextView.setText(prefix + numberDisplayFormatter(values[0]));
        yTextView.setText(prefix + numberDisplayFormatter(values[1]));
        zTextView.setText(prefix + numberDisplayFormatter(values[2]));

    }

    private void initGps() {
        mGpsManager = new GpsManager(this, new GpsManagerListener() {

            @Override
            public void onGpsLocationUpdate(long time, float accuracy, double latitude,
                    double longitude, double altitude, float bearing, float speed) {
                try {
                    if (mGpsLocationView != null) {
                        mGpsLocationView.setText("Lat: " + latitude + "\nLon: " + longitude);
                    }
                    mGpsLocationWriter.write(
                            time + "," + accuracy + "," + latitude + "," + longitude + ","
                                    + altitude + "," + bearing + "," + speed + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onGpsNmeaUpdate(long time, String nmeaString) {
                try {
                    mGpsNmeaWriter.write(time + "," + nmeaString + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onGpsStatusUpdate(
                    long time, int maxSatellites, int actualSatellites, int timeToFirstFix) {
                try {
                    mGpsStatusWriter.write(time + "," + maxSatellites + "," + actualSatellites + ","
                            + timeToFirstFix + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
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
        mBatteryTempBarImageView = (BarImageView) findViewById(R.id.temperature_barImageView);
        mBatteryTempTextView = (TextView) findViewById(R.id.batteryTemp_text);
        mBatteryTempSpacerTextView = (TextView) findViewById(R.id.batteryTemp_text_spacer);
        IntentFilter batteryTemperatureFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(broadcastReceiver, batteryTemperatureFilter);
    }

    /**
     * Prints all the sensors to LogCat.
     */
    private void printSensors() {
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 0; i < sensors.size(); i++) {
            Sensor s = sensors.get(i);
            Log.d("DEBUG", s.getName());
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
            mGpsLocationWriter.close();
            mGpsStatusWriter.close();
            mGpsNmeaWriter.close();
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

    private void cleanup() {
        try {
            mGpsManager.shutdown();
            synchronized (mIsRecording) {
                if (mCamcorderView != null) {
                    if (mIsRecording) {
                      mCamcorderView.stopRecording();
                    }
                    mCamcorderView.releaseRecorder();
                }
                if (mCameraView != null) {
                    if (mIsRecording) {
                      mCameraView.stop();
                    }
                    mCameraView.releaseCamera();
                }
                mIsRecording = false;
            }
            startRecTime = 0;
            ((ImageButton) findViewById(R.id.button_record)).setImageResource(
                  R.drawable.rec_button_up);
            if (mRecTimeTextView != null) {
                mRecTimeTextView.setText(R.string.start_rec_time);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeFiles();
            showDialog(PROGRESS_ID);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            boolean shouldExitApp;
            synchronized (mIsRecording) {
                shouldExitApp = !mIsRecording;
            }
            cleanup();
            if (shouldExitApp) {
              finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
