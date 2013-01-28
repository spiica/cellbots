import datetime
import json
import time

class CallsignGenerator:

  _next_id = 1

  @classmethod
  def get_new(cls):
    id_ = cls._next_id
    cls._next_id += 1
    return 'Unknown'+str(cls._next_id())

class TelemetryPacket:

  @staticmethod
  def create_from_json(json_data):
    data = json.loads(json_data)
    callsign = None
    timestamp = None
    position = None
    velocity = None
    acceleration = None
    def parse_callsign(callsign):
      if callsign is not None:
        return str(callsign).strip()
      return CallsignGenerator.get_new()
    def parse_timestamp(timestamp):
      try:
        return float(timestamp)
      except:
        return time.time()
    def parse_position(position):
      try:
        if len(position) != 3:
          return None
        return map(float, position)
      except:
        return None
    def parse_velocity(velocity):
      try:
        if len(velocity) != 3:
          return None
        return map(float, velocity)
      except:
        return None
    def parse_acceleration(acceleration):
      try:
        if len(acceleration) != 3:
          return None
        return map(float, acceleration)
      except:
        return None
    callsign = parse_callsign(data[0])
    timestamp = parse_timestamp(data[1])
    position = parse_position(data[2])
    velocity = parse_velocity(data[3])
    acceleration = parse_acceleration(data[4])
    return TelemetryPacket(callsign=callsign, timestamp=timestamp, position=position, velocity=velocity, acceleration=acceleration)

  def __init__(self, callsign=None, timestamp=None, position=None, velocity=None, acceleration=None):
    self.callsign = callsign
    self.timestamp = timestamp
    self.position = position
    self.velocity = velocity
    self.acceleration = acceleration

  def encode(self):
    return json.dumps([self.callsign, self.timestamp, self.position, self.velocity, self.acceleration])
