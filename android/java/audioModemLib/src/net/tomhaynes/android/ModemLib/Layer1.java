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

public abstract class Layer1 {
	protected Layer2 l2 = null;
	public final static int LITTLE_ENDIAN = 0;
	public final static int BIG_ENDIAN = 1;
	
	public Layer1(Layer2 l2) {
		this.l2 = l2;
	}
	
	@Override
	protected void finalize() {
		this.l2 = null;
	}
	
	public abstract short[] modulate(byte[] buffer, int len);
	
	public void demodulate(short[] buffer, int length) {
		float[] fbuffer = new float[buffer.length];
		for (int i = 0; i < buffer.length; i++) {
				fbuffer[i] = (buffer[i] * (1f/Short.MAX_VALUE));
		}
		demodulate(fbuffer,length);
	}
	
	public abstract void demodulate(float[] buffer, int length);
}
