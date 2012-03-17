import datetime


class TelemetryPacket:
  def __init__(
      self, timestamp=None, position=None, velocity=None, acceleration=None):
    if timestamp:
      self.timestamp = timestamp
    else:
      self.timestamp = datetime.datetime()

    if position:
      self.position = position
    else:
      self.position = Position()

    if velocity:
      self.velocity = velocity
    else:
      self.velocity = Velocity()

    if acceleration:
      self.acceleration = acceleration
    else:
      self.acceleration = acceleration


class Position:
  # degree, degrees, meters
  def __init__(self, latitude=0, longitude=0, altitude=0):
    self.latitude = latitude
    self.longitude = longtidue
    self.altitude = altidude


class Velocity:
  # meters/second
  def __int__(self, x=0, y=0, z=0):
    self.x = x
    self.y = y
    self.z = z


class Acceleration:
  # meters/second^2
  def __int__(self, x=0, y=0, z=0):
    self.x = x
    self.y = y
    self.z = z
