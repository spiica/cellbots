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

  private static int        slotNumber       = 0;

  private static long       lastPhoneTimestamp;


  public String getServletInfo()
  {
    return "Servlet for handeling communication with phone";
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {

    if (StateHolder.getInstance("").newVideoFrameAvilble( lastPhoneTimestamp))
    {
      res.getOutputStream().write(StateHolder.getInstance("").getVideoFrame(slotNumber));
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
        CellbotProtos.AudioVideoFrame frame = CellbotProtos.AudioVideoFrame.parseFrom(req.getInputStream());
        StateHolder.getInstance("").setVideoFrame(frame);

    }
    catch (IOException e)
    {
      e.printStackTrace();
      // toClient.println("A problem occured: could not write file: "+path +
      // "Please try again.");
    }

  }

}
