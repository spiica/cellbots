package com.tgnourse.aprs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class SensorDataRunnable implements Runnable {
	
	/*
	 * This class prepares the sensors and builds the AX.25 audio clips and
	 * plays them. Pressing the run button registers listeners for all of the
	 * sensors that we are interested in. When all of the sensors have reported
	 * in, we build a frame then pass it to the audio player. After we have a
	 * copy of all the sensor data, we reset the "all present flag" then wait
	 * for new data to arrive. If the run button is pressed again (stop) then we
	 * unregister the sensor listeners which stops the whole process. The whole
	 * read/process/play loop takes about one second in real time.
	 */
	// 22050 Sample Rate
	/*
	 * public final static int sampleRate = 22050; public final static short[]
	 * SAMP22 = { 0, 19222, 31134, 31206, 19410, 233, -19031, -31060, -31276,
	 * -19598, -466, 18842, 30985, 31345, 19784, 700, -18650, -30908, -31412,
	 * -19970 };
	 * 
	 * public final static short[] SAMP12 = { 0, 10987, 20702, 28020, 32093,
	 * 32451, 29051, 22287, 12942, 2099, -8986, -19032, -26874, -31604, -32675,
	 * -29962, -23780, -14844 };
	 */
	// 44100 Sample rate
	public final static int sampleRate = 44100;
	/*
	 * public final static short[] SAMP22 = { 0, 10103, 19222, 26467, 31134,
	 * 32766, 31206, 26604, 19410, 10325, 233, -9881, -19032, -26329, -31060,
	 * -32765, -31276, -26740, -19598, -10546, -466, 9658, 18842, 26189, 30985,
	 * 32761, 31345, 26874, 19784, 10767, 700, -9435, -18650, -26049, -30908,
	 * -32756, -31412, -27007, -19970, -10987 };
	 */
	public final static short[] SAMP22 = { 0, 10103, 19222, 26467, 31134,
			32766, 31206, 26604, 19410, 10325, 233, -9881, -19032, -26329,
			-31060, -32765, -31276, -26740, -19598, -10546, -466, 9658, 18842,
			26189, 30985, 32761, 31345, 26874, 19784, 10767, 700, -9435,
			-18650, -26049, -30908, -32756 };
	public final static short[] SAMP12 = { 0, 5574, 10987, 16079, 20702, 24721,
			28020, 30501, 32093, 32750, 32451, 31206, 29051, 26049, 22287,
			17875, 12942, 7632, 2099, -3494, -8986, -14217, -19032, -23293,
			-26874, -29672, -31604, -32615, -32675, -31782, -29962, -27269,
			-23780, -19598, -14844, -9658 };
	private final static double fullCycle = 2 * Math.PI;
	private final static int baud = 1200;
	private final static double numberOfSamples = sampleRate / baud;
	// might need to be a double
	// private final SensorManager mSensorManager;
	// private final Sensor mAccel, mGyro, mMag, mTemp, mPress, mLight;

	private TextView frameTextView;
	private TextView timeStampTextView;
	private TextView aprsTextView;
	private boolean play;
	private Handler handler;
	private double phase;
	
	private SensorDataCollector data;

	public SensorDataRunnable(Handler handler, TextView frame, TextView timeStamp, TextView aprs, SensorDataCollector data) {
		this.handler = handler;
		this.frameTextView = frame;
		this.timeStampTextView = timeStamp;
		this.aprsTextView = aprs;
		this.data = data;
	}

	public void run() {
		/*
		 * Setup sensor listeners mSensorManager =
		 * (SensorManager)getSystemService(SENSOR_SERVICE); mAccel =
		 * mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); mGyro =
		 * mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); mMag =
		 * mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); mTemp =
		 * mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		 * mPress = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		 * mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		 */

		short[] sound;
		play = true;

		// set up audio player
		int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
				sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
				AudioTrack.MODE_STREAM);
		track.play();

		while (play) {
			String src = "KC9NZJ" + (char) 0xd;
			String dst = "100KFT" + (char) 0x3;
			final String info = makeInfoField(data.getTime(), data.getLocation(), data.getAccel(),
					data.getGyro(), data.getMag(), data.getTemp(), data.getPress(), data.getLight());
			final String frame = makeAprsFrame(src, dst, info);
			
			sound = encodeBell202(frame, 30, 10);
			track.write(sound, 0, sound.length);
			handler.post(new Runnable() {
				public void run() {
					if (frameTextView != null) frameTextView.setText(frame);
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					Date date = new Date();
					if (timeStampTextView != null) timeStampTextView.setText(dateFormat.format(date));
					if (aprsTextView != null) aprsTextView.setText(info);
				}
			});
		}

		track.stop();
		track.flush();
		track.release();
	}

	public boolean playing() {
		return play;
	}

	public void stop() {
		play = false;
	}

	public String makeInfoField(
	// X = left(-) to right(+) across screen
	// Y = bottom(-) to top(+) across screen
	// Z = behind(-) to front(+) of screen
			long time, // timestamp in mS
			double[] gps, // lat, long, alt
			float[] accelerometer, // x, y, z (m/S^2)
			float[] gyroscope, // x, y, z (radians/S)
			float[] magnetometer, // x, y, z (uT)
			float[] temperature, // degree C
			float[] pressure, // hPa (millibar)
			float[] light // SI Lux
	) {
		String oMsgType = "/";
		String oTable = "/";
		String oSymbol = "S";
		String oTime = "083522h";
		String oGpsLat = d2dm(gps[0], true); // "3725.32N";
		String oGpsLon = d2dm(gps[1], false); // "12205.04W";
		String oGpsAlt = "/A="
				+ ("000000" + String.valueOf(Math.floor(gps[2]))
						.substring(0, 6));
		String oGps = oGpsLat + oTable + oGpsLon + oSymbol + oGpsAlt;
		String oAccelX = intToBase91((int) ((20 + accelerometer[0]) * 100), 2);
		String oAccelY = intToBase91((int) ((20 + accelerometer[1]) * 100), 2);
		String oAccelZ = intToBase91((int) ((20 + accelerometer[2]) * 100), 2);
		String oAccel = "A" + oAccelX + oAccelY + oAccelZ;
		String oGyroX = intToBase91((int) ((35 + gyroscope[0]) * 100), 2);
		String oGyroY = intToBase91((int) ((35 + gyroscope[1]) * 100), 2);
		String oGyroZ = intToBase91((int) ((35 + gyroscope[2]) * 100), 2);
		String oGyro = "G" + oGyroX + oGyroY + oGyroZ;
		String oMagX = intToBase91((int) ((3750 + magnetometer[0]) * 100), 3);
		String oMagY = intToBase91((int) ((3750 + magnetometer[1]) * 100), 3);
		String oMagZ = intToBase91((int) ((3750 + magnetometer[2]) * 100), 3);
		String oMag = "M" + oMagX + oMagY + oMagZ;
		String oTemp = intToBase91((int) ((274 + temperature[0]) * 10), 2);
		String oPress = intToBase91((int) (pressure[0] * 100), 3);
		String oIlum = intToBase91((int) light[0], 3);
		String oEnviro = "E" + oTemp + oPress + "I" + oIlum;
		return oMsgType + oTime + oGps + oAccel + oGyro + oMag + oEnviro;
	}

	public String makeAprsFrame(String source, String destination, String info) {
		char control = 0x03;
		char protocol = 0xf0;
		String src = makeAddress(source, true);
		String dst = makeAddress(destination);
		String frame = dst + src + control + protocol + info;
		String fcs = fcs(frame);
		return frame + fcs;
	}

	public String makeAddress(String text) {
		return makeAddress(text, false);
	}

	public String makeAddress(String text, boolean isFinal) {
		/*
		 * The Address Field The address field is 7 bytes long. The first 6
		 * bytes are the call sign right padded with spaces if needed. The call
		 * sign can only contain 0-9 and A-Z (not a-z). Only the lower 7 bits
		 * are used (0-127), which gets left-shifted one. The LSB is used as a
		 * "more data" bit where 0 means more data and 1 means stop. This
		 * function will set this bit if isFinal is true. The SSID is a number
		 * from 0-15 (4 bits) which is left-shifted four. The return value is 7
		 * characters long.
		 */
		char ssid = 0x0;
		text = text.toUpperCase();
		String call = "";
		// Is the last character an SSID of 0-15?
		if (text.charAt(text.length() - 1) <= 0xF) {
			ssid = text.charAt(text.length() - 1);
			Log.i("Rocket!", "Found SSID of " + (int) ssid + ":" + text);
			ssid = (char) ((ssid << 4) & 0xF0);
			if (isFinal) {
				ssid++;
			}
		} else {
			Log.i("Rocket!", "No SSID found:" + ":" + text);
		}
		// limit call sign to [0-9A-Z]
		for (int i = 0; i < text.length(); i++) {
			char thisChar = text.charAt(i);
			if (isAlphaNumeric(thisChar)) {
				// left-shift 1 all call sign characters
				call += (char) ((thisChar << 1) & 0xFF);
			} else {
				call += "@";
			}
		}
		// pad/limit call sign to 6 characters by adding 0x32<<1 to end
		call = (call + "@@@@@@").substring(0, 6);
		Log.i("Rocket!", "Address built:(" + (call + ssid).length() + ")"
				+ call + ssid);
		return call + ssid;
	}

	public short[] encodeBell202(String frame) {
		return encodeBell202(frame, 30, 10);
	}

	public short[] encodeBell202(String frame, int leadDelay, int endDelay) {
		// short[] output = new short[14600];
		short[] output = new short[(frame.length() + leadDelay + endDelay + 1)
				* 8 * SAMP22.length];

		// play clean tone
		/*
		 * short[] thisSample = SAMP22;
		 * 
		 * short[] output = new short[thisSample.length * 1000]; for (int i = 0;
		 * i < output.length;) { for(short sample: thisSample) {output[i++] =
		 * sample;} } if (frame != "") { return output; }
		 */

		boolean lastSamp12 = true;
		byte runningOnes = 0;
		int charIndex = 0;
		int outIndex = 0;

		// send lead flags
		// for each bit in nextChar
		// if 0, send different sample from last time
		// , update lastSamp12
		// , runningOnes = 0;
		// if 1, send same sample as last time
		// , update lastSamp12
		// , runningOnes++
		// , if runningOnes = 5, send extra 0
		// , runningOnes = 0
		// send trailing flags

		// A flag is 0x7E or 0b01111110
		for (int index = 0; index < leadDelay; index++) {
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(2200)) {
				output[outIndex++] = sample;
			}
			for (short sample : getCycles(1200)) {
				output[outIndex++] = sample;
			}
		}

		// String debug = "";
		for (char nextChar : frame.toCharArray()) {
			// debug = "Processing " + nextChar + ":" + (int)nextChar + " ";
			for (charIndex = 0; charIndex < 8; charIndex++) {
				if ((nextChar & 0x1) == 0) {
					// for(short sample: getCycles(1200)) {output[outIndex++] =
					// sample;}

					// 0 means send different sample from last time
					if (lastSamp12) {
						// debug += "0/22 ";
						for (short sample : getCycles(2200)) {
							output[outIndex++] = sample;
						}
						lastSamp12 = false;
					} else {
						// debug += "0/12 ";
						for (short sample : getCycles(1200)) {
							output[outIndex++] = sample;
						}
						lastSamp12 = true;
					}
					runningOnes = 0;

				} else {
					// for(short sample: getCycles(2200)) {output[outIndex++] =
					// sample;}

					// 1 means send same sample as last time
					if (lastSamp12) {
						// debug += "1/12 ";
						for (short sample : getCycles(1200)) {
							output[outIndex++] = sample;
						}
						// lastSamp12 = true;
					} else {
						// debug += "1/22 ";
						for (short sample : getCycles(2200)) {
							output[outIndex++] = sample;
						}
						// lastSamp12 = false;;
					}
					runningOnes++;
					if (runningOnes == 5) {
						// debug += "*";
						charIndex--;
						nextChar >>>= 1;
						nextChar <<= 1;
					}

				}
				nextChar >>>= 1;
			}
			// Log.i("encodeBell202",debug);
		}

		for (int index = 0; index < endDelay; index++) {
			if (lastSamp12) {
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
			} else {
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(1200)) {
					output[outIndex++] = sample;
				}
				for (short sample : getCycles(2200)) {
					output[outIndex++] = sample;
				}
			}
		}

		Log.i("FrameOutput", "Output length = " + outIndex);
		return output;
	}

	public String d2dm(double degrees, boolean isLat) {
		double minutes = 60 * (Math.abs(degrees) - Math.abs((int) degrees));
		String degWhole = right("000" + Math.abs((int) degrees), 3);
		String minWhole = right("00" + (int) minutes, 2);
		String minDec = right("00" + (int) ((minutes - (int) minutes) * 100), 2);
		if (isLat) {
			degWhole = right(degWhole, 2);
		}
		String output = degWhole + minWhole + "." + minDec;
		if (isLat) {
			if (degrees >= 0) {
				output += "N";
			} else {
				output += "S";
			}
		} else {
			if (degrees >= 0) {
				output += "E";
			} else {
				output += "W";
			}
		}
		return output;
	}

	public boolean isAlphaNumeric(char character) {
		// is it a number?
		if (character >= 0x30 && character <= 0x39) {
			return true;
		}
		// is it a upper case letter
		if (character >= 0x41 && character <= 0x5A) {
			return true;
		}
		// is it a lower case letter
		if (character >= 0x61 && character <= 0x7A) {
			return true;
		}
		return false;
	}

	public String right(String text, int len) {
		return text.substring(text.length() - len, text.length());
	}

	public String fcs(String frame) {
		return "XX";
	}

	public String intToBase91(int value) {
		return intToBase91(value, 0);
	}

	public String intToBase91(int value, int length) {
		String output = "";
		double power = 0;
		if (length < 1) {
			length = 1;
			while (value / java.lang.Math.pow(91, length) > 1) {
				length++;
			}
		}
		while (length > 0) {
			length--;
			power = java.lang.Math.pow(91, length);
			output += (char) (value / power + 33);
			value %= power;
		}
		return output;
	}

	public short[] getCycles(double frequency) {
		// double phase = the phase of the last sample (global)
		// int sampleRate = the audio sample rate (global)
		// double fullCycle = 2 * Math.PI(global)
		// int baud = baud rate (global)
		// double numberOfSamples = number of samples required at give sample
		// rate (global)
		// String debug = "";
		double step = ((frequency / baud) * fullCycle) / numberOfSamples;
		// the phase step for the next value
		short[] output = new short[(int) numberOfSamples];
		int outputIndex = 0;
		while (outputIndex < numberOfSamples) {
			output[outputIndex++] = (short) (Short.MAX_VALUE * Math.sin(phase));
			// debug += ((short)(Short.MAX_VALUE * Math.sin(phase))) + ", ";
			phase += step;
		}
		// Log.i("getCycles","f:" + frequency + " s:" + step);
		return output;
	}
}
