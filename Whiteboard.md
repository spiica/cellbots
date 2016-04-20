# The Whiteboard is for Information #

As a stepping stone to a more advanced robot, a dict of 2D arrays called whiteboard has been added which is place for events and sensor values to be stored on the phone.

This information will be used in the future when attempting to determine location, surroundings, etc.

Currently the ReaderThead parses input that contains a ':' and posts values onto the whiteboard along with the time it was read. A couple of other commands such as the w command is also logged.

Post to the whiteboard yourself simply call:
```
  addToWhiteboard("SensorName", "Sensor Value")
```

To read the latest value of your sensor from the whiteboard:
```
  value = whiteboard["SensorName"][0][0]
  timeSaved = whiteboard["SensorName"][0][1]
```

To print all the sensor's latest values:
```
  print WhiteboardToString()
```

To print all the values from all the sensor's:
```
  print WhiteboardToString(True)
```

Here is an example
```
addToWhiteboard("z",14)
addToWhiteboard("u",20)
addToWhiteboard("u",21)
addToWhiteboard("u",23)
addToWhiteboard("gps","11111,22222")
addToWhiteboard("chicken","in the chopper")

print WhiteboardToString(True)
```
Would output
```
chicken
        value: in the chopper
        time : Saturday, 24. April 2010 12:46AM
z
        value: 14
        time : Saturday, 24. April 2010 12:46AM
u
        value: 23
        time : Saturday, 24. April 2010 12:46AM
        value: 21
        time : Saturday, 24. April 2010 12:46AM
        value: 20
        time : Saturday, 24. April 2010 12:46AM
gps
        value: 11111,22222
        time : Saturday, 24. April 2010 12:46AM

```


The whiteboard has a maximum length for each value defined by MAX\_WHITEBOARD\_LENGTH which is currently 30 at time of writing.

For debugging, I've also added a new android command of "whiteboard" and "whiteboardfull" which prints the whiteboard.