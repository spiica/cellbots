# Introduction #

Release notes for latest TRRSTAN, Celljoust, and Cellserv:

The current version of celljoust runs both servos forward when the forward command is sent.   This is because we were using servos with one wired Clockwise and the other counter-clockwise.   To switch a servo from CW to CCW you must open the case and re-solder the wires on the motor in the opposite configuration.  I will try to get a software fix for this but I have found it generally works better to do the hardware fix because the cheep servos have a non-linar response curve.   +5  speed is not the same deg/sec as -5 speed.

If you use the wii controller the keys are DPad = move while held down,  A = Stop, +,- =adjust speed.

When you first assemble a new TRRSTAN and have the software installed,  you must trim the servos.   Plug in in the robot and turn it on.  Then set your phone volume to max.   Start celljoust, press menu, and tap "configure servos" This will take you to a screen where you can adjust the center point of the servos until they stay still or almost still.  Press back when you are done and your settings will be saved.   You may have to force-quit and restart celljoust since I think there are still some threading problems.

It is best to run cellserv using tomcat on a local machine if you want low lag. You can try the GAE server at celljoust.appspot.com, but It runs very poorly there.

To look at the video on localhost,  first intall tomcat and the cellserv.war, then use the url  localhost:8080/CellServ?BOTID=mybotid in Chrome or FF.    Also note that the URL is usually case sensitive(except on winxp), so check what case tomcat used when you installed the war. Configure the bot id  and your server url by pressing menu in the android app.      The BOTID is so that you can use multiple bots on the same server, and so nobody can see your video stream unless they have your BOTID.

We found a dry solder joint on one of the assembled kits we were testing.  If you have one channel where the servo simply will not move,  first make sure that the headphone jack is plugged all the way in to the phone.  If it does not go in all the way only the left channel will work.   If that does not help,  closely examine the solder joints on the large beige chip.  On a dry joint you will see sharp edges, a good joint has the sharp edges covered in solder.


# Details #

Add your content here.  Format your content with:
  * Text in **bold** or _italic_
  * Headings, paragraphs, and lists
  * Automatic links to other wiki pages