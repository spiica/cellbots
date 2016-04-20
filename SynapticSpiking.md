# Synaptic Spiking Interface Project #

# Introduction #

Synaptic spiking is how biological systems communicate.  Millions of years of evolution have created this system of communication.  Understanding it is to understand engineering perfection.


# Overview of Electronic Interface for Reading Synaptic Spikes #

  1. Signal goes into the pins
  1. ignal is initially amplified by the Instrumentation amp 5x)
  1. Signal is then amplified again by another op amp
  1. Signal is then put through filters (filtering is needed to reduce noise, fluorescent lights for example)
  1. Signal is then amplified a third time by another op amp
  1. Signal is then fed to an audio out that sends +- 1 volt to represent the spikes

If the audio out jack is put into a computer the raw audio out can be converted from analog to a digital wav file!  From this wave file synaptic spikes can be studied.

# Details #

Parts Needed:
  * AD623N (5x Gain Instrumentation Amp) <br> <a href='http://www.analog.com/en/amplifiers-and-comparators/instrumentation-amplifiers/ad627/products/product.html'>http://www.analog.com/en/amplifiers-and-comparators/instrumentation-amplifiers/ad627/products/product.html</a>  <br> <a href='http://www.analog.com/static/imported-files/packages/PKG_PDF/PDIP(N)/N_8.pdf'>http://www.analog.com/static/imported-files/packages/PKG_PDF/PDIP(N)/N_8.pdf</a>
<ul><li>TLC2272344P  (213x Gain Amp (Bandpass 700Hz - 3000Hz)<br>
</li><li>HLM386MJ (200x Gain Audio Ampliﬁer)<br>
</li><li>Sewing needle for the electrode interface</li></ul>

Parts can be ordered here:<br>
<br>
<a href='http://cgi.ebay.com/AD623-Instrumentation-Amplifier_W0QQitemZ250440498203QQcmdZViewItemQQptZLH_DefaultDomain_0?hash=item3a4f6abc1b#ht_555wt_940'>http://cgi.ebay.com/AD623-Instrumentation-Amplifier_W0QQitemZ250440498203QQcmdZViewItemQQptZLH_DefaultDomain_0?hash=item3a4f6abc1b#ht_555wt_940</a>


<a href='http://cgi.ebay.com/AD627-Micro-power-Instrumentation-Amplifier-Offers-Supe_W0QQitemZ150420211813QQcmdZViewItemQQptZLH_DefaultDomain_0?hash=item2305be4865#ht_718wt_940'>http://cgi.ebay.com/AD627-Micro-power-Instrumentation-Amplifier-Offers-Supe_W0QQitemZ150420211813QQcmdZViewItemQQptZLH_DefaultDomain_0?hash=item2305be4865#ht_718wt_940</a>

Possible Digikey versions of analog device chips:<br>
<br>
<a href='http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&name=AD623ANZ-ND'>http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&amp;name=AD623ANZ-ND</a>

<a href='http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&name=AD627BNZ-ND'>http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&amp;name=AD627BNZ-ND</a>



TLC2272344P<br>
<br>
<a href='http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&name=296-7132-5-ND'>http://search.digikey.com/scripts/DkSearch/dksus.dll?Detail&amp;name=296-7132-5-ND</a>


$2.50 for a chip with all 4 of those op amps in one chip