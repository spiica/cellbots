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

#if defined(MOTOR_2_WIRE)

MotorControl* MotorControl::self = NULL;
bool MotorControl::instantiated = false;

// For a Vex robot using 2-wire motors for the wheels,
MotorControl::MotorControl() : state_(NOT_MOVING),
                 move_type_(NONE),
                 motor0_(0),
                 motor1_(1),
                 move_id(0),
                 curr_move_(new RMove()),
                 motor_(CQEMotorTraj::GetRef()) {
  motor0_.SetPIDVGains(motor0_, 100, 0, 500, 0);
  motor1_.SetPIDVGains(motor1_, 100, 0, 500, 0);
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
  motor_.MoveVelocity(motor0_, left_vel * 1000, left_accl * 1000);
  motor_.MoveVelocity(motor1_, -right_vel * 1000, right_accl * 1000);
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
    motor_.MoveVelocity(motor0_, move->left_vel, move->left_accl);
    motor_.MoveVelocity(motor1_, -move->right_vel, move->right_accl);
  } else if (move->move_type == BWD_CONTINUOUS) {
    motor_.MoveVelocity(motor0_, -move->left_vel, move->left_accl);
    motor_.MoveVelocity(motor1_, move->right_vel, move->right_accl);
  } else if (move->move_type == TURN_LEFT_CONTINUOUS) {
    motor_.MoveVelocity(motor0_, move->left_vel, move->left_accl);
    motor_.MoveVelocity(motor1_, move->right_vel, move->right_accl);
  } else if (move->move_type == TURN_RIGHT_CONTINUOUS) {
    motor_.MoveVelocity(motor0_, -move->left_vel, move->left_accl);
    motor_.MoveVelocity(motor1_, -move->right_vel, move->right_accl);
  } else if (move->move_type == FWD_DURATION) {
    motor_.MoveVelocity(motor0_, move->left_vel, move->left_accl);
    motor_.MoveVelocity(motor1_, -move->right_vel, move->right_accl);
    usleep(move->duration * 1000);
    if (mid == move_id) { // stop if new command has not overriden this one
      motor_.Stop(motor0_);
      motor_.Stop(motor1_);
    }
  }
}

void MotorControl::Stop() {
  motor_.Stop(motor0_);
  motor_.Stop(motor1_);
}
#endif
