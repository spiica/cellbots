#!/usr/bin/perl

# TODO: 
# 1. Should also be pulling data from aprs-is, noam.aprs2.net:14580
# 2. when *.kml is passed in parse out the position/name data 
#       ie. prediction from habhub

use Ham::APRS::FAP;
use Device::SerialPort;
use Switch;

# Some globals.
$ECHO_INPUT  = 1; # print all sentences to be processed
$ECHO_SERIAL = 0; # print all tty input bytes
$ECHO_GPS    = 0; # print all GPS sentences
$ECHO_APRS   = 0; # print all APRS packets

$HOSTNAME = "Yggi";

%data = ();

# Serial port device path, and settings.
# @serial = ("/dev/serial/by-id/usb-Qualcomm_Incorporated_Qualcomm_Gobi_2000-if01-port0", 8, "none", 1, 9600);
@serial = ("/dev/ttyUSB2", 8, "none", 1, 9600);

# URL for the kml server.
$kml_server = "http://192.168.0.153:8080/aprs/";

#####################
# Setup and start the Serial connection

my $port = Device::SerialPort->new($serial[0]);

if (! defined $port) {
  print "could not connect to port [".join(":", @serial)."]\n";
}
else {
  $port->databits($serial[1]);
  $port->baudrate($serial[4]);
  $port->parity($serial[2]);
  $port->stopbits($serial[3]);
  print "Serial port opened [".join(":", @serial)."]\n";
}

#####################
# Parse the contents of any specified files. 

foreach $file (@ARGV) {
  print "reading file $file\n";
  open FILE, "<$file";
  foreach $line (<FILE>) {
    processSentence($line);
  }
  close FILE;
}


#####################
# Endlessly monitor the serial port for gps and aprs info.

if (defined $port) {
  print "Starting APRS/GPS monitor\n";
  while(1) {
    my $byte=$port->read(1);

    if ($byte eq "\r" ) {
      $byte = "\n";
    }

    if ($ECHO_SERIAL) {
      print $byte;
    }

    if ($byte eq "\n") {
      processSentence($sentence);
      $sentence = "";
    }
    else {
      $sentence .= $byte;
    }
  }
}

#####################
# All Done.

print "Exiting.\n";


# Clean up the received data, and send to the right parser.
sub processSentence {
   my ($sentence) = @_;

   # remove whitespace
   $sentence =~ s/^\s+//;
   $sentence =~ s/\s+$//;

   if ($sentence eq "") {
     return; # Empty sentence.
   }

   if ($ECHO_INPUT) {
     print $sentence."\n";
   }

   if ( $sentence =~ /^\$/ ) {
     # Starts with a '$' must be GPS
     if ($ECHO_GPS) {
       print "$sentence\n";
     }
     parseGPS($sentence);
   }
   else {
     # everything else is an APRS packet, obviously.
     if ($ECHO_APRS) {
       print "$sentence\n";
     }
     parseAPRS($sentence);
   }
}

# Parse and report APRS packet.
# example fields ...
# symbolcode: >
# body: !3721.68N/12200.99W>325/005/A=000131
# posresolution: 18.52
# speed: 9.26
# latitude: 37.3613333333333
# origpacket: N6ZFJ-1>APT312,W6CX-3,N6ZX-3,WIDE2*:!3721.68N/12200.99W>325/005/A=000131
# srccallsign: N6ZFJ-1
# altitude: 39.9288
# course: 325
# symboltable: /
# longitude: -122.0165
# dstcallsign: APT312
# digipeaters: ARRAY(0x16c7cd0)
# format: uncompressed
# messaging: 0
# posambiguity: 0
# type: location
# header: N6ZFJ-1>APT312,W6CX-3,N6ZX-3,WIDE2*

sub parseAPRS {
  my ($aprspacket) = @_;
  my %packetdata;
  print "$aprspacket\n";
 my $retval = Ham::APRS::FAP::parseaprs($aprspacket, \%packetdata);
 if ($retval == 1) {
   if ($ECHO_APRS) {
     printAPRS(\%packetdata);
   }
   positionInfo(
		$packetdata{'srccallsign'},
		$packetdata{'latitude'},
		$packetdata{'longitude'},
		$packetdata{'altitude'},
		$packetdata{'speed'},
		$packetdata{'course'},
		-1
	       );

  } else {
        warn "Parsing failed: $packetdata{resultmsg} ($packetdata{resultcode})\n";
  }
}

sub printAPRS {
  my ($hashref) = @_;
  print ">>> APRS PACKET\n";
  while (my ($key, $value) = each(%{$hashref})) {
    print "$key: $value\n";
  }
  print "\n\n";
}

# Parse and report GPS packet.
sub parseGPS {
  my ($gps_sentence) = @_;

  if ($gps_sentence =~ /^\$([A-Z]+)/)  {
     my ($type) = ($1);
     switch ($type) {
        case "GPGGA" { parseGPGGA($gps_sentence); }
        # case "GPRMC" { parseGPRMC($gps_sentence); }
     }
  }
}

sub parseGPGGA {
  my @gpgga = split ",",$_[0];
  positionInfo($HOSTNAME, 
	       $gpgga[2].$gpgga[3],  # LAT
	       $gpgga[4].$gpgga[5],  # LONG
	       $gpgga[9].$gpgga[10], # ALT
               -1, # speed            
               -1, # heading
               $gpgga[1]             # UTC
	      );
}

sub parseGPRMC {
  my @gprmc = split ",", $_[0];
  if ($gprmc[2] ne "A") {
    # data invalid indicated.
    return;
  }
  positionInfo($HOSTNAME, 
               $gprmc[3].$gprmc[4], # LAT
	       $gprmc[5].$gprmc[6], # LONG
	       -1,                  # ALT
	       $gprmc[7]."knots",   # SPEED
	       $gprmc[8]."degrees", # HEADING
	       $gprmc[1]            # UTC
	       );
}

# New position info reported, add to the local DB then update kml.
# Todo: -1 is passed in when value is not reported, deal with that.
sub positionInfo {
  my ($id, $lat, $long, $alt, $speed, $heading, $utc) = @_;
  print localtime(time)." > Got report from $id [ $lat : $long : $alt ] \n";
  if (!defined $data{$id}) {
    $data{$id} = [];
  }
  my @report = ($utc, $lat, $long, $alt, $speed, $heading);
  push @{$data{$id}} , \@report;
  
  # ahem, this might be wasteful, consider a RESTful server intermediary that
  # generates the kml on demand instead
  # updateKML();

  # TODO(arshan): passing local current time, instead of UTC as reported
  PostTo($kml_server, $id, time(), $lat, $long, $alt, $speed, $heading);
}

# Post appropriately formatted message to the kml-server.
sub PostTo {
  my ($url, $id, $utc, $lat, $long, $alt, $speed, $heading) = @_;
  my $json = "[\"$id\", \"$utc\", [\"$lat\", \"$long\", \"$alt\"], [\"$speed\", \"$heading\", \"0\"], [\"0\",\"0\",\"0\"]]"; 
  my $cmd = "wget --post-data '$json' -o /dev/null $url";
  print "$cmd\n";
  `$cmd`;
}

# Use local DB to generate new kml file, atomic softlink of new kml file.
sub updateKML {
  open KML, ">monitor.kml";
  foreach $callsign ( keys %data ) {
    print KML "\n## $callsign \n";
    foreach $entry ( @{$data{$callsign}} ) {
      my ($utc, $lat, $long, $alt, $speed, $heading) = @{$entry};
      print KML "$utc : $lat X $long\n";
    }
  }
  close KML;
}
