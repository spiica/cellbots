package com.cellbots.cellserv.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */

public interface WiimoteServiceAsync 
{

  public void handleButtonDown(int buttonid, AsyncCallback<Integer> callback);
  public void handleButtonUp(int buttonid, AsyncCallback<Integer> callback);
}
