package com.cellbots.cellserv.server;

import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.ControllerState;
import com.cellbots.CellbotProtos.ControllerState.Builder;

public class StateHolder
{

  private static int                           MAX_SLOTS         = 10;

  private static CellbotProtos.PhoneState      phoneState[]      = new CellbotProtos.PhoneState[MAX_SLOTS];

  private static CellbotProtos.ControllerState controllerState[] = new CellbotProtos.ControllerState[MAX_SLOTS];
  
  private static Builder csBuilder = CellbotProtos.ControllerState.newBuilder();
  
  
  public static void setPhoneState(int slotNumber, CellbotProtos.PhoneState ps)
  {
    phoneState[slotNumber] = ps;
  }
  
  public static CellbotProtos.PhoneState getPhoneState(int slotNumber)
  {
    return phoneState[slotNumber];
  }

  public static CellbotProtos.ControllerState getControllerState(int slotNumber)
  {
    ControllerState cs = csBuilder.build();
    csBuilder=null;
    
    return cs;
  }

  public static boolean newVideoFrameAvilble(int slotNumber, long timestamp)
  {
    return phoneState[slotNumber] != null && phoneState[slotNumber].hasVideoFrame() && phoneState[slotNumber].getTimestamp() != timestamp;
  }

  public static byte[] getVideoFrame(int slotNumber)
  {
    if (phoneState[slotNumber] != null && phoneState[slotNumber].hasVideoFrame())
      return phoneState[slotNumber].getVideoFrame().toByteArray();
    else
      return null;
  }

  public static boolean newControllerStateAvailble(int slotNumber, long timestamp)
  {
    //controllerState[slotNumber] != null &&
    
    return csBuilder!=null && csBuilder.getKeyEventCount() > 0;
  }

  public static void addKeyEvent(com.cellbots.CellbotProtos.ControllerState.KeyEvent.Builder key)
  {
    
    if(csBuilder == null)
    csBuilder = CellbotProtos.ControllerState.newBuilder(); 
    
    csBuilder.setTimestamp(System.currentTimeMillis());
    csBuilder.addKeyEvent(key);
    
  }
  
}
