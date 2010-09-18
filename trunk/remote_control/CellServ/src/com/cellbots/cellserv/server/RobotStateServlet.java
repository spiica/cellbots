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
import com.cellbots.CellbotProtos.PhoneState;
import com.cellbots.CellbotProtos.PhoneState.Builder;
import com.cellbots.SchemaCellbotProtos;
import com.cellbots.SchemaCellbotProtos.PhoneState.BuilderSchema;
import com.cellbots.SchemaCellbotProtos.PhoneState.MessageSchema;
import com.dyuproject.protostuff.JsonIOUtil;
import com.dyuproject.protostuff.Output;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

public class RobotStateServlet extends HttpServlet
{

  /**
   * 
   */
  private static final long serialVersionUID = 6703781028562576421L;

  private static long       lastPhoneTimestamp;

  private static long       lastControllerTimestamp;

  public String getServletInfo()
  {
    return "Servlet for handeling communication with phone";
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {

    // final SchemaCellbotP json = new SchemaCellbotProtos();

    //res.setContentType("application/json");

    /*
     * PhoneState.Builder ps = PhoneState.newBuilder();
     * json.mergeFrom(request.getInputStream(), greet);
     * 
     * String name = greet.getName(); if(name==null || name.length()==0)
     * greet.setName("Anonymous");
     * 
     * greet.setId(greetCount.incrementAndGet())
     * .setStatus(Greet.Status.ACKNOWLEDGED) .setMessage("Hello " +
     * greet.getName() + " from server @ " + new Date());
     */

    // SchemaCellbotProtos.PhoneState.WRITE.writeTo(output,
    // StateHolder.getInstance("").getPhoneState());

    // Schema<CellbotProtos.PhoneState> schema =

    Schema<com.cellbots.CellbotProtos.PhoneState> schema = new SchemaCellbotProtos.PhoneState.MessageSchema();

    PhoneState ps = StateHolder.getInstance("").getPhoneState();

    byte[] bytes;
    if (ps != null)
    {
      bytes =   JsonIOUtil.toByteArray(ps, schema, true);
      res.getOutputStream().write(bytes);
    }

  }

  /**
   * Write survey results to output file in response to the POSTed form. Write a
   * "thank you" to the client.
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
  {
    // first, set the "content type" header of the response
    // res.setContentType("text/html");

    try
    {
      CellbotProtos.PhoneState state = CellbotProtos.PhoneState.parseFrom(req.getInputStream());
      StateHolder.getInstance("").setPhoneState(state);

      if (StateHolder.getInstance("").newControllerStateAvailble(lastControllerTimestamp))
      {
        System.out.println("writing new controller msg");
        ControllerState cs = StateHolder.getInstance("").getControllerState();
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
