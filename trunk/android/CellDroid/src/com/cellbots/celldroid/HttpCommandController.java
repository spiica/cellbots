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

import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class reads command from the specified HTTP URL.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class HttpCommandController {
    
    private static final String TAG = "HttpCommandController";
    
    private String mHttpCmdUrl;
    
    private HttpMessageListener mMessageListener;
    
    private String prevCmd;
    
    private boolean stopReading;
    
    private URL url;
    
    public HttpCommandController(String cmdUrl, HttpMessageListener listener) {
        mMessageListener = listener;
        mHttpCmdUrl = cmdUrl;
        stopReading = false;
    }
    
    public void listenForMessages() {
        stopReading = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                while (!stopReading) {
                    try {
                        if (url == null) {
                            url = new URL(mHttpCmdUrl);
                        }
                        URLConnection cn = url.openConnection();
                        cn.connect();
                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(cn.getInputStream()), 1024);
                        String cmd = rd.readLine();
                        if (cmd != null && !cmd.equals(prevCmd) && mMessageListener != null) {
                            mMessageListener.onMessage(cmd);
                            prevCmd = cmd;
                        }
                        Thread.sleep(200);
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Error processing URL: " + e.getMessage());
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading command from URL: " + mHttpCmdUrl + " : " +
                                e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    public void disconnect() {
        stopReading = true;
    }
    
    public interface HttpMessageListener {
        public void onMessage(String message);
    }
}
