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
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * View that handles the video recording functionality. Parts of this code were
 * taken from memofy's tutorial at:
 * http://memofy.com/memofy/show/2008c618f15fc61801ca038cbfe138/how-to-use-mediarecorder-in-android
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class FrontCamcorderPreview extends AbstractCamcorderPreview implements
        SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    public static final String TAG = "CELLBOTS LOGGER";

    private LoggerApplication application;

    private MediaRecorder recorder;

    private SurfaceHolder holder;

    private FileOutputStream fos;

    private static final int FRAME_RATE = 15;

    private static final int CIF_WIDTH = 640;

    private static final int CIF_HEIGHT = 480;

    private boolean initialized;

    private Camera camera;

    private boolean previewing = false;

    public FrontCamcorderPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        application = (LoggerApplication) context.getApplicationContext();

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void startPreview() {
        Log.v(TAG, "startPreview");
        if (camera == null) {
            // If the activity is paused and resumed, camera device has been
            // released and we need to open the camera.
            try {
                camera = Camera.open(1);                
            } catch (NoSuchMethodError e) {
                // Method call not available below API level 9. We can't access
                // the front camera.
                e.printStackTrace();
                return;
            }
        }

        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            camera.setDisplayOrientation(90);
            camera.startPreview();
            previewing = true;
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }

        initializeRecording();
    }

    private void closeCamera() {
        Log.v(TAG, "closeCamera");
        if (camera == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        // If we don't lock the camera, release() will fail.
        camera.lock();
        camera.release();
        camera = null;
        previewing = false;
    }

    @Override
    public void initializeRecording() {
        if (!initialized) {
            if (camera == null) {
                camera = Camera.open(1);
                camera.setDisplayOrientation(90);
            }
            camera.unlock();
            recorder = new MediaRecorder();
            recorder.setOnErrorListener(this);
            recorder.setOnInfoListener(this);
            recorder.setCamera(camera);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            String path = application.getVideoFilepath();

            Log.i(TAG, "Video file to use: " + path);

            final File file = new File(path);
            final File directory = file.getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
                try {
                    throw new IOException(
                            "Path to file could not be created. " + directory.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Directory could not be created. " + e.toString());
                }
            }

            if (file.exists()) {
                file.delete();
            }

            if (recorder != null) {
                try {
                    file.createNewFile();
                    fos = new FileOutputStream(path);
                    recorder.setOutputFile(fos.getFD());
                    recorder.setVideoSize(CIF_WIDTH, CIF_HEIGHT);
                    recorder.setVideoFrameRate(FRAME_RATE);
                    recorder.setPreviewDisplay(holder.getSurface());
                    recorder.prepare();
                } catch (IllegalStateException e) {
                    Log.e("ls", e.toString(), e);
                } catch (IOException e) {
                    Log.e("ls", e.toString(), e);
                }
            }
            initialized = true;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        Log.e(TAG, "Error received in media recorder: " + what + ", " + extra);
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        Log.e(TAG, "Info received from media recorder: " + what + ", " + extra);
    }

    @Override
    public void releaseRecorder() throws IOException {

        if (recorder != null) {
            recorder.reset();
            recorder.release();
        }

        if (fos != null) {
            fos.close();
        }

        closeCamera();

        initialized = false;
    }

    @Override
    public void startRecording() {
        recorder.start();
    }

    @Override
    public void stopRecording() {
        if (recorder != null) {
            recorder.stop();
        }
    }

}
