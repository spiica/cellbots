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

package com.cellbots.ioioshoot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.view.SurfaceView;

/**
 * Displays a detected face on the screen.
 *
 * @author raymes@google.com (Raymes Khoury)
 */
public class FaceDetectView extends SurfaceView implements FaceDetectThread.Callback {
    private PointF mCurrentFace;

    private PointF mImageSize;

    private FaceDetectThread mFaceDetectorThread;

    private int mWidth;

    private int mHeight;

    private float mEyesDistance;

    private float mConfidence;

    private ShootActivity parent;

    public FaceDetectView(Context context, int width, int height) {
        super(context);
        parent = (ShootActivity) context;
        setWillNotDraw(false);
        mCurrentFace = new PointF();
        mImageSize = new PointF();
        mFaceDetectorThread = new FaceDetectThread();
        mFaceDetectorThread.start();
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mImageSize.x == 0 || mImageSize.y == 0)
            return;
        double x = mCurrentFace.x * mWidth / mImageSize.x;
        double y = mCurrentFace.y * mHeight / mImageSize.y;
        canvas.setMatrix(null);
        Log.i("", "drawing");
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (mConfidence * 75), 0, 255, 0));
        canvas.drawCircle((int) x, (int) y, (int) (mEyesDistance * 3), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        paint.setColor(Color.argb(150, 0, 255, 0));
        canvas.drawCircle((int) x, (int) y, (int) (mEyesDistance * 3), paint);
    }

    @Override
    public void faceDetected(PointF face, float eyesDistance, float confidence, PointF imageSize) {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBackgroundColor(Color.argb(125, 255, 0, 0));
                parent.openFire();
            }
        });

        /*
         *mImageSize.set(imageSize); mCurrentFace.set(face); mEyesDistance =
         * eyesDistance; mConfidence = confidence;
         */
        postInvalidate();
    }

    @Override
    public void faceNotDetected() {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBackgroundColor(Color.TRANSPARENT);
                parent.ceaseFire();
            }
        });
    }

}
