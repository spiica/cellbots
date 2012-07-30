from google.appengine.ext import db
from telemetry_packet import TelemetryPacket

import cgi
import webapp2

class Track(db.Model):
  # Each name identifies a new track.
  name = db.StringProperty()

  # The password is used as a basic security measure so we don't get garbage.
  # A request must contain both the name and password for it to get accepted
  # in to the database.
  password = db.StringProperty()

  # If true, this track will no longer accept data.
  finished = db.BooleanProperty()

  timestamp = db.DateTimeProperty(auto_now_add=True)


class TrackEntry(db.Model):
  track = db.ReferenceProperty(Track)

  # contents are a JSON-encoded TelemetryPacket
  json_payload = db.StringProperty()

  # The raw APRS frame in a base91 string.
  aprs_payload = db.StringProperty()

  timestamp = db.DateTimeProperty(auto_now_add=True)


class MainPage(webapp2.RequestHandler):
  def get(self):
      self.response.headers['Content-Type'] = 'text/plain'
      self.response.out.write('Hello, webapp World!')


class Payload(webapp2.RequestHandler):
  def post(self):
    self.response.headers['Content-Type'] = 'text/plain'

    name = self.request.get('name')
    password = self.request.get('password')
    payload = self.request.get('payload')

    if not name or not password or not payload:
      self.response.out.write('Invalid Parameters\n')
      self.response.set_status(400)
      return

    # Ensure this is a valid trakc.
    track = Track.gql("WHERE NAME IS :1 AND PASSWORD IS :2", name, password)
    if not track:
      self.response.out.write('Invalid Parameters\n')
      self.response.set_status(400)
      return

    self.response.out.write('OK')

app = webapp2.WSGIApplication([('/', MainPage),
                               ('/payload', Payload)],
                              debug=True)
