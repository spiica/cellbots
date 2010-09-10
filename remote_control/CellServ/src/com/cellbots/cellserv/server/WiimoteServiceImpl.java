package com.cellbots.cellserv.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.ControllerState.Builder;
import com.cellbots.CellbotProtos.ControllerState.KeyEvent;
import com.cellbots.cellserv.client.WiimoteService;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class WiimoteServiceImpl extends RemoteServiceServlet implements WiimoteService
{

  /**
   * 
   */
  private static final long serialVersionUID = -1101119412732207496L;

  public int handleButtonDown(int buttonid)
  {
    // TODO Auto-generated method stub
    System.out.println("Got Button Down : " + buttonid);

    KeyEvent.Builder key = KeyEvent.newBuilder();

    key.setKeyDown(true);
    key.setKeyCode("" + buttonid);

    StateHolder.addKeyEvent(key);
    
    

    // GWT.log("test");Å
    return 1;
  }

  public int handleButtonUp(int buttonid)
  {
    // TODO Auto-generated method stub
    System.out.println("Got Button Up : " + buttonid);

    KeyEvent.Builder key = KeyEvent.newBuilder();

    key.setKeyUp(true);
    key.setKeyCode("" + buttonid);

    StateHolder.addKeyEvent(key);

    // GWT.log("test");
    return 1;
  }

}
