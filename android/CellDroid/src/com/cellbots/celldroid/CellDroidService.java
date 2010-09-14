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

import com.cellbots.celldroid.GTalkController.GTalkMessageListener;
import com.cellbots.celldroid.HttpCommandController.HttpMessageListener;
import com.cellbots.celldroid.ICellDroidService.Stub;

import org.jivesoftware.smack.packet.Message;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service that manages the connection to the robot and to the
 * remote-controller.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
public class CellDroidService extends Service {

    private static final String TAG = "CellDroidService";

    private static final String ACTION = "android.intent.action.USE_CELLDROID_SERVICE";

    private static final String CATEGORY = "android.intent.category.CELLDROID_SERVICE";
    
    private GTalkController gtalk;
    
    private HttpCommandController httpCmdController;

    private RobotBtController robotController;

    private boolean connected = false;
    
    private AndroidCommandProcessor commandProcessor;

    private GTalkMessageListener msgListener = new GTalkMessageListener() {
        @Override
        public void onConnected() {
            connected = true;
        }

        @Override
        public void onDisconnected() {
            connected = false;
        }

        @Override
        public void onMessage(Message msg) {
            Log.w(TAG, msg.getFrom() + ": " + msg.getBody());
            if (!commandProcessor.processCommand(msg.getBody())) {
                robotController.write(msg.getBody());
            }
            gtalk.send(msg.getFrom(), "ACK");
        }
    };

    private HttpMessageListener httpMsgListener = new HttpMessageListener() {
        @Override
        public void onMessage(String message) {
            Log.w(TAG, "HTTP command: " + message);
            if (!commandProcessor.processCommand(message)) {
                robotController.write(message);
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        super.onCreate();
        commandProcessor = new AndroidCommandProcessor(this);
    }

    @Override
    public void onDestroy() {
        commandProcessor.shutdown();
        super.onDestroy();
    }

    public boolean isConnected() {
        return connected;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            for (String category : intent.getCategories()) {
                if (category.equals(CATEGORY)) {
                    return mBinder;
                }
            }
        }
        return null;
    }

    /**
     * Stub for exposing the service's interface.
     */
    private final ICellDroidService.Stub mBinder = new Stub() {
        @Override
        public void connect(String username, String password, String cmdUrl, String robotBt) {
            robotController = new RobotBtController(username, robotBt);
            gtalk = new GTalkController(username, password, msgListener);
            gtalk.connect();
            gtalk.listenForMessages();
            if (cmdUrl != null && !cmdUrl.equals("")) {
                httpCmdController = new HttpCommandController(cmdUrl, httpMsgListener);
                httpCmdController.listenForMessages();
            }
        }

        @Override
        public void disconnect() {
            if (gtalk != null)
                gtalk.disconnect();
            if (httpCmdController != null)
                httpCmdController.disconnect();
            if (robotController != null)
                robotController.disconnect();
        }
    };
}
