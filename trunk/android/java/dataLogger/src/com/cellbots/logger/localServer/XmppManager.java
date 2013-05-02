
package com.cellbots.logger.localServer;

import android.content.Context;
import android.util.Log;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 * Wrapper to simplify working with asmack XMPP.
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class XmppManager {
    public interface XmppMessageListener {
        public void onMessageReceived(String from, String message);
    }

    private static final String TAG = "XmppManager";

    // TODO: Make this configurable!
    private static final String SERVICE = "gmail.com";

    private String mUsername;

    private String mPassword;

    private Context mParent;

    private XMPPConnection mConnection;

    private XmppMessageListener mMessageListener;

    public XmppManager(
            Context parent, XmppMessageListener messageListener, String username, String password) {
        mParent = parent;
        int index = username.indexOf('@');
        mUsername = index >= 0 ? username.substring(0, index) : username;
        mPassword = password;
        mUsername += "@" + SERVICE;
        mMessageListener = messageListener;
    }

    public void connect() {
        // Create a connection
        SmackAndroid.init(mParent);
        int DNSSRV_TIMEOUT = 1000 * 30; // 30s
        try {
            SASLAuthentication.supportSASLMechanism("PLAIN", 0);
            AndroidConnectionConfiguration conf = new AndroidConnectionConfiguration(
                    SERVICE, DNSSRV_TIMEOUT);
            conf.setTruststoreType("AndroidCAStore");
            conf.setTruststorePassword(null);
            conf.setTruststorePath(null);
            mConnection = new XMPPConnection(conf);

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

                        mConnection.addPacketListener(new PacketListener() {
                                @Override
                            public void processPacket(Packet packet) {
                                Message message = (Message) packet;
                                if (message.getBody() != null) {
                                    mMessageListener.onMessageReceived(
                                            message.getFrom(), message.getBody());
                                }
                            }
                        }, new MessageTypeFilter(Message.Type.chat));

                    } catch (XMPPException ex) {
                        Log.e(TAG, "Failed to connect/login as " + mUsername);
                        Log.e(TAG, ex.toString());
                        mConnection = null;
                    }
                }
            }).start();
        } catch (Exception e) {
            // TODO(clchen): Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (mConnection != null) {
            mConnection.disconnect();
        }
    }

    public void sendMessage(String to, String message) {
        Log.e(TAG, "To:" + to + ", Message:" + message);
        if (to == null) {
            return;
        }
        int index = to.indexOf('/');
        to = index >= 0 ? to.substring(0, index) : to;
        if (mConnection == null) {
            return;
        }
        Message msg = new Message(to, Message.Type.chat);
        msg.setBody(message);
        try {
            mConnection.sendPacket(msg);
        } catch (IllegalStateException e) {
            // Got disconnected.
            Log.e(TAG, "Disconnected. Failed to send: " + message);
        }
    }

}
