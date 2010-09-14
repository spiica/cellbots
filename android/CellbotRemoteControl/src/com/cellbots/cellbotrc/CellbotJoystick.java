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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * This View allows joystick-like touch interaction to obtain direction and speed which can
 * be used to drive the robot.
 * 
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 *
 */
public class CellbotJoystick extends ImageView {

    private static final String TAG = "CellbotJoystick";
    
    private int mWidth;
    
    private int mHeight;
    
    private int mCenterX;
    
    private int mCenterY;
    
    private float mPrevX, mPrevY;
    
    private float mDirection = 0;    // Default direction is forward
    
    private float mSpeed = 20;   // % speed
    
    private float mJoystickRadius;
    
    private Paint mPaint;
    
    private JoystickEventListener mJoystickListener;
    
    private float animJoystickRadius;
    
    private Handler refreshHandler = null;
    
    private boolean stopped;
    
    private Runnable doStopAnim = new Runnable() {
        @Override
        public void run() {
            invalidate();
            animJoystickRadius = Math.max(animJoystickRadius - 20, 0);
            if (animJoystickRadius > 0) {
                refreshHandler.removeCallbacks(doStopAnim);
                refreshHandler.postDelayed(this, 50);
            }
        }
    };
    
    public CellbotJoystick(Context ct, JoystickEventListener joystickListener) {
        super(ct);
        setImageDrawable(ct.getResources().getDrawable(R.drawable.joystick));
        mJoystickListener = joystickListener;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(5);
        refreshHandler = new Handler();
    }

    @Override
    public void drawableStateChanged() {
        mWidth = getWidth();
        mHeight = getHeight();
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        mJoystickRadius = Math.min(mCenterX, mCenterY);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = animJoystickRadius > 0 || stopped ? animJoystickRadius : mJoystickRadius;
        canvas.drawLine(mCenterX, mCenterY,
                mCenterX + (int) (radius * Math.sin(mDirection * Math.PI / 180)),
                mCenterY - (int) (radius * Math.cos(mDirection * Math.PI / 180)), mPaint);
    }
    
    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mJoystickListener != null) {
                handleStop();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            boolean moved = false;
            float distFromCenter = distance(mCenterX, mCenterY, event.getX(), event.getY());
            if (distFromCenter > mJoystickRadius) {
                mPrevX = mCenterX;
                mPrevY = mCenterY;
                handleStop();
            }
            if (distance(event.getX(), event.getY(), mPrevX, mPrevY) > 10) {
                moved = true;
                mDirection = direction(mCenterX, mCenterY, event.getX(), event.getY());
                mSpeed = distFromCenter / mJoystickRadius * 200 / 3 / 2;
                animJoystickRadius = 0;
                stopped = false;
                if (mJoystickListener != null) {
                    mJoystickListener.onAction(mDirection, mSpeed);
                    invalidate();
                }
                mPrevX = event.getX();
                mPrevY = event.getY();
            }
            return true;
        }
        return true;
    }
    
    /**
     * Handles the operations to be performed when the user intends to stop the robot.
     */
    private void handleStop() {
        if (animJoystickRadius <= 0) {
            stopped = true;
            initStopAnimation();
        }
        if (mJoystickListener != null) {
            mJoystickListener.onStopBot();
        }
    }
    
    /**
     * Initiates animation when stop gesture if performed on the joystick.
     */
    private void initStopAnimation() {
        animJoystickRadius = mJoystickRadius;
        refreshHandler.removeCallbacks(doStopAnim);
        refreshHandler.postDelayed(doStopAnim, 0);
    }
    
    private float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }
    
    private float direction(float fromX, float fromY, float toX, float toY) {
        float dir = (float) (90 + (Math.atan2(toY - fromY, toX - fromX)) * 180 / Math.PI);
        dir = dir > 180 ? (-90 + dir - 270) : dir;
        return dir;
    }
    
    public interface JoystickEventListener {
        public void onAction(float direction, float speed);
        public void onStopBot();
    }
}
