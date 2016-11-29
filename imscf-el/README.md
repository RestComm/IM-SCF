=======
# IM-SCF - Execution Layer server
In order to properly handle huge traffic while maintaining the robustness of the architecture, the IM-SCF is implemented by two types of servers:
* Signaling Layer server
* Execution Layer server

Execution Layer servers implement the "logic" of IM-SCF. Roughly, their task is to interpret the messages from SIP application servers and core network components and send the appropriate messages to the other side.
