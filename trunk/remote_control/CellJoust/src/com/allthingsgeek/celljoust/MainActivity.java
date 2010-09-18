package com.allthingsgeek.celljoust;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.allthingsgeek.celljoust.R;
import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.AudioVideoFrame;
import com.cellbots.CellbotProtos.ControllerState;
import com.cellbots.CellbotProtos.PhoneState.Builder;
import com.google.protobuf.ByteString;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements Callback
{
  public static final String    PREFS_NAME    = "ServoBotPrefsFile";

  private static final String   TAG           = "CellJoust";

  PulseGenerator                noise;

  Movement                      mover;

  private SurfaceHolder         mHolder;

  public static String                putUrl        = "";

  private SurfaceView           mPreview;

  private Camera                mCamera;

  private boolean               mTorchMode;

  // private HttpState mHttpState;

  private Rect                  r;

  private int                   previewHeight = 0;

  private int                   previewWidth  = 0;

  private int                   previewFormat = 0;

  private byte[]                mCallbackBuffer;

  byte[]                        buff;

  private ByteArrayOutputStream out;

  private ConversionWorker      convWorker;

  public static SensorManager   sensorManager;

  RobotStateHandler             state;

  // public static CellbotProtos.ControllerState controllerState;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServoOn");
    // wl.acquire();
    // wl.release();

    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    int ipAddress = wifiInfo.getIpAddress();

    noise = PulseGenerator.getInstance();
    mover = Movement.getInstance();
     
    loadPrefs();
    
    mTorchMode = false;

    out = new ByteArrayOutputStream();

    setContentView(R.layout.main);

    if (sensorManager == null)
    {
      sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
    }

    startListening();
    
    mPreview = (SurfaceView) findViewById(R.id.preview);
    mHolder = mPreview.getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    noise.pause();

  }
  
  private void loadPrefs()
  {
    // Restore preferences
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    putUrl = settings.getString("REMOTE_EYES_PUT_URL", "http://example.com:8080/cellserv");
    noise.setOffsetPulsePercent(settings.getInt("servo1Percent", 50), 0);
    noise.setOffsetPulsePercent(settings.getInt("servo2Percent", 50), 1);
    noise.setOffsetPulsePercent(settings.getInt("servo3Percent", 50), 2);
    noise.setOffsetPulsePercent(settings.getInt("servo4Percent", 50), 3);
    mover.setOffset(settings.getInt("wheelOffset", 0));

  }

  @Override
  public void onResume()
  {
    loadPrefs(); 
    super.onResume();
  }
   
  private Handler handler = new Handler()
                          {

                            @Override
                            public void handleMessage(Message msg)
                            {
                              // utterTaunt((String) msg.obj);
                            }

                          };

  private synchronized void startListening()
  {
    Log.d(TAG, "startListening called");
    
    convWorker = new ConversionWorker();
    
    if (state == null)
    {
      try
      {
        state = RobotStateHandler.getInstance(handler);
      }
      catch (IOException e)
      {
        // TODO Auto-generated catch block
        Log.e(TAG, "error getting robot state handler instace", e);
      }
      // Initialize text-to-speech. This is an asynchronous operation.
      // The OnInitListener (second argument) is called after initialization
      // completes.

    }

    if (!state.listening)
    {

      // Toast.makeText(CONTEXT, "Current IP:" + state.getLocalIpAddress(),
      // Toast.LENGTH_LONG);
      // ProgressDialog.show(me, msg,
      // "Searching for a Bluetooth serial port...");

      ProgressDialog btDialog = null;

      String connectivity_context = Context.WIFI_SERVICE;
      WifiManager wifi = (WifiManager) getSystemService(connectivity_context);

      this.registerReceiver(state.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

      this.registerReceiver(state.mWifiInfoReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

      state.startListening(btDialog, wifi);

    }

  }

  private synchronized void stopListening()
  {

    Log.d(TAG, "stopListening called");

    convWorker.kill();
    
    try
    {
      this.unregisterReceiver(state.mBatInfoReceiver);
    }
    catch (Exception e)
    {
    }

    try
    {
      this.unregisterReceiver(state.mWifiInfoReceiver);
    }
    catch (Exception e)
    {
    }

    //if (state.isAlive())
    //{
      //state.stopListening();
    //}

  }

  protected void onDestroy()
  {
    noise.stop();
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle item selection
    switch (item.getItemId())
    {
      case R.id.setup:
        Intent i = new Intent(this, SetupActivity.class);
        startActivity(i);
        break;
      case R.id.quit:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    return mover.processKeyDownEvent(keyCode);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event)
  {
    return mover.processKeyUpEvent(keyCode);
  }

  public void surfaceCreated(SurfaceHolder holder)
  {
    mCamera = Camera.open();
    try
    {
      mCamera.setPreviewDisplay(holder);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder)
  {
    mCamera.stopPreview();
    mCamera.release();
    mCamera = null;
    mCamera = null;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
  {
    mHolder.setFixedSize(w, h);
    // Start the preview
    Parameters params = mCamera.getParameters();
    previewHeight = params.getPreviewSize().height;
    previewWidth = params.getPreviewSize().width;
    previewFormat = params.getPreviewFormat();

    // Crop the edges of the picture to reduce the image size
    int crop = 0;
    r = new Rect(crop, crop, previewWidth - crop, previewHeight - crop);

    mCallbackBuffer = new byte[497664];

    mCamera.setParameters(params);
    mCamera.setPreviewCallbackWithBuffer(new PreviewCallback()
    {
      public void onPreviewFrame(byte[] imageData, Camera arg1)
      {
        convWorker.nextFrame(imageData);
      }
    });
    mCamera.addCallbackBuffer(mCallbackBuffer);
    mCamera.startPreview();
    setTorchMode(mTorchMode);
  }

  private void setTorchMode(boolean on)
  {
    if (mCamera != null)
    {
      Parameters params = mCamera.getParameters();
      if (on)
      {
        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
      }
      else
      {
        params.setFlashMode(Parameters.FLASH_MODE_AUTO);
      }
      mTorchMode = on;
      mCamera.setParameters(params);
    }
  }

  class ConversionWorker extends Thread
  {

    //private HttpConnection mConnection;
    
    HttpClient httpclient;
    boolean alive;
    HttpPost post;
    boolean sending = false;
    
    public ConversionWorker()
    {
      // setDaemon(true);
      
      //this client should automatically reuse its connection
      httpclient = new DefaultHttpClient();  
      alive = true;
      post = new HttpPost(putUrl+"/video");
      start();
    }

    public void kill()
    {
      alive = false;
      this.notify();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public synchronized void run()
    {
      try
      {
        wait();// wait for initial frame
      }
      catch (InterruptedException e)
      {
      }
      while (alive)
      {

        try
        {
          YuvImage yuvImage = new YuvImage(mCallbackBuffer, previewFormat, previewWidth, previewHeight, null);
          yuvImage.compressToJpeg(r, 20, out); // Tweak the quality here

          //state.setVideoFrame(ByteString.copyFrom(out.toByteArray()));
          
          AudioVideoFrame.Builder avFrame = AudioVideoFrame.newBuilder(); 
          
          avFrame.setData(ByteString.copyFrom(out.toByteArray()));
          
          post.setEntity(new ByteArrayEntity(avFrame.build().toByteArray()));
          
          Log.i(TAG, "sending video");
          sending = true;
          HttpResponse resp = httpclient.execute(post);
          sending = false;
          Log.i(TAG, "sent video");
         
          //InputStream resStream = resp.getEntity().getContent();

          //ControllerState cs = ControllerState.parseFrom(resStream);

          //mover.processControllerStateEvent(cs);
        }
        catch (UnsupportedEncodingException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
          
        }
        catch (IllegalStateException e)
        {
          e.printStackTrace();
        }
        catch (com.google.protobuf.InvalidProtocolBufferException e)
        {
          // e.printStackTrace();
          // resetConnection();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
        finally
        {
          out.reset();
          if (mCamera != null)
          {
            mCamera.addCallbackBuffer(mCallbackBuffer);
          }
          // isUploading = false;
        }

        try
        {
          wait();// wait for next frame
        }
        catch (InterruptedException e)
        {
        }
      }
    }

    synchronized boolean nextFrame(byte[] frame)
    {
      if (this.getState() == Thread.State.WAITING && ! sending)
      {
        // ok, we are ready for a new frame:
        // curFrame = frame;
        // do the work:
        this.notify();
        return true;
      }
      else
      {
        // ignore it
        return false;

      }

    }
  }

}
