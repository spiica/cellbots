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
# See http://www.cellbots.com for more information

__license__ = 'Apache License, Version 2.0'

import android
import os
import time
import xmpp
import ConfigParser
from threading import Thread


def commandByXMPP():
  global xmppUsername
  global xmppPassword
  global xmppClient
  if not xmppUsername:
    xmppUsername = droid.getInput('Username')['result']
  if not xmppPassword:
    xmppPassword = droid.getInput('Password')['result']
  jid = xmpp.protocol.JID(xmppUsername)
  xmppClient = xmpp.Client(jid.getDomain(), debug=[])
  xmppClient.connect(server=(xmppServer, xmppPort))
  try:
    xmppClient.RegisterHandler('message', XMPP_message_cb)
  except:
    exitCellbot('XMPP error. You sure the phone has an internet connection?')
  if not xmppClient:
    exitCellbot('XMPP Connection failed!')
    return
  auth = xmppClient.auth(jid.getNode(), xmppPassword, 'botty')
  if not auth:
    exitCellbot('XMPP Authentication failed!')
    return
  xmppClient.sendInitPresence()
  print "XMPP username for the robot is:\n" + xmppUsername
  runRemoteControl()


# Handle XMPP messages coming from commandByXMPP
def XMPP_message_cb(session, message):
  jid = xmpp.protocol.JID(message.getFrom())
  global operator
  operator = jid.getNode() + '@' + jid.getDomain()
  command = message.getBody()
  print str(command)
      
# Listen for incoming Bluetooth resonses. If this thread stops working, try rebooting. 
class bluetoothReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
 
  def run(self):
    while True:
      if not droid.bluetoothReady():
        time.sleep(0.05)
        continue
        result += droid.bluetoothRead()
        if '\n' in result:
          npos = result.find('\n')
          yield result[:npos]
          result = result[npos+1:]
          print result

# Initialize Bluetooth outbound if configured for it
def initializeBluetooth():
  droid.toggleBluetoothState(True)
  droid.bluetoothConnect("00001101-0000-1000-8000-00805F9B34FB") #this is a magic UUID for serial BT devices
  droid.makeToast("Initializing Bluetooth connection")
  time.sleep(3)

# Send command out of the device over XMPP
def commandOut(msg):
  if outputMethod == "outputBluetooth":
    droid.bluetoothWrite(msg + '\n')
  else:
    global xmppRobotUsername
    global previousMsg
    global lastMsgTime
    if not xmppRobotUsername:
      xmppPassword = droid.getInput('Robot user')['result']
    # Don't send the same message repeatedly unless 1 second has passed
    if msg != previousMsg or (time.time() > lastMsgTime + 1000):
      xmppClient.send(xmpp.Message(xmppRobotUsername, msg))
    previousMsg=msg
    lastMsgTime = time.time()
  

def runRemoteControl():
  while 1:
    sensor_result = droid.readSensors()
    pitch=int(sensor_result.result['pitch'])
    roll=int(sensor_result.result['roll'])

    # Assumes the phone is held in portrait orientation

    # People naturally hold the phone slightly pitched forward.
    # Translate and scale a bit to keep the values mostly in -100:100
    pitch = pitch + 60
    if pitch > 0:
      speed = int(2 * pitch)
    else:
      speed = int(25.0 + pitch)

    # Gutter near upright
    if pitch > -25 and pitch < 5:
      speed = 0

    # Some empirical values, and also a gutter (dead spot) in the middle.
    direction = int(-roll * 4 / 2.4)
    if direction < 20 and direction > -20:
      direction = 0

    # clamp
    if speed > 100:
      speed = 100
    elif speed < -100:
      speed = 100

    if direction  > 100:
      direction = 100
    elif direction < -100:
      direction = -100

    # Okay, speed and direction are now both in the range of -100:100.
    # Speed=100 means to move forward at full speed.  direction=100 means
    # to turn right as much as possible.

    # Treat direction as the X axis, and speed as the Y axis.
    # If we're driving a differential-drive robot (each wheel moving forward
    # or back), then consider the left wheel as the X axis and the right
    # wheel as Y.
    # If we do that, then we can translate [speed,direction] into [left,right]
    # by rotating by -45 degrees.
    # See the writeup at [INSERT URL HERE]

    # This actually rotates by 45 degrees and scales by 1.414, so that full
    # forward = [100,100]
    right = speed - direction
    left = speed + direction

    # But now that we've scaled, asking for full forward + full right turn
    # means the motors need to go to 141.  If we're asking for > 100, scale
    # back without changing the proportion of forward/turning
    if abs(left) > 100 or abs(right) > 100:
      scale = 1.0
      # if left is bigger, use it to get the scaling amount
      if abs(left) > abs(right):
        scale = 100.0 / left
      else:
        scale = 100.0 / right
      
      left = int(scale * left)
      right = int(scale * right)

    # print speed,direction,left,right

    if speed == 0 and direction == 0:
      print 'steady'
      commandOut('s')

    elif pitch > 50:
      print 'off (or upside down)'
      commandOut('s')
  
    else:
      #command = 'pass "w %d %d;"' % (left, right)
      command = "w %d %d" % (left, right)
      print command
      commandOut(command)

    time.sleep(0.25)

#Non-configurable settings
droid = android.Android()
previousMsg = ""
lastMsgTime = time.time()

# Get configurable options from the ini file
config = ConfigParser.ConfigParser()
config.read("/sdcard/ase/scripts/cellbotRemoteConfig.ini")
xmppServer = config.get("xmpp", "server")
xmppPort = config.getint("xmpp", "port")
xmppUsername = config.get("xmpp", "username")
xmppPassword = config.get("xmpp", "password")
xmppRobotUsername = config.get("xmpp", "robotUsername")
outputMethod = config.get("control", "outputMethod")

# The main loop that fires up a telnet socket and processes inputs
def main():
  print "Lay the phone flat to pause the program.\n"
  droid.startSensing()
  if outputMethod == "outputBluetooth":
    initializeBluetooth()
    #readerThread = bluetoothReader()
  else:
    commandByXMPP()
  #readerThread.start()
  droid.makeToast("Move the phone to control the robot")
  runRemoteControl()

if __name__ == '__main__':
    main()
