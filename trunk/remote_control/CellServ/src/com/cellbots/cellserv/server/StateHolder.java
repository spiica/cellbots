package com.cellbots.cellserv.server;

import java.util.HashMap;

import com.cellbots.CellbotProtos;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class StateHolder
{

  private CellbotProtos.PhoneState              phoneState;

  private CellbotProtos.ControllerState         controllerState;

  private CellbotProtos.AudioVideoFrame         avFrame;

  private CellbotProtos.ControllerState.Builder csBuilder = CellbotProtos.ControllerState.newBuilder();
  
  private static HashMap<String, StateHolder> instances = new HashMap<String, StateHolder>();

  // private MemcacheService phoneStates =
  // MemcacheServiceFactory.getMemcacheService();

  private StateHolder()
  {
  }

  public static StateHolder getInstance(String botID)
  {

    if (! instances.containsKey(botID))
    {
      instances.put(botID, new StateHolder());
    }
    return instances.get(botID);
  }

  public void setPhoneState(CellbotProtos.PhoneState ps)
  {
    phoneState = ps;
  }

  public void setVideoFrame(CellbotProtos.AudioVideoFrame av)
  {
    avFrame = av;
  }

  public CellbotProtos.PhoneState getPhoneState()
  {
    return phoneState;
  }

  public CellbotProtos.ControllerState getControllerState()
  {
    CellbotProtos.ControllerState cs = csBuilder.build();
    csBuilder = null;
    return cs;
  }

  public boolean newVideoFrameAvilble()
  {
    return avFrame != null;// && instance.avFrame.getTimestamp() !=
  }

  public boolean newPhoneStateAvilble()
  {
    return phoneState != null;// && instance.phoneState.getTimestamp()
  }

  public byte[] getVideoFrame()
  {
    if (avFrame != null && avFrame.hasData())
      return avFrame.getData().toByteArray();
    else
      return null;
  }

  public boolean newControllerStateAvailble()
  {
    return csBuilder != null && csBuilder.getKeyEventCount() > 0;
  }

  public void addKeyEvent(com.cellbots.CellbotProtos.ControllerState.KeyEvent.Builder key)
  {
    if (csBuilder == null)
      csBuilder = CellbotProtos.ControllerState.newBuilder();

    csBuilder.setTimestamp(System.currentTimeMillis());
    csBuilder.addKeyEvent(key);
  }
}
