import time
import webapp2

from datetime import datetime


from aprs import AprsStorage
from tracking import TrackingManager
from mock_telemetry import MockTelemetry

from lxml import etree
from pykml.factory import KML_ElementMaker as KML
from pykml.factory import ATOM_ElementMaker as ATOM
from pykml.factory import GX_ElementMaker as GX

KML_MIME = 'application/vnd.google-earth.kml+xml'
XML_MIME = 'text/xml'

class KmlStyle:

  def __init__(self, id_, styles):
    self.id = id_
    self.styles = styles

  def url(self):
    return '#'+self.id

  def append_to(self, element):
    for style in self.styles:
      element.append(style)

  @staticmethod
  def create_style(id_, color, width, scale, icon):
    style = KML.Style(id=id_)
    if scale is not None:
      icon_style = KML.IconStyle(KML.scale(scale))
      if icon is not None:
        icon_style.append(KML.Icon(KML.href(icon)))
      style.append(icon_style)
    if (color is not None) and (width is not None):
      line_style = KML.LineStyle(KML.color(color), KML.width(width))
      style.append(line_style)
    return KmlStyle(id_, [style])

  @staticmethod
  def create_style_map(id_, n_style, h_style):
    style = \
      KML.StyleMap(
        KML.Pair(
          KML.key('normal'),
          KML.styleUrl('#'+n_style.id),
        ),
        KML.Pair(
          KML.key('highlight'),
          KML.styleUrl('#'+h_style.id),
        ),
        id=id_,
      )
    return KmlStyle(id_, n_style.styles+h_style.styles+[style])

class KmlStyleUtils:

  STYLE_PREFIX = 'style'
  DEFAULT_STYLE = 0
  CALLSIGN_TO_STYLE = {
    'KC1C-4': 2,
    'KC1C-5': 3,
    'KJ6ORU': 1,
    'Mock1': 2,
    'Mock0': 3,
  }
  CALLSIGN_INFO = {
    'KC1C-4': 'sustainer',
    'KC1C-5': 'booster',
    'KJ6ORU': 'payload',
    'KI6NKO-10': 'balloon primary',
    'KC9NZJ-2': 'balloon backup',
    'KC9NZJ-7': 'Tom',
    'KC1C': 'Casey',
    'Mock1': 'sustainer',
    'Mock0': 'booster',
  }
  TOP_ALTITUDES_CALLSIGNS = ['KC1C-4', 'KC1C-5', 'KJ6ORU']
  # , 'Mock0', 'Mock1'

  num_styles = 4
  n_colors = ['9900fffff', '9900ff00', '99ff0000', '990000ff']
  h_colors = ['ff00fffff', 'ff00ff00', 'ffff0000', 'ff0000ff']
  n_widths = ['2', '4', '6', '6']
  h_widths = ['4', '6', '8', '8']
  n_scales = ['1', '1', '1', '1']
  h_scales = ['1.2', '1.2', '1.2', '1.2']
  icons = ['/static/red_circle.png', '/static/track-0.png', '/static/track-0.png', '/static/track-0.png']
  #models = [None, None, None, None]

  @classmethod
  def get_style_url_for_callsign(cls, callsign):
    index = cls.get_style_index_for_callsign(callsign)
    return '#'+cls.get_style_id_for_index(index)

  #@classmethod
  #def get_model_url_for_callsign(cls, callsign, base_url):
  #  index = cls.get_style_index_for_callsign(callsign)
  #  model = cls.models[index]
  #  if model is None:
  #    return None
  #  return base_url+model

  @classmethod
  def get_callsign_info(cls, callsign):
    if callsign in cls.CALLSIGN_INFO:
      info = ' ['+cls.CALLSIGN_INFO[callsign]+']'
    else:
      info = ''
    return info

  @classmethod
  def get_style_index_for_callsign(cls, callsign):
    if callsign in cls.CALLSIGN_TO_STYLE:
      index = cls.CALLSIGN_TO_STYLE[callsign]
    else:
      index = cls.DEFAULT_STYLE
    return index

  @classmethod
  def get_style_id_for_index(cls, index):
    return cls.STYLE_PREFIX+str(index)

  @classmethod
  def append_all_styles_to(cls, base_url, doc):
    for style in cls._get_all_styles(base_url):
      style.append_to(doc)

  @classmethod
  def _get_all_styles(cls, base_url):
    styles = []
    for index in range(cls.num_styles):
      styles.append(cls._get_style(base_url, index))
    return styles

  @classmethod
  def _get_style(cls, base_url, index):
    id_ = cls.get_style_id_for_index(index)
    icon = base_url+cls.icons[index] if cls.icons[index] is not None else None
    style = KmlStyle.create_style_map(
      id_,
      KmlStyle.create_style(id_+'n', cls.n_colors[index], cls.n_widths[index], cls.n_scales[index], icon),
      KmlStyle.create_style(id_+'h', cls.h_colors[index], cls.h_widths[index], cls.h_scales[index], icon))
    return style

class KmlTelemetryPacket:

  def __init__(self, packet):
    self.packet = packet

  def has_timestamp(self):
    return self.packet.timestamp is not None

  def has_position(self):
    return self.packet.position is not None

  def get_nice_timestamp(self):
    try:
      return datetime.utcfromtimestamp(self.packet.timestamp).isoformat()
    except:
      return '['+str(self.packet.timestamp)+']'

  def get_nice_position(self):
    position = self.packet.position
    # Format for <gx:coord>: longitude latitude altitude
    return '%f %f %f' % (position[1], position[0], position[2])

  def get_geo_url(self):
    position = self.packet.position
    geo_url = 'http://maps.google.com/maps?q=loc:%.5f%%2c%.5f' % (position[0], position[1])
    return geo_url

  def append_telemetry_to(self, element):
    if self.has_timestamp():
      element.append(KML.when(self.get_nice_timestamp()))
    if self.has_position():
      if self.packet.position[2] != 110001:
        element.append(GX.coord(self.get_nice_position()))

class KmlPlacemark:

  DOC_ID = 'kamel_doc'
  DUMMY_PM_ID = 'dummy_placemark'

  def __init__(self, tpm):
    self.tpm = tpm
    self.callsign = tpm.callsign
    self.pm_id = 'placemark'+str(tpm.id)
    self.track_id = self.pm_id+'_track'
    self.packet_index = 0
    self.last_packet = None
    self.is_new = True

  def has_next_packet(self):
    return self.packet_index < len(self.tpm.packets)

  def next_packet(self):
    return KmlTelemetryPacket(self.tpm.packets[self.packet_index])

  def get_next_packet(self):
    if not self.has_next_packet():
      return None
    self.last_packet = self.next_packet()
    self.packet_index += 1
    return self.last_packet

  def get_description(self):
    description = '<strong>%s</strong><br><br>' % (self.callsign)
    if self.last_packet is not None:
      if self.last_packet.has_timestamp():
        description += 'Last packet received at: %s<br>' % (self.last_packet.get_nice_timestamp())
      if self.last_packet.has_position():
        description += 'Location: %s [<a href="%s">map</a>]</a><br>' % (self.last_packet.get_nice_position(), self.last_packet.get_geo_url())
    return description

  def get_placemark(self, style_url=None, model_url=None):
    label = self.callsign+KmlStyleUtils.get_callsign_info(self.callsign)
    track = self.get_track(model_url)
    placemark = KML.Placemark(id=self.pm_id)
    placemark.append(KML.name(label))
    placemark.append(KML.description(self.get_description()))
    if style_url is not None:
      placemark.append(KML.styleUrl(style_url))
    placemark.append(track)
    return placemark

  def get_track(self, model_url=None):
    track = GX.Track(id=self.track_id)
    track.append(GX.altitudeMode('absolute'))
    while self.has_next_packet():
      self.get_next_packet().append_telemetry_to(track)      
    #if model_url is not None:
    #  track.append(KML.Model(KML.Link(KML.href(model_url)), KML.Orientation(KML.heading(180))))
    return track

  def generate_update(self, change=None, create=None, delete=None):
    placemark = GX.Placemark(targetId=self.pm_id)
    track = GX.Track(targetId=self.track_id)
    while self.has_next_packet():
      placemark.append(KML.description(self.get_description()))
      self.get_next_packet().append_telemetry_to(track)
    if placemark.countchildren() > 0:
      change.append(placemark)
    if track.countchildren() > 0:
      create.append(track)

class GoogleEarthSession:

  _sessions = dict()
  _next_sid = 1

  # (longitude, latitude, altitude, heading, tilt, range)
  CAMERA = (-119.122438, 40.853570, 2000, 0, 70, 20000)

  @classmethod
  def get(cls, sid = 0):
    if sid in cls._sessions:
      return cls._sessions[sid]
    elif sid == 0:
      session = GoogleEarthSession(cls._next_sid)
      cls._sessions[cls._next_sid] = session
      cls._next_sid += 1
      return session
    else:
      return None

  def __init__(self, sid):
    self.sid = sid
    self.camera = self.CAMERA
    self.styles = {}
    self.placemarks = {}

  def update_placemarks(self):
    for id_, pm in TrackingManager.get().placemarks.items():
      if id_ not in self.placemarks:
        self.placemarks[id_] = KmlPlacemark(pm)

  def get_look_at(self):
    return \
      KML.LookAt(
        KML.longitude(self.camera[0]),
        KML.latitude(self.camera[1]),
        KML.altitude(self.camera[2]),
        KML.heading(self.camera[3]),
        KML.tilt(self.camera[4]),
        KML.range(self.camera[5]),
      )

  def get_look_at_link(self, path, refresh = 1):
    return \
      KML.NetworkLink(
        KML.Link(
          KML.href(path),
          KML.refreshMode('onInterval'),
          KML.refreshInterval(str(refresh)),
        ),
      )

  def init_kml(self, request, response):
    doc = KML.Document(id=KmlPlacemark.DOC_ID)
    KmlStyleUtils.append_all_styles_to(request.host_url, doc)
    #doc.append(KML.Placemark(self.get_look_at(), id=KmlPlacemark.DUMMY_PM_ID))
    #doc.append(self.get_network_link(request.path_url + '?sid=' + str(self.sid)))
    for id_, kpm in self.placemarks.items():
      if kpm.is_new:
        style_url = KmlStyleUtils.get_style_url_for_callsign(kpm.callsign)
        model_url = None # KmlStyleUtils.get_model_url_for_callsign(kpm.callsign, request.host_url)
        placemark = kpm.get_placemark(style_url, model_url)
        doc.append(placemark)
        kpm.is_new = False
    return KML.kml(doc)

  def update_kml(self, request, response):
    change = KML.Change()
    create = KML.Create()
    delete = KML.Delete()
    doc = KML.Document(targetId=KmlPlacemark.DOC_ID)
    for id_, kpm in self.placemarks.items():
      if kpm.is_new:
        style_url = KmlStyleUtils.get_style_url_for_callsign(kpm.callsign)
        model_url = None # KmlStyleUtils.get_model_url_for_callsign(kpm.callsign, request.host_url)
        placemark = kpm.get_placemark(style_url, model_url)
        doc.append(placemark)
        kpm.is_new = False
      else:
        kpm.generate_update(change = change, create = create, delete = delete)
    if doc.countchildren() > 0:
      create.append(doc)
    update = KML.Update(KML.targetHref(request.path_url))
    if change.countchildren() > 0:
      update.append(change)
    if create.countchildren() > 0:
      update.append(create)
    if delete.countchildren() > 0:
      update.append(delete)
    network_link_control = KML.NetworkLinkControl(update)
    return KML.kml(network_link_control)

  def serve_kml(self, request, response):
    self.update_placemarks()
    if 'sid' not in request.params:
      doc = self.init_kml(request, response)
    else:
      doc = self.update_kml(request, response)
    response.out.write(etree.tostring(etree.ElementTree(doc), pretty_print=True))

class KmlGeneratorHandler(webapp2.RequestHandler):

  def get(self):
    AprsStorage.get()
    MockTelemetry.inject_packets()
    sid = int(self.request.get('sid', 0))
    session = GoogleEarthSession.get(sid)
    if session is None:
      self.redirect(self.request.path_url)
      return
    self.response.headers['Content-Type'] = XML_MIME
    session.serve_kml(self.request, self.response)

class TrackerMiscHandler(webapp2.RequestHandler):

  def get(self):
    AprsStorage.get()
    MockTelemetry.inject_packets()
    top_altitudes = map(TrackingManager.get().get_top_altitude, KmlStyleUtils.TOP_ALTITUDES_CALLSIGNS)
    if self.request.path == '/alt/v':
      for callsign, top_altitude in zip(KmlStyleUtils.TOP_ALTITUDES_CALLSIGNS, top_altitudes):
        label = callsign+KmlStyleUtils.get_callsign_info(callsign)
        self.response.out.write('%f %f %s<br>\n' % (top_altitude, 3.281 * top_altitude, label))
    elif self.request.path == '/trk/':
      self.response.out.write('<table cellpadding="0" cellspacing="15">\n')
      self.response.out.write('<tr><th align="left">Callsign</th><th align="left">Last reported position</th>\n')
      for id_, tpm in TrackingManager.get().placemarks.items():
        callsign = tpm.callsign
        last_packet = tpm.last_packet
        if (last_packet is not None) and (last_packet.position is not None):
          position = last_packet.position
          zoom_level = 19
          pos_url = 'geo:%.5f,%.5f?z=%d&q=loc:%.5f%%2c%.5f' % (position[0], position[1], 20, position[0], position[1])
          label = '<a href="%s">%s</a>%s' % (pos_url, callsign, KmlStyleUtils.get_callsign_info(callsign))
          pos_info = 'lat=%10.5f lon=%10.5f alt=%8.1f' % (position[0], position[1], position[2])
        else:
          label = callsign+KmlStyleUtils.get_callsign_info(callsign)
          pos_info = 'n/a'
        self.response.out.write('<tr><td>%s</td><td>%s</td></tr>\n' % (label, pos_info))
      self.response.out.write('</table>\n')
    else:
      self.response.out.write(int(max(top_altitudes)))
