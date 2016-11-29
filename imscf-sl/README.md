=======
# IM-SCF - Signaling Layer server
In order to properly handle huge traffic while maintaining the robustness of the architecture, the IM-SCF is implemented by two types of servers:
* Signaling Layer server
* Execution Layer server

The Signaling Layer’s task is to communicate with telco systems using SS7 and SCTP protocols. The Signaling Layer acts as the message middleware between the Execution Layer and the connecting
systems – the Execution Layer uses it as a messaging system. The Signaling Layer does not process messages neither from telco system nor from Execution Layer, it just sends the messages to their appropriate destination.
