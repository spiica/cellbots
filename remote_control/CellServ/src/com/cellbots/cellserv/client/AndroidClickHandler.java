package com.cellbots.cellserv.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class AndroidClickHandler implements ClickHandler
{

  WiimoteServiceAsync wiiService;
  private int         keyCode;

  public AndroidClickHandler(WiimoteServiceAsync service, int code)
  {
    wiiService = service;
    keyCode = code;
  }

  public void onClick(ClickEvent event)
  {
    wiiService.handleButtonDown(keyCode, new AsyncCallback<Integer>()
    {
      public void onFailure(Throwable caught)
      {
        GWT.log(caught.getMessage());
      }

      public void onSuccess(Integer result)
      {

      }
    });

  }

}
