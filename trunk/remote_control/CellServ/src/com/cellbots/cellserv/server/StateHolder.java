package com.cellbots.cellserv.server;

import java.io.IOException;


import com.cellbots.CellbotProtos;
import com.gargoylesoftware.htmlunit.Cache;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.protobuf.ByteString;

public class StateHolder
{


  private  CellbotProtos.PhoneState      phoneState;

  private  CellbotProtos.ControllerState controllerState; 
  
  private  CellbotProtos.AudioVideoFrame avFrame; 
  
  private  CellbotProtos.ControllerState.Builder csBuilder = CellbotProtos.ControllerState.newBuilder();
  
  private static StateHolder instance;
  
  
  //private   MemcacheService phoneStates = MemcacheServiceFactory.getMemcacheService();
  
  private StateHolder() 
  {
  }
  
  
  public static StateHolder getInstance(String botID) 
  {
    
    if (instance == null)
    {
      instance = new StateHolder();
    }
    return instance;
  }
  
  public void setPhoneState(CellbotProtos.PhoneState ps)
  {
    instance.phoneState  = ps;
   // cache.put()

  }

  public void setVideoFrame( CellbotProtos.AudioVideoFrame av)
  {
    instance.avFrame = av;
   // cache.put()

  }
  
  public  CellbotProtos.PhoneState getPhoneState()
  {
    return instance.phoneState;
  }

  public  CellbotProtos.ControllerState getControllerState()
  {
    CellbotProtos.ControllerState cs = csBuilder.build();
    csBuilder=null;
    
    return cs;
  }

  public  boolean newVideoFrameAvilble( long timestamp)
  {
    return instance.avFrame != null && instance.avFrame.getTimestamp() != timestamp;
  }

  
  public  boolean newPhoneStateAvilble(long timestamp)
  {
    return instance.phoneState != null && instance.phoneState.getTimestamp() != timestamp;
  }

  public  byte[] getVideoFrame(int slotNumber)
  {
    if (instance.avFrame != null && instance.avFrame.hasData())
      return instance.avFrame.getData().toByteArray();
    else
      return null;
  }

  public  boolean newControllerStateAvailble( long timestamp)
  {
    //controllerState[slotNumber] != null &&
    
    return csBuilder!=null && csBuilder.getKeyEventCount() > 0;
  }

  public  void addKeyEvent(com.cellbots.CellbotProtos.ControllerState.KeyEvent.Builder key)
  {
    
    if(csBuilder == null)
    csBuilder = CellbotProtos.ControllerState.newBuilder(); 
    
    csBuilder.setTimestamp(System.currentTimeMillis());
    csBuilder.addKeyEvent(key);
    
  }
  
}
