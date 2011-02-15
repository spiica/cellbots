/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Roambot controls a 2-wheeled Vex robot built using 2-wire or 3-wire
 * motors from Vex robotics. Compile and run this code on the Vex Pro
 * microcontroller. Send commands to the Vex robot over a Bluetooth chip
 * connected to the UART serial port of the Vex Pro. Roambot can understand
 * the following commands:
 *
 * 1. w left-vel right-vel left-accl right-accl
 * 2. f (move forward for 4 seconds with a fixed speed)
 * 3. b (move back continuously with a fixed speed)
 * 4. l (turn left continuously with a fixed speed)
 * 5. r (turn right continuously with a fixed speed)
 * 6. s (stop)
 *
 * These commands are not currently in conformance with the motor-sensor spec.
 *
 * Author: Chaitanya Gharpure (chaitanyag@google.com)
 *
 **/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <string.h>
#include "qegpioint.h"
#include "qemotortraj.h"
#include "qeservo.h"
#include "motor.h"

#include <iostream>

namespace cellbot {

// Default speed as the percentage of maximum
int speed = 40;

int head_vertical_angle = 50;

int head_horiz_angle = 170;

bool stopped = false;

// Default acceleration as percentage of maximum
static unsigned int ACCL = 100;

MotorControl *motor_control = MotorControl::GetInstance();

// get motor reference
CQEServo &servo = CQEServo::GetRef();

// Parse wheel velocities from a "w <l> <r>" command, where <l> is the left
// wheel velocity and <r> is the right wheel velocity.
void parseAndSetWheelVelocities(char* cmd) {
  char* tok;
  tok = strtok(cmd, " ");
  if (tok[0] != 'W' && tok[0] != 'w') return;

  tok = strtok(NULL, " ");
  if (tok == NULL) return;
  int left_vel = atoi(tok);

  tok = strtok(NULL, " ");
  if (tok == NULL) return;
  int right_vel = atoi(tok);

  tok = strtok(NULL, " ");
  int left_accl = 30;
  int right_accl = 30;
  if (tok != NULL) {
    left_accl = atoi(tok);
    tok = strtok(NULL, " ");
    if (tok != NULL)
      right_accl = atoi(tok);
  }
  motor_control->SetWheelVelocity(left_vel, right_vel, left_accl, right_accl);
}

// Starts listening over the UART serial input to which Bluetooth is connected.
// Each command sent over Bluetooth is expected to be terminated by a newline
// character ('\n').
void startListen() {
  struct termios tio;
  struct termios stdio;
  int tty_fd;

  unsigned char c = 'D';

  memset(&stdio, 0, sizeof(stdio));
  stdio.c_iflag = 0;
  stdio.c_oflag = 0;
  stdio.c_cflag = 0;
  stdio.c_lflag = 0;
  stdio.c_cc[VMIN] = 1;
  stdio.c_cc[VTIME] = 0;
  tcsetattr(STDOUT_FILENO, TCSANOW, &stdio);
  tcsetattr(STDOUT_FILENO, TCSAFLUSH, &stdio);
  fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);

  memset(&tio,0,sizeof(tio));
  tio.c_iflag = 0;
  tio.c_oflag = 0;
  tio.c_cflag = CS8 | CREAD | CLOCAL;
  tio.c_lflag = 0;
  tio.c_cc[VMIN] = 1;
  tio.c_cc[VTIME] = 5;

  tty_fd = open("/dev/ttyAM1", O_RDWR | O_NONBLOCK);
  cfsetospeed(&tio, B115200);
  cfsetispeed(&tio, B115200);

  tcsetattr(tty_fd, TCSANOW, &tio);
  char cmd[128];
  int pos = 0;
  while (1)
  {
    if (read(tty_fd, &c, 1) > 0) {
      if (c == '\n') {
        cmd[pos] = '\0';
        pos = 0;
        printf("%s : %c\n", cmd, cmd[0]);
        // The command is checked and appropriate action is taken.
        // Currently this is not in conformance to the motor-sensor spec.
        // TODO (chaitanyag): Fix this so that it matches with the spec.
        if (strcmp(cmd, "q") == 0) {  // Quit
          break;
        } else if (strcmp(cmd, "ds") == 0) {  // decrease speed
          speed -= 5;
        } else if (strcmp(cmd, "is") == 0) {  // increase speed
          speed += 5;
        } else if (strcmp(cmd, "hu") == 0) {  // move head up
          int pos = servo.GetCommand(3);
          if (pos > 255)
            servo.SetCommand(3, 50);
          else
            servo.SetCommand(3, pos + 10);
        } else if (strcmp(cmd, "hd") == 0) {  // move head down
          int pos = servo.GetCommand(3);
          if (pos > 255)
            servo.SetCommand(3, 50);
          else
            servo.SetCommand(3, pos - 10);
        } else if (strcmp(cmd, "hl") == 0) {  // move head left
          int pos = servo.GetCommand(2);
          if (pos > 255)
            servo.SetCommand(2, 170);
          else
            servo.SetCommand(2, pos + 10);
        } else if (strcmp(cmd, "hr") == 0) {  // move head right
          int pos = servo.GetCommand(2);
          if (pos > 255)
            servo.SetCommand(2, 170);
          else
            servo.SetCommand(2, pos - 10);
        } else if (strcmp(cmd, "hc") == 0) {  // move head up
          servo.SetCommand(2, head_horiz_angle);
          servo.SetCommand(3, head_vertical_angle);
        } else if (cmd[0] == 'w' || cmd[0] == 'W') {
          parseAndSetWheelVelocities(cmd);
        } else if (cmd[0] == 'f' || cmd[0] == 'F') {
          motor_control->Move(
              MotorControl::FWD_CONTINUOUS, speed, ACCL, 0, 0);
        } else if (cmd[0] == 'b' || cmd[0] == 'B') {
          motor_control->Move(
              MotorControl::BWD_CONTINUOUS, speed, ACCL, 0, 0);
        } else if (cmd[0] == 'r' || cmd[0] == 'R') {
          motor_control->Move(
              MotorControl::TURN_RIGHT_CONTINUOUS, speed, ACCL, 0, 0);
        } else if (cmd[0] == 'l' || cmd[0] == 'L') {
          motor_control->Move(
              MotorControl::TURN_LEFT_CONTINUOUS, speed, ACCL, 0, 0);
        } else if (cmd[0] == 's' || cmd[0] == 'S') {
          motor_control->Stop();
        }
      } else {
        cmd[pos++] = c;
      }
    }
  }
  servo.SetCommand(2, 132.5);
  servo.SetCommand(3, 132.5);
  CQEServo::Release();

  close(tty_fd);
}
};

int main(int argc, char **argv)
{
  cellbot::startListen();
  return 1;
}
