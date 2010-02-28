# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
# What is does:
# 1. Opens a socket for incoming telnet commands to be pushed out via serial.
# 2. Parses the first character of the input tring to take an action.
#
# See README.txt for a full description of each command option.

__author__ = 'Ryan Hickman <rhickman@gmail.com>'
__license__ = 'Apache License, Version 2.0'

import os
import time
import socket
import select
import sys
import android
import math

audioOn=True
audioRecordingOn=False
cardinalMargin=10
cardinals={}
cardinals['n']=('North','0')
cardinals['e']=('East','90')
cardinals['w']=('West','270')
cardinals['s']=('South','180')

droid = android.Android()
svr_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# The main loop that fires up a telnet socket and processes inputs
def main():
  rs = []
  telnet_port = 9002

  droid.makeToast("Firing up telnet socket...")
  svr_sock.bind(('', telnet_port))
  svr_sock.listen(3)
  svr_sock.setblocking(0)

  print "Ready to accept telnet. Use this device's IP on port %s\n" % telnet_port
  print "Send the letter 'q' to quit the program.\n"
  droid.makeToast("Ready!")
  droid.startSensing()
  droid.startLocating()

  while 1:
    r,w,_ = select.select([svr_sock] + rs, [], [])

    for cli in r:
      if cli == svr_sock:
        new_cli,addr = svr_sock.accept()
        rs = [new_cli]
      else:   
        input = cli.recv(1024)
        input = input.replace('\r','')
        input = input.replace('\n','')
        print "received: %s" % input
        commandParse(input)


# Speak using TTS or make toasts
def speak(msg):
  if audioOn:
    droid.speak(msg)
  else:
    droid.makeToast(msg)

# Point towards a specific compass heading
def orientToAzimuth(azimuth):
  onTarget = False
  stopTime = time.time() + 5000
  while not onTarget or time.time() > stopTime:
    results = droid.readSensors()

    if results['result'] is not None:
      currentHeading = results['result']['azimuth']
      msg = "Azimuth: %d Heading: %d" % (azimuth,currentHeading)
      delta = azimuth - currentHeading
      if math.fabs(delta) > 180:
        if delta < 0:
          adjustment = math.fabs(delta) + 360
        else:
          adjustment = math.fabs(delta) - 360
      else:
        adjustment = delta
      adjustmentAbs = math.fabs(adjustment)
      if adjustmentAbs < cardinalMargin:
        msg = "Goal achieved! Facing %d degrees, which is within the %d degree margin of %d!" % (currentHeading, cardinalMargin, azimuth)
        speak(msg)
        commandOut('s')
        onTarget = True
      else:
        if adjustment > cardinalMargin:
          print "Moving %d right." % adjustmentAbs
          commandOut('r')
        if adjustment < (cardinalMargin * -1):
          print "Moving %d left." % adjustmentAbs
          commandOut('l')
        time.sleep(adjustmentAbs/90)
        commandOut('s')
        time.sleep(1)
    else:
      msg = "Could not start sensors."


# Send command out of the device (currently serial but other protocals could be added)
def commandOut(msg):
  os.system("echo '%s\n' > /dev/ttyMSM2" % msg)

# Parse the first character of incoming commands to determine what action to take
def commandParse(input):
  command = input[:1]
  commandValue = input[2:]
  if command == 'a':
    global audioRecordingOn
    audioRecordingOn = not audioRecordingOn
    if audioRecordingOn:
      speak("Starting audio recording")
      droid.startAudioRecording("/sdcard/cellbot.3gp")
    else:
      droid.stopAudioRecording()
      speak("Stopping audio recording")
      print "Audio file located on /sdcard/cellbot.3gp"
  elif command == 'b':
    speak("Moving backward")
    commandOut('b')
  elif command == 'c':
    orientToAzimuth(int(commandValue[:3]))
  elif command == 'd':
    speak(time.strftime("Current time is %_I %M %p on %A, %B %_e, %Y "))
  elif command == 'f':
    speak("Moving forward")
    commandOut('f')
  elif command == 'h':
    speak("Hello. I am ready to play!")
  elif command == 'l':
    speak("Moving left")
    commandOut('l')
  elif command == 'm':
    global audioOn
    audioOn = not audioOn
    speak("Audio mute toggled")
  elif command == 'p':
    msg = "Orienting %s" % cardinals[commandValue[:1]][0]
    speak(msg)
    orientToAzimuth(int(cardinals[commandValue[:1]][1]))
  elif command == 'q':
    speak("Bye bye!")
    svr_sock.close()
    droid.stopSensing()
    droid.stopLocating()
    sys.exit("Exiting program after receiving 'q' command.")
  elif command == 'r':
    speak("Moving right")
    commandOut('r')
  elif command == 's':
    commandOut('s')
  elif command == 't':
    speak(commandValue)
  elif command == 'x':
    location = droid.readLocation()['result']
    addresses = droid.geocode(location['latitude'], location['longitude'])
    firstAddr = addresses['result']['result'][0]
    if firstAddr['postal_code'] is None:
      speak("Failed to find location.")
    else:
      msg = 'You are in %(locality)s, %(admin_area)s' % firstAddr
      speak(msg)
  else:
    droid.makeToast("Unknown command")


if __name__ == '__main__':
    main()