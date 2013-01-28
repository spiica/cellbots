import os.path
import json
import webapp2

from telemetry import TelemetryPacket
from tracking import TrackingManager

class AprsStorage:

  APRS_DATABASE = 'aprs.txt'

  _instance = None

  @classmethod
  def get(cls):
    if cls._instance == None:
      cls._instance = cls()
    return cls._instance

  def __init__(self):
    self.data = []
    #self.reload_packets()

  def reload_packets(self):
    self.data = []
    for raw_packet in self._read_from_data_store():
      self._post_packet(raw_packet)

  def post_packet(self, raw_packet):
    self._post_packet(raw_packet)
    self._append_to_data_store(raw_packet)

  def _post_packet(self, raw_packet):
    TrackingManager.get().add_packet(TelemetryPacket.create_from_json(raw_packet))
    self.data.append(raw_packet)

  @staticmethod
  def _read_from_data_store(path = APRS_DATABASE):
    data = []
    try:
      f = open(path, 'r')
      def strip_line(line):
        return line.strip()
      data = map(strip_line, f.readlines())
      f.close()
    except:
      pass
    return data

  @staticmethod
  def _append_to_data_store(raw_packet, path = APRS_DATABASE):
    try:
      f = open(path, 'a')
      f.write(raw_packet+'\n')
      f.close()
    except:
      pass

class AprsHandler(webapp2.RequestHandler):

  def get(self):
    data = AprsStorage.get().data
    if len(data):
      self.response.out.write('<br>\n'.join(reversed(data)))
    else:
      self.response.out.write('no data :(')

  def post(self):
    AprsStorage.get().post_packet(self.request.body)
    self.response.out.write('OK')
