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
#ifndef SRC_MOTOR_H_
#define SRC_MOTOR_H_

// Change this to MOTOR_2_WIRE to build for the 2-wire Vex motors.
#define MOTOR_3_WIRE

#if defined(MOTOR_2_WIRE)
#include "qemotortraj.h"
#else
#include "qeservo.h"
#endif

class RMove;

// MotorControl enables basic motion commands for Vex robots. This class is
// currently implemented by motor.cc and motor3wire.cc for Vex robots with
// 2-wire and 3-wire motors. Implement this class such that motor0_ maps to the
// left wheel of the robot and motor1_ maps to the right wheel.
class MotorControl {
 public:
  // Indicates type of motion.
  enum MoveType {
    FWD_CONTINUOUS,         // Continuous forward motion
    BWD_CONTINUOUS,         // Continuous backward motion
    FWD_DURATION,           // Forward motion for a fixed duration
    BWD_DURATION,           // Backward motion for a fixed duration
    FWD_DISTANCE,           // Forward motion for a fixed distance
    BWD_DISTANCE,           // Backward motion for a fixed distance
    TURN_RIGHT_CONTINUOUS,  // Continuous right turn motion
    TURN_LEFT_CONTINUOUS,   // Continuous left turn motion
    TURN_LEFT_DURATION,     // Left turn for fixed duration
    TURN_RIGHT_DURATION,    // Right turn for fixed duration
    TURN_LEFT_ANGLE,        // Left turn through a fixed angle
    TURN_RIGHT_ANGLE,       // Right turn through a fixed angle
    NONE
  };

  // Indicates the current state of the robot.
  enum State {
    MOVING,
    NOT_MOVING
  };

  ~MotorControl();

  // Gets the singleton instance of the class. When a new instance is created,
  // default values are assigned to the member variable, including the motor
  // ports. It might be needed to call MotorControl::SetMotorPorts to specify
  // ports the motors are connected to. See MotorControl::SetMotorPorts for
  // more info.
  static MotorControl* GetInstance();

  // Sets the physical port number to which each motor is connected.
  // * 2-wire motors can be connected to motor input # 13, 14, 15 or 16,
  // corresponding to physical port numbers 0, 1, 2, 3. The default
  // configuration assumes that left wheel motor (motor0_) is connected
  // to port 0 (motor input 13), and the right wheel motor (motor1_) is
  // connected to port 1 (motor input 14).
  // * 3-wire motors can be connected to motor input # 1, 2, ... , 12,
  // corresponding to physical port numbers 0, 1, ... , 11. The default
  // configuration assumes that left wheel motor (motor0_) is connected
  // to port 0 (motor input 1), and the right wheel motor (motor1_) is
  // connected to port 1 (motor input 2).
  void SetMotorPorts(int motor0_port, int motor1_port) {
    motor0_ = motor0_port;
    motor1_ = motor1_port;
  }

  // Sets the specified wheel velocities starting with the specified
  // acceleration.
  void SetWheelVelocity(int left_vel, int right_vel, int left_accl,
      int right_accl);

  // Executes the specified move with the given velocity and acceleration.
  // For continuous moves, |duration| and |displacement| are ignored.
  void Move(MoveType move_type, int vel, int accl, int duration,
      int displacement);

  // Checks if the previously requested move is done. This does not apply for
  // continuous moves.
  void IsDoneMoving();

  // Stops the robot.
  void Stop();

  // Executes the specified |move|.
  void DoMove(RMove* move);

 private:
  // private constructor for singleton class.
  MotorControl();

  State state_;
  short int move_type_;
  int motor0_;
  int motor1_;
  int move_id;
  RMove* curr_move_;

#if defined(MOTOR_2_WIRE)
  CQEMotorTraj& motor_;
#else
  CQEServo& motor_;
#endif

  static MotorControl *self;
  static bool instantiated;
};

// RMove encapsulates information about a single robot move.
class RMove {
 public:
  // Type of the move
  MotorControl::MoveType move_type;
  // Left wheel velocity
  int left_vel;
  // Right wheel velocity
  int right_vel;
  // Left wheel acceleration
  int left_accl;
  // Rigth wheel acceleration
  int right_accl;
  // Displacement
  int disp;
  // Duration of motion
  int duration;

  RMove(MotorControl::MoveType type, int lv, int rv, int la, int ra,
      int dis, int dur) :
    move_type(type),
    left_vel(lv),
    right_vel(rv),
    left_accl(la),
    right_accl(ra),
    disp(dis),
    duration(dur) {
  }

  RMove() :
    move_type(MotorControl::NONE),
    left_vel(0),
    right_vel(0),
    left_accl(0),
    right_accl(0),
    disp(0),
    duration(0) {
  }

  // Sets the parameters for this move
  void Set(MotorControl::MoveType type, int lv, int rv, int la, int ra,
      int dis, int dur) {
    move_type = type;
    left_vel = lv;
    right_vel = rv;
    left_accl = la;
    right_accl = ra;
    disp = dis;
    duration = dur;
  }

  ~RMove();
};

#endif // SRC_MOTOR_H_
