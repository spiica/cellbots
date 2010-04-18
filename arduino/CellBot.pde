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
#define BUFFERSIZE 20

// ** GENERAL SETTINGS ** - General preference settings
boolean DEBUGGING = false; // Whether debugging output over serial is on by defauly (can be flipped with 'h' command)
const int ledPin = 13; // LED turns on while running servos

// ** SERVO SETTINGS ** - Configurable values based on pins used and servo direction
const int servoPinLeft = 3;
const int servoPinRight = 5;
const int servoDirectionLeft = 1; // Use either 1 or -1 for reverse
const int servoDirectionRight = -1; // Use either 1 or -1 for reverse
int servoCenterLeft = 90; // PWM setting for no movement on left servo
int servoCenterRight = 90; // PWM setting for no movement on right servo
int servoPowerRange = 30; // PWM range off of center that servos respond best to (set to 30 to work in the 60-120 range off center of 90)
const long maxRunTime = 2000; // Maximum run time for servos without additional command. * Should use a command to set this. *
int speedMultiplier = 5; // Default speed setting. Uses a range from 1-10

// ** RANGE FINDING *** - The following settings are for ultrasonic range finders. OK to lave as-is if you don't have them on your robot
long dist, microseconds, cm, inches; // Used by the range finder for calculating distances
const int rangePinForward = 7; // Digital pin for the forward facing range finder (for object distance in front of bot)
const int rangeToObjectMargin = 0; // Range in cm to forward object (bot will stop when distance closer than this - set to 0 if no sensor)
const int rangePinForwardGround = 8; // Digital pin for downward facing range finder on the front (for edge of table detection)
const int rangeToGroundMargin = 0; // Range in cm to the table (bot will stop when distance is greater than this  set to 0 if no sensor)
const int rangeSampleCount = 3; // Number of range readings to take and average for a more stable value

// Create servo objects to control the servos
Servo myservoLeft;
Servo myservoRight;

// No config required for these parameters
boolean servosActive = false; // assume servos are not moving when we begin
boolean servosForcedActive = false; // will only stop when considered dangerous
unsigned long stopTime=millis(); // used for calculating the run time for servos
char incomingByte; // Holds incoming serial values
char msg[8]; // For passing back serial messages
char inBytes[BUFFERSIZE]; //Buffer for serial in messages
int serialIndex = 0; 
int serialAvail = 0;

void setup() {
  pinMode(servoPinLeft, OUTPUT);
  pinMode(servoPinRight, OUTPUT);
  pinMode(ledPin, OUTPUT);
  digitalWrite(servoPinLeft,0);
  digitalWrite(servoPinRight,0);
  Serial.begin(9600);
} 

// Convert directional text commands ("forward"/"backward") into calculated servo speed
int directionValue(char* directionCommand, int servoDirection) {
  if (directionCommand == "forward") {
    return (10 * speedMultiplier * servoDirection);
  }
  else if (directionCommand == "backward") {
    return (-10 * speedMultiplier * servoDirection);
  }
  else {
    if (DEBUGGING) { Serial.println("Houston, we have a problem!"); }
    return 0; // Attemp to set value to center - this shouldn't be needed
  }
}

// Translate text commands into PWM values for the bot to move (left servo command, right servo command)
unsigned long moveBot(char* commandLeft, char* commandRight) {
  int valueLeft = directionValue(commandLeft, servoDirectionLeft) + servoCenterLeft;
  int valueRight = directionValue(commandRight, servoDirectionRight) + servoCenterRight;
  driveServos(valueLeft, valueRight);
}

// Drive servo motors to move the robot using PWM values for left and right
unsigned long driveServos(int valueLeft, int valueRight) {
  digitalWrite(ledPin, HIGH);   // set the LED on
  // Restart the servo PWM and send them commands
  myservoLeft.attach(servoPinLeft);
  myservoRight.attach(servoPinRight);
  myservoLeft.write(valueLeft);
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
  serialReply("st"); // Tell the phone that the robot stopped
}

// Read and process the values from an ultrasonic range finder (you can leave this code in even if you don't have one)
long getDistanceSensor(int ultrasonicPin) {
  // Take multiple readings and average them
  microseconds = 0;
  for(int sample = 1 ; sample <= rangeSampleCount; sample ++) {
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

long microsecondsToCentimeters(long microseconds) {
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}

long microsecondsToInches(long microseconds) {
  // According to Parallax's datasheet for the PING))), there are
  // 73.746 microseconds per inch (i.e. sound travels at 1130 feet per
  // second).  This gives the distance travelled by the ping, outbound
  // and return, so we divide by 2 to get the distance of the obstacle.
  // See: http://www.parallax.com/dl/docs/prod/acc/28015-PING-v1.3.pdf
  // Same is true for the MaxSonar by MaxBotix
  return microseconds / 74 / 2;
}

//Converts a string to integer. ignores first non numbers. handles - sign.
int strToInt(char* str) {
  int sum = 0;
  int i = 0;
  //find the end of the string
  while (str[i] != '\0'){i++;}
  int multiplier = 0;
  int tens = 0;
  int num = 0;
  //convert to integer
  for (i = i-1; i >= 0; i--) {
    if (str[i] == '-') {
      sum = sum * -1;
    } else if (str[i] >= 48 && str[i] < 58) { //ensures its a number
      num = str[i] - 48; //ascii to int
      tens = pow(10,multiplier); //pow is slightly inaccurate but good enough
      num = num * tens;
      sum += num;
      multiplier++;
    }
  }  
  return sum;
}

// Replies out over serial and handles pausing and flushing the data to deal with Android serial comms
void serialReply(char* tmpmsg) {
  Serial.print("<");
  Serial.print(tmpmsg); // Send the message back out the serial line
  Serial.println(">");
  //Wait for the serial debugger to shut up
  delay(200); //this is a magic number
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
    if (dist > rangeToGroundMargin) {
      safe = true;
    }
    else if (DEBUGGING) {Serial.print("End of surface reached - ");}
  }
  if (rangeToGroundMargin == 0 && rangeToObjectMargin == 0) {return true;}
  return safe;
}

// Check if enough time has elapsed to stop the bot and if it is safe to proceed
void checkIfStopBot() {
  if (not servosForcedActive and servosActive and (stopTime < millis() or not safeToProceed())) {
    stopBot();
    servosActive = false;
  } else if (not safeToProceed()) {
    stopBot();
    servosActive = false;
  }
}

// Reads serial input if available and parses command when full command has been sent. 
void readSerialInput() {
  serialAvail = Serial.available();
  //Read what is available
  for (int i = 0; i < serialAvail; i++) {
    //Store into buffer.
    inBytes[i + serialIndex] = Serial.read();
    //Check for command end. 
    
    if (inBytes[i + serialIndex] == '\n' || inBytes[i + serialIndex] == ';' || inBytes[i + serialIndex] == '>') { //Use ; when using Serial Monitor
       inBytes[i + serialIndex] = '\0'; //end of string char
       parseCommand(inBytes); 
       serialIndex = 0;
    }
    else {
      //expecting more of the command to come later.
      serialIndex += serialAvail;
    }
  }  
}

// Dance
void showOff() {
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
}

// Cleans and parses the command
void parseCommand(char* com) {
  if (com[0] == '\0') { return; } //bit of error checking
  int start = 0;
  //get start of command
  while (com[start] != '<'){
    start++; 
    if (com[start] == '\0') {
      //its not there. Must be old version
      start = -1;
      break;
    }
  }
  start++;
  //Shift to beginning
  int i = 0;
  while (com[i + start - 1] != '\0') {
    com[i] = com[start + i];
    i++; 
  } 
  performCommand(com);
}

void performCommand(char* com) {  
  if (strcmp(com, "d") == 0) { // Do a little demo
    showOff();
  } else if (strcmp(com, "f") == 0) { // Forward
    stopTime = moveBot("forward","forward");
    servosActive = true;
  } else if (strcmp(com, "r") == 0) { // Right
    stopTime = moveBot("forward","backward");
    servosActive = true;  
  } else if (strcmp(com, "l") == 0) { // Left
    stopTime = moveBot("backward","forward");
    servosActive = true;  
  } else if (strcmp(com, "b") == 0) { // Backward
    stopTime = moveBot("backward","backward");
    servosActive = true;
  } else if (strcmp(com, "s") == 0) { // Stop
    stopBot();
    servosActive = false;
  } else if (strcmp(com, "x") == 0) { // Read and print forward facing distance sensor
    dist = getDistanceSensor(rangePinForward);
    itoa(dist, msg, 10); // Turn the dist int into a char
    serialReply(msg); // Send the distance out the serial line
  } else if (strcmp(com, "z") == 0) { // Read and print ground facing distance sensor
    dist = getDistanceSensor(rangePinForwardGround);
    itoa(dist, msg, 10); // Turn the dist int into a char
    serialReply(msg); // Send the distance out the serial line
  } else if (strcmp(com, "h") == 0) { // Help mode - debugging toggle
    // Print out some basic instructions when first turning on debugging
    if (not DEBUGGING) {
      Serial.println("Ready to listen to commands! Try ome of these:");
      Serial.println("F (forward), B (backward), L (left), R (right), S (stop), D (demo).");
      Serial.println("Also use numbers 1-9 to adjust speed (0=slow, 9=fast).");
    }
    DEBUGGING = !DEBUGGING;
  } else if (strcmp(com, "1") == 0 || strcmp(com, "2") == 0 || strcmp(com, "3") == 0 || strcmp(com, "4") == 0 || strcmp(com, "5") == 0 || strcmp(com, "6") == 0 || strcmp(com, "7") == 0 || strcmp(com, "8") == 0 || strcmp(com, "9") == 0 || strcmp(com, "0") == 0) {
    //I know the preceeding condition is dodgy but it will change soon 
    if (DEBUGGING) { Serial.print("Changing speed to "); }
    int i = com[0];
    speedMultiplier = i - 48; // Set the speed multiplier to a range 1-10 from ASCII inputs 0-9
    if (DEBUGGING) { Serial.println(speedMultiplier); }
    // Blink the LED to confirm the new speed setting
    for(int speedBlink = 1 ; speedBlink <= speedMultiplier; speedBlink ++) { 
      digitalWrite(ledPin, HIGH);   // set the LED on           
      delay(250);
      digitalWrite(ledPin, LOW);   // set the LED off
      delay(250);
    }  
  } else if (com[0] == 'c') { // Calibrate center PWM settings for both servos ex: "c 90 90"
    int valueLeft=90, valueRight=90;
    sscanf (com,"c %d %d",&valueLeft, &valueRight); // Parse the input into multiple values
    servoCenterLeft = valueLeft;
    servoCenterRight = valueRight;
    stopTime = driveServos(servoCenterLeft, servoCenterRight); // Drive the servos with their center value (should result in no movement when calibrated)
    servosActive = true;
    if (DEBUGGING) {
      Serial.print("Calibrated servo centers to ");
      Serial.print(servoCenterLeft);
      Serial.print(" and ");
      Serial.println(servoCenterRight);
    }
  } else if (strcmp(com, "i") == 0) { // Toggle servo to infinite active mode so it doesn't time out automatically
    servosForcedActive = !servosForcedActive; // Stop only when dangerous
    if (DEBUGGING) {
      Serial.print("Infinite rotation toggled to ");
      if (servosForcedActive){Serial.println("on");}
      else {Serial.println("off");}
    }
  } else if (com[0] == 'w') { // Handle "wheel" command and translate into PWM values ex: "w -100 100" [range is from -100 to 100]
    int valueLeft=90, valueRight=90;
    sscanf (com,"w %d %d",&valueLeft, &valueRight); // Parse the input into multiple values
    valueLeft = valueLeft * servoDirectionLeft; // Flip positive to negative if needed based on servo direction value setting
    valueRight = valueRight * servoDirectionRight;
    valueLeft = map(valueLeft, -100, 100, (servoCenterLeft - servoPowerRange), (servoCenterLeft + servoPowerRange)); // Maps to the narrow range that the servo responds to
    valueRight = map(valueRight, -100, 100, (servoCenterRight - servoPowerRange), (servoCenterRight + servoPowerRange));
    stopTime = driveServos(valueLeft, valueRight);
    servosActive = true;
  } else { 
    serialReply(com);// Echo nknown command back (this may result in a loop if the other side also does this)
    if (DEBUGGING) {
      Serial.print("Unknown command");
      Serial.println(com);
    }
  }
}
// Main loop running at all times
void loop() 
{
  readSerialInput();
  checkIfStopBot();
}

