/*
 * sonar.cc
 *
 *  Created on: Feb 16, 2011
 *      Author: chaitanyag
 */

#include "sonar.h"

#define MICROSEC_PER_INCH 150
#define MICROSEC_PER_CM 58
#define BIAS 300

SonarControl* SonarControl::instance_ = NULL;

SonarControl::SonarControl(int input_pin, int output_pin)
  :input_pin_(input_pin), output_pin_(output_pin) {
  CQEGpioInt& gpio = CQEGpioInt::GetRef();

  // Reset all pins to 0 esp. the input
  gpio.SetData(0x0000);
  // Set the input pin's IO direction. Note that it
  // needs to be set to 1 which is output according to documentation.
  gpio.SetDataDirection(GetBitmask(input_pin));
}
SonarControl::~SonarControl() { }

int SonarControl::GetBitmask(int pin) {
  return 0x0001 << pin;
}

unsigned long SonarControl::ComputeTimeDifference(struct timeval* old_time,
                                                  struct timeval* new_time) {
  unsigned long difference;
  difference = new_time->tv_usec - old_time->tv_usec;
  difference += (new_time->tv_sec - old_time->tv_sec) * 1.0e6;

  return difference;
}

int SonarControl::GetDistanceInCm() {
  return readings_.GetValue();
}

/* static */
void SonarControl::IOCallback(unsigned int io, struct timeval* ptv) {
  if (io == instance_->input_pin_) {
    instance_->was_pulse_fired_ = true;
    instance_->last_pulse_send_time_ = *ptv;
  } else if (io == instance_->output_pin_ && instance_->was_pulse_fired_) {
    // TODO(anuragag): This code is taken from the sonartest example
    // and we may need to tune some of the values here.
    unsigned long time_diff =
        SonarControl::ComputeTimeDifference(&(instance_->last_pulse_send_time_), ptv);
    if (time_diff > BIAS) {
      instance_->readings_.AddValue((time_diff - BIAS) / MICROSEC_PER_CM);
    }
  }
}

void SonarControl::StartReading() {
  CQEGpioInt& gpio = CQEGpioInt::GetRef();

  instance_ = this;
  gpio.RegisterCallback(input_pin_, &SonarControl::IOCallback);
  gpio.RegisterCallback(output_pin_, &SonarControl::IOCallback);
  gpio.SetInterruptMode(input_pin_, QEG_INTERRUPT_NEGEDGE);
  gpio.SetInterruptMode(output_pin_, QEG_INTERRUPT_NEGEDGE);
}

void SonarControl::SendPulse() {
  CQEGpioInt& gpio = CQEGpioInt::GetRef();
  gpio.SetData(GetBitmask(input_pin_));
  // Busy wait for a while
  for (int d=0; d<100000; d++);
  gpio.SetData(0x0000);
  for (int d=0; d<100000; d++);
}

void SonarControl::StopReading() {
  CQEGpioInt& gpio = CQEGpioInt::GetRef();

  gpio.UnregisterCallback(input_pin_);
  gpio.UnregisterCallback(output_pin_);
}

SonarControl::SonarReadings::SonarReadings() { }
SonarControl::SonarReadings::~SonarReadings() { }

unsigned long SonarControl::SonarReadings::GetValue() { return reading_; }

void SonarControl::SonarReadings::AddValue(unsigned long val) { reading_ = val; }


