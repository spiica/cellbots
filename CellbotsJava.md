**Cellbots** is a Java application for the Android. You will need the [Android SDK](http://developer.android.com/sdk/index.html) in order to make modifications to the application.

## Overview ##

The cellbots application consists of the following layers
  * The Logic layer that sets the mode of operation for the phone and gathers the necessary information.
  * The Communication layer that implements the  communication between two Andriod phones. This layer supports channels such as custom HTTP relay, App Engine HTTP relay or XMPP
  * The Translation layer that translates the commands received from the user to platform specific commands. The platforms supported are iRobot Create, LEGO Mindstorms, VEX Pro,  Arduino.

### Extending the application ###

  * [Adding a new Controller service to the application](AddController.md)
  * [Adding a new View to the application](AddView.md)
  * [Command Control API’s / Protocol for each of the robot platforms](CommandSet.md)
  * The personas App Engine system