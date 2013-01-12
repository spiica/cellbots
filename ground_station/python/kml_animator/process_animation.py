#!/usr/bin/python
#-119.160238,40.855990,05374 <!-- 16110 sats:8 UTC 20:41:00 --> 
index_page = '''
<html>
   <head>
      <title>X20 Rocket Launch</title>
      <script src="https://www.google.com/jsapi"> </script> 
      <script src="http://earth-api-samples.googlecode.com/svn/trunk/lib/kmldomwalk.js" type="text/javascript"> </script>
      <script type="text/javascript">

         var ge;
         var tour;
         google.load("earth", "1");

         function init() {
            google.earth.createInstance('map3d', initCB, failureCB);
         }

         function initCB(instance) {
            ge = instance;
            ge.getWindow().setVisibility(true);
            ge.getNavigationControl().setVisibility(ge.VISIBILITY_SHOW);

            var href = '%s/x20.kml';
            google.earth.fetchKml(ge, href, fetchCallback);

            function fetchCallback(fetchedKml) {
               // Alert if no KML was found at the specified URL.
               if (!fetchedKml) {
                  setTimeout(function() {
                     alert('Bad or null KML');
                  }, 0);
                  return;
               }

               // Add the fetched KML into this Earth instance.
               ge.getFeatures().appendChild(fetchedKml);

               // Walk through the KML to find the tour object; assign to variable 'tour.'
               walkKmlDom(fetchedKml, function() {
                  if (this.getType() == 'KmlTour') {
                     tour = this;
                     return false;
                  }
               });
            }
         }

         function failureCB(errorCode) {
         }

         // Tour control functions.
         function enterTour() {
            if (!tour) {
               alert('No tour found!');
               return;
            }
            ge.getTourPlayer().setTour(tour);
            ge.getTourPlayer().play();
         }
         function pauseTour() {
            ge.getTourPlayer().pause();
         }
         function resetTour() {
            ge.getTourPlayer().reset();
         }
         function exitTour() {
            ge.getTourPlayer().setTour(null);
         }

         google.setOnLoadCallback(init);
      </script>
   </head>
   <body>

      <div id="map3d" style="height: 700px; width: 1200px;"></div>
      <div id ="controls">
         <input type="button" onclick="enterTour()" value="Travel To Black Rock Desert and Launch X20 Rocket"/>
         <input type="button" onclick="resetTour()" value="Reset"/>
      </div>

   </body>
</html>'''


#HEADER DEFINITION
header = '''<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2"
 xmlns:gx="http://www.google.com/kml/ext/2.2"
 xmlns:kml="http://www.opengis.net/kml/2.2"
 xmlns:atom="http://www.w3.org/2005/Atom">
<Document>

  <!-- HUNDRED K LINE! -->
  <Style id="hundredk">
    <LineStyle>
      <color>7700ff00</color>
      <Width>100</Width>
    </LineStyle>
  </Style>
  <Placemark>
    <styleUrl>#hundredk</styleUrl>
    <LineString id="hundredk">
      <extrude>0</extrude>
      <tessellate>1</tessellate>
      <altitudeMode>absolute</altitudeMode>
      <coordinates>
	-119.112413,40.853570,31669
	-119.515,40.903570,31669
      </coordinates>
    </LineString>
  </Placemark>


 <Placemark id="hundredkballoon">
     <name>100K feet above launch altitude</name>
    <Style>
       <IconStyle>
         <Icon>
         </Icon>
       </IconStyle>
     </Style>
     <Point>
       <gx:altitudeMode>absolute</gx:altitudeMode>
       <coordinates>-119.3,40.87,31669</coordinates>
     </Point>
 </Placemark>

 <Placemark id="achievementballoon">
    <Style>
       <IconStyle>
         <Icon>
         </Icon>
       </IconStyle>
       <BalloonStyle>
      <bgColor>ff444444</bgColor>
      <text><![CDATA[
      <font face="sans-serif" color="white" size="+3"><b>Achievement Unlocked:</b>&nbsp;Reached Carmack Micro Prize altitude!</font>
      ]]></text>
     </BalloonStyle>
     </Style>
     <description>
       Achievement Unlocked: Reached Carmack Micro Prize altitude!
     </description>
     <Point>
       <gx:altitudeMode>absolute</gx:altitudeMode>
       <coordinates>-119.3,40.87,31669</coordinates>
     </Point>
 </Placemark>

 <!-- WHOLEROCKET TRAIL -->
  <Style id="wholeplume">
    <LineStyle>
      <color>ff0077ff</color>
      <Width>10</Width>
    </LineStyle>
  </Style>

  <Placemark>
    <styleUrl>#wholeplume</styleUrl>
    <LineString id="wholeTrack">
      <extrude>0</extrude>
      <tessellate>1</tessellate>
      <altitudeMode>absolute</altitudeMode>
      <coordinates>
        %s
      </coordinates>
    </LineString>
  </Placemark>
  <!-- WHOLE MODEL -->
  <Placemark>
    <Model>
      <altitudeMode>absolute</altitudeMode>
      <Location id="wholeLocation">
          <longitude>%f</longitude>
          <latitude>%f</latitude>
          <altitude>%f</altitude>
      </Location>
      <Orientation id="wholeOrientation">>
        <heading>0</heading>
        <tilt>0</tilt>
        <roll>0</roll>
        </Orientation>
      <Scale id="wholeScale">
        <x>10</x>
        <y>10</y>
        <z>10</z>
      </Scale>
      <Link>
        <href>%s/whole.dae</href>
      </Link>
    </Model>
  </Placemark>

 <!-- SUSTAINERROCKET TRAIL -->
  <Style id="sustainerplume">
    <LineStyle>
      <color>ff0077ff</color>
      <Width>10</Width>
    </LineStyle>
  </Style>

  <Placemark>
    <styleUrl>#sustainerplume</styleUrl>
    <LineString id="sustainerTrack">
      <extrude>0</extrude>
      <tessellate>1</tessellate>
      <altitudeMode>absolute</altitudeMode>
      <coordinates>
        %s
      </coordinates>
    </LineString>
  </Placemark>
  <!-- SUSTAINER MODEL -->
  <Placemark>
    <Model>
      <altitudeMode>absolute</altitudeMode>
      <Location id="sustainerLocation">
          <longitude>%f</longitude>
          <latitude>%f</latitude>
          <altitude>%f</altitude>
      </Location>
      <Orientation id="sustainerOrientation">>
        <heading>0</heading>
        <tilt>0</tilt>
        <roll>0</roll>
        </Orientation>
      <Scale id="sustainerScale">
        <x>0</x>
        <y>0</y>
        <z>0</z>
      </Scale>
      <Link>
        <href>%s/sustainer.dae</href>
      </Link>
    </Model>
  </Placemark>



 <!-- BOOSTERROCKET TRAIL -->
  <Style id="boosterplume">
    <LineStyle>
      <color>ff0077ff</color>
      <Width>10</Width>
    </LineStyle>
  </Style>
  <Placemark>
    <styleUrl>#boosterplume</styleUrl>
    <LineString id="boosterTrack">
      <extrude>0</extrude>
      <tessellate>1</tessellate>
      <altitudeMode>absolute</altitudeMode>
      <coordinates>
        %s
      </coordinates>
    </LineString>
  </Placemark>
  <!-- BOOSTER MODEL -->
  <Placemark>
    <Model>
      <altitudeMode>absolute</altitudeMode>
      <Location id="boosterLocation">
          <longitude>%f</longitude>
          <latitude>%f</latitude>
          <altitude>%f</altitude>
      </Location>
      <Orientation id="boosterOrientation">>
        <heading>0</heading>
        <tilt>0</tilt>
        <roll>0</roll>
        </Orientation>
      <Scale id="boosterScale">
        <x>0</x>
        <y>0</y>
        <z>0</z>
      </Scale>
      <Link>
        <href>%s/booster.dae</href>
      </Link>
    </Model>
  </Placemark>

  <gx:Tour>
    <name>X20 Rocket Launch</name>
    <gx:Playlist>

      <!-- Fly to our start location -->
      <gx:FlyTo>
        <gx:duration>%d</gx:duration>
        <Camera>
          <longitude>%f</longitude>
          <latitude>%f</latitude>
          <altitude>%f</altitude>
          <heading>0</heading>
          <tilt>90</tilt>
          <roll>0</roll>
          <altitudeMode>absolute</altitudeMode>
        </Camera>
      </gx:FlyTo>'''

#### END OF HEADER

def vehicle_kml(name, duration, long, lat, alt, heading, tilt, roll, track_coord, balloon_actions, scale):
    vehicle_template = '''      
      <!-- Rocket --> 
      <gx:AnimatedUpdate>
        <gx:duration>%d</gx:duration>
        <Update>
          <targetHref></targetHref>
          <Change>
            <Location targetId="%sLocation">
              <longitude>%f</longitude>
              <latitude>%f</latitude>
              <altitude>%f</altitude>
            </Location>
           </Change>
           <Change>
            <Orientation targetId="%sOrientation">
              <heading>%f</heading>
              <tilt>%f</tilt>
              <roll>%f</roll>
              </Orientation>
          </Change> 
           <Change>
	     <LineString targetId="%sTrack">
	       <coordinates>
                %s
	       </coordinates>
	     </LineString>
          </Change>
          %s
          <Change>
            <Scale targetId="%sScale">
              <x>%d</x>
              <y>%d</y>
              <z>%d</z>
              </Scale>
          </Change> 
        </Update>
      </gx:AnimatedUpdate>
      '''
    return vehicle_template % (duration, name,long, lat, alt, name, heading, tilt, roll, name, track_coord, balloon_actions, name, scale, scale, scale)

def camera_kml(duration, long, lat, alt, heading, tilt, roll):
    camera_template = '''
      <!-- Camera -->
      <gx:FlyTo>
        <gx:duration>%d</gx:duration>
        <gx:flyToMode>smooth</gx:flyToMode>
        <Camera>
          <longitude>%f</longitude>
          <latitude>%f</latitude>
          <altitude>%f</altitude>
          <heading>%f</heading>
          <tilt>%f</tilt>
          <roll>%f</roll>
          <altitudeMode>absolute</altitudeMode>
        </Camera>
      </gx:FlyTo>
      '''
    return camera_template  % (duration, long, lat, alt, heading, tilt, roll)

tail = '''
      <!-- Final Camera Zoom-->
      <gx:FlyTo>
        <gx:duration>2</gx:duration>
        <gx:flyToMode>smooth</gx:flyToMode>
        <Camera>
          <longitude>-119.32</longitude>
          <latitude>39.75</latitude>
          <altitude>10000</altitude>
          <heading>0</heading>
          <tilt>90</tilt>
          <roll>0</roll>
          <altitudeMode>absolute</altitudeMode>
        </Camera>
      </gx:FlyTo>

    </gx:Playlist>
  </gx:Tour>
</Document>
</kml>
'''


import math
import sys

def distance(origin, destination):
    lat1, lon1 = origin
    lat2, lon2 = destination
    radius = 6371 # km

    dlat = math.radians(lat2-lat1)
    dlon = math.radians(lon2-lon1)
    a = math.sin(dlat/2) * math.sin(dlat/2) + math.cos(math.radians(lat1)) \
        * math.cos(math.radians(lat2)) * math.sin(dlon/2) * math.sin(dlon/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    d = radius * c
    return d

if sys.argv.__len__() == 1:
    print "must specify a serverroot - eg http://www.corp.google.com/~wdavies"
    sys.exit(1)

findex = open('index.html', 'w')
findex.write(index_page % sys.argv[1])
findex.close()

curr_time = 0
total_time = 0
log_time = 10
prev_long = 0
prev_lat = 0
prev_alt = 0
prev_degree = 0
fkml = open('x20.kml', 'w')
f = open('./input.kml', 'r')
coords = ""
balloon = ""
seperation = 0

sep_long = 0
sep_lat = 0
sep_alt = 0

for line in f:
     long, lat, alt, time = line.strip().split("\t")
     long = float(long)
     lat = float(lat)
     alt = float(alt)
     hr, mi, sec = map(int, time.split(":"))
     if curr_time == 0:
         coords = "%f,%f,%f\n" % (long, lat, alt)
         fkml.write(header % (coords, long, lat, alt, sys.argv[1], 
                              coords, long, lat, alt, sys.argv[1],  
                              coords, long, lat, alt, sys.argv[1],
                              5, (long - 0.0), (lat - 0.05),  alt ))
         start_time =  hr * 3600 + mi * 60 + sec
         prev_time =  hr * 3600 + mi * 60 + sec
         prev_long = long
         prev_lat = lat
         prev_alt = alt
     curr_time = hr * 3600 + mi * 60 + sec
     time_diff = curr_time - prev_time
     total_time = curr_time - start_time
     if alt == 33089:
         balloon = '''
           <Change>
            <Placemark targetId="achievementballoon">
              <gx:balloonVisibility>1</gx:balloonVisibility>
            </Placemark>
          </Change>'''
     else:
         balloon = '''<Change>
            <Placemark targetId="achievementballoon">
              <gx:balloonVisibility>0</gx:balloonVisibility>
            </Placemark>
          </Change>'''
     sys.stderr.write("time: %d, (%d), alt: %d\n" % (curr_time - start_time,  time_diff, alt))
     if time_diff >= 10 or alt == 33089 or alt == 7533:
         if alt == 7533.0 :
             sys.stderr.write("SEPERATION ADJUSTMENT!\n")
             long = -119.159620
             lat = 40.859520
             alt = 8279
             sep_long = long
             sep_lat = lat
             sep_alt = alt
             seperation = 1
         sys.stderr.write("DISPLAY: time: %d, (%d), alt: %d\n" % (curr_time - start_time,  time_diff, alt))
         horiz = distance([prev_lat, prev_long], [lat, long])
         vert =  (alt - prev_alt)/1000
         degree = math.degrees(math.atan(vert/horiz))
         coords = coords +  "%f,%f,%f\n" % (long, lat, alt)
         if seperation == 0:
             fkml.write(vehicle_kml("sustainer", log_time, long, lat, alt, 0, 0, 90-degree, coords, balloon, 0))
             fkml.write(vehicle_kml("booster", log_time, long, lat, alt, 0, 0, 90-degree, coords, balloon, 10))
             fkml.write(vehicle_kml("whole", log_time, long, lat, alt, 0, 0, 90-degree, coords, balloon, 10))
             fkml.write(camera_kml(log_time, (long - 0.0), (lat - 0.1),  alt*0.8, 0, 90, 0))
         if seperation == 1:
             fkml.write(vehicle_kml("sustainer", 1, long, lat, alt, 0, 0, 90-degree, coords, balloon, 10))
             fkml.write(vehicle_kml("booster", 1, sep_long, sep_lat, sep_alt, 0, 0, 90-degree, "", balloon, 10))
             fkml.write(vehicle_kml("whole", 1, long, lat, alt, 0, 0, 90-degree, "", balloon, 0))
             fkml.write(camera_kml(1, (long - 0.0), (lat - 0.1),  alt*0.8, 0, 90, 0))
         if seperation > 1:
             sep_alt = sep_alt * 0.35
             fkml.write(vehicle_kml("sustainer", log_time, long, lat, alt, 0, 0, 90-degree, coords, balloon, 10))
             fkml.write(vehicle_kml("booster", log_time, sep_long, sep_lat, sep_alt, 0, 0, 90-degree, "", balloon, 10))
             fkml.write(vehicle_kml("whole", log_time, long, lat, alt, 0, 0, 90-degree, "", balloon, 0))
             fkml.write(camera_kml(log_time, (long - 0.0), (lat - 0.3),  alt*0.8, 0, 90, 0))
         if seperation == 1:
             seperation = 2
         prev_time = curr_time
         prev_long = long
         prev_lat = lat
         prev_alt = alt
         prev_degree = degree
         if log_time > 1:
           log_time -= 1
fkml.write(tail)
fkml.close()
f.close()

