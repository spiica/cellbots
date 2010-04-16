import android
import os
import time
import xmpp
import ConfigParser


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

# Send command out of the device over XMPP
def commandOut(msg):
  global xmppRobotUsername
  global previousMsg
  global lastMsgTime
  if not xmppRobotUsername:
    xmppPassword = droid.getInput('Robot user')['result']
  if msg != previousMsg or (time.time() > lastMsgTime + 1000):
    xmppClient.send(xmpp.Message(xmppRobotUsername, msg))
  previousMsg=msg
  lastMsgTime = time.time()
  

def runRemoteControl():
  upright=True
  time.sleep(1.0) # give the sensors a chance to start up
  while 1:
    sensor_result = droid.readSensors()

  # for N1, breaks droid
    pitch=int(sensor_result.result['pitch'])
    roll =int(sensor_result.result['roll'])

  # for Droid, breaks n1
  # pitch=int(sensor_result['result']['pitch'])
  # roll =int(sensor_result['result']['roll'])

    pitch = pitch + 60
    if pitch > 0:
      pitch_speed = int(2 * pitch)
    else:
      pitch_speed = -int(25.0 + pitch)

    direction = -roll * 4
    if direction < 20 and direction > -20:
      direction = 0

    # clamp
    if pitch_speed > 100:
      pitch_speed = 100

    #print pitch_speed,direction,pitch,roll

    if pitch in range(-100,-25):
      wheel_value = -pitch_speed
      command = 'pass "w %d %d;"' % (wheel_value, wheel_value)
      print command
      commandOut(command)

      #commandOut('b')

    elif pitch in range(-25, 5):
      print 'steady'
      commandOut('s')

    elif pitch in range(5, 50):
      wheel_value = pitch_speed
      command = 'pass "w %d %d;"' % (wheel_value, wheel_value)
      print command
      commandOut(command)

      #commandOut('f')

    elif pitch in range(50,65):
      print 'off'
      commandOut('s')

    else:
      print 'probably upside down'
      commandOut('s')
      upright=False

    time.sleep(0.5)

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

# The main loop that fires up a telnet socket and processes inputs
def main():
  print "Lay the phone flat to exit the program.\n"
  droid.startSensing()
  droid.makeToast("Move the phone to control the robot")
  commandByXMPP()

if __name__ == '__main__':
    main()
