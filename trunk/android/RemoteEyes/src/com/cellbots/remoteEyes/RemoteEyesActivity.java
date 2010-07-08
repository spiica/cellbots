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
package com.cellbots.remoteEyes;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * Remote eyes turns your Android device into a wireless web cam.
 *
 * With Froyo's camera enhancements (ability to manage the preview memory buffer
 * + access to Skia for quickly converting from YUV to JPG), it is possible to
 * capture and upload preview frames fast enough for it to look like video.
 * 
 * The behavior is that a new frame will not be captured and processed until the
 * previous frame has been processed and uploaded. This means that if the
 * connection slows down, the video may have brief stutters, but it won't fall
 * behind.
 * 
 * See put.php for a simple, working example of what is running server side.
 * Once you have put.php uploaded to your server, be sure to remember to update
 * "putUrl", "server", and "port" in the Remote Eyes code to match your server
 * instead of "myexampleserver.com".
 * 
 * See remote_eyes.html for the client HTML page that can be used to see what
 * Remote Eyes is seeing.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */


public class RemoteEyesActivity extends Activity implements Callback {
    private SurfaceHolder mHolder;

    private final String putUrl = "http://myexampleserver.com/dropbox/put.php";
    private final String server = "myexampleserver.com";
    private final int port = 80;

    private SurfaceView mPreview;

    private Camera mCamera;

    private HttpConnection mConnection;

    private HttpState mHttpState;

    private Rect r;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        out = new ByteArrayOutputStream();

        resetConnection();
        mHttpState = new HttpState();

        setContentView(R.layout.main);
        mPreview = (SurfaceView) findViewById(R.id.preview);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void resetConnection() {
        mConnection = new HttpConnection(server, port);
        try {
            mConnection.open();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private int previewHeight = 0;

    private int previewWidth = 0;

    private int previewFormat = 0;

    private boolean isUploading = false;

    byte[] output;

    byte[] mCallbackBuffer;

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mHolder.setFixedSize(w, h);
        // Start the preview
        Parameters params = mCamera.getParameters();
        previewHeight = params.getPreviewSize().height;
        previewWidth = params.getPreviewSize().width;
        previewFormat = params.getPreviewFormat();

        // Crop the edges of the picture to reduce the image size
        r = new Rect(100, 100, previewWidth - 100, previewHeight - 100);

        output = new byte[15000];
        mCallbackBuffer = new byte[460800];
        
        mCamera.setParameters(params);
        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
            public void onPreviewFrame(byte[] imageData, Camera arg1) {
                if (!isUploading) {
                    isUploading = true;
                    uploadImage(imageData);
                }
            }
        });
        mCamera.addCallbackBuffer(mCallbackBuffer);
        mCamera.startPreview();
    }

    ByteArrayOutputStream out;

    private void uploadImage(byte[] imageData) {
        try {
            YuvImage yuvImage = new YuvImage(imageData, previewFormat, previewWidth, previewHeight,
                    null);
            yuvImage.compressToJpeg(r, 20, out); // Tweak the quality here - 20
                                                 // seems pretty decent for
                                                 // quality + size.
            PutMethod put = new PutMethod(putUrl);
            put.setRequestBody(new ByteArrayInputStream(out.toByteArray()));
            put.execute(mHttpState, mConnection);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            resetConnection();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            resetConnection();
        } finally {
            out.reset();
            mCamera.addCallbackBuffer(mCallbackBuffer);
            isUploading = false;
        }

    }

}
