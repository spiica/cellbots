package com.cellbots.cellserv.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class WiimoteEntry implements EntryPoint
{
  
  static final String JSON_URL = "/robotState";

  
  final static Label messageLabel = new Label("Status:ok");;
  public void onModuleLoad()
  {

    final WiimoteServiceAsync wiiService = GWT.create(WiimoteService.class);
    final VerticalPanel mainPanel = new VerticalPanel();
    final VerticalPanel controlPanel = new VerticalPanel();
    final HorizontalPanel horizontalPanel = new HorizontalPanel();
    final HorizontalPanel hudPanel = new HorizontalPanel();
    final Timer elapsedTimer;
    final Timer sensorTimer;
    
    final Button fwdButton = new Button("FWD");
    fwdButton.addClickHandler(new AndroidClickHandler(wiiService,AndroidKeyCode.KEYCODE_DPAD_UP));
    final Button bkwdButton = new Button("BKWD");
    bkwdButton.addClickHandler(new AndroidClickHandler(wiiService,AndroidKeyCode.KEYCODE_DPAD_DOWN));
    final Button leftButton = new Button("LEFT");
    leftButton.addClickHandler(new AndroidClickHandler(wiiService,AndroidKeyCode.KEYCODE_DPAD_LEFT));
    final Button rightButton = new Button("RIGHT");
    rightButton.addClickHandler(new AndroidClickHandler(wiiService,AndroidKeyCode.KEYCODE_DPAD_RIGHT));
    final Button stopButton = new Button("STOP");
    stopButton.addClickHandler(new AndroidClickHandler(wiiService,AndroidKeyCode.KEYCODE_DPAD_CENTER));
    
    final Image videoImage = new Image("/video");
    

    videoImage.addErrorHandler(new ErrorHandler()
    {
      public void onError(ErrorEvent event)
      {
        //messageLabel.setText("Video not ready");
        
      }
    });

    videoImage.setUrl("video");

    fwdButton.setWidth("100%");
    bkwdButton.setWidth("100%");

    horizontalPanel.add(leftButton);
    horizontalPanel.add(stopButton);
    horizontalPanel.add(rightButton);

    controlPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    controlPanel.add(fwdButton);
    controlPanel.add(horizontalPanel);
    controlPanel.add(bkwdButton);

    hudPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    hudPanel.add(controlPanel);
    hudPanel.add(messageLabel);

    mainPanel.setWidth("600px");
    mainPanel.setBorderWidth(2);
    mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    mainPanel.add(videoImage);

    mainPanel.add(hudPanel);

    RootPanel.get().add(mainPanel);

    
    // Create a new timer
    elapsedTimer = new Timer () {
      public void run() {
        videoImage.setUrl("video?"+System.currentTimeMillis());
      }
    };
    
    // Create a new timer
    sensorTimer = new Timer () {
      public void run() {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, JSON_URL);
        try 
        {
            builder.setHeader("Content-Type", "application/json");
            builder.sendRequest(null, new RequestCallback() {
                public void onError(Request request, Throwable exception) 
                {
                    displayError("Couldn't retrieve JSON");
                }
                public void onResponseReceived(Request request, Response response) 
                {
                    if (response.getStatusCode() == 200)
                      showPhoneState(PhoneState.parse(response.getText()));
                    else
                        displayError("Couldn't retrieve JSON");
                }
            });
        }
        catch (RequestException e) 
        {
            displayError("Couldn't retrieve JSON");
        }
      }
    };
    
    // Schedule the timer for every 1/2 second (500 milliseconds)
    elapsedTimer.scheduleRepeating(50);
    sensorTimer.scheduleRepeating(1000);

  }

  
  void showPhoneState(PhoneState state)
  {
      //vp.add(new Label("Greet #" + greet.getId() + ": " + greet.getMessage() + " | " + greet.getStatus().getNumber()));
      if(state.hasOrientation())
      {
        messageLabel.setText("Aimuth="+state.getOrientation().getAzimuth());
      }
  }
  
  static void displayError(String error)
  {
    messageLabel.setText(error);
  }

}
