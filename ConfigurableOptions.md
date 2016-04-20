# Optional Settings #

Feel free to modify as little or much code as you wish but these are the main settings you'll want to explore:

  * **audioOn=False**
> The default settings to indicates whether the robot should speak or not. It is off by default so it doesn't interfere with the speech recognition but can be turned on with the "a" or "audio" command.
  * **currentSpeed=1**
> This is the default speed of the robot that works with the Truckbot Arduino code but could be adapted to other bots.
  * **cardinalMargin=10**
> This is the number of degrees that we allow as a margin of error when doing compass headings.
  * **inputMethod=commandByVoice**
> Determined which function to run be default when starting up to accept incoming commands. Current input methods are 'commandByVoice' or 'commandByTelnet'.