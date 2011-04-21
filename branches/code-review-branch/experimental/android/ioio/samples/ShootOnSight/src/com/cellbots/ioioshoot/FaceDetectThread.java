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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A worker thread which will detect faces and notify any callbacks if a face is
 * detected.
 *
 * @author raymes@google.com (Raymes Khoury)
 */
public class FaceDetectThread extends Thread implements CameraView.Callback {
    interface Callback {
        public void faceDetected(
                PointF face, float eyesDistance, float confidence, PointF imageSize);

        public void faceNotDetected();
    }

    private class Image {
        byte[] mData;

        int mHeight;

        int mWidth;

        int mFormat;

        boolean mReversed;

        public Image(byte[] data, int width, int height, int format, boolean reversed) {
            mData = data;
            mWidth = width;
            mHeight = height;
            mFormat = format;
            mReversed = reversed;
        }
    }

    private ArrayBlockingQueue<Image> mImages;

    private PointF mImageSize;

    private Face[] mFaces;

    private ArrayList<Callback> callbacks;

    private FaceDetector mFaceDetector;

    private boolean mProcessing;

    private static final float MIN_QUALITY = 0.4f;

    private static final float INCREMENT_QUALITY = 0.1f;

    public FaceDetectThread() {
        mImages = new ArrayBlockingQueue<Image>(1);
        mImageSize = new PointF();
        mFaces = new Face[1];
        callbacks = new ArrayList<Callback>();
        mProcessing = false;
    }

    public void addCallback(Callback c) {
        callbacks.add(c);
    }

    public void addFace(byte[] data, int width, int height, int format, boolean reversed) {
        boolean processing = false;
        synchronized (this) {
            processing = mProcessing;
        }
        if (mImages.isEmpty() && !processing) {
            Image newImage = new Image(data.clone(), width, height, format, reversed);
            mImages.add(newImage);
        }
    }

    @Override
    public void run() {
        while (true) {
            Image current = null;
            try {
                current = mImages.take();
                synchronized (this) {
                    mProcessing = true;
                }
            } catch (InterruptedException e) {
                continue;
            }
            Face f = findFace(current.mData, current.mWidth, current.mHeight, current.mFormat);
            if (f != null) {
                PointF point = new PointF();
                f.getMidPoint(point);
                if (current.mReversed) {
                    point.x = mImageSize.x - point.x;
                }
                for (Callback c : callbacks) {
                    c.faceDetected(point, f.eyesDistance(), f.confidence(), mImageSize);
                }
            } else {
                for (Callback c : callbacks) {
                    c.faceNotDetected();
                }
            }
            synchronized (this) {
                mProcessing = false;
            }

        }

    }

    public Face findFace(byte[] data, int width, int height, int format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(data, format, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        float factor = (float) 0.8;
        image = Bitmap.createScaledBitmap(
                image, (int) (width * factor), (int) (height * factor), true);
        mImageSize.set(image.getWidth(), image.getHeight());

        if (mFaceDetector == null)
            mFaceDetector = new FaceDetector(image.getWidth(), image.getHeight(), 1);
        int faceCount = mFaceDetector.findFaces(image, mFaces);
        if (faceCount > 0) {
            return mFaces[0];
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.cellbots.directcontrol.CameraView.Callback#imageReady(byte[],
     * int, int, int)
     */
    @Override
    public void imageReady(byte[] data, int width, int height, int format, boolean reversed) {
        addFace(data, width, height, format, reversed);
    }

}
