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

__author__ = 'Ryan Hickman <rhickman@gmail.com>'
#Contributers = Glen Arrowsmith glen@cellbots.com
__license__ = 'Apache License, Version 2.0'

import os
import time
import datetime
import socket
import select
import sys
import math
import shlex
import netip
import xmpp
import ConfigParser
import string
import re
import robot
from threading import Thread

# Listen for incoming serial responses. If this thread stops working, try rebooting. 
class serialReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
  def run(self):
    process = robot.getSerialIn()
    while process:
      try:
        botReply = process.readline()
        if botReply:
          if len(botReply.strip()) > 1:
            splitReply = botReply.split(':')
            if len(splitReply) == 2:
              addToWhiteboard(splitReply[0], splitReply[1])
            outputToOperator("Bot says %s " % botReply)
      except:
        outputToOperator("Reader Thread Errored")

#Whiteboard is used to store sensor values and their history
def addToWhiteboard(key, value):
  global whiteboard
  timeAdded = datetime.datetime.today()
  valueAndTime = [value, timeAdded]
  try:
    #get the old values
    values = whiteboard[key]
  except KeyError:
    #Its a new sensor. Create it.
    values = []
  values.insert(0, valueAndTime)
  #stop memory leaks
  while len(values) >= MAX_WHITEBOARD_LENGTH:
    #remove the last one
    del values[len(values)-1]
  #add back to the whiteboard
  whiteboard[key] = values

#For debugging. 
def WhiteboardToString(includeHistory=False):
  global whiteboard
  str = ""
  for key in whiteboard:
    str += key + '\n'
    for history in whiteboard[key]:
      str += '\tvalue: %s' % history[0] + '\n'
      str += '\ttime : %s' % history[1].strftime("%A, %d. %B %Y %I:%M%p") + '\n'
      if includeHistory == False:
        break        
  return str
        
        
# Listen for incoming Bluetooth resonses. If this thread stops working, try rebooting. 
class bluetoothReader(Thread):
  def __init__ (self):
    Thread.__init__(self)
    
  def run(self):
    while True:
      if not robot.bluetoothReady():
        time.sleep(0.05)
        continue
        botReply += robot.bluetoothRead()
        if '\n' in result:
          npos = botReply.find('\n')
          yield botReply[:npos]
          botReply = botReply[npos+1:]
          outputToOperator("Bot says %s " % botReply)

# Initialize Bluetooth outbound if configured for it
def initializeBluetooth():
  robot.toggleBluetoothState(True)
  robot.bluetoothConnect("00001101-0000-1000-8000-00805F9B34FB") #this is a magic UUID for serial BT devices
  robot.makeToast("Initializing Bluetooth connection")
  time.sleep(3)
  
# Command input via open telnet port
def commandByTelnet():
  rs = []
  svr_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  print "Firing up telnet socket..."
  try:
    svr_sock.bind(('', telnetPort))
    svr_sock.listen(3)
    svr_sock.setblocking(0)
    print "Ready to accept telnet. Use %s on port %s\n" % (phoneIP, telnetPort)
  except socket.error, (value,message):
    print "Could not open socket: " + message
    print "You can try using %s on port %s\n" % (phoneIP, telnetPort)

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
        if input != '':
          print "Received: '%s'" % input
          commandParse(input)

# Command input via XMPP chat
def commandByXMPP():
  global xmppUsername
  global xmppPassword
  global xmppClient
  if not xmppUsername:
    xmppUsername = robot.getInput('Username')['result']
  if not xmppPassword:
    xmppPassword = robot.getInput('Password')['result']
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
  try:
    while True:
      xmppClient.Process(1)
  except KeyboardInterrupt:
    pass

# Handle XMPP messages coming from commandByXMPP
def XMPP_message_cb(session, message):
  jid = xmpp.protocol.JID(message.getFrom())
  global operator
  operator = jid.getNode() + '@' + jid.getDomain()
  command = message.getBody()
  commandParse(str(command))

# Command input via speech recognition
def commandByVoice(mode='continuous'):
  try:
    listen = robot.recognizeSpeech()
    voiceCommands = str(listen['result'])
  except:
    voiceCommands = ""
  outputToOperator("Voice commands: %s" % voiceCommands)
  commandParse(voiceCommands)
  if mode == 'continuous':
    commandByVoice()

# Speak using TTS or make toasts
def speak(msg,override=False):
  global previousMsg
  if (audioOn and msg != previousMsg) or override:
    robot.speak(msg)
  elif msg != previousMsg:
    robot.makeToast(msg)
  outputToOperator(msg)
  previousMsg=msg

# Handle changing the speed setting  on the robot
def changeSpeed(newSpeed):
  global currentSpeed
  if newSpeed in ["0","1","2","3","4","5","6","7","8","9"]:
    msg = "Changing speed to %s" % newSpeed
    commandOut(newSpeed)
    currentSpeed=newSpeed
  else:
    msg = "Speed %s is out of range [0-9]" % newSpeed
  speak(msg)
   
# Point towards a specific compass heading
def orientToAzimuth(azimuth):
  onTarget = False
  stopTime = time.time() + 5000
  while not onTarget and time.time() < stopTime:
    results = robot.readSensors()

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
        outputToOperator(msg)
        speak(msg)
        commandOut('s')
        onTarget = True
      else:
        if adjustment > cardinalMargin:
          outputToOperator("Moving %d right." % adjustmentAbs)
          commandOut('r')
        if adjustment < (cardinalMargin * -1):
          outputToOperator("Moving %d left." % adjustmentAbs)
          commandOut('l')
        time.sleep(adjustmentAbs/180)
        commandOut('s')
        time.sleep(1)
    else:
      msg = "Could not start sensors."


# Send command out of the device via Bluetooth or serial
def commandOut(msg):
  if outputMethod == "outputBluetooth":
    robot.bluetoothWrite(msg + '\n')
  else:
    robot.writeSerialOut("echo '<%s>' > /dev/ttyMSM2" % msg)

# Display information on screen and/or reply to the human operator
def outputToOperator(msg):
  print msg
  try:
    if (inputMethod == 'commandByXMPP'):
      xmppClient.send(xmpp.Message(operator, msg))
  except:
    pass
  
# Shut down sensing and other open items and quit
def exitCellbot(msg='Exiting'):
  if outputMethod == "outputSerial":
    svr_sock.close()
  robot.stopSensing()
  robot.stopLocating()
  #The following was throwing an error. Do we need to manually kill the thread?
  #global readerThread
  #readerThread.join()
  sys.exit(msg)

# Parse the first character of incoming commands to determine what action to take
def commandParse(input):
  try:
    commandList = shlex.split(input)
  except:
    commandList = []
    outputToOperator("Could not parse command")
  try:
    command = commandList[0].lower()
  except IndexError:
    command = ""
  try:
    commandValue = commandList[1]
  except IndexError:
    commandValue = ""
  try:
    commandValue2 = commandList[2]
  except IndexError:
    commandValue2 = ""

  if command in ["a", "audio", "record"]:
    global audioRecordingOn
    audioRecordingOn = not audioRecordingOn
    fileName=time.strftime("/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.3gp")
    if audioRecordingOn:
      outputToOperator("Starting audio recording")
      robot.makeToast("Starting audio recording")
      robot.startAudioRecording(fileName)
    else:
      robot.stopAudioRecording()
      robot.makeToast("Stopping audio recording")
      outputToOperator("Stopped audio recording. Audio file located at '%s'" % fileName)
  elif command  in ["b", "back", "backward", "backwards"]:
    speak("Moving backward")
    commandOut('b')
  elif command in ["compass", "heading"]:
    orientToAzimuth(int(commandValue[:3]))
  elif command in ["d", "date"]:
    speak(time.strftime("Current time is %_I %M %p on %A, %B %_e, %Y"))
  elif command in ["f", "forward", "forwards", "scoot"]:
    speak("Moving forward")
    commandOut('f')
  elif command in ["h", "hi", "hello"]:
    speak("Hello. Let's play.")
  elif command in ["l", "left"]:
    speak("Moving left")
    commandOut('l')
  elif command in ["m", "mute", "silence"]:
    global audioOn
    audioOn = not audioOn
    speak("Audio mute toggled")
  elif command in ["p", "point", "pointe", "face", "facing"]:
    msg = "Orienting %s" % cardinals[commandValue[:1]][0]
    speak(msg)
    try:
      orientToAzimuth(int(cardinals[commandValue[:1]][1]))
    except:
      outputToOperator("Could not orient towards " + commandValue)
  elif command in ["q", "quit", "exit"]:
    speak("Bye bye!")
    exitCellbot("Exiting program after receiving 'q' command.")
  elif command in ["r", "right"]:
    speak("Moving right")
    commandOut('r')
  elif command in ["s", "stop"]:
    commandOut('s')
    outputToOperator("Stopping")
  elif command in ["t", "talk", "speak", "say"]:
    speak(input.replace(command, ''),True)
  elif command in ["v", "voice", "listen", "speech"]:
    robot.makeToast("Launching voice recognition")
    outputToOperator("Launching voice recognition")
    commandByVoice("onceOnly")
  elif command in ["x", "location", "gps"]:
    try:
      location = robot.readLocation()['result']
      addresses = robot.geocode(location['latitude'], location['longitude'])
      firstAddr = addresses['result']['result'][0]
      msg = 'You are in %(locality)s, %(admin_area)s' % firstAddr
    except:
      msg = "Failed to find location."
    speak(msg)
  elif command == "speed":
    if commandValue in ["0","1","2","3","4","5","6","7","8","9"]:
      changeSpeed(commandValue)
    else:
      outputToOperator("Invalid speed setting: '%s'" % command)
  elif command in ["faster", "hurry", "fast", "quicker"]:
    changeSpeed(currentSpeed + 1)
  elif command in ["slower", "slow", "chill"]:
    changeSpeed(currentSpeed - 1)
  elif command in ["move", "go", "turn"]:
    commandParse(commandValue)
  elif command in ["send", "pass"]:
    commandOut(commandValue)
  elif command in ["range", "distance", "dist", "z"]:
    commandOut("fr")
    outputToOperator("Checking distance")    
    #ReaderThread thread will handle the response.
  elif command in ["c", "config", "calibrate"]:
    commandOut("c" + commandValue + " " + commandValue2)
    msg = "Calibrating servos to center at %s and %s" % (commandValue, commandValue2)
    outputToOperator(msg)
  elif command in ["w", "wheel"]:
    commandOut("w" + commandValue + " " + commandValue2)
    msg = "Driving servos with %s and %s" % (commandValue, commandValue2)
    outputToOperator(msg)
    addToWhiteboard("w", commandValue + " " + commandValue2)
  elif command in ["i", "infinite"]:
    commandOut("i")
    outputToOperator("Toggled infinite rotation mode on robot")
  elif command in ["picture", "takepicture", "takePicture"]:
    fileName=time.strftime("/sdcard/cellbot_%Y-%m-%d_%H-%M-%S.jpg")
    robot.cameraTakePicture(fileName)
    outputToOperator("Took picture. Image file located at '%s'" % fileName)
    addToWhiteboard("Picture", fileName)
  elif command in ["whiteboard", "whiteboardfull"]:
    if command == "whiteboardfull":
      outputToOperator(WhiteboardToString(True))
    else:
      outputToOperator(WhiteboardToString())
  elif command in ["reset"]:
    commandOut("reset")
    outputToOperator("Reset hardwares settings to default")
  elif command in ["pair", "pairing"]:
    commandOut("p")
    outputToOperator("Asking Bluetooth module to go into pairing")
  else:
    outputToOperator("Unknown command: '%s'" % command)

#Non-configurable settings
cardinals = {}
cardinals['n']=('North','0')
cardinals['e']=('East','90')
cardinals['w']=('West','270')
cardinals['s']=('South','180')

#defines the dict of sensor values and their history
whiteboard = {}
MAX_WHITEBOARD_LENGTH = 30
previousMsg = ""
audioRecordingOn = False
phoneIP = netip.displayNoLo()

# Get configurable options from the ini file
config = ConfigParser.ConfigParser()
configFilePath = "/sdcard/ase/scripts/cellbotConfig.ini"
config.read(configFilePath)
if config.has_option("basics", "phoneType"):
  phoneType = config.get("basics", "phoneType")
else:
  phoneType = "android"
  config.set("basics", "phoneType", phoneType)
robot = robot.Robot(phoneType)
audioOn = config.getboolean("basics", "audioOn")
currentSpeed = config.getint("basics", "currentSpeed")
cardinalMargin = config.getint("basics", "cardinalMargin")
telnetPort = config.getint("control", "port")
inputMethod = config.get("control", "inputMethod")
if config.has_option("control", "outputMethod"):
  outputMethod = config.get("control", "outputMethod")
else:
  dialog = robot.dialogCreateAlert("Select outputMethod")
  options = ['outputSerial', 'outputBluetooth']
  robot.dialogSetItems(options)
  robot.dialogShow()
  result = robot.dialogGetResponse(dialog)["result"]
  outputMethod = options[result['item']]
  config.set("control", "outputMethod", outputMethod)
xmppServer = config.get("xmpp", "server")
xmppPort = config.getint("xmpp", "port")
xmppUsername = config.get("xmpp", "username")
xmppPassword = config.get("xmpp", "password")
with open(configFilePath, 'wb') as configfile:
  config.write(configfile)


# Raise the sails and fire the cannons
def main():
  outputToOperator("Send the letter 'q' or say 'quit' to exit the program.\n")
  robot.startSensing()
  robot.startLocating()
  if outputMethod == "outputBluetooth":
    initializeBluetooth()
    readerThread = bluetoothReader()
  else:
    serialReader.lifeline = re.compile(r"(\d) received")
    readerThread = serialReader()
  readerThread.start()
  global currentSpeed
  commandOut(str(currentSpeed))
  robot.makeToast("Initiating input method...")
  globals()[inputMethod]()

if __name__ == '__main__':
    main()
