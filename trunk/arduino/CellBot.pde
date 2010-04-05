/*
  Servo driven robot commanded by serial input
 
 Looks for a set of ASCII characters in the signal to send
 commands to a set of servos to drive a small robot. LED pin #13
 will remain lit during servo movement and blink for speed changes.
 
 
 The minimum circuit:
 * LED attached from pin 13 to ground (or use built-in LED on most Arduino's)
 * Servos with signal wires connected to pins 3 and 5 (5v power and ground for
 servos can also be wired into Arduino, or power can come from external source)
 * Serial input connected to RX pin 0
 * Serial output connected to TX pin 1
 
 Additional circuits (optional):
 * Forward facing ultrasonic range finder on digital pin 7
 * Downward facing ultrasonic range finder on digital pin 8
 
 Note: If you don't yet have a serial device to connect with, you can use the 
 built in Serial Monitor in the Arduino software when connect via USB for testing.
 Also, be sure to disconect RX & TX pins from other devices when trying to program
 the Arduino over USB.
 
 created 2010
 by Tim Heath, Ryan Hickman, and Glen Arrowsmith
 Visit http://www.cellbots.com for more information
 */

#include <Servo.h> 

// ** GENERAL SETTINGS ** - General preference settings
boolean DEBUGGING = false; // Whether debugging output over serial is on by defauly (can be flipped with 'h' command)
const int ledPin = 13; // LED turns on while running servos

// ** SERVO SETTINGS ** - Configurable values based on pins used and servo direction
const int servoPinLeft = 3;
const int servoPinRight = 5;
const int servoDirectionLeft = 1; // Use either 1 or -1 for reverse
const int servoDirectionRight = -1; // Use either 1 or -1 for reverse
const long maxRunTime = 2000; // Maximum run time for servos without additional command. * Should use a command to set this. *
int speedMultiplier = 5; // Default speed setting. Uses a range from 1-10

// ** RANGE FINDING *** - The following settings are for ultrasonic range finders. OK to lave as-is if you don't have them on your robot
long dist, microseconds, cm, inches; // Used by the range finder for calculating distances
const int rangePinForward = 7; // Digital pin for the forward facing range finder (for object distance in front of bot)
const int rangeToObjectMargin = 25; // Range in cm to forward object (bot will stop when distance closer than this - set to 0 if no sensor)
const int rangePinForwardGround = 8; // Digital pin for downward facing range finder on the front (for edge of table detection)
const int rangeToGroundMargin = 0; // Range in cm to the table (bot will stop when distance is greater than this  set to 0 if no sensor)
const int rangeSampleCount = 3; // Number of range readings to take and average for a more stable value

// Create servo objects to control the servos
Servo myservoLeft;
Servo myservoRight;

// No config required for these parameters
boolean servosActive = false; // assume servos are not moving when we begin
unsigned long stopTime=millis(); // used for calculating the run time for servos
char incomingByte; // Holds incoming serial values
char msg[8]; // For passing back serial messages

void setup() 
{
  if(1){
    pinMode(servoPinLeft, OUTPUT);
    pinMode(servoPinRight, OUTPUT);
    pinMode(ledPin, OUTPUT);
    digitalWrite(servoPinLeft,0);
    digitalWrite(servoPinRight,0);
    Serial.begin(9600);
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
    if (DEBUGGING) { Serial.println("Houston, we have a problem!"); }
    servoValue = 0; // Attemp to set value to center - this shouldn't be needed
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
  if (DEBUGGING) {
    Serial.print("Moving left servo ");
    Serial.print(valueLeft, DEC);
    Serial.print(" and right servo ");
    Serial.println(valueRight, DEC);
  }
  stopTime=millis() + maxRunTime; // Set time to stop running based on allowable running time
  return stopTime;
}

// Stop the bot
void stopBot() {
  myservoLeft.detach();
  myservoRight.detach();
  digitalWrite(ledPin, LOW);  // Turn the LED off
  if (DEBUGGING) { Serial.println("Stopping both servos"); }
  serialReply("stopped"); // Tell the phone that the robot stopped
}

// Read and process the values from an ultrasonic range finder (you can leave this code in even if you don't have one)
long getDistanceSensor(int ultrasonicPin){
  // The Parallax PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  // The Maxsonar does not seem to need this part but it does not hurt either
  pinMode(ultrasonicPin, OUTPUT);
  digitalWrite(ultrasonicPin, LOW);
  delayMicroseconds(2);
  digitalWrite(ultrasonicPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(ultrasonicPin, LOW);
  
  // The same pin is used to read the signal from the ultrasonic detector: a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(ultrasonicPin, INPUT);
  // Take multiple readings and average them
  microseconds = 0;
  for(int sample = 1 ; sample <= rangeSampleCount; sample ++) {
    microseconds += pulseIn(ultrasonicPin, HIGH);
    delayMicroseconds(5); // Very short pause between readings
  }
  microseconds = microseconds / rangeSampleCount;
  // Convert the averaged sensor reading to centimeters and return it
  cm = microsecondsToCentimeters(microseconds);
  inches = microsecondsToInches(microseconds);
  if (DEBUGGING) {
    Serial.print("Micro: "); Serial.print(microseconds); 
    Serial.print(" Inches: "); Serial.print(inches);
    Serial.print(" cm: "); Serial.println(cm);
  }
  return cm;
}

long microsecondsToCentimeters(long microseconds){
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}

long microsecondsToInches(long microseconds){
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  // Same is true for the MaxSonar by MaxBotix
  return microseconds / 74 / 2;
}

// Replies out over serial and handles pausing and flushing the data to deal with Android serial comms
void serialReply(char* msg){
  Serial.println(msg); // Send the message back out the serial line
  //Wait for the serial debugger to shut up
  delay(100); //this is a magic number
  Serial.flush(); //clears all incoming data
}

// Checks range finders to see if it is safe to continue moving (* need to add way to know which direction we're moving *)
boolean safeToProceed(){
  boolean safe = false; // Assume it isn't safe to proceed
  // Check the distance to the nearest object in front of the bot and stop if too close
  if (rangeToObjectMargin != 0){  // Don't bother sending if margin set to zero because it hangs when no sensor present
    dist = getDistanceSensor(rangePinForward);
    if (dist > rangeToObjectMargin) {
      safe = true;
    }
    else if (DEBUGGING) {Serial.print("Object too close in front - ");}
  }
  // Check the distance to the ground in front of the bot to make sure the table is still there
  if (rangeToGroundMargin != 0){  // Don't bother sending if margin set to zero because it hangs when no sensor present
    dist = getDistanceSensor(rangePinForwardGround);
    if (dist < rangeToGroundMargin) {
      safe = true;
    }
    else if (DEBUGGING) {Serial.print("End of surface reached - ");}
  }
  return safe;
}

// Main loop running at all times
void loop() 
{
  // Check if enough time has elapsed to stop the bot and if it is safe to proceed
  if (servosActive and (stopTime < millis() or not safeToProceed())) {
    stopBot();
    servosActive = false;
  }
  else {
    // See if there's incoming serial data
    if (Serial.available() > 0) {
      // Read the oldest byte in the serial buffer
      incomingByte = Serial.read();

      switch(incomingByte) {
      case 'd': // Do a little demo
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
      case 'f': // Forward
        stopTime = moveBot("forward","forward");
        servosActive = true;
        break;
      case 'r': // Right
        stopTime = moveBot("forward","backward");
        servosActive = true;
        break;   
      case 'l': // Left
        stopTime = moveBot("backward","forward");
        servosActive = true;
        break;   
      case 'b': // Backward
        stopTime = moveBot("backward","backward");
        servosActive = true;
        break;
      case 's': // Stop
        stopBot();
        servosActive = false;
        break;
      case 'x': // Read and print forward facing distance sensor
        dist = getDistanceSensor(rangePinForward);
        itoa(dist, msg, 10); // Turn the dist int into a char
        serialReply(msg); // Send the distance out the serial line
        break;
      case 'z': // Read and print ground facing distance sensor
        dist = getDistanceSensor(rangePinForwardGround);
        itoa(dist, msg, 10); // Turn the dist int into a char
        serialReply(msg); // Send the distance out the serial line
        break;
      case 'h': // Help mode - debugging toggle
         // Print out some basic instructions when first turning on debugging
         if (not DEBUGGING) {
           Serial.println("Ready to listen to commands! Try ome of these:");
           Serial.println("F (forward), B (backward), L (left), R (right), S (stop), D (demo).");
           Serial.println("Also use numbers 1-9 to adjust speed (0=slow, 9=fast).");
           }
        DEBUGGING = !DEBUGGING;
        break;
      default: // If it isn't one of the above, test if it is a number:
        // If it's an ASCII character between 49 and 57, which is numbers 1-9
        if (incomingByte >= 49 and incomingByte <= 57){
          if (DEBUGGING) { Serial.print("Changing speed to "); }
          if (DEBUGGING) { Serial.println(incomingByte); }
          speedMultiplier = incomingByte - 48; // Set the speed multiplier to a range 1-10 from ASCII inputs 0-9
          // Blink the LED to confirm the new speed setting
          for(int speedBlink = 1 ; speedBlink <= speedMultiplier; speedBlink ++) { 
            digitalWrite(ledPin, HIGH);   // set the LED on           
            delay(250);
            digitalWrite(ledPin, LOW);   // set the LED off
            delay(250);
          }          
        }
        else {
          if (DEBUGGING) { Serial.print("Unrecognized input value: "); }
          if (DEBUGGING) { Serial.println(incomingByte); }
        }
      }
    }
  }
}

