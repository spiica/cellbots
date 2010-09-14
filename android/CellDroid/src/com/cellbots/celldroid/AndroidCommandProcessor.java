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

package com.cellbots.celldroid;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * Processes commands that are targeted at the Android device running on the
 * robot. This handles tasks such as the TextToSpeech, Camera, etc.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class AndroidCommandProcessor {

    private Context mParent;

    private TextToSpeech mTts;

    public AndroidCommandProcessor(Context ctx) {
        mParent = ctx;
        mTts = new TextToSpeech(mParent, new OnInitListener() {
            @Override
            public void onInit(int status) {
                mTts.speak("I need your clothes, your boots, and your motorcycle.", 0, null);
            }
        });
    }

    public boolean processCommand(String command) {
        Log.e("debug", command);
        // Check if this command is intended for the Android device itself
        if (command.startsWith("speak:")) {
            if (mTts != null) {
                mTts.speak(command.replaceFirst("speak:", ""), 0, null);
            }
            return true;
        } else if (command.startsWith("video on")) {
            Intent i = new Intent();
            i.setClassName("com.cellbots.remoteEyes", "com.cellbots.remoteEyes.RemoteEyesActivity");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mParent.startActivity(i);
            return true;
        } else if (command.startsWith("torch on")) {
            Intent i = new Intent("android.intent.action.REMOTE_EYES_COMMAND");
            i.putExtra("TORCH", true);
            mParent.sendBroadcast(i);
            return true;
        } else if (command.startsWith("torch off")) {
            Intent i = new Intent("android.intent.action.REMOTE_EYES_COMMAND");
            i.putExtra("TORCH", false);
            mParent.sendBroadcast(i);
            return true;
        }
        return false;
    }

    public void shutdown() {
        if (mTts != null) {
            mTts.shutdown();
        }
    }

}
