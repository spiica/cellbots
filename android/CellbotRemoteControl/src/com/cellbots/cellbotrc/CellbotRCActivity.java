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

package com.cellbots.cellbotrc;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.client.ClientProtocolException;

import com.cellbots.cellbotrc.CellbotJoystick.JoystickEventListener;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * A remote control for driving Cellbots. The remote control talks to the
 * cellbot's Android over xmpp using Google Talk accounts. The cellbot Android
 * is able to share a video stream using a known shared location with Remote
 * Eyes. XMPP code based on example from:
 * http://credentiality2.blogspot.com/2010
 * /03/xmpp-asmack-android-google-talk.html
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class CellbotRCActivity extends Activity implements JoystickEventListener {
    private static final String TAG = "Cellbot Remote Control";

    public static final int VOICE_RECO_CODE = 42;

    private String CONTROLLER_ACCOUNT;

    private String CONTROLLER_PASS;

    private String ROBOT_ACCOUNT;

    private String REMOTE_EYES_IMAGE_URL;
    
    private String COMMAND_PUT_URL;
    
    public int state = 0;

    private URL url;

    private ImageView remoteEyesImageView;
    
    private LinearLayout mJoystickLayout;
    
    private LinearLayout mButtonLayout;
    
    private MenuItem mUiTypeMenuitem, mPrefMenuitem;
    
    private CellbotJoystick mJoystick;

    private Chat chatControl;

    private boolean mTorchOn = false;
    
    private SendCommandTask mSendCommandTask;
    
    private OnClickListener speakClickListener = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            startActivityForResult(intent, VOICE_RECO_CODE);
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        CONTROLLER_ACCOUNT = prefs.getString("CONTROLLER_ACCOUNT", "");
        CONTROLLER_PASS = prefs.getString("CONTROLLER_PASS", "");
        ROBOT_ACCOUNT = prefs.getString("ROBOT_ACCOUNT", "");
        REMOTE_EYES_IMAGE_URL = prefs.getString("REMOTE_EYES_IMAGE_URL", "");
        COMMAND_PUT_URL = prefs.getString("COMMAND_PUT_URL", "");

        if ((CONTROLLER_ACCOUNT.length() < 1) || (CONTROLLER_PASS.length() < 1)
                || (ROBOT_ACCOUNT.length() < 1) || (REMOTE_EYES_IMAGE_URL.length() < 1)) {
            Intent i = new Intent();
            i.setClass(this, PrefsActivity.class);
            this.startActivity(i);
            finish();
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                XMPPConnection xmpp = new XMPPConnection("gmail.com");
                try {
                    xmpp.connect();
                    xmpp.login(CONTROLLER_ACCOUNT, CONTROLLER_PASS);
                } catch (XMPPException e) {
                    Log.v(TAG, "Failed to connect to " + xmpp.getHost());
                    e.printStackTrace();
                }
                ChatManager chatmanager = xmpp.getChatManager();
                chatControl = chatmanager.createChat(ROBOT_ACCOUNT, new MessageListener() {
                    // THIS CODE NEVER GETS CALLED FOR SOME REASON
                    public void processMessage(Chat chat, Message message) {
                        try {
                            Log.v(TAG, "Got:" + message.getBody());
                            chat.sendMessage(message.getBody());
                        } catch (XMPPException e) {
                            Log.v(TAG, "Couldn't respond:" + e);
                        }
                        Log.v(TAG, message.toString());
                    }
                });

                // Turn on remote eyes
                sendCommand("video on");

                // TODO: We're not taking feedback from the robot yet...
                PacketFilter filter = new AndFilter(new PacketTypeFilter(Message.class),
                        new FromContainsFilter(ROBOT_ACCOUNT));

                // Collect these messages
                PacketCollector collector = xmpp.createPacketCollector(filter);

                while (true) {
                    Packet packet = collector.nextResult();

                    if (packet instanceof Message) {
                        Message msg = (Message) packet;
                        // Process message
                        Log.v(TAG, "Got message:" + msg.getBody());
                    }
                }

            }

        }).start();

        setContentView(R.layout.main);

        remoteEyesImageView = (ImageView) findViewById(R.id.remoteEyesView);
        remoteEyesImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!mTorchOn) {
                    sendCommand("torch on");
                    mTorchOn = true;
                } else {
                    sendCommand("torch off");
                    mTorchOn = false;
                }
            }
        });

        mSendCommandTask = new SendCommandTask();

        mButtonLayout = (LinearLayout) findViewById(R.id.button_layout);

        mJoystickLayout = (LinearLayout) findViewById(R.id.joystick_layout);
        mJoystick = new CellbotJoystick(this, this);
        mJoystickLayout.addView(mJoystick);

        ImageButton forward = (ImageButton) findViewById(R.id.forward);
        forward.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendCommand("f");
            }
        });

        ImageButton left = (ImageButton) findViewById(R.id.left);
        left.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendCommand("l");
            }
        });

        ImageButton right = (ImageButton) findViewById(R.id.right);
        right.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendCommand("r");
            }
        });

        ImageButton backward = (ImageButton) findViewById(R.id.backward);
        backward.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendCommand("b");
            }
        });

        ImageButton stop = (ImageButton) findViewById(R.id.stop);
        stop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendCommand("s");
            }
        });

        ((ImageButton) findViewById(R.id.speak)).setOnClickListener(speakClickListener);
        ((ImageButton) findViewById(R.id.speak1)).setOnClickListener(speakClickListener);

        new UpdateImageTask().execute();
    }

    private boolean sendCommand(String command) {
        if (mJoystickLayout.getVisibility() == View.VISIBLE) {
            // Wait until the previous PUT is done only for stop command.
            while(!mSendCommandTask.isReady() && command.equals("s"));
            if (mSendCommandTask.isReady()) {
                mSendCommandTask = new SendCommandTask();
                mSendCommandTask.execute(command);
            }
            return true;
        } else {
            try {
                if (chatControl != null) {
                    chatControl.sendMessage(command);
                    return true;
                }
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void updateRemoteEyesView(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            public void run() {
                Bitmap oldBmp = (Bitmap) remoteEyesImageView.getTag();
                remoteEyesImageView.setImageBitmap(bmp);
                remoteEyesImageView.setTag(bmp);
                if (oldBmp != null) {
                    oldBmp.recycle();
                }
            }
        });
    }

    private class UpdateImageTask extends UserTask<Void, Void, Bitmap> {
        @Override
        @SuppressWarnings("unchecked")
        public Bitmap doInBackground(Void... params) {

            try {
                if (url == null) {
                    url = new URL(REMOTE_EYES_IMAGE_URL);
                }
                URLConnection cn = url.openConnection();
                cn.connect();
                InputStream stream = cn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(stream);
                stream.close();
                return bmp;
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        public void onPostExecute(Bitmap bmp) {
            if (bmp != null) {
                updateRemoteEyesView(bmp);
            }
            new UpdateImageTask().execute();
        }
    }

    private class SendCommandTask extends UserTask<String, Void, Void> {
        
        private HttpConnection mConnection;

        private HttpState mHttpState;
        
        private final int port = 80;

        private boolean isReady = true;
        
        public SendCommandTask() {
            super();
            mHttpState = new HttpState();
            resetConnection();
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public Void doInBackground(String... params) {
            if (params == null || params.length == 0 ||
                COMMAND_PUT_URL == null || COMMAND_PUT_URL.equals("")) return null;
            isReady = false;
            try {
                PutMethod put = new PutMethod(COMMAND_PUT_URL);
                put.setRequestBody(new ByteArrayInputStream(params[0].getBytes()));
                put.execute(mHttpState, mConnection);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                resetConnection();
            } catch (IOException e) {
                e.printStackTrace();
                resetConnection();
            } finally {
                isReady = true;
                if (mConnection != null)
                    mConnection.close();
            }
            return null;
        }
        
        public boolean isReady() {
            return isReady;
        }
        
        private void resetConnection() {
            if (COMMAND_PUT_URL == null || COMMAND_PUT_URL.equals("")) return;
            try {
                mConnection = new HttpConnection(new URL(COMMAND_PUT_URL).getHost(), port);
                mConnection.open();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mPrefMenuitem = menu.add(R.string.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        mUiTypeMenuitem = menu.add(R.string.ui_type_joystick);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        if (item == mPrefMenuitem) {
            intent = new Intent(this, PrefsActivity.class);
            startActivity(intent);
            finish();
        } else if (item == mUiTypeMenuitem) {
            if (item.getTitle().equals(getResources().getString(R.string.ui_type_joystick))) {
                item.setTitle(getResources().getString(R.string.ui_type_buttons));
                mButtonLayout.setVisibility(View.INVISIBLE);
                mButtonLayout.setLayoutParams(new LayoutParams(0, 0));
                mJoystickLayout.setVisibility(View.VISIBLE);
                mJoystickLayout.setLayoutParams(
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT));
            } else {
                item.setTitle(getResources().getString(R.string.ui_type_joystick));
                mJoystickLayout.setVisibility(View.INVISIBLE);
                mJoystickLayout.setLayoutParams(new LayoutParams(0, 0));
                mButtonLayout.setVisibility(View.VISIBLE);
                mButtonLayout.setLayoutParams(
                        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSpeechAlert(String input) {
        Builder speechAlertBuilder = new Builder(this);
        final EditText speechInput = new EditText(this);
        speechInput.setText(input);
        speechAlertBuilder.setView(speechInput);
        speechAlertBuilder.setPositiveButton("Do it!", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                sendCommand(speechInput.getText().toString());
            }
        });
        speechAlertBuilder.setNeutralButton("Speak!", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                sendCommand("speak: " + speechInput.getText().toString());
            }
        });
        speechAlertBuilder.setNegativeButton("Cancel", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        speechAlertBuilder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECO_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<String> results = data.getExtras().getStringArrayList(
                        RecognizerIntent.EXTRA_RESULTS);
                showSpeechAlert(results.get(0));
            }
        }
    }

    /* (non-Javadoc)
     * @see com.cellbots.cellbotrc.CellbotJoystick.JoystickEventListener#onAction(int, int)
     */
    @Override
    public void onAction(float direction, float speed) {
        if (direction >= 0 && direction <= 90) {
            sendCommand("w " + (int) speed + " " + (int) (speed - direction / 90 * speed));
        } else if (direction > 90) {
            sendCommand("w -" + (int) speed + " -" +
                    (int) (speed - (180 - direction) / 90 * speed));
        } else if (direction < 0 && direction >= -90) {
            sendCommand("w " + (int) (speed - Math.abs(direction) / 90 * speed) +
                    " " + (int) speed);
        } else if (direction < -90) {
            sendCommand("w -" + (int) (speed - (180 - Math.abs(direction)) / 90 * speed) +
                    " -" + (int) speed);
        }
    }

    /* (non-Javadoc)
     * @see com.cellbots.cellbotrc.CellbotJoystick.JoystickEventListener#onStopBot()
     */
    @Override
    public void onStopBot() {
        sendCommand("s");
    }
}
