/*
  Basic two servo robot commanded by serial input
 
 Looks for a set of ASCII characters in the signal to send
 commands to a set of servos to drive a small robot. LED pin #13
 will remain lit during servo movement.
 
 
 The circuit:
 * LED attached from pin 13 to ground (or use built-in LED on most Arduino's)
 * Servos with signal wires connected to pins 2 and 4 (5v power and ground for
 servos can also be wired into Arduino, or power can come from external source)
 * Serial input connected to RX pin 0
 * Serial output connected to TX pin 1
 
 Note: If you don't yet have a serial device to connect with, you can use the 
 built in Serial Monitor in the Arduino software when connect via USB for testing.
 
 
 created 2010
 by Tim Heath & Ryan Hickman
 http://www.cellbots.com
 */

#include <Servo.h> 

// Configurable values based on pins used and servo direction
const int servoPinLeft = 2;
const int servoPinRight = 4;
const int servoDirectionLeft = 1; // Use either 1 or -1 for reverse
const int servoDirectionRight = -1; // Use either 1 or -1 for reverse
const int ledPin = 13; // LED turns on while running servos
const long maxRunTime = 2000; // maximum run time for servos without additional command

// Create servo objects to control the servos
Servo myservoLeft;
Servo myservoRight;

// No config required for these parameters
int speedMultiplier = 1; // uses a range from 1-10
boolean servosActive = false; // assume servos are not moving when we begin
unsigned long stopTime=millis(); // used for calculating the run time for servos
char incomingByte; // Holds incoming serial values

void setup() 
{
  if(1){
    pinMode(servoPinLeft, OUTPUT);
    pinMode(servoPinRight, OUTPUT);
    pinMode(ledPin, OUTPUT);
    digitalWrite(servoPinLeft,0);
    digitalWrite(servoPinRight,0);
    Serial.begin(9600);

    // Print out some basic instructions when monitoring over serial connection
    Serial.println("Ready to listen to commands! Try ome of these:");
    Serial.println("F (forward), B (backward), L (left), R (right), S (stop), D (demo).");
    Serial.println("Also use numbers 1-9 to adjust speed (0=slow, 9=fast).");
  }
} 

// Convert directional text commands ("forward"/"backward") into calculated servo speed
int directionValue(char* directionCommand, int servoDirection){
  int servoValue;

  if (directionCommand == "forward"){
    servoValue = (10 * speedMultiplier * servoDirection);
  }
  else if (directionCommand == "backward") {
    servoValue = (-10 * speedMultiplier * servoDirection);
  }
  else {
    Serial.println("Houston, we have a problem!");
    servoValue = 0; // attemp to set value to center - this shouldn't be needed
  }

  servoValue+=90;

  return servoValue;
}

// Command the bot to move (left servo command, right servo command)
unsigned long  moveBot(char* commandLeft, char* commandRight) {
  digitalWrite(ledPin, HIGH);   // set the LED on
  // Restart the servo PWM and send them commands
  myservoLeft.attach(servoPinLeft);
  myservoRight.attach(servoPinRight);
  int valueLeft = directionValue(commandLeft, servoDirectionLeft);
  myservoLeft.write(valueLeft);
  int valueRight = directionValue(commandRight, servoDirectionRight);
  myservoRight.write(valueRight);

  // Spit out some diagnosis info over serial
  Serial.print("Moving left servo ");
  Serial.print(valueLeft, DEC);
  Serial.print(" and right servo ");
  Serial.println(valueRight, DEC);
  
  stopTime=millis() + maxRunTime; // set up allowable running time in ms

  return stopTime;
}

// Stop the bot
void stopBot() {
  myservoLeft.detach();
  myservoRight.detach();
  digitalWrite(ledPin, LOW);    // set the LED off
  Serial.println("Stopping both servos");
}

// Main loop running at all times
void loop() 
{
  // Check to see if enough time has elapsed to stop the bot if not stopped already
  if(stopTime < millis() and servosActive) {
    Serial.print("Running time expired - ");
    stopBot();
    servosActive = false;
  }
  else {
    // See if there's incoming serial data
    if (Serial.available() > 0) {
      // Read the oldest byte in the serial buffer
      incomingByte = Serial.read();

      switch(incomingByte) {
      case 'd':
      case 'D':
        // Do a little demo
        speedMultiplier = 1;
        moveBot("forward","backward"); 
        delay(500); 
        stopBot();
        moveBot("backward","forward"); 
        delay(500); 
        stopBot();
        moveBot("forward","forward"); 
        delay(2000); 
        stopBot();
        moveBot("backward","backward"); 
        delay(3000); 
        stopBot();
        speedMultiplier = 1; 
        moveBot("forward","forward"); 
        delay(1000);
        speedMultiplier = 3; 
        moveBot("forward","forward"); 
        delay(1000);
        speedMultiplier = 5; 
        moveBot("forward","forward"); 
        delay(2000); 
        stopBot();
        speedMultiplier = 3; 
        moveBot("backward","backward"); 
        delay(3000); 
        stopBot();
        break;
      case 'f':
      case 'F': // Forward
        stopTime = moveBot("forward","forward");
        servosActive = true;
        break;
      case 'r':
      case 'R': // Right
        stopTime = moveBot("forward","backward");
        servosActive = true;
        break;   
      case 'l':
      case 'L': // Left
        stopTime = moveBot("backward","forward");
        servosActive = true;
        break;   
      case 'b':
      case 'B':  // Backward
        stopTime = moveBot("backward","backward");
        servosActive = true;
        break;
      case 's':
      case 'S':  // Stop
        stopBot();
        servosActive = false;
        break;
        // If it ain't one of the above test if it is a number:
      default:
        // If it's an ASCII character between 49 and 57, which is numbers 1-9
        if (incomingByte >= 49 and incomingByte <= 57){
          Serial.print("Changing speed to ");
          Serial.println(incomingByte);
          speedMultiplier = incomingByte - 48; // Set the speed multiplier to a range 1-10 from ASCII inputs 0-9
        }
        else {
          Serial.print("Unrecognized input value: ");
          Serial.println(incomingByte);
        }
      }
    }
  }
}



