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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

/**
 * Sender and receiver for GTalk-based remote-controller, using the asmack library.
 * http://code.google.com/p/asmack
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class GTalkController {
    
    private static final String TAG = "GTalkController";
    
    private static final String SERVICE = "gmail.com";
    
    private String mUsername;
    
    private String mPassword;
    
    private XMPPConnection mConnection;
    
    private GTalkMessageListener mMessageListener;
    
	public GTalkController(String username, String password, GTalkMessageListener listener) {
	    int index = username.indexOf('@');
        mUsername = index >= 0 ? username.substring(0, index) : username;
        index = password.indexOf('@');
        mPassword = index >= 0 ? password.substring(0, index) : password;
		mUsername += "@" + SERVICE;
		mMessageListener = listener;
	}
	
	public void connect() {
		// Create a connection
        mConnection = new XMPPConnection(SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnection.connect();
                    Log.i(TAG, "Connected to " + mConnection.getHost());
                    mConnection.login(mUsername, mPassword);
                    Log.i(TAG, "Logged in as " + mConnection.getUser());
                    // Set the status to available
                    Presence presence = new Presence(Presence.Type.available);
                    mConnection.sendPacket(presence);
                    if (mMessageListener != null) {
                        mMessageListener.onConnected();
                    }
                } catch (XMPPException ex) {
                    Log.e(TAG, "Failed to connect/login as " + mUsername);
                    Log.e(TAG, ex.toString());
                    mConnection = null;
                }            
            }
        }).start();
	}
	
	public void send(String to, String message) {
	    int index = to.indexOf('@');
	    to = index >= 0 ? to.substring(0, index) : to;
		if (mConnection == null)
			return;
		Message msg = new Message(to + "@" + SERVICE, Message.Type.chat);
        msg.setBody(message);
        mConnection.sendPacket(msg);
	}
	
	public void disconnect() {
	    if (mConnection != null)
	        mConnection.disconnect();
	    if (mMessageListener != null)
            mMessageListener.onDisconnected();
	}
	
	public void setGTalkMessageListener(GTalkMessageListener listener) {
		mMessageListener = listener;
	}
	
	public void listenForMessages() {
		// Add a packet listener to get messages sent to us
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        mConnection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                if (message.getBody() != null && mMessageListener != null) {
                	mMessageListener.onMessage(message);
                }
            }
        }, filter);
	}
	
	public interface GTalkMessageListener {
		public void onMessage(Message msg);
		public void onConnected();
		public void onDisconnected();
	}
}
