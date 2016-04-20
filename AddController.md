## Adding a new robot controller ##

Cellbots provides an IRobotControllerService Interface which developers must implement, in order to make their robot drivable through Cellbots. More specifically, a developer must

  1. Implement an Android Service which implements the interface. For example, take a look at the class com.cellbots.local.robotcontrollerservice.DefaultRobotControllerService
  1. The service’s `<service>` declaration in the AndroidManifest.xml file should contain an Action in the Intent filter with the name "com.cellbots.controller.ROBOT\_CONTROLLER\_SERVICE", by which Cellbots identifies the controller. Please see the AndroidManifest.xml for Cellbots.