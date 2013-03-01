#!/usr/bin/env python
import webapp2

from aprs import AprsHandler
from earth import KmlGeneratorHandler
from earth import TrackerMiscHandler
from mock_telemetry import MockTelemetryEnablerHandler
from static import ListHandler
from static import StaticHandler

class MainHandler(webapp2.RequestHandler):
  def get(self):
    self.response.out.write('<br><h1 align="center"><a href="/kml/rocket.kml">February 2, 2013 - Android Rocket Launch</a></h1>')

app = webapp2.WSGIApplication([
    ('/', MainHandler),
    ('/aprs/.*', AprsHandler),
    ('/kml/.*', KmlGeneratorHandler),
    ('/alt/.*', TrackerMiscHandler),
    ('/trk/.*', TrackerMiscHandler),
    ('/mock/.*', MockTelemetryEnablerHandler),
    ('/lst/.*', ListHandler),
    ('/static/.*', StaticHandler),
    ], debug=True)
