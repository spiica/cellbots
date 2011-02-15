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
 * Author: Chaitanya Gharpure (chaitanyag@google.com)
 **/
#include "motor.h"
#include "algorithm"

using namespace std;

#if defined(MOTOR_3_WIRE)

// The 3-wire motors take a PWM signal in the range of 0 to 255.
// It turns out that 132.5 is the center. So 132.5 + x and 132.5 - x
// produce same speed in opposite directions. Also, PWM output
// 132.5 +- 30 does not produce any motion. Hence I take 112.5 and 152.5
// as the base.
#define LEFT_VEL(vel) min(255, max(0, (int) (vel == 0 ? 132.5 : (vel < 0 ? 152.5 - vel : 112.5 - vel))))
#define RIGHT_VEL(vel) min(255, max(0, (int) (vel == 0 ? 132.5 : (vel < 0 ? 112.5 + vel : 152.5 + vel))))

MotorControl* MotorControl::self = NULL;
bool MotorControl::instantiated = false;

MotorControl::MotorControl() : state_(NOT_MOVING),
                 move_type_(NONE),
                 motor0_(0),
                 motor1_(1),
                 move_id(0),
                 curr_move_(new RMove()),
                 motor_(CQEServo::GetRef()) {
}

MotorControl::~MotorControl() {
  instantiated = false;
}

MotorControl* MotorControl::GetInstance() {
  if (!instantiated) {
    self = new MotorControl();
    instantiated = true;
  }
  return self;
}

void MotorControl::SetWheelVelocity(int left_vel, int right_vel,
    int left_accl, int right_accl) {
  printf("Setting wheel vels: %d %d\n", left_vel, right_vel);
  motor_.SetCommand(motor0_, LEFT_VEL(left_vel));
  motor_.SetCommand(motor1_, RIGHT_VEL(right_vel));
}

void* MoveThread(void* mv) {
  MotorControl::GetInstance()->DoMove((RMove*) mv);
  return 0;
}

void MotorControl::Move(MoveType move_type, int vel, int accl,
    int duration, int disp) {
  curr_move_->Set(move_type, vel, vel, accl, accl, disp, duration);
  pthread_t thread;
  pthread_create(&thread, NULL, MoveThread, curr_move_);
}

// Call this function from a thread, otherwise it may block the main thread.
void MotorControl::DoMove(RMove* move) {
  int mid = ++move_id;
  if (move->move_type == FWD_CONTINUOUS) {
    motor_.SetCommand(motor0_, LEFT_VEL(move->left_vel));
    motor_.SetCommand(motor1_, RIGHT_VEL(move->right_vel));
  } else if (move->move_type == BWD_CONTINUOUS) {
    motor_.SetCommand(motor0_, LEFT_VEL(-move->left_vel));
    motor_.SetCommand(motor1_, RIGHT_VEL(-move->right_vel));
  } else if (move->move_type == TURN_LEFT_CONTINUOUS) {
    motor_.SetCommand(motor0_, LEFT_VEL(-move->left_vel));
    motor_.SetCommand(motor1_, RIGHT_VEL(move->right_vel));
  } else if (move->move_type == TURN_RIGHT_CONTINUOUS) {
    motor_.SetCommand(motor0_, LEFT_VEL(move->left_vel));
    motor_.SetCommand(motor1_, RIGHT_VEL(-move->right_vel));
  } else if (move->move_type == FWD_DURATION) {
    usleep(move->duration * 1000);
    if (mid == move_id) { // stop if new command has not overriden this one
      motor_.SetCommand(motor0_, 132.5);
      motor_.SetCommand(motor1_, 132.5);
    }
  }
}

void MotorControl::Stop() {
  motor_.SetCommand(motor0_, 132.5);
  motor_.SetCommand(motor1_, 132.5);
}
#endif
