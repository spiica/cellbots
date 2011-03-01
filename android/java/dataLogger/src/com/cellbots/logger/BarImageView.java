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


package com.cellbots.logger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Image View for drawing the temperature and storage bars.
 * This is the same as a regular image view except it is designed to let
 * you programatically erase part of it from the top (to show less of a
 * meter).
 * 
 * @author clchen@google.com (Charles L. Chen)
 */
public class BarImageView extends ImageView {
    private float percentage = (float) 1.0;
    
    public BarImageView(Context context){
        super(context);
    }
    public BarImageView(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    public BarImageView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }
    
    @Override
    public void onDraw(Canvas c){
        super.onDraw(c);        
        Paint p = new Paint(Color.TRANSPARENT);
        //p.setColorFilter(new PorterDuffColorFilter(Color.TRANSPARENT, Mode.XOR));
        c.drawRect(0, 0, this.getWidth(), this.getHeight() * ((float)1.0 - percentage), p);
    }

    public void setPercentage(float p){
        percentage = p;
        invalidate();
    }
}
