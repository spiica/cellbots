# How to use the Cellbots Java app #

This page explains the various workflows in the Cellbots Java app. We will see how a Cellbot can be controlled directly and from a remote via both HTTP and GTalk.

# Direct Control #

If you have only one phone, you may directly control your robot from the phone. Perform the following steps to create a direct control Cellbot:

  * On the "New Cellbot" screen, press "Add new Cellbot."
  * Enter "Cellbot's Name"
  * Set the "Cellbot Type" by selecting the appropriate controller.
  * Set "Cellbot Bluetooth" by selection the Bluetooth device name to use to talk to the robot.
  * Press "Done."
  * On the following screen, press "Connect" to connect to the Cellbot.

Pressing "Scan QR code" opens the Barcode Scanner app, which can be used to scan the values off of a QR code pasted on the robot.

# Share a Cellbot using GTalk #

  * Long press a Cellbot in the list of Cellbots.
  * Press "Share" in the pop-up menu.
  * Set communication method as "Google Talk."
  * Set "Cellbot's GMail" as the GMail username of the account of the Cellbot's owner. For example, mylegobot@gmail.com.
  * Set the GMail account's password.
  * Press "Done".

# Share a Cellbot using HTTP #

  * Long press a Cellbot in the list of Cellbots.
  * Press "Share" in the pop-up menu.
  * Set communication method as "Custom HTTP."
  * Set "Cellbot's URL" as the URL of the HTTP relay server.
  * Check "Use local server" to run HTTP server on the phone. This option works only on the local WiFi network.

# Control a remote shared Cellbot using GTalk #

  * On the "New Cellbot" screen, press "Communicate to Shared Cellbot."
  * Set "Web Cellbot's Name" as the name of the remote control for the shared Cellbot.
  * Set "Communication Method" as "Google Talk."
  * Set "Web Cellbot's GMail" as the GMail username of the remote Cellbot which you with to connect to. For example, mylegobot@gmail.com.
  * Alternatively, you may import the settings by scanning a QR code displayed by pressing the "Show QR Code" button on the "Local Cellbot" screen.
  * Go to Menu - > Settings on the Cellbots list screen, and make sure that "Remote's GMail" and "Remote's Password" are set. For example, johnsmith@gmail.com. To be able to control the Cellbot, johnsmith@gmail.com and mylegobot@gmail.com should be GTalk buddies.
  * Press "Done."

# Control a remote shared Cellbot using Custom HTTP #

  * On the "New Cellbot" screen, press "Communicate to Shared Cellbot."
  * Set "Web Cellbot's Name" as the name of the remote control for the shared Cellbot.
  * Set "Communication Method" as "Custom HTTP."
  * Set "Web Cellbot's URL" as the URL of the relay server set by the remote Cellbot.
  * Alternatively, you may import the settings by scanning a QR code displayed by pressing the "Show QR Code" button on the "Local Cellbot" screen.
  * Press "Done."


# Control a remote shared Cellbot using the Web interface #

To use the remote Web interface, the communication method of the shared Cellbot should be "Custom HTTP" with "Use local server" checked.

  * Connect the shared Cellbot.
  * Make sure the on-board phone is connected on Wi Fi.
  * Make sure the remote computer is on the same Wi Fi network as the on-board phone.
  * Visit the page http://cellbot-ip:8080/index.html form a browser on our computer, where cellbot-ip is the IP address of the shared Cellbot displayed on the Cellbot screen on the phone.