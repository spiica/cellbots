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
__license__ = 'Apache License, Version 2.0'

import android
import os
import time

#Basic setup values
droid=android.Android()
droid.startSensing()
time.sleep(1)
lastTorque=0

#Calibration settings (we should get some of these upon boot up instead)
K1=0.05
K2=1.5
K3=0.25
K4=1.5
centerPWM=63
ymagCalibration=40
pitchCalibration=95
repeatCount=3
lastPitch=pitchCalibration

#Send command out of the device (currently serial but other protocals could be added)
def commandOut(msg):
  os.system(r"echo -e '%s' > /dev/ttyMSM2" % msg)

#Continuous loop that takes 20 - 80ms (on a G1) so no need to sleep on top of that
while 1:
  #Take multiple readings and average them (G1's have a LOT of noise between reading - better filters would help)
  pitch=0
  ymag=0
  for i in range(0, repeatCount):
    pitch+=droid.readSensors()['result']['pitch']
    ymag+=droid.readSensors()['result']['ymag']
  pitch = (pitch / repeatCount) + pitchCalibration
  ymag = ((ymag / repeatCount) + ymagCalibration)

  #Use current pitch, change in pitch, current speed, and the Y-axis mag value to calculate speed and direction
  angle_velocity = pitch - lastPitch
  torque = (pitch * K1) + (angle_velocity * K2) + (lastTorque * K3)
  #+ (ymag * K4)
  torque = int(min(torque, centerPWM))
  lastTorque = torque
  lastPitch = pitch

  #turn the torque value into individual PWM settings for each servo
  left = centerPWM + torque
  right = centerPWM - torque
  leftPWM = max(min(left, 127), 1)
  rightPWM = max(min(right, 127), 1)

  #Send the serial commands to the Pololu serial servo controller
  serialCommand = '\x80\x01\x02\x01' + chr(leftPWM) + '\x80\x01\x02\x02' + chr(rightPWM)
  commandOut(serialCommand)

  #Output some debugging to watch what values are triggering movement
  print "P:" + str(pitch + K1) + " V:" + str(angle_velocity * K2) + " T:" + str(lastTorque * K3) + " Y:" + str(ymag * K4)
  print "T: " + str(torque) + " L:" + str(leftPWM) + " R:" + str(rightPWM)

