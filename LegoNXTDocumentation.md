# Lego and cellbots #

The Lego directory in the source code provides example code for receiving and interpreting cellbots commands as well as commands to run demo behaviors. These can be used with any program that wraps text commands into messages using Lego's Bluetooth message system and mailbox 1. They can also be used as a starting point for writing other behaviors or commands for communicating with a Lego NXT robot.

# Lego NXT Setup #

The demo code is designed to work with the shooter bot base for movement. For the demo behaviors add an ultrasonic sensor aimed forward using sensor port 4 and an active light sensor on the front pointing down using sensor port 3.

# Compiling the code #

The source for the sample program can be found in lego/nxc/source from the root of your cellbots svn directory. You will need the NXC compiler to build the sample project. Instructions for downloading and installing the compiler can be found on the [NXC site](http://bricxcc.sourceforge.net/nbc/).

A Makefile for using make on Mac and Linux is included in lego/nxc, though before using it you must set a path to the nbc compiler by setting the variable NXC\_PATH to the path to the nbc and nxtcom runnables. This can be done with the command "export NXC\_PATH=<path to nbc>" or by adding the line "NXC\_PATH=<path to nbc>" to the Makefile near the top. For Windows machines follow the NXC instructions for building a Lego program.

Running "make all" from the lego/nxc directory after you've set up the NXC\_PATH will create a build directory, compile the demo program into that directory, and send it to an NXT connected with a USB cable. It will download to the first NXT found.

# Sending Commands #

To send messages to the software on the Lego NXT you need to pair and connect a bluetooth device with your NXT, then wrap the message in a mailbox message. The demo software uses mailbox '1' which is index 0. Essentially, you use the following format:

  * Byte 0: 0x80 (direct command with no response from the NXT)
  * Byte 1: 0x09 (message command type)
  * Byte 2: 0x01 (Mailbox number, 0-9)
  * Byte 3: Message Size (including null terminating character)
  * Byte 4-N: Message data, where N = Message size + 3 and ends with a null terminating character

See Lego's Bluetooth Developer Kit for more detailed information on sending commands to the NXT.

The following commands are supported by the demo code using message wrapping:

| command {required argument} (optional argument) | argument description | argument description | notes |
|:------------------------------------------------|:---------------------|:---------------------|:------|
| f (speed)                                       | speed: '0' to '9'    |                      | drive forward |
| b (speed)                                       | speed: '0' to '9'    |                      | drive backward |
| l (speed)                                       | speed: '0' to '9'    |                      | turn left |
| r (speed)                                       |speed: '0' to '9'     |                      | turn right |
| s                                               |                      |                      | stop the wheels|
| '0' through '9'                                 | a single number, '0'-'9' |                      | sets the default speed for f/b/l/r |
| w {velocity} {velocity}                         | velocity: "-100" to "100" |                      | set wheel velocities |
| hl (speed)                                      | speed: '0' to '9'    |                      | turn the third motor left (arbitrary) |
| hr (speed)                                      | speed: '0' to '9'    |                      | turn the third motor right |
| df {distance} (speed)                           | distance: in degrees. | speed: '0' to '9'    | drive forward a set distance |
| db {distance} (speed)                           | distance: in degrees. | speed: '0' to '9'    | drive backward a set distance |
| dl {distance} (speed)                           | distance: in degrees. | speed: '0' to '9'    | turn left a set distance |
| dr {distance} (speed)                           | distance: in degrees. | speed: '0' to '9'    | turn right a set distance |
| hdl {distance} (speed)                          | distance: in degrees. | speed: '0' to '9'    | turn head a set distance left |
| hdr {distance} (speed)                          | distance: in degrees. | speed: '0' to '9'    | turn head a set distance right |
| demoLineFollow                                  |                      |                      | Follows a line |
| demoFindWall (distance)                         | distance: in cm, distance from wall to stop |                      | Drives until within (distance) or 20 cm of a wall |
| demoStop                                        |                      |                      | Stops any in progress demo behaviors |