import android
import os
import time

droid=android.Android()
droid.startSensing()
time.sleep(1)

upright=True

# Send command out of the device (currently serial but other protocals could be added)
def commandOut(msg):
  os.system(r"echo -e '%s' > /dev/ttyMSM2" % msg)
  
def commandServos(direction, speed=0):
  if direction == 'forward':
    left = '\x80\x01\x02\x01\x01'
    right = '\x80\x01\x02\x02\x45'
  elif direction == 'backward':
    left = '\x80\x01\x02\x01\x45'
    right = '\x80\x01\x02\x02\x01'
  else:
    left = '\x80\x01\x02\x01\x40'
    right = '\x80\x01\x02\x02\x40'    

  commandOut(left)
  commandOut(right)

while upright:
  pitch=droid.readSensors()['result']['pitch']
  print pitch
  if pitch in range(-180, -150):
    print 'fell backward'
    commandServos('stop')
    upright=False
  elif pitch in range(-149,-90):
    print 'backward'
    commandServos('backward')
  elif pitch in range(-90, -85):
    print 'steady'
    commandServos('stop')
  elif pitch in range(-85, -30):
    print 'forward'
    commandServos('forward')
  elif pitch in range(-30, 0):
    print 'fell forward'
    commandServos('stop')
    upright=False
  else:
    print 'probably upside down'
    commandServos('stop')
    upright=False

  time.sleep(1)
