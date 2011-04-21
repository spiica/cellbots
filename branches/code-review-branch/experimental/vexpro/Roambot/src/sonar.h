/*
 * sonar.h
 *
 *  Created on: Feb 16, 2011
 *      Author: anuragag
 */

#ifndef SRC_SONAR_H_
#define SRC_SONAR_H_

#include <stdio.h>
#include <unistd.h>
#include "qegpioint.h"

#define MAX_READINGS 50

class SonarControl {
 public:
  explicit SonarControl(int input_pin, int output_pin);
  ~SonarControl();

   void StartReading();
   void StopReading();
   int GetDistanceInCm();
   static void IOCallback(unsigned int io, struct timeval* ptv);
   void SendPulse();

 private:
   static int GetBitmask(int pin);
   static unsigned long ComputeTimeDifference(struct timeval* old_time,
                                                            struct timeval* new_time);

   struct timeval last_pulse_send_time_;
   bool was_pulse_fired_;
   unsigned int input_pin_;
   unsigned int output_pin_;

   class SonarReadings {
     public:
       SonarReadings();
       ~SonarReadings();

       unsigned long GetValue();
       void AddValue(unsigned long reading);
     private:
     // unsigned long readings[MAX_READINGS];
       // For now just maintain the latest reading but switch
       // to an averaged counter later on to make sonar more
       // accurate.
       unsigned long reading_;
   };

   SonarReadings readings_;

   // TODO: have a cleaner way of dealing with callbacks.
   static SonarControl* instance_;
};

#endif /* SRC_SONAR_H_ */
