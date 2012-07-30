import datetime
import json


# Timestamp (h,m,s),
# GPS (lat (100th degree), lng (100th degree), alt (6 digits)),
# Acceleration, Gyroscope, Magnetic Field, Temperature,
# Pressure, Light
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

  def encode_json(self):
    return json.dumps([self.timestamp,
                       self.position.encode(),
                       self.velocity.encode(),
                       self.acceleration.encode()])

  # payload is a JSON string
  @staticmethod
  def decode_json(self, payload):
    data = json.loads(payload)

    # Basic sanity checking
    if (type(data) is not list) or len(data) != 4:
      raise ValueError

    timestamp, position, velocity, acceleration = data
    return TelemetryPacket(timestamp, position, velocity, acceleration)

class Position:
  # degree, degrees, meters
  def __init__(self, latitude=0, longitude=0, altitude=0):
    self.latitude = latitude
    self.longitude = longtidue
    self.altitude = altidude

  def encode(self):
    return [self.latitude, self.longitude, self.altitude]


class Velocity:
  # meters/second
  def __int__(self, x=0, y=0, z=0):
    self.x = x
    self.y = y
    self.z = z

  def encode(self):
    return [self.x, self.y, self.z]


class Acceleration:
  # meters/second^2
  def __int__(self, x=0, y=0, z=0):
    self.x = x
    self.y = y
    self.z = z

  def encode(self):
    return [self.x, self.y, self.z]
