class TrackingPlacemark:
  def __init__(self, id_, callsign):
    self.id = id_
    self.callsign = callsign
    self.packets = []
    self.last_packet = None

  def add_packet(self, packet):
    self.packets.append(packet)
    self.last_packet = packet

class CallsignMapper:

  def __init__(self):
    self.next_id = 1
    self.callsign_to_id = {}

  def get_new_id(self):
    id_ = self.next_id
    self.next_id += 1
    return id_

  def get_id_for_callsign(self, callsign=None):
    if callsign is None:
      return self.get_new_id()
    if callsign not in self.callsign_to_id:
      self.callsign_to_id[callsign] = self.get_new_id()
    return self.callsign_to_id[callsign]  

class TrackingManager:

  _instance = None

  @classmethod
  def get(cls):
    if cls._instance == None:
      cls._instance = cls()
    return cls._instance

  def __init__(self):
    self.callsign_mapper = CallsignMapper()
    self.placemarks = {}
    self.top_altitudes = {}

  def add_packet(self, packet):
    id_ = self.callsign_mapper.get_id_for_callsign(packet.callsign)
    if id_ not in self.placemarks:
      self.placemarks[id_] = TrackingPlacemark(id_, packet.callsign)
      self.top_altitudes[id_] = 0
    self.placemarks[id_].add_packet(packet)
    alt = packet.position[2] if packet.position is not None else 0
    if (alt != 110001) and (self.top_altitudes[id_] < alt):
      self.top_altitudes[id_] = alt

  def get_top_altitude(self, callsign):
    id_ = self.callsign_mapper.get_id_for_callsign(callsign)
    if id_ not in self.top_altitudes:
      return 0
    return int(self.top_altitudes[id_])
