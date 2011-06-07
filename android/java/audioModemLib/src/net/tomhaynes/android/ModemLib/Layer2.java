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

public abstract class Layer2 {
	protected Listener inputListener = null;
	
	public Layer2(Listener listener) {
		inputListener = listener;
	}
	
	public static abstract class Listener {
		public abstract void onFrameReady(L2frame frame, boolean status, String error);
	}
	
	/*
	 * Receive functions
	 */
	
	/**
	 * Receives one bit (LSB of b) and adds it to frame buffer.
	 * When frame buffer is full, it will be processed automatically.
	 * 
	 * @param b Least significant bit of b is the RX bit.
	 */
	public abstract void rxbit(int b);
	
	/*
	 * Transmit functions
	 */
	
	public abstract boolean prepareFrame(L2frame frame);
	public abstract void txFrame(L2frame frame);
}
