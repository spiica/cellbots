Notes on setting up an iphone to be cellbot with configuration Teensy 2.0 Arduino, Macbook Pro, MacOSX 10.5.8, Iphone 3 run IOS 5.0

# General Steps for Software #

  * Jailbreak with redsnow
  * Go to Cydia and install:
  * festival-lite (create .wav files from text)
  * Erica utilities for play utility (plays .wav files)
  * openssh
  * vim
  * wget
  * python
  * mobile terminal
  * minicom

# Pinout Notes #

PodBreakout v1.4 to Teensy 2.0:

  * 2 to GND
  * 11 to D2

Servo pins:

  * servo 1 B5
  * server 2 B6

9 Volt battery voltage regulator:
  * + to pin 1
  * - to pin 2
  * capacitor pin 2 to 3
  * pin 3 to teensy 2.0 power
  * pin 2 to teensy 2.0 GND
