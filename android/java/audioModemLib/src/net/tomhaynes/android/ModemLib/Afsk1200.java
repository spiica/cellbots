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

import android.util.FloatMath;

public final class Afsk1200 extends Layer1 {
	private int dcd_shreg = 0;
	private int sphase = 0;
	private int lasts = 0;
	private int subsamp = 0;
	
	public final static int FREQ_MARK = 1200;
	public final static int FREQ_SPACE = 2200;
	public final static int FREQ_SAMP = 22050;
	public final static int BAUD = 1200;
	public final static int SUBSAMP = 2;
	
	public final static int CORRLEN = (FREQ_SAMP/BAUD); //18
	public final static int SPHASEINC = (0x10000*BAUD*SUBSAMP/FREQ_SAMP); //7133
	
	private final static float[] corr_mark_i = new float[CORRLEN];
	private final static float[] corr_mark_q = new float[CORRLEN];
	private final static float[] corr_space_i = new float[CORRLEN];
	private final static float[] corr_space_q = new float[CORRLEN];
	private final static short[] mark = new short[CORRLEN];
	private final static short[] space = new short[CORRLEN];
	
	public Afsk1200(Layer2 l2) {
		super(l2);
		float f;
		int i;
		dcd_shreg = 0;
		sphase = 0;
		lasts = 0;
		subsamp = 0;
		//create filter curves
		for (f = 0, i = 0; i < CORRLEN; i++) {
			corr_mark_i[i] = FloatMath.cos(f);
			corr_mark_q[i] = FloatMath.sin(f);
			mark[i] = (short) (corr_mark_q[i] * Short.MAX_VALUE);
			f += 2.0 * Math.PI * FREQ_MARK / FREQ_SAMP;
		}
		for (f = 0, i = 0; i < CORRLEN; i++) {
			corr_space_i[i] = FloatMath.cos(f);
			corr_space_q[i] = FloatMath.sin(f);
			space[i] = (short) (corr_space_q[i] * Short.MAX_VALUE);
			f += 2.0 * Math.PI * FREQ_SPACE / FREQ_SAMP;
		}
	}
	
	@Override
	public void demodulate(float[] buffer, int length) {
		float f;
		int curbit;
		int index = 0;
		
		if (subsamp > 0) {
			int numfill = SUBSAMP - subsamp;
			if (length < numfill) {
				subsamp += length;
				return;
			}
			
			index += numfill;
			length -= numfill;
			subsamp = 0;
		}
		
		/*
		 *  TODO explore adding a zero point crossing (0x) pre-filter
		 *  @1200Hz another 0x should appear in ~18 samples
		 *  @2200Hz another 0x should appear in ~10 samples
		 */
		for (; length >= SUBSAMP; length -= SUBSAMP, index += SUBSAMP) {
			f = Tools.fsqr(Tools.mac(buffer, index, corr_mark_i, CORRLEN)) +
					Tools.fsqr(Tools.mac(buffer, index, corr_mark_q, CORRLEN)) -
					Tools.fsqr(Tools.mac(buffer, index, corr_space_i, CORRLEN)) -
					Tools.fsqr(Tools.mac(buffer, index, corr_space_q, CORRLEN));
			dcd_shreg <<= 1;
			if (f > 0) {
				dcd_shreg |= 1;
			} else {
				dcd_shreg |= 0;
			}
			//Log.i(TAG,'0'+Integer.toString(dcd_shreg & 1));
			
			if (((dcd_shreg ^ (dcd_shreg >>> 1)) & 1) > 0) {
				if (sphase < (0x8000-(SPHASEINC/2)))
					sphase += SPHASEINC/8;
				else
					sphase -= SPHASEINC/8;
			}
			sphase += SPHASEINC;
			if (sphase >= 0x10000) {
				sphase &= 0xffff;
				lasts <<= 1;
				lasts |= dcd_shreg & 1;
				curbit = (lasts ^ (lasts >>> 1) ^ 1) & 1;	
				l2.rxbit(curbit);
			}
		}
		subsamp = length;
	}
	
	@Override
	public short[] modulate(byte[] buffer, int len) {
		short[] mBuffer = new short[len*8*CORRLEN];
		byte temp = 0;
		boolean isMark = true;
		short[] next = mark;
		for (int i = 0; i < len; i++) {
			if(Thread.interrupted()) {
				mBuffer = new short[0];
				return mBuffer;
			}
			temp = buffer[i];
			for (int j = 0; j < 8; j++) {
				if (((temp>>>j)&1)==0) {
					if (isMark) {
						next = space;
						isMark = false;
					} else {
						next = mark;
						isMark = true;
					}
					//for (int k = 0; k < CORRLEN; k ++) {
					//	System.arraycopy(mark, 0, mBuffer, (k*CORRLEN) + (j*CORRLEN*CORRLEN) + (i*8*CORRLEN*CORRLEN), CORRLEN);
					//}
					System.arraycopy(next, 0, mBuffer, (j*CORRLEN) + (i*8*CORRLEN), CORRLEN);
					//Log.i("test","mark");
				} else {
					//for (int k = 0; k < CORRLEN; k ++) {
					//	System.arraycopy(space, 0, mBuffer, (k*CORRLEN) + (j*CORRLEN*CORRLEN) + (i*8*CORRLEN*CORRLEN), CORRLEN);
					//}
					System.arraycopy(next, 0, mBuffer, (j*CORRLEN) + (i*8*CORRLEN), CORRLEN);
					//Log.i("test","space");
				}
			}
		}
		return mBuffer;
		
	}
}
