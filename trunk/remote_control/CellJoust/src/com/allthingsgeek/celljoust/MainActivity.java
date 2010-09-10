

package com.allthingsgeek.celljoust;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.http.HttpConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.allthingsgeek.celljoust.R;
import com.cellbots.CellbotProtos;
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
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
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
  public static final String                  PREFS_NAME    = "ServoBotPrefsFile";

  PulseGenerator                              noise;

  Movement                                    mover;

  private SurfaceHolder                       mHolder;

  private String                              putUrl        = "";

  private String                              server        = "";

  private SurfaceView                         mPreview;

  private Camera                              mCamera;

  private boolean                             mTorchMode;

  private HttpConnection                      mConnection;

  // private HttpState mHttpState;

  private Rect                                r;

  private int                                 previewHeight = 0;

  private int                                 previewWidth  = 0;

  private int                                 previewFormat = 0;

  private boolean                             isUploading   = false;

  private byte[]                              mCallbackBuffer;

  byte[]                                      buff;

  private ByteArrayOutputStream               out;
  
  private ConversionWorker convWorker;

  //public static CellbotProtos.ControllerState controllerState;

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

    // Restore preferences
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    putUrl = settings.getString("REMOTE_EYES_PUT_URL", "http://example.com:8080/cellserv/video");
    noise.setOffsetPulsePercent(settings.getInt("servo1Percent", 50), 0);
    noise.setOffsetPulsePercent(settings.getInt("servo2Percent", 50), 1);
    noise.setOffsetPulsePercent(settings.getInt("servo3Percent", 50), 2);
    noise.setOffsetPulsePercent(settings.getInt("servo4Percent", 50), 3);
    mover.setOffset(settings.getInt("wheelOffset", 0));

    mTorchMode = false;

    out = new ByteArrayOutputStream();

    resetConnection();
    // mHttpState = new HttpState();

    setContentView(R.layout.main);
    
    convWorker = new ConversionWorker();
    
    mPreview = (SurfaceView) findViewById(R.id.preview);
    mHolder = mPreview.getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    noise.pause();
    

/*
    mPreview.setOnClickListener(new OnClickListener()
    {
      public void onClick(View v)
      {
        // setTorchMode(!mTorchMode);
      }
    });

    this.registerReceiver(new BroadcastReceiver()
    {
      @Override
      public void onReceive(Context context, Intent intent)
      {
        boolean useTorch = intent.getBooleanExtra("TORCH", false);
        // setTorchMode(useTorch);
      }
    }, new IntentFilter("android.intent.action.REMOTE_EYES_COMMAND"));
    */
  }

  protected void onDestroy()
  {
    super.onDestroy();
    noise.stop();
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

  private void resetConnection()
  {
    Log.e("server", server);
    /*
     * mConnection = new HttpConnection(server, port); try { mConnection.open();
     * } catch (IOException e) { // TODO Auto-generated catch block
     * e.printStackTrace(); }
     */
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
        if (!isUploading)
        {
          isUploading = true;
          convWorker.nextFrame(imageData);
          
        }
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

  
  class ConversionWorker extends Thread {
  
    
    public ConversionWorker() {
        //setDaemon(true);   
        start();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public synchronized void run() {            
        try {
            wait();//wait for initial frame
        } catch (InterruptedException e) {}
        while(true) {

          try
          {
            YuvImage yuvImage = new YuvImage(mCallbackBuffer, previewFormat, previewWidth, previewHeight, null);
            yuvImage.compressToJpeg(r, 20, out); // Tweak the quality here

            Builder state = CellbotProtos.PhoneState.newBuilder();

            state.setVideoFrame(ByteString.copyFrom(out.toByteArray()));
            state.setTimestamp(System.currentTimeMillis());

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost post = new HttpPost(putUrl);

            post.setEntity(new ByteArrayEntity(state.build().toByteArray()));

            HttpResponse resp = httpclient.execute(post);

            InputStream resStream = resp.getEntity().getContent();

              ControllerState cs = ControllerState.parseFrom(resStream);
              
              
              Log.i("RemoteEyes Got Controller State", cs.toString());
              mover.processControllerStateEvent(cs);

            // Log.i("RemoteEyes","got this from server :"+response);
          }
          catch (UnsupportedEncodingException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          catch (IllegalStateException e)
          {
            e.printStackTrace();
            resetConnection();
          }
          catch (com.google.protobuf.InvalidProtocolBufferException e)
          {
            //e.printStackTrace();
            //resetConnection();
          }
          catch (IOException e)
          {
            e.printStackTrace();
            resetConnection();
          }
          finally
          {
            out.reset();
            if (mCamera != null)
            {
              mCamera.addCallbackBuffer(mCallbackBuffer);
            }
            isUploading = false;
          }       
            
            try {
                wait();//wait for next frame
            } catch (InterruptedException e) {}
        }
    }
    
    synchronized boolean nextFrame(byte[] frame) {
        if(this.getState() == Thread.State.WAITING) 
        {
          //ok, we are ready for a new frame:
           // curFrame = frame;
            //do the work:
            this.notify();
            return true;
        } else {
            //ignore it
          return false;

        }
        
    }
}
  
}
