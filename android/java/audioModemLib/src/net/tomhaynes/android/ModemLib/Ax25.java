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
package net.tomhaynes.android.ModemLib;

import android.util.Log;

public class Ax25 extends Layer2 {
	private char[] buffer;
	private int bufptr;
	private boolean rxstate;
	private int rxbitstream;
	private int rxbitbuf;
	
	public Ax25(Listener listener) {
		super(listener);
		buffer = new char[256];
		bufptr = 0;
		rxstate = false;
		rxbitstream = 0;
		rxbitbuf = 0;
	}
	
	/*
	 * Receive functions
	 */
	

	@Override
	public void rxbit(int bit) {
		rxbitstream <<= 1;
		if (bit > 0) rxbitstream |= 1; 
			else rxbitstream |= 0;
		if ((rxbitstream & 0xFF) == 0x7e) { // flag
			if (rxstate && bufptr > 2) {
				processFrame();
			}
			rxstate = true;
			bufptr = 0;
			rxbitbuf = 0x80;
			return;
		}
		if ((rxbitstream & 0x7f) == 0x7f) { // 7 bits in a row - bad
			rxstate = false;
			return;
		}
		if (!rxstate) return;
		if ((rxbitstream & 0x3f) == 0x3e) return; // stuffed bit - don't add
		if ((rxbitstream & 1) > 0) rxbitbuf |= 0x100; // correct stuffed bit
		if ((rxbitbuf & 1) > 0) {
			if (bufptr >= buffer.length) { // packet size too large
				rxstate = false;
				return;
			}
			buffer[bufptr++] = (char) (rxbitbuf >>> 1);
			rxbitbuf = 0x80;
			return;
		}
		rxbitbuf >>>= 1;
	}
	
	public void processFrame() {
		Ax25frame frame = new Ax25frame();
		boolean state = frame.setFrame(buffer, bufptr);
		inputListener.onFrameReady(frame, state, "");
	}
	/*
	 * TX Functions
	 */
	
	@Override
	public boolean prepareFrame(L2frame frame) {
		boolean status = ((Ax25frame)frame).makeFrame();
		Log.i("test","Attempt to process frame was a " + (status ? "SUCCESS" : "FAILURE"));
		return status;
	}
	
	@Override
	public void txFrame(L2frame frame) {
		int inserts = 0;
		char hold = 0;
		
	}
}
