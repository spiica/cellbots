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
package com.cellbots.ioioshoot;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import java.io.IOException;

/**
 * View for handling the camera.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class CameraView extends SurfaceView implements Callback {
    private Camera mCamera;

    private SurfaceHolder holder;

    private byte[] mCallbackBuffer = new byte[460800];

    private FaceDetectThread faceDetectorCallback;

    public interface Callback {
        public void imageReady(byte[] data, int width, int height, int format, boolean reversed);
    }

    /**
     * @param context
     */
    public CameraView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void addCallback(FaceDetectThread faceDetectThread) {
        faceDetectorCallback = faceDetectThread;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        acquireCamera(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        final Parameters params = mCamera.getParameters();
        Log.e("debug 1", params.getPreviewSize().width + ", " + params.getPreviewSize().height);
        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] imageData, Camera arg1) {
                if (faceDetectorCallback != null) {
                    faceDetectorCallback.imageReady(imageData, params.getPreviewSize().width,
                            params.getPreviewSize().height, params.getPreviewFormat(), false);
                }
                if (mCamera != null) {
                    mCamera.addCallbackBuffer(mCallbackBuffer);
                }
            }
        });
        mCamera.addCallbackBuffer(mCallbackBuffer);
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    public void acquireCamera(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

}
