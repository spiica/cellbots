# Introduction #

This page discusses methods for sending video,  and viewing that video on a client.  There are two primary ways to send video,  send H.263 encoded video over RTP,  or send a series of JPEGS over UDP.

The main criteria I am using to evaluate solutions are low lag,  and little or no software to install on the client.


Their can be different clients, but I think the ideal one would be a web page with HTML 5 video, that shows robot data and accepts command input.  This page could be hosted on the phone or possibly on the Google app engine.


# Details #


Currently three methods have been successfully tried,

  1. Using Qik or uStream: lots of lag, requires internet access
  1. Streaming H.263 to VLC: less lag,internet not required, bandwidth is low enough for 3g
  1. Streaming JPEGS to a custom processing app  - least lag, needs lots of bandwidth,wifi only.


The feasibility of streaming H.263 through a webpage is being examined.  If this proves to be unworkable,  I can convert the existing processing client to an applet.