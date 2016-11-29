=======
# IM-SCF - Common package for Signaling and Execution Layers

The Execution and Signaling Layer servers share some functionality that is implemented in this common package. Usually, configuration and management modules are very similar in the servers:

* Configuration handling: This module is responsible for interpreting the domain configuration, setting up the related modules and responding to configuration changes.
* Management: JMX beans that enables the IM-SCF components to be configured.
* LwComm (Lightweight Communication): This module is responsible for the communication between Signaling Layer and Execution Layer nodes

