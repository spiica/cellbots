package com.cellbots.cellserv.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.RespectBinding;

import org.apache.commons.httpclient.HttpStatus;

import com.cellbots.CellbotProtos;
import com.cellbots.CellbotProtos.ControllerState;

public class VideoServlet extends HttpServlet
{

  private static final long serialVersionUID = -1542374763947377440L;

  private String            resultsDir;

  private static int        slotNumber       = 0;

  private static long       lastPhoneTimestamp;

  private static long       lastControllerTimestamp;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    // Store the directory that will hold the survey-results files
    resultsDir = getInitParameter("resultsDir");

    // If no directory was provided, can't handle clients
    if (resultsDir == null)
    {
      resultsDir = System.getProperty("java.io.tmpdir");
      // throw new UnavailableException(this,
      // "Not given a directory to write survey results!");
    }

  }

  public String getServletInfo()
  {
    return "Servlet for handeling communication with phone";
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {

    if (StateHolder.newVideoFrameAvilble(slotNumber, lastPhoneTimestamp))
    {
      res.getOutputStream().write(StateHolder.getVideoFrame(slotNumber));
    }

    else
    {
      res.setStatus(HttpStatus.SC_NOT_MODIFIED);
    }

  }

  /**
   * Write survey results to output file in response to the POSTed form. Write a
   * "thank you" to the client.
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {
    // first, set the "content type" header of the response
    //res.setContentType("text/html");

    try
    {
        CellbotProtos.PhoneState state = CellbotProtos.PhoneState.parseFrom(req.getInputStream());
        StateHolder.setPhoneState(slotNumber, state);
       
      if (StateHolder.newControllerStateAvailble(slotNumber, lastControllerTimestamp))
      {
        System.out.println("writing new controller msg");
        ControllerState cs = StateHolder.getControllerState(slotNumber);
        res.getOutputStream().write(cs.toByteArray());
        lastControllerTimestamp = cs.getTimestamp();

        
      }

    }
    catch (IOException e)
    {
      e.printStackTrace();
      // toClient.println("A problem occured: could not write file: "+path +
      // "Please try again.");
    }

  }

}
