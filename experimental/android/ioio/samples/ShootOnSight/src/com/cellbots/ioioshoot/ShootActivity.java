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

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashMap;

import ioio.lib.IOIO;
import ioio.lib.IOIOException.ConnectionLostException;
import ioio.lib.IOIOException.InvalidOperationException;
import ioio.lib.IOIOException.InvalidStateException;
import ioio.lib.IOIOException.OperationAbortedException;
import ioio.lib.IOIOException.SocketException;
import ioio.lib.Output;
import ioio.lib.pic.IOIOImpl;

/**
 * Sample app for IOIO board. When a face is detected, it will turn on the
 * trigger. The idea is that the trigger pin is attached to some automated toy
 * gun that can be fired electronically.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class ShootActivity extends Activity {
    private CameraView mCameraView;

    private FaceDetectView mFaceDetectView;

    private FaceDetectThread mFaceDetectThread;

    private IOIO ioio;

    private Output<Boolean> trigger;

    private TextToSpeech mTts;

    private OnUtteranceCompletedListener mUtteranceCompletedListener =
            new OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    if (trigger != null) {
                        try {
                            trigger.write(true);
                        } catch (ConnectionLostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvalidStateException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Display display = getWindowManager().getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int height = metrics.heightPixels;
        int width = metrics.widthPixels;
        mFaceDetectThread = new FaceDetectThread();
        mFaceDetectThread.start();
        mCameraView = new CameraView(this);
        mCameraView.addCallback(mFaceDetectThread);
        mFaceDetectView = new FaceDetectView(this, width, height);
        mFaceDetectThread.addCallback(mFaceDetectView);

        height = 480;
        width = 640;
        setContentView(mCameraView);
        addContentView(mFaceDetectView, new LayoutParams(width, height));

        final Activity self = this;

        mTts = new TextToSpeech(this, new OnInitListener() {
            @Override
            public void onInit(int arg0) {
                mTts.setOnUtteranceCompletedListener(mUtteranceCompletedListener);
            }
        });

        ioio = IOIOImpl.getInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ioio.waitForConnect();
                    trigger = ioio.openDigitalOutput(1, false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mTts != null) {
                                mTts.speak("Locked and loaded.", 0, null);
                            }
                            Toast.makeText(self, "Ready", 0).show();
                            isReady = true;
                        }
                    });
                } catch (OperationAbortedException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (ConnectionLostException e) {
                    e.printStackTrace();
                } catch (InvalidOperationException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean inAttackMode = false;

    private boolean isReady = false;

    public void openFire() {
        if (!isReady) {
            return;
        }
        if (inAttackMode) {
            return;
        }
        inAttackMode = true;
        if (mTts != null) {
            HashMap<String, String> mTtsParams = new HashMap<String, String>();
            mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ATTACK");
            mTts.speak("Target detected! Initiating attack sequence!", 0, mTtsParams);
        }
    }

    public void ceaseFire() {
        if (!isReady) {
            return;
        }
        if (trigger != null) {
            try {
                trigger.write(false);
            } catch (ConnectionLostException e) {
                e.printStackTrace();
            } catch (InvalidStateException e) {
                e.printStackTrace();
            }
        }
        if (!inAttackMode) {
            return;
        }
        inAttackMode = false;
        if (mTts != null) {
            mTts.speak("Target lost.", 0, null);
        }
    }
}
