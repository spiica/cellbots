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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

/**
 * @author tgnourse@google.com (Thomas Nourse)
 */
public class PreviewSurfaceView extends SurfaceView implements Callback {
	
	private DashboardCamera camera;

	public PreviewSurfaceView(Context context, AttributeSet attrs)  {
		super(context, attrs);  
		getHolder().addCallback(this); 
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); 
		this.camera = new DashboardCamera(getContext(), getHolder().getSurface());
	}
	
	public DashboardCamera getCamera() {
		return camera;
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Do nothing.
	}

	public void surfaceCreated(SurfaceHolder holder) {
		camera.enableRecording();
		camera.startRecordingNextBuffer();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.disableRecording();
	}
	
    public boolean onTouchEvent(MotionEvent event) { 
        if (event.getAction() == MotionEvent.ACTION_DOWN) { 
        	 camera.stopAndSaveRecording();
        	 camera.startRecordingNextBuffer();
             return true; 
        } 
        return false; 
    }

}
