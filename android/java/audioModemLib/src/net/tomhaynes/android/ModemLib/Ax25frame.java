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

public class Ax25frame extends L2frame {
	// TODO Add checking for extended PID values
	/**
	 * Function naming formats and meanings
	 * 
	 * Intermediate data:
	 *   setFunction(DATA) accepts intermediate data from user
	 *   getFunction() requires dissect or set before valid
	 *   isFunction() requires dissect or set before valid
	 *   
	 * Raw data:
	 *   pushFunction(DATA) accepts raw data from user
	 *   pullFunction() requires push or make before valid
	 *   
	 * Intermediate to Raw data conversion:
	 *   makeFunction() requires dissect or set before valid
	 *   
	 * Raw to Intermediate data conversion:
	 *   dissectFunction() requires make or push before valid
	 *   
	 */
	/* ***********************************
	 * Constants
	 *************************************/
	public static final int CONTROL_BYTE_NOT_SET = -4;
	
	public static final int I_FRAME = 0;
	public static final int S_FRAME = 1;
	public static final int U_FRAME = 3;
	
	public static final int INVALID_FRAME_TYPE = -1;
	public static final int FRAME_TYPE_NOT_AVAILABLE = -2;
	
	public static final int INVALID_SEQUENCE = -1;
	
	public static final int INVALID_COMMAND = -1;
	public static final int COMMAND_NOT_AVAILABLE = -2;
	public static final int NO_COMMAND = -3;
	public static final int COMMAND_NOT_SET = -4;
	
	public static final int PID_INVALID = -1;
	public static final int PID_NOT_SET = -4;
	
	// S_FRAME Commands
	public static final int SCMD_RR = 0;
	public static final int SCMD_RNR = 1;
	public static final int SCMD_REJ = 2;
	
	// U_FRAME Commands
	public static final int UCMD_UI = 0;
	public static final int UCMD_DM = 3;
	public static final int UCMD_SABM = 7;
	public static final int UCMD_DISC = 8;
	public static final int UCMD_UA = 12;
	public static final int UCMD_FRMR = 17;
	
	public static final int MAX_FRAME_LENGTH = 512;
	public static final int MAX_ADDRESS_LENGTH = 70;
	public static final int MAX_CONTROL_LENGTH = 1;
	public static final int MAX_PID_LENGTH = 2;
	public static final int MAX_FCS_LENGTH = 2;
	public static final int MAX_INFO_LENGTH = 256;
	
	// Command/Response Bits
	public static final int CR_RESPONSE_V1 = 0x0;
	public static final int CR_RESPONSE_V2 = 0x1;
	public static final int CR_COMMAND_V2 = 0x2;
	public static final int CR_COMMAND_V1 = 0x3;
	public static final int CR_RESPONSE_COMPRESSED = 0x4;
	public static final int CR_COMMAND_COMPRESSED = 0x5;
	public static final int CR_COMMAND_NOT_SET = -4;
	
	public static final String CALL_NOT_SET = "";
	public static final int SSID_NOT_SET = -4;
	public static final int INFO_NOT_AVAILABLE = -4;
	public static final int REPEATER_INDEX_OUT_OF_RANGE = -1;
	
	public static final int ADDRESS_RESERVED_NOT_USED = 0x3;
	
	public static final int HID_NOT_SET = -4;
	public static final int QSO_NOT_SET = -4;
	
	/* ************************************
	 * Class variables
	 **************************************/
	// Frame fields
	private char[] _frame;
	private char[] _address;
	private char _control;
	private char _pid;
	private char[] _fcs;
	private char[] _info;
	
	// Address field bits - all
	private char[] address;
	private String sourceCall = CALL_NOT_SET;
	private int sourceSSID = SSID_NOT_SET;
	private int sourceReserved = ADDRESS_RESERVED_NOT_USED;
	private String destinationCall = CALL_NOT_SET;;
	private int destinationSSID = SSID_NOT_SET;
	private int destinationReserved = ADDRESS_RESERVED_NOT_USED;
	private int command = CR_COMMAND_NOT_SET; // Command/Response Version 1: 00b = ?, 11b = ? / Version 2: 01b = Response, 10b = Command
	
	// Address field bits - compressed header
	private boolean isCompressed = false;
	private int hid = HID_NOT_SET;
	private int qso = QSO_NOT_SET;
	
	// Address field bits - optional
	private String[] repeaterCall = new String[8];
	private int[] repeaterSSID = new int[] {-1, -1, -1, -1, -1, -1, -1, -1};
	private boolean[] hasBeenRepeated = new boolean[] {false, false, false, false, false, false, false, false};
	private int[] repeaterReserved = new int[] {0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3, 0x3};
	private int repeaterCount = 0;
	private final static int LAST_REPEATER = 7; //index of last repeater element in array
	
	// control byte
	private int control = CONTROL_BYTE_NOT_SET;
	private int controlLength = 1; // can not be determined from frame, must be explicitly set
	private int senderSequence = INVALID_SEQUENCE; //0-7, I only
	private int receiverSequence = INVALID_SEQUENCE; //0-7, I & S only
	private boolean pf = false;
	private int sCommand = COMMAND_NOT_SET; //0-3, 2 bits compressed to right of byte
	private int uCommand = COMMAND_NOT_SET; //0-31, 5 bits compressed to right of byte
	private int frameType = FRAME_TYPE_NOT_AVAILABLE; //0(I), 1(S), 3(U);

	// PID byte
	private int pid = PID_NOT_SET;
	
	// payload
	private char[] info;
	
	// FCS
	private int fcs = 0;
	
	/* *************************************
	 * Constructor
	 * *************************************/
	
	/**
	 * Creates a new Layer2frame of type Ax25
	 */
	public Ax25frame() {
		l2Type = "Ax25";
	}
	
	/* *************************************
	 * Frame processing
	 * *************************************/
	
	public boolean makeFrame() {
		return false; //TODO
	}
	
	public boolean setFrame(char[] frame) {
		return setFrame(frame, frame.length);
	}
	
	/**
	 * takes frame and splits it in to sub fields then calls the respective
	 * processors on each field. If any operations fail, this returns False.
	 * If everything succeeds, this returns True.
	 * 
	 * @return True on success, False on failure
	 */
	public boolean setFrame(char[] buffer, int len) {
		//Log.i("test","Atempting to process frame of length " + len + " with a buffer of length " + buffer.length);
		int pos = 0;
		int i = 0;
		if (buffer.length < 10 || buffer.length < len || buffer.length > MAX_FRAME_LENGTH) return false;
		_frame = new char[len];
		System.arraycopy(buffer, 0, _frame, 0, len);
		// address ends at first byte where the 0 bit is 1 or MAX_ADDRESS_LENGTH
		for (i = 0; pos < len && i < MAX_ADDRESS_LENGTH; pos++, i++) {
			if ((_frame[pos] & 1) != 0) break;
		}
		_address = new char[i+1];
		System.arraycopy(buffer, 0, _address, 0, i + 1);
		if (!setAddress(_address, _address.length)) return false;
		
		// control field
		_control = (char) (buffer[++pos] & 0xFF);
		if (!setControl(_control)) return false;
		
		// PID and info fields
		if (frameType == I_FRAME || (frameType == U_FRAME && uCommand == UCMD_UI)) {
			_pid = (char) (buffer[++pos] & 0xFF);
			setPID(_pid);
			if (len - pos - 2 > MAX_INFO_LENGTH) return false;
			_info = new char[len - pos - 2];
			pos++;
			for (i = 0; pos < len - 2 && i < MAX_INFO_LENGTH; pos++, i++) {
				_info[i] = buffer[pos];
			}
			setInfo(_info, _info.length);
		}
		if (frameType == U_FRAME && uCommand == UCMD_FRMR) {
			_info = new char[] {_frame[pos], _frame[pos+1], _frame[pos+2]};
			pos += 3;
			setInfo(_info, 3);
		}
		
		// FCS field
		_fcs = new char[] {buffer[pos], buffer[pos+1]};
		setFCS(((_fcs[0] & 0xFF) << 8) | (_fcs[1] & 0xFF));
		
		
		return true;
	}
	/* *************************************
	 * Address field processing
	 * *************************************
	 * Compressed header:
	 * 87654321 87654321 87654321 87654321 87654321 87654321 87654321
	 * ??????QQ QQQQQQCT 11111122 22223333 33444444 55555566 6666SSSS
	 * 
	 * Q = QSO bit
	 * C = LAPB Command/Response (1=command, 0=response)
	 * T = Header type (1=compressed, 0=normal)
	 * 1-6 = Call Sign bits
	 * S = SSID bits
	 * ? = unknown function, stored in hid bits
	 * 
	 * Normal header:
	 * 87654321 87654321 87654321 87654321 87654321 87654321 87654321
	 * 11111110 22222220 33333330 44444440 55555550 66666660 CRRSSSS0
	 * 
	 * 1-6 = Call Sign bits
	 * 0 = Expansion bits, last byte in address field set to 1
	 * C = Command/Response bit, one in source and one in destination SSID bytes
	 *     or Has Been Repeated bit if in repeater field
	 * R = Reserved bit, set to 1 if not used
	 * S = SSID
	 */
	
	/**
	 * Builds the address byte array from preset values. These other functions, at a minimum,
	 * must have been called before makeAddress() can succeed.
	 * 
	 * For Compressed Headers:
	 *   setCompressed(True)
	 *   setQSO(qso)
	 *   setCommand(command)
	 *   setDestination(call, ssid)
	 *   
	 *  For Normal Headers:
	 *    setCompressed(False)
	 *    setCommand(command)
	 *    setSource(call, ssid)
	 *    setDestination(call, ssid)
	 *   
	 */
	public char[] makeAddress() {
		if (command == COMMAND_NOT_SET) return null;
		if (destinationCall == CALL_NOT_SET) return null;
		if (destinationSSID == SSID_NOT_SET) return null;
		if (isCompressed) {
			if (command != CR_COMMAND_COMPRESSED || command != CR_RESPONSE_COMPRESSED) return null;
			if (hid == HID_NOT_SET) return null;
			if (qso == QSO_NOT_SET) return null;
			address = new char[7];
			address[0] = (char) (((hid << 2) & 0xFC) | ((qso >>> 6) & 0x3));
			address[1] = (char) (((qso << 2) & 0xFC) | 0x1);
			address[2] = (char) (((destinationCall.charAt(0) << 2) & 0xFC) | ((destinationCall.charAt(1) >>> 4) & 0x03));
			address[3] = (char) (((destinationCall.charAt(1) << 4) & 0xF0) | ((destinationCall.charAt(2) >>> 2) & 0x0F));
			address[4] = (char) (((destinationCall.charAt(2) << 6) & 0xC0) | (destinationCall.charAt(3) & 0x3F));
			address[5] = (char) (((destinationCall.charAt(3) << 2) & 0xFC) | ((destinationCall.charAt(4) >>> 4) & 0x03));
			address[6] = (char) (((destinationCall.charAt(1) << 4) & 0xF0) | (destinationSSID & 0x0F));;
			if (command == CR_COMMAND_COMPRESSED) {
				address[1] |= 0x2;
			}
			return address;
		} else {
			if (command != CR_COMMAND_V1 || command != CR_RESPONSE_V1 ||
				command != CR_COMMAND_V2 || command != CR_RESPONSE_V2) return null;
			if (sourceCall == CALL_NOT_SET) return null;
			if (sourceSSID == SSID_NOT_SET) return null;
			char[] newAddress = buildAddress(destinationCall, destinationSSID, (command & 0x2) >>> 1 != 0, destinationReserved, false);
			if (newAddress == null) return null;
			address = new char[14 + (repeaterCount * 7)];
			System.arraycopy(newAddress, 0, address, 0, 7);
			newAddress = buildAddress(sourceCall, sourceSSID, (command & 0x1) != 0, sourceReserved, repeaterCount == 0);
			if (newAddress == null) return null;
			System.arraycopy(newAddress, 0, address, 7, 7);
			for (int i = 0; i < repeaterCount; i++) {
				newAddress = buildAddress(repeaterCall[i], repeaterSSID[i], hasBeenRepeated[i], repeaterReserved[i], repeaterCount == i + 1);
				if (newAddress == null) return null;
				System.arraycopy(newAddress, 0, address, 13 + (i * 7), 7);
			}
			return address;
		}
	}
	
	private char[] buildAddress(String call, int ssid, boolean command, int reserved, boolean last) {
		if (ssid < 0 || ssid > 16) return null;
		if (call == CALL_NOT_SET) return null;
		call = call + "      ";
		char[] address = new char[7];
		for (int i = 0; i < 6; i++) {
			address[i] = call.charAt(i);
		}
		if (command) address[7] = 0x70;
		address[7] |= (reserved & 0x3) << 5;
		address[7] |= (ssid & 0xF) << 1;
		if (last) address[7] |= 0x1;
		return null;
	}
	
	public boolean setAddress(char[] address, int len) {
		if (address.length < len || len < 7 || len > MAX_ADDRESS_LENGTH || len % 7 != 0) return false;
		if ((address[1] & 1) == 1) {
			// Compressed header
			isCompressed = true;
			hid = address[0] >>> 2;
			qso = address[0] << 6 | address[1] >>> 2;
			command = (address[1] & 2) != 0 ? CR_COMMAND_COMPRESSED : CR_RESPONSE_COMPRESSED;
			destinationCall  = "" + (((address[2] >>> 2) & 0x3f)+0x20);
			destinationCall += "" + ((((address[2] << 4) | ((address[3] >>> 4) & 0xf)) &0x3f)+0x20);
			destinationCall += "" + ((((address[3] << 2) | ((address[4] >> 6) & 3)) & 0x3f)+0x20);
			destinationCall += "" + ((address[4] & 0x3f)+0x20);
			destinationCall += "" + (((address[5] >> 2) & 0x3f)+0x20);
			destinationCall += "" + ((((address[5] << 4) | ((address[6] >> 4) & 0xf)) & 0x3f)+0x20);
			destinationSSID = address[6] & 0xF;
			Log.i("test","New Address Block: Compressed to " + destinationCall + "-" + destinationSSID + " qso " + qso + (command != 0 ? "COMMAND" : "RESPONSE") + "(" + hid + ")");
		} else {
			// Normal header
			int i;
			int j;
			if (len < 14) return false;
			destinationCall = "";
			for (i = 0; i < 6; i++) {
				if ((address[i] &0xfe) != 0x40) { 
					destinationCall += Character.toString((char)(address[i] >>> 1));
				}
			}
			destinationReserved = (address[6] >>> 5) & 0x3;
			destinationSSID = (address[6] >>> 1) & 0xF;
			sourceCall = "";
			for (i = 7; i < 13; i++) {
				if ((address[i] &0xfe) != 0x40) { 
					sourceCall += Character.toString((char)(address[i] >>> 1));
				}
			}
			sourceReserved = (address[13] >>> 5) & 0x3;
			sourceSSID = (address[6] >>> 1) & 0xF;
			command = ((address[6] >>> 6) & 0x2) | ((address[13] >>> 7) & 0x1);
			Log.i("test","New Address Block: from " + sourceCall + "-" + sourceSSID + "(" + sourceReserved + ")" +
					" to " + destinationCall + "-" + destinationSSID + "(" + destinationReserved + ") " + 
					((command & 2) == 0 ?
							((command & 1) == 0 ? "RESPONSE V1" : "RESPONSE V2") :
							((command & 1) == 0 ? "COMMAND V2" : "COMMAND V1"))
			);
			if (len == 14) return true;
			int ri = 0;
			for (i = 14; i < len; i += 7) {
				repeaterCall[ri] = "";
				for (j = 0; j < 6; j++) {
					if ((address[i+j] &0xfe) != 0x40) { 
						repeaterCall[ri] += Character.toString((char)(address[i+j] >>> 1));
					}
				}
				repeaterReserved[ri] = (address[6] >>> 5) & 0x3;
				repeaterSSID[ri] = (address[6] >>> 1) & 0xF;
				hasBeenRepeated[ri] = (((address[6] >>> 7) & 1) != 0);
				Log.i("test","Adding Repeater: " + repeaterCall[ri] + "-" + repeaterSSID[ri] + "(" + repeaterReserved[ri] + ")" + (hasBeenRepeated[ri] ? " Repeated" : ""));
				ri++;
			}
			repeaterCount = ri + 1;
			return true;
		}
		return false;
	}
	
	public char[] getAddress() {
		return address;
	}
	
	/**
	 * Verifies that call only contains valid characters but not that call is
	 * a valid call sign.
	 * 
	 * TODO This is not very robust yet. If should be fixed up some.
	 * 
	 * @param call
	 * @return
	 */
	public boolean validateCall(String call) {
		return call.matches("[a-zA-Z0-9 ]*");
	}
	
	/**
	 * Sets the source call sign. Call is truncated to the first 6 characters
	 * or right padded with spaces if less than 6 characters are available. All
	 * characters will be shifted to upper case. Valid characters for a call sign are 
	 * A-Z and 0-9 but compressed headers can encode ASCII 32-95 and normal headers
	 * can encode ASCII 0-127. If makeFrame() or makeAddress() will fail if 
	 * isCompressed() is true and any address field contains characters not between
	 * ASCII 32 and 95. It is safest to stick with A-Z (ASCII 65-90) and 0-9 (ASCII
	 * 48-57). validateCall() will help you make sure it is correct.
	 * 
	 * If any part fails, all values will remain unchanged and this will return false.
	 * 
	 * @param call The call sign of the station
	 * @param ssid The SSID of the station. 0-16
	 * @return True on success, false on failure
	 */
	public boolean setSource(String call, int ssid) {
		return setSource(call, ssid, ADDRESS_RESERVED_NOT_USED);
	}
	
	/**
	 * Sets the source call sign. Call is truncated to the first 6 characters
	 * or right padded with spaces if less than 6 characters are available. All
	 * characters will be shifted to upper case. Valid characters for a call sign are 
	 * A-Z and 0-9 but compressed headers can encode ASCII 32-95 and normal headers
	 * can encode ASCII 0-127. If makeFrame() or makeAddress() will fail if 
	 * isCompressed() is true and any address field contains characters not between
	 * ASCII 32 and 95. It is safest to stick with A-Z (ASCII 65-90) and 0-9 (ASCII
	 * 48-57).  validateCall() will help you make sure it is correct.
	 * 
	 * The reserved bits are used by cooperation of the net. If not used they should
	 * be set to ADDRESS_RESERVED_NOT_USED.
	 * 
	 * If any part fails, all values will remain unchanged and this will return false.
	 * 
	 * @param call The call sign of the station
	 * @param ssid The SSID of the station. 0-16
	 * @param reserved the reserved bits
	 * @return True on success, false on failure
	 */
	public boolean setSource(String call, int ssid, int reserved) {
		if (reserved < 0 || reserved > 3) return false;
		String oldCall = sourceCall;
		if (!setSourceCall(call)) return false;
		if (!setSourceSSID(ssid)) {
			sourceCall = oldCall;
			return false;
		}
		sourceReserved = reserved;
		return true;
	}
	
	public boolean setSourceCall(String call) {
		call.toUpperCase();
		call += "      ";
		for(int i = 0; i < 6; i++) {
			if (i < 0 || i > 127) {
				sourceCall += call.substring(i, i + 1);
			}
		}
		return true;
	}
	
	public String getSourceCall() {
		return sourceCall;
	}
	
	public boolean setSourceSSID(int ssid) {
		if (ssid < 0 || ssid > 16) return false;
		sourceSSID = ssid;
		return true;
	}
	
	public int getSourceSSID() {
		return sourceSSID;
	}
	
	public boolean setSourceReserved(int reserved) {
		if (reserved < 0 || reserved > 3) return false;
		sourceReserved = reserved;
		return true;
	}
	
	public int getSourceReserved(int index) {
		return sourceReserved;
	}
	
	public boolean setDestination(String call, int ssid) {
		return setDestination(call, ssid, ADDRESS_RESERVED_NOT_USED);
	}
	
	public boolean setDestination(String call, int ssid, int reserved) {
		if (reserved < 0 || reserved > 3) return false;
		String oldCall = destinationCall;
		if (!setDestinationCall(call)) return false;
		if (!setDestinationSSID(ssid)) {
			destinationCall = oldCall;
			return false;
		}
		destinationReserved = reserved;
		return true;
	}
	
	public boolean setDestinationCall(String call) {
		call.toUpperCase();
		call += "      ";
		for(int i = 0; i < 6; i++) {
			if (i < 0 || i > 127) { 
				destinationCall += call.substring(i, i + 1);
			}
		}
		return true;
	}
	
	public String getDestinationCall() {
		return destinationCall;
	}
	
	public boolean setDestinationSSID(int ssid) {
		if (ssid < 0 || ssid > 16) return false;
		destinationSSID = ssid;
		return true;
	}
	
	public int getDestinationSSID() {
		return destinationSSID;
	}
	
	public boolean setDestinationReserved(int reserved) {
		if (reserved < 0 || reserved > 3) return false;
		sourceReserved = reserved;
		return true;
	}
	
	public int getDestinationReserved() {
		return destinationReserved;
	}
	
	public boolean setCommandResponse(int cr) {
		if (cr < 0 || cr > 5) return false;
		destinationReserved = cr;
		return true;
	}
	
	public int getCommandResponse() {
		return command;
	}
	
	public boolean isCommand() {
		switch(command) {
		case CR_COMMAND_V1:
			return true;
		case CR_COMMAND_V2:
			return true;
		case CR_COMMAND_COMPRESSED:
			return true;
		default:
			return false;
		}
	}
	
	public boolean isResponse() {
		switch(command) {
		case CR_RESPONSE_V1:
			return true;
		case CR_RESPONSE_V2:
			return true;
		case CR_RESPONSE_COMPRESSED:
			return true;
		default:
			return false;
		}
	}
	public boolean deleteRepeater(int index) {
		if (index >= repeaterCount) return false;
		if (index < LAST_REPEATER) {
			System.arraycopy(repeaterCall, index + 1, repeaterCall, index, LAST_REPEATER - index);
			System.arraycopy(repeaterSSID, index + 1, repeaterSSID, index, LAST_REPEATER - index);
			System.arraycopy(repeaterReserved, index + 1, repeaterReserved, index, LAST_REPEATER - index);
			System.arraycopy(hasBeenRepeated, index + 1, hasBeenRepeated, index, LAST_REPEATER - index);
		}
		repeaterCall[LAST_REPEATER] = CALL_NOT_SET;
		repeaterSSID[LAST_REPEATER] = SSID_NOT_SET;
		repeaterReserved[LAST_REPEATER] = ADDRESS_RESERVED_NOT_USED;
		hasBeenRepeated[LAST_REPEATER] = false;
		repeaterCount--;
		return true;
	}
	
	public boolean addRepeater(String call, int ssid, boolean repeated) {
		return addRepeater(call, ssid, repeated, ADDRESS_RESERVED_NOT_USED);
	}
	
	public boolean addRepeater(String call, int ssid, boolean repeated, int reserved) {
		if (repeaterCount > LAST_REPEATER) return false;
		repeaterCount++;
		if (!setRepeater(repeaterCount - 1, call, ssid, repeated, reserved)) {
			repeaterCount--;
			return false;
		}
		return true;
	}
	
	public boolean setRepeater(int index, String call, int ssid, boolean repeated) {
		return setRepeater(index, call, ssid, repeated, ADDRESS_RESERVED_NOT_USED);
	}
	
	public boolean setRepeater(int index, String call, int ssid, boolean repeated, int reserved) {
		if (index >= repeaterCount) return false;
		if (reserved < 0 || reserved > 3) return false;
		String oldCall = repeaterCall[index];
		if (!setRepeaterCall(index, call)) return false;
		int oldSSID = repeaterSSID[index];
		if (!setRepeaterSSID(index, ssid)) {
			repeaterCall[index] = oldCall;
			return false;
		}
		if (!setRepeaterReserved(index, reserved)) {
			repeaterCall[index] = oldCall;
			repeaterSSID[index] = oldSSID;
			return false;
		}
		hasBeenRepeated[index] = repeated;
		return true;
	}
	
	public boolean setRepeaterCall(int index, String call) {
		if (index >= repeaterCount) return false;
		call.toUpperCase();
		call += "      ";
		for(int i = 0; i < 6; i++) {
			if (i < 0 || i > 127) {
				repeaterCall[index] += call.substring(i, i + 1);
			}
		}
		return true;
	}
	
	public String getRepeaterCall(int index) {
		if (index >= repeaterCount) return "";
		return repeaterCall[index];
	}
	
	public boolean setRepeaterSSID(int index, int ssid) {
		if (index >= repeaterCount) return false;
		if (ssid < 0 || ssid > 16) return false;
		repeaterSSID[index] = ssid;
		return true;
	}
	
	public int getRepeaterSSID(int index) {
		if (index >= repeaterCount) return REPEATER_INDEX_OUT_OF_RANGE;
		return repeaterSSID[index];
	}
	
	public boolean setRepeaterRepeated(int index, boolean status) {
		if (index >= repeaterCount) return false;
		hasBeenRepeated[index] = status;
		return true;
	}
	
	public boolean getRepeaterRepeated(int index) {
		if (index >= repeaterCount) return false;
		return hasBeenRepeated[index];
	}
	
	public boolean setRepeaterReserved(int index, int reserved) {
		if (index >= repeaterCount) return false;
		if (reserved < 0 || reserved > 3) return false;
		repeaterReserved[index] = reserved;
		return true;
	}
	
	public int getRepeaterReserved(int index) {
		if (index > repeaterCount) return REPEATER_INDEX_OUT_OF_RANGE;
		return repeaterReserved[index];
	}
	
	public int getRepeaterCount() {
		return repeaterCount;
	}
	
	public boolean setQSO(int qso) {
		this.qso = qso;
		return true;
	}
	
	public int getQSO() {
		return qso;
	}
	
	public boolean setHID(int hid) {
		if (hid >= 0x3F) return false;
		this.hid = hid;
		return true;
	}
	
	public int getHID() {
		return hid;
	}
	public boolean isCompressed() {
		return isCompressed;
	}
	
	public void setCompressed(boolean compressed) {
		isCompressed = compressed;
	}
	/* *************************************
	 * Control byte processing
	 * *************************************/
	
	/**
	 * Returns the raw control byte. If the control byte was not set using 
	 * setControl(BYTE) or created using makeControl() this will return
	 * CONTROL_BYTE_NOT_SET.
	 */
	public int getControl() {
		return control & 0xFF;
	}
	
	/**
	 * Builds and returns the raw control byte. If all the appropriate options have
	 * not been set this will return CONTROL_BYTE_NOT_SET. This will also update the 
	 * class instance control byte so you can retrieve it later using getControl().
	 * You can set the options using setControl(BYTE) and modify options or...
	 * 
	 * For I_FRAME:
	 *   setFrameType(I_FRAME);
	 *   setSenderSequence(INT);
	 *   setReceiverSequence(INT);
	 *   setPF(BOOLEAN);
	 *   makeControl();
	 *   
	 * For S_FRAME:
	 *   setFrameType(S_FRAME);
	 *   setReceiverSequence(INT);
	 *   setSCommand(INT);
	 *   setPF(BOOLEAN);
	 *   makeControl();
	 *   
	 * For U_FRAME:
	 *   frameType(U_FRAME);
	 *   setUCommand(INT);
	 *   setPF(BOOLEAN);
	 *   makeControl();
	 *   
	 * @return
	 */
	public int makeControl() {
		int control = CONTROL_BYTE_NOT_SET;
		switch (frameType) {
		case I_FRAME: //rrrpsss0
			if (senderSequence == INVALID_SEQUENCE) break;
			if (receiverSequence == INVALID_SEQUENCE) break;
			control = 0;
			control |= (senderSequence & 0x7) << 1;
			control |= (receiverSequence & 0x7) << 1;
			break;
		case S_FRAME: //rrrpcc01
			if (receiverSequence == INVALID_SEQUENCE) break;
			if (sCommand == COMMAND_NOT_SET) break;
			control = 1;
			control |= (receiverSequence & 0x7) << 5;
			control |= (sCommand & 0x3) << 2;
			break;
		case U_FRAME: //cccpcc11
			if (uCommand == COMMAND_NOT_SET) break;
			control = 3;
			control |= (uCommand & 0x3) << 2;
			control |= (uCommand & 0x1C) << 3; 
			break;
		default:
			return CONTROL_BYTE_NOT_SET;
		}
		if (control != CONTROL_BYTE_NOT_SET) {
			if (pf) control |= 0x10;
		}
		this.control = control;
		return control;
	}
	
	/**
	 * Accepts the raw control byte and updates the frame fields based on those values.
	 * If possible it will set the Sender Sequence, Receiver Sequence, P/F bit, Command
	 * and Frame Type.
	 * 
	 * @param control
	 */
	public boolean setControl(char control) {
		if (controlLength == 1) { // Standard AX25 control field
			switch (control & 3) {
			case I_FRAME:
				receiverSequence = (control >>> 5) & 7;
				senderSequence = (control >>> 1) & 7;
				frameType = I_FRAME;
				Log.i("test","I Frame RX=" + receiverSequence + " TX=" + senderSequence);
				break;
			case S_FRAME:
				receiverSequence = (control >>> 5) & 7;
				sCommand = (control >>> 2) & 0x3;
				frameType = S_FRAME;
				Log.i("test","S Frame RX=" + receiverSequence + " type " + getCommandName());
				break;
			case U_FRAME:
				uCommand = ((control >>> 3) & 0x1c) | ((control >>> 2) & 0x3);
				frameType = U_FRAME;
				Log.i("test","U Frame type " + getCommandName());
				break;
			}
			pf = ((control >>> 4) & 1) != 0;
			this.control = control;
			return true;
		} else { // Extended AX25 control field
			// TODO add support for extended control fields
			return false;
		}
	}
	
	/**
	 * This will set the P/F bit if true and un-set it it if false
	 * 
	 * @param state True to set, False to un-set
	 */
	public void setPF(boolean state) {
		pf = state;
	}
	
	/**
	 * If the P/F bit is set this will return true, otherwise false.
	 * 
	 * @return True of P/F bit set, False if P/F bit not set
	 */
	public boolean getPF() {
		return pf;
	}
	
	/**
	 * If frame was set as an I_FRAME using setControl(BYTE) or setFrameType(INT) this will
	 * return true, otherwise false;
	 * 
	 * @return True if I_FRAME, False if not
	 */
	public boolean isIFrame() {
		if (frameType == I_FRAME) return false;
		return false;
	}
	
	/**
	 * If frame was set as an S_FRAME using setControl(BYTE) or setFrameType(INT) this will
	 * return true, otherwise false;
	 * 
	 * @return True if S_FRAME, False if not
	 */
	public boolean isSFrame() {
		if (frameType == S_FRAME) return false;
		return false;
	}
	
	/**
	 * If frame was set as an U_FRAME using setControl(BYTE) or setFrameType(INT) this will
	 * return true, otherwise false;
	 * 
	 * @return True if U_FRAME, False if not
	 */
	public boolean isUFrame() {
		if (frameType == U_FRAME) return false;
		return false;
	}
	
	/**
	 * Sets the frame type. If an invalid type is passed, the value will remain unchanged
	 * and this will return false.
	 * 
	 * @param type 0,1,3 with known values of I_FRAME, S_FRAME, U_FRAME
	 * @return True on success, False on failure
	 */
	public boolean setFrameType(int type) {
		switch(type) {
		case I_FRAME:
			frameType = I_FRAME;
			return true;
		case S_FRAME:
			frameType = S_FRAME;
			return true;
		case U_FRAME:
			frameType = U_FRAME;
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Gets the frame type. If the frame type has not been set yes using
	 * setFrameType(INT) or setControl(BYTE) this will return FRAME_TYPE_NOT_AVAILABLE.
	 * If an invalid frame type was set this will return INVALID_FRAME_TYPE.
	 * 
	 * @return 0,1,3 with known values of I_FRAME, S_FRAME, U_FRAME
	 *         INVALID_FRAME_TYPE, FRAME_TYPE_NOT_AVAILABLE on failure
	 */
	public int getFrameType() {
		switch (frameType) {
		case I_FRAME:
			return I_FRAME;
		case S_FRAME:
			return S_FRAME;
		case U_FRAME:
			return U_FRAME;
		case FRAME_TYPE_NOT_AVAILABLE:
			return FRAME_TYPE_NOT_AVAILABLE;
		default:
			return INVALID_FRAME_TYPE;
		}
	}
	
	/**
	 * Sets the command number for S_FRAME frame types. Valid commands are
	 * between 0 and 3. If an invalid command it passed, the value will
	 * remain unchanged and the function will return False.
	 * 
	 * To extract the command from the original control byte you must use
	 * the command (control >>> 2) & 0x3
	 * 
	 * @param 0-3 with known good values of SCMD_RR, SCMD_RNR, SCMD_REJ
	 * @return True on success, False on failure
	 */
	public boolean setSCommand(int command) {
		if (command > 3 || command < 0) return false;
		sCommand = command;
		return true;
	}
	
	/**
	 * Gets the command number for S_FRAME frame types. If the command
	 * has not been set yet using setControl(BYTE) or setSCommand(INT) then
	 * it will return COMMAND_NOT_SET.
	 * 
	 * To realign the return value with original bit position you must use
	 * the command (value & 3) << 2
	 * 
	 * @return 0-3 with known values of SCMD_RR, SCMD_RNR, SCMD_REJ
	 *         COMMAND_NOT_SET on failure
	 */
	public int getSCommand() {
		return sCommand;
	}
	
	/**
	 * Sets the command number for U_FRAME frame types. Valid commands are
	 * between 0 and 31. If an invalid command it passed, the value will
	 * remain unchanged and the function will return False.
	 * 
	 * To extract the command from the original control byte you must use
	 * the command ((control >>> 3) & 0x1c) | ((control >>> 2) & 0x3)
	 * 
	 * @param command 0-31 with known values of UCDM_UI, UCMD_DM, UCMD_SAMB, UCMD_DISC, UCMD_UA, UCMD_FRMR
	 * @return True on success, False on failure
	 */
	public boolean setUCommand(int command) {
		if (command > 31 || command < 0) return false;
		uCommand = command;
		return true;
	}
	
	/**
	 * Gets the command number for U_FRAME frame types. If the command
	 * has not been set yet using setControl(BYTE) or setUCommand(INT) then
	 * it will return COMMAND_NOT_SET.
	 * 
	 * To realign the return value with original bit position you must use
	 * the command (((value & 0x3) << 2) | ((value & 0x1C) << 3))
	 * 
	 * @return 0-31 with known values of UCDM_UI, UCMD_DM, UCMD_SAMB, UCMD_DISC, UCMD_UA, UCMD_FRMR
	 *         COMMAND_NOT_SET on failure
	 */
	public int getUCommand() {
		return uCommand;
	}
	
	/**
	 * Returns the command number. If the command is unknown for
	 * S_FRAME or U_FRAME it will return INVALID_COMMAND. If this is an
	 * I_FRAME it will return NO_COMMAND. If the frame type has not been
	 * set yet using setContol(BYTE) or using setSCommand(INT) or
	 * setUCommand(INT) and setFrameType(INT) it will return INVALID_COMMAND.
	 * Some command numbers overlap so you must check the frame type using
	 * getFrameType()
	 * 
	 * @return for I_FRAME: NO_COMMAND
	 *         for S_FRAME: 0-3 with known values of SCMD_RR, SCMD_RNR, SCMD_REJ
	 *         for U_FRAME: 0-31 with known values of UCDM_UI, UCMD_DM, UCMD_SAMB, UCMD_DISC, UCMD_UA, UCMD_FRMR
	 *         failures for all frame types: INVALID_COMMAND, COMMAND_NOT_AVAIABLE, COMMAND_NOT_SET
	 */
	public int getCommand() {
		if (control == CONTROL_BYTE_NOT_SET) return COMMAND_NOT_AVAILABLE;
		switch (frameType) {
		case I_FRAME:
			return NO_COMMAND;
		case S_FRAME:
			return sCommand;
		case U_FRAME:
			return uCommand;
		default:
			return INVALID_COMMAND;
		}
	}
	
	/**
	 * Returns the name of the command. If the command is unknown for
	 * S_FRAME or U_FRAME it will return "Unknown t Command nn" with
	 * t being S or U depending on the frame type and nn being the command
	 * number tried. If this is an I_FRAME it will return "No Command".
	 * If the frame type has not been set yet using setContol(BYTE) or
	 * using setSCommand(INT) or setUCommand(INT) and setFrameType(INT)
	 * it will return "Invalid Command".
	 * 
	 * @return The name of the command
	 */
	public String getCommandName() {
		switch (frameType) {
		case I_FRAME:
			return "No Command";
		case S_FRAME:
			switch (sCommand) {
			case 0: //RR
				return "Receive Ready";
			case 1: //RNR
				return "Receive Not Ready";
			case 2: //REJ
				return "Reject";
			case COMMAND_NOT_SET:
				return "Command not set";
			default: //undefined
				return "Unknown S Command" + sCommand;
			}
		case U_FRAME:
			switch (uCommand) {
			case 0: //UI
				return "Unnumbered Information";
			case 3: //DM
				return "Disconnected Mode";
			case 7: //SAMB
				return "Set Asynchronous Balanced Mode";
			case 8: //DISC
				return "Disconnect";
			case 12: //UA
				return "Unnumbered Acknowledge";
			case 17: //FRMR
				return "Frame Reject";
			case COMMAND_NOT_SET:
				return "Command not set";
			default:
				return "Unknown U Command " + uCommand;
			}
		default:
			return "Invalid Command";
		}
	}
	
	/**
	 * The Sender Sequence is a number between 0 and 7. It is only valid for
	 * I_FRAME frame types. If sequence is not between 0 and 7, Sender
	 * Sequence will remain unchanged and the function will return False.
	 * 
	 * @param sequence 0-7
	 * @return True on success, False on failure
	 */
	public boolean setSenderSequence(int sequence) {
		if (sequence > 7 || sequence < 0) return false;
		senderSequence = sequence;
		return true;
	}
	
	/**
	 * The Sender Sequence is a number between 0 and 7. It is only available
	 * from I_FRAME frame types. If setControl(BYTE) or setSenderSequence(INT)
	 * has not been called yet, this will return INVALID_SEQUENCE.
	 * 
	 * @return 0-7 or INVALID_SEQUENCE
	 */
	public int getSenderSequence() {
		return senderSequence;
	}
	
	/**
	 * The Receiver Sequence is a number between 0 and 7. It is only valid for
	 * I_FRAME and S_FRAME frame types. If sequence is not between 0 and 7,
	 * Receiver Sequence will remain unchanged and the function will return
	 * False.
	 * 
	 * @param sequence 0-7
	 * @return True on success, False on failure
	 */
	public boolean setReceiverSequence(int sequence) {
		if (sequence > 7 || sequence < 0) return false;
		receiverSequence = sequence;
		return true;
	}
	
	/**
	 * The Receiver Sequence is a number between 0 and 7. It is only available
	 * from I_FRAME and S_FRAME frame types. If setControl(BYTE) or
	 * setReceiverSequence(INT) has not been called yet, this will return INVALID_SEQUENCE.
	 * 
	 * @return 0-7 or INVALID_SEQUENCE
	 */
	public int getReceiverSequence() {
		return receiverSequence;
	}
	
	/* *************************************
	 * PID byte processing
	 * 
	 * The Protocol ID is only used in
	 * I_FRAMEs
	 * *************************************/
	
	/**
	 * Sets the PID value. If value is not between 0 and 255 this will return false,
	 * otherwise true.
	 */
	public boolean setPID(int value) {
		if (value > 0xFF || value < 0) return false;
		pid = value;
		Log.i("test","PID=" + pid + " " + getPIDName());
		return true;
	}
	
	/**
	 * Gets the PID value used. If the PID has not been set yet using setPID(INT) this
	 * will return PID_NOT_SET. If the PID is not a value from 0-255 this will return
	 * PID_INVALID.
	 * 
	 * @return 0-255 on success, PID_NOT_SET or PID_INVALID on failure.
	 */
	public int getPID() {
		return pid;
	}
	
	/**
	 * Gets the name of the Layer 3 protocol used. If the PID has not been set yet using
	 * setPID(INT) this will return "PID Not Set". If the PID is unknown, this will return
	 * "PID nn Unknown" where nn is the PID number.
	 * 
	 * @return The name of the layer 3 protocol
	 */
	public String getPIDName() {
		switch (pid) {
		case 0x01:
			return "ISO 8208/CCITT X.25 PLP";
		case 0x06:
			return "Compressed TCP/IP packet. Van Jacobson (RFC 1144)";
		case 0x07:
			return "Uncompressed TCP/IP packet. Van Jacobson (RFC 1144)";
		case 0x08:
			return "Segmentation fragment";
		case 0xC3:
			return "TEXNET datagram protocol";
		case 0xC4:
			return "Link Quality Protocol";
		case 0xCA:
			return "Appletalk";
		case 0xCB:
			return "Appletalk ARP";
		case 0xCC:
			return "ARPA Internet Protocol";
		case 0xCD:
			return "ARPA Address resolution";
		case 0xCE:
			return "FlexNet";
		case 0xCF:
			return "NET/ROM";
		case 0xF0:
			return "No layer 3 protocol implemented";
		case 0xFF:
			return "Escape character. Next octet contains more Level 3 protocol information.";
		case PID_NOT_SET:
			return "PID Not Set";
		}
		switch (pid & 0x30) {
		case 0x00:
			return "Reserved for future layer 3 protocols";
		case 0x10:
			return "AX.25 layer 3 implemented";
		case 0x20:
			return "AX.25 layer 3 implemented";
		case 0x30:
			return "Reserved for future layer 3 protocols";
		}
		return "PID " + pid + "Unknown";
	}
	
	/* *************************************
	 * Info processing
	 * *************************************/
	public boolean makeInfo() {
		return false; //TODO
	}
	
	public boolean setInfo(char[] info, int len) {
		this.info = info;
		Log.i("test","INFO " + "(" + _info.length + ")" + infoToString());
		return false; //TODO
	}
	
	public String infoToString() {
		String result = "";
		for (int i = 0; i < _info.length; i++) {
			result += (int)_info[i] + ".";
			//result += Character.toString(_info[i]) + ".";
		}
		return result;
	}
	public char[] getInfo() {
		return info;
	}
	
	/* *************************************
	 * FCS processing
	 * *************************************/
	public boolean makeFCS() {
		int crc = 0xffff;
		int index = 0;
		int cnt = _frame.length;
		for (; cnt > 0; cnt--)
			crc = (crc >> 8) ^ crc_ccitt_table[(crc ^ _frame[index++]) & 0xff];
		crc ^= 0xffff;
		fcs = (crc & 0xffff);
		return true;
	}
	
	public boolean setFCS(int fcs) {
		if (fcs < 0 || fcs > 0xFFFF) return false;
		this.fcs = fcs;
		//Log.i("test","FCS=" + fcs + "["+ (int)_fcs[0] +"]["+ (int)_fcs[1] +"]" +(checkFCS() ? " PASSED" : " FAILED"));
		return true;
	}
	
	public int getFCS() {
		return fcs;
	}
	
	public boolean checkFCS() {
		int crc = 0xffff;
        int index = 0;
        int cnt = _frame.length;
        for (; cnt > 0; cnt--)
                crc = (crc >> 8) ^ crc_ccitt_table[(crc ^ _frame[index++]) & 0xff];
        return (crc & 0xffff) == 0xf0b8;
	}
	
	public static final int crc_ccitt_table[] = {
        0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536, 0x74bf,
        0x8c48, 0x9dc1, 0xaf5a, 0xbed3, 0xca6c, 0xdbe5, 0xe97e, 0xf8f7,
        0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
        0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed, 0xcb64, 0xf9ff, 0xe876,
        0x2102, 0x308b, 0x0210, 0x1399, 0x6726, 0x76af, 0x4434, 0x55bd,
        0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e, 0xfae7, 0xc87c, 0xd9f5,
        0x3183, 0x200a, 0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c,
        0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef, 0xea66, 0xd8fd, 0xc974,
        0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb,
        0xce4c, 0xdfc5, 0xed5e, 0xfcd7, 0x8868, 0x99e1, 0xab7a, 0xbaf3,
        0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a,
        0xdecd, 0xcf44, 0xfddf, 0xec56, 0x98e9, 0x8960, 0xbbfb, 0xaa72,
        0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab, 0x0630, 0x17b9,
        0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a, 0xb8e3, 0x8a78, 0x9bf1,
        0x7387, 0x620e, 0x5095, 0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738,
        0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb, 0xa862, 0x9af9, 0x8b70,
        0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c, 0xd3a5, 0xe13e, 0xf0b7,
        0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64, 0x5fed, 0x6d76, 0x7cff,
        0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad, 0xc324, 0xf1bf, 0xe036,
        0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7, 0x6c7e,
        0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e, 0xf2a7, 0xc03c, 0xd1b5,
        0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd,
        0xb58b, 0xa402, 0x9699, 0x8710, 0xf3af, 0xe226, 0xd0bd, 0xc134,
        0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c,
        0xc60c, 0xd785, 0xe51e, 0xf497, 0x8028, 0x91a1, 0xa33a, 0xb2b3,
        0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9, 0x2f72, 0x3efb,
        0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9, 0x8120, 0xb3bb, 0xa232,
        0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a,
        0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a, 0xb0a3, 0x8238, 0x93b1,
        0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9,
        0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab, 0xa022, 0x92b9, 0x8330,
        0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78
	};
}

/*
boolean process(Ax25frame frame) {
	int len = frame._frameptr;
	char[] bp = frame._frame;
	int bp_cnt = 0;
	char i;
	error = "";
	if (len < 10) {
		error = "Frame too short, <10 bits";
		return false;
	}
	
	if (checkCRC) {
		if (!Tools.check_crc_ccitt(bp,len)) { // CRC failed
			error = "CRC failed";
			if (crcIsFatal) {return false;}
		}
	}
	
	len -=2;
	if ((bp[1] & 1) != 0) {
		// Compressed Header
		
		
		frame.sourceCall = "?";
		frame.sourceSSID = 0;
		frame.isCompressed = true;
		frame.hid = (char)(bp[0] >>> 2);
		frame.qso = (char)((bp[0] << 6) | (bp[1] >>>2));
		frame.command = (bp[1] & 2) != 0 ? (char) 1 : 0;
		
		frame.destinationCall = "";
		frame.destinationCall += Character.toString((char)(((bp[2] >>> 2) & 0x3f)+0x20));
		frame.destinationCall += Character.toString((char)((((bp[2] << 4) | ((bp[3] >>> 4) & 0xf)) &0x3f)+0x20));
		frame.destinationCall += Character.toString((char)((((bp[3] << 2) | ((bp[4] >> 6) & 3)) & 0x3f)+0x20));
		frame.destinationCall += Character.toString((char)((bp[4] & 0x3f)+0x20));
		frame.destinationCall += Character.toString((char)(((bp[5] >> 2) & 0x3f)+0x20));
		frame.destinationCall += Character.toString((char)((((bp[5] << 4) | ((bp[6] >> 4) & 0xf)) & 0x3f)+0x20));
        
		frame.destinationSSID = (bp[6] & 0xf);
		
        bp_cnt += 7;
        len -= 7;
	} else {
		// Normal Header
		if (len < 15) { 
			error += ".Frame header too short";
			return false;
		}
		if ((bp[6] & 0x80) != (bp[13] & 0x80)) {
            frame.command = (char) (bp[6] & 0x80);
        }
		String source = "";
		String destination = "";
		for(i = 7; i < 13; i++) {
			if ((bp[i] &0xfe) != 0x40) { 
				source += Character.toString((char)(bp[i] >>> 1));
			}
		}
		frame.sourceCall = source;
		frame.sourceSSID = ((bp[13] >>> 1) & 0xf);
        for(i = 0; i < 6; i++) {
            if ((bp[i] &0xfe) != 0x40) {
            	destination += Character.toString((char)(bp[i] >>> 1));
            }
        }
        frame.destinationCall = destination;
        frame.destinationSSID = ((bp[6] >>> 1) & 0xf);
        bp_cnt += 14;
        len -= 14;
        while ((!((bp[bp_cnt-1] & 1) != 0)) && (len >= 7)) {
        	String station = "";
        	for(i = 0; i < 6; i++) {
            	if ((bp[i] &0xfe) != 0x40) {
            		station += Character.toString((char)(bp[i] >>> 1));
            	}
            }
        	if (frame.repeaterCount < 8) {
        		frame.repeaterCall[frame.repeaterCount] = station;
        		frame.repeaterSSID[frame.repeaterCount] = ((bp[6] >>> 1) & 0xf);
        		frame.repeaterCount++;
        	}
        	bp_cnt += 7;
            len -= 7;
        }
	}
	if(len == 0) {
		error += ".Partial header received";
		return true;
	}
	
	// Decode control byte
	frame.setControl((i = bp[bp_cnt++]));
	len--;
	//frame.pf = (((i>>>4) & 1) != 0);
	//if ((i & 1) == 0) {
		// Info Frame
		//frame.isIFrame = true;
		//frame.receiverSequence = (byte) ((i>>>5) & 7);
		//frame.senderSequence = (byte) ((i>>>1) & 7);
	//} else if ((i & 2) != 0) {
		// Unnumbered Frame
		//frame.isUFrame = true;
		//frame.uCommand = (byte) (((i>>>3) & 0x1c) | ((i>>>2) & 0x3));
		//switch (frame.uCommand) {
		//case 0x7: //SAMB
		//	frame.controlFieldType = "Set Asynchronous Balanced Mode";
		//	break;
		//case 0x8: //DISC
		//	frame.controlFieldType = "Disconnect";
		//	break;
		//case 0x3: //DM
		//	frame.controlFieldType = "Disconnected Mode";
		//	break;
		//case 0xc: //UA
		//	frame.controlFieldType = "Unnumbered Acknowledge";
		//	break;
		//case 0x11: //FRMR
		//	frame.controlFieldType = "Frame Reject";
		//	break;
		//case 0x0: //UI
		//	frame.controlFieldType = "Unnumbered Information";
		//	break;
		//default:
		//	frame.controlFieldType = "Unknown U Control Type[" + Integer.toString(frame.uCommand)+ "]";
		//	break;
		//}
	//} else {
		// Supervisory
		//frame.isSFrame = true;
		//frame.sCommand = (byte) ((i >>> 2) & 0x3);
		//frame.receiverSequence = (byte) ((i >>> 5) & 7);
		//switch (frame.sCommand) {
		//case 0x0: //RR
		//	frame.controlFieldType = "Receive Ready";
		//	break;
		//case 0x1: //RNR
		//	frame.controlFieldType = "Receive Not Ready";
		//	break;
		//case 0x2: //REJ
		//	frame.controlFieldType = "Reject";
		//	break;
		//default: //undefined
		//	frame.controlFieldType = "Unknown S Control Type[" + Integer.toString(frame.sCommand)+ "]";
		//	break;
		//}
		
	//}
	
	//if (len == 0) {
	//	return true;
	//}
	
	
	//frame.pid = (bp[bp_cnt++]);
	//len--;
	
	//System.arraycopy(bp, bp_cnt, frame.info, 0, len);
	
	
	return true;
}
*/
