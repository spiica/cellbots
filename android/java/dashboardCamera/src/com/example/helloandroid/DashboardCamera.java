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

package com.example.helloandroid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class DashboardCamera {
	private static final String BUFFER_1 = "buffer1.3gp";
	private static final String BUFFER_2 = "buffer2.3gp";
	
	Context context;
	Surface surface;
	
	private MediaRecorder recorder; 
	private boolean recording = false;
	private boolean enabled = false;
	private String lastBuffer;
	private String nextBuffer;
	
	public DashboardCamera(Context context, Surface surface) {
		this.context = context;
		this.surface = surface;
		this.recorder = null;
		
		// Set up the buffer locations.
		try {
			// Create the files if they don't exist. Note that this should clear the
			// current contents of the buffers.
			FileOutputStream buffer1;
			buffer1 = context.openFileOutput(BUFFER_1, Context.MODE_WORLD_READABLE);
			buffer1.close();
			FileOutputStream buffer2;
			buffer2 = context.openFileOutput(BUFFER_2, Context.MODE_WORLD_READABLE);
			buffer2.close();
		} catch (FileNotFoundException e) {
			Log.e("HelloAndroid", "FileNotFoundException: " + e.toString());
		} catch (IOException e) {
			Log.e("HelloAndroid", "IOException: " + e.toString());
		}
		
		// Set the last and next buffers in this configuration initially.
		lastBuffer = BUFFER_2;
		nextBuffer = BUFFER_1;
	}
	
	private String getPathForBuffer(String buffer) {
		File file = context.getFileStreamPath(buffer);
		return file.getAbsolutePath();
	}
	
	private boolean setUpRecording(String file, Surface surface) {
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); 
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); 
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); 
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
		// recorder.setVideoSize(480, 320); 
		// recorder.setVideoFrameRate(15); 
		// recorder.setMaxDuration(10000); 
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		recorder.setOutputFile(getPathForBuffer(nextBuffer));
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		recorder.setPreviewDisplay(surface);
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		try {
	    	 recorder.prepare(); 
	     } catch (IllegalStateException e) { 
	          e.printStackTrace();
	          return false;
	     } catch (IOException e) {
	    	  e.printStackTrace();
	          return false;
	     } 
	     (new Exception()).printStackTrace(); // TODO(tgnourse): Remove
	     return true;
	}
	
	/**
	 * Start recording with the next available buffer. Does nothing except log 
	 * if recording is disabled or the camera is already recording.
	 */
	public synchronized boolean startRecordingNextBuffer() {
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		if (enabled) {
			(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
			if (!recording) {
				(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
				if (recorder != null) {
					Log.e("HelloAndroid", "Recorder should be null!");
					return false;
				}
				recorder = new MediaRecorder();
				// Sleep for half a second between recordings hopefully to get around a Froyo bug??
				/*
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		        if (setUpRecording(nextBuffer, surface)) {
		        	(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
			        recorder.start();
			        (new Exception()).printStackTrace(); // TODO(tgnourse): Remove
			        recording = true;
			        String currentBuffer = nextBuffer;
			        Log.i("HelloAndroid", "Recording started to buffer: " + getPathForBuffer(currentBuffer));
			        nextBuffer = lastBuffer;
			        lastBuffer = currentBuffer;
			        Log.i("HelloAndroid", "When recording is restarted it will use this buffer: " + getPathForBuffer(nextBuffer));
		        } else {
		        	Log.e("HelloAndroid", "Cannot start recording. Failed to set up recording.");
		        	issueShortToast("Could not start recording. Camera is not available.");
		        	recording = false;
		        	return false;
		        }
			} else {
				Log.e("HelloAndroid", "Recording was already started!");
			}
		} else {
			// Log.i("HelloAndroid", "Cannot start recording. Recording is not enabled!");
		}
		return true;
	}
	
	/**
	 *  Stop recording. Does nothing except log if recording is disabled or the camera
	 *  is not recording.
	 */
	public synchronized void stopRecording() {
		(new Exception()).printStackTrace(); // TODO(tgnourse): Remove
		if (enabled) {
			Log.i("HelloAndroid", "Going to attempt to stop recording.");
			if (recording) {
				recorder.stop(); 
		  	    recorder.reset();
		  	    recorder.release();
		  	    Log.i("HelloAndroid", "stop() reset() release() and null");
		  	    recorder = null;
		        recording = false;
		        Log.i("HelloAndroid", "Recording was stopped.");
			} else {
				Log.e("HelloAndroid", "Recording was not stopped!");
			}
		} else {
			// Log.i("HelloAndroid", "Cannot stop recording. Recording is not enabled!");
		}
	}
	
	/**
	 * Enables any future recording. Note that this method does not start recording.
	 */
	public synchronized void enableRecording() {
		enabled = true;
		// issueLongToast("Tap the screen to save a video.");
	}
	
	/**
	 * Stops any current recording and disables any future recording.
	 */
	public synchronized void disableRecording() {
		stopRecording();
		enabled = false;
		// issueShortToast("Stopped video buffering.");
	}
	
	/**
	 * Stops and re-starts recording on the next buffer. Will start recording if it
	 * is not already started.
	 */
	public synchronized void toggleBuffers() {
		stopRecording();
		startRecordingNextBuffer();
	}
	
	/** 
	 * Stop and save the current recording. Does not restart it. Does nothing if
	 * recording is disabled.
	 * @return TODO
	 */
	public synchronized boolean stopAndSaveRecording() {
		if (enabled) {
			// Disable the recording because we have to be guaranteed that this
			// executes in this method.
			disableRecording();
			// Asynchronously save the video to the SD card and show a progress
			// dialog.
			new SaveVideo().execute();
		}
		return true;
	}
	
	/**
	 * Saving the video to file takes too long for the UI to remain responsive. As a result,
	 * we do it in a separate AsyncTask which makes it easy to have a progress dialog.
	 */
	private class SaveVideo extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog = new ProgressDialog(context);
		private String message;
		
		protected void onPreExecute() {
			dialog.setMessage("Saving video...");
			dialog.setCancelable(false);
			dialog.show();
		}
		
		protected Void doInBackground(Void... unused) {
			try {
				copyBuffer(nextBuffer, getSDCardOutputStream("_1of2"));
				copyBuffer(lastBuffer, getSDCardOutputStream("_2of2"));
			} catch (FileNotFoundException e) {
				message = "Could not save the video. The SD card may not be available.";
				return null;
			} catch (IOException e) {
				message = "Could not save the video.";
				return null;
			}
			message = "Video was saved to the SD card.";
			return null;
		}
		
		protected void onPostExecute(Void unused) {
			dialog.dismiss();
			issueLongToast(message);
			
			// Enable and restart recording.
			enableRecording();
			startRecordingNextBuffer();
		}
	}
	
	private void copyBuffer(String buffer, OutputStream output) throws IOException {
		// Open the input and output streams.
		BufferedOutputStream outputStream =
			new BufferedOutputStream(output);
		BufferedInputStream inputStream =
			new BufferedInputStream(context.openFileInput(buffer));
		// Copy the file byte by byte.
		while (inputStream.available() > 0) {
			outputStream.write(inputStream.read());
		}
		outputStream.close();
		inputStream.close();
	}
	
	private void issueShortToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
	
	private void issueLongToast(String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	
	private String getFileName(long time, String suffix) {
		Date date = new Date(time);
		SimpleDateFormat format = new SimpleDateFormat("EEE-MMM-d-yyyy_HH-mm-ss");
		return "dashcam_" + format.format(date);
	}
	
	private OutputStream getSDCardOutputStream(String suffix) throws FileNotFoundException {
		// TODO(tgnourse): Put these files into a directory.
		long timeTakenMilliSeconds = System.currentTimeMillis(); 
		String path = "/sdcard/" + getFileName(timeTakenMilliSeconds, suffix) + suffix + ".3gp";
		Log.i("HelloAndroid", "Going to save a video to: " + path);
		return new FileOutputStream(path);
	}

	/*
	private OutputStream getGalleryOutputStream() throws FileNotFoundException {
		// Copy the file to the gallery.
		ContentValues values = new ContentValues();

		long timeTakenMilliSeconds = System.currentTimeMillis(); 
		long timeTakenSeconds = timeTakenMilliSeconds / 1000;
		values.put(MediaStore.Video.VideoColumns.TITLE, getFileName(timeTakenMilliSeconds));
		values.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, getFileName(timeTakenMilliSeconds));
	    values.put(MediaStore.Video.VideoColumns.DATE_ADDED, timeTakenSeconds);
	    values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, timeTakenMilliSeconds);
	    values.put(MediaStore.Video.VideoColumns.MIME_TYPE, "video/3gpp");
	    // TODO(tgnourse): When testing on my NexusOne the videos don't seem to end
	    // up in this album. Instead they go to "videos" and the thumbnails are wrong
	    // until the gallery scans the SD card again.
	    values.put(MediaStore.Video.VideoColumns.ALBUM, "Dashboard Camera");
	    
	    ContentResolver contentResolver = context.getContentResolver();
	    Uri uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
	    
	    Log.i("HelloAndroid", "Going to save video:" + lastBuffer + 
	    		"(" + getPathForBuffer(lastBuffer) + ") " +
	    		" to: " + uri);
	    
	    return context.getContentResolver().openOutputStream(uri);
	    
	    // Broadcasting this intent didn't seem to make any difference.
		// Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
		// Log.i("HelloAndroid", intent.toString());
		// context.sendBroadcast(intent);
	}*/
}
