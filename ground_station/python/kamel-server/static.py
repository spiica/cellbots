import mimetypes
import os
import os.path
import webapp2

STATIC_DIR = 'static'
KML_MIME = 'application/vnd.google-earth.kml+xml'
XML_MIME = 'text/xml'
PLAIN_MIME = 'text/plain'

class ListHandler(webapp2.RequestHandler):

  def get(self):
    self.response.out.write(self.request.headers)
    self.response.out.write('<ul>\n')
    for name in os.listdir(STATIC_DIR):
      self.response.out.write(
          '<li><a href="%s">%s</a></li>\n' % (
              os.path.join('/', STATIC_DIR, name),
              name))
    self.response.out.write('</ul>\n')

class StaticHandler(webapp2.RequestHandler):

  def get(self):
    path = os.path.join(STATIC_DIR, os.path.basename(self.request.path))
    if os.path.exists(path):
      (mime_type, _) = mimetypes.guess_type(path)
      self.response.headers['Content-Type'] = XML_MIME if path.endswith('.kml') else mime_type
      f = open(path, 'r')
      data = f.read()
      f.close()
      self.response.write(data)
