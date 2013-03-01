import re
import time
import webapp2

from datetime import datetime

from telemetry import TelemetryPacket
from tracking import TrackingManager

#MOCK_FILES = ['static/Beeline-154.kml', 'static/Beeline-157.kml']
MOCK_FILES = ['static/Beeline-157.kml']
LAUNCH_TIME = datetime(2012, 9, 11, 20, 38, 50)
DELTA_LONG = -1.673048
DELTA_LAT = -2.904241

class MockTelemetry:

  _instance = None
  _enabled = False

  @classmethod
  def enable_mock_telemetry(cls):
    prev = cls._enabled
    cls._enabled = True
    return prev

  @classmethod
  def inject_packets(cls):
    if cls._enabled:
      cls.get()._inject_packets()

  @classmethod
  def get(cls):
    if cls._instance == None:
      cls._instance = cls.create()
    return cls._instance

  @classmethod
  def create(cls, mock_files=MOCK_FILES, launch_time=LAUNCH_TIME):
    if launch_time is not None:
      time_offset = time.time()-cls._datetime_to_timestamp(launch_time)
    else:
      time_offset = None
    def get_mock(index):
      return cls._parse_mock_file(MOCK_FILES[index], 'Mock'+str(index), time_offset)
    tracks = [get_mock(index) for index in range(len(mock_files))]
    return cls(tracks)

  @classmethod
  def _parse_mock_file(cls, path, callsign, time_offset=None):
    data = cls._read_file(path)
    capture_date = cls._parse_capture_date(data)
    raw_gps_data = cls._parse_raw_gps_data(data)
    packets = []
    for rec in raw_gps_data:
      dt = datetime.strptime(capture_date+' '+rec[5], '%m/%d/%Y %H:%M:%S')
      timestamp = cls._datetime_to_timestamp(dt)
      if time_offset is not None:
        timestamp += time_offset
      position = (float(rec[1])+DELTA_LAT, float(rec[0])+DELTA_LONG, float(rec[2]))
      packets.append(TelemetryPacket(callsign=callsign, timestamp=timestamp, position=position))
    return packets

  @staticmethod
  def _datetime_to_timestamp(dt):
    return time.mktime(dt.utctimetuple())

  @staticmethod
  def _read_file(path):
    f = open(path, 'r')
    data = f.read()
    f.close()
    return data

  @staticmethod
  def _parse_capture_date(data):
    # Example input: <!-- Capture Date: 09/11/2012 -->
    match = re.search(r'<!-- Capture Date: (.*?) -->', data)
    if match is None:
      return ''
    return match.groups()[0]

  @staticmethod
  def _parse_raw_gps_data(data):
    # Example input: -119.125642,40.849728,01185 <!-- 15 sats:11 UTC 03:40:19 -->
    return re.findall(r'(.*?),(.*?),(.*?) <!-- ([0-9]+) sats:([0-9]+) UTC ([^ ]+) -->', data)

  def __init__(self, tracks):
    self.tracks = tracks
    self.indexes = [0] * len(tracks)

  def _inject_packets(self, tracking_manager=None):
    if tracking_manager is None:
      tracking_manager = TrackingManager.get()
    for i in range(len(self.tracks)):
      index = self.indexes[i]
      track = self.tracks[i]
      while index < len(track):
        packet = track[index]
        if packet.timestamp > time.time():
          break
        tracking_manager.add_packet(packet)
        index += 1
      self.indexes[i] = index

class MockTelemetryEnablerHandler(webapp2.RequestHandler):

  def get(self):
    prev = MockTelemetry.enable_mock_telemetry()
    self.response.out.write('Mock telemetry ENABLED. Previous status was: '+str(prev)+'.')
