Calimero-core
=============

Library with core functionality for KNX network access.

* The Java 8 version is [on this branch](https://github.com/calimero-project/calimero/tree/feat/jse-embd8-c1).
* A port for [Java ME Embedded 8.1](http://www.oracle.com/technetwork/java/embedded/javame/embed-me/overview/index.html) is [on this branch](https://github.com/calimero-project/calimero/tree/jme-embd).

The packages (jars) currently distributed are based on the master branch, and compatible to Java ME CDC (JSR 218) with the Foundation Profile, i.e., "Java 1.4".
Future versions will use the Java 8 branch, requiring Java SE Embedded 8/compact1. The update will (probably) be with v2.3.


Download
--------

~~~ sh
# Either using git
$ git clone https://github.com/calimero-project/calimero.git calimero-core

# Or using hub
$ hub clone calimero-project/calimero calimero-core
~~~

Supported Features
--------

### Access Protocols
* KNXnet/IP
* KNX IP
* KNX RF USB (Java 8 branch only)
* KNX USB (Java 8 branch only)
* KNX FT1.2 Protocol (serial connections)
* TP-UART [__NEW__: Access TP1 networks via TP-UART]

#### KNXnet/IP
* Discovery and Self-description
* Tunneling
* Routing
* Busmonitor
* Device Management

#### KNX IP
* Routing

### Process Communication
* DPT encoding/decoding of Java/KNX data types
* Process Communicator client
* Group Monitor

#### Supported Datapoint Types (DPTs)
* 1.x - Boolean, e.g., Switch, Alarm
* 2.x - Boolean controlled, e.g., Switch Controlled, Enable Controlled
* 3.x - 3 Bit controlled, e.g., Dimming, Blinds
* 5.x - 8 Bit unsigned value, e.g., Scaling, Tariff information
* 6.x - 8 Bit signed value, e.g., Percent (8 Bit), Status with mode
* 7.x - 2 octet unsigned value, e.g., Unsigned count, Time period
* 9.x - 2 octet float value, e.g., Temperature, Humidity
* 10.x - Time
* 11.x - Date
* 12.x - 4 octet unsigned value
* 13.x - 4 octet signed value, e.g., Counter pulses, Active Energy
* 14.x - 4 octet float value, e.g., Acceleration, Electric charge
* 16.x - String, e.g., ASCII string, ISO-8859-1 string (Latin 1)
* 17.x - Scene number
* 18.x - Scene control
* 19.x - Date with time
* 20.x - 8 Bit enumeration, e.g., Occupancy Mode, Blinds Control Mode (Java 8 branch only)
* 28.x - UTF-8 string
* 29.x - 64 Bit signed value, e.g., Active Energy, Apparent energy
* 232.x - RGB color value

### Busmonitor
Access via KNXnet/IP, KNX USB, and FT1.2

#### Raw Frame Decoding
* TP1
* KNX IP
* PL110
* PL132
* RF

### Management
* KNX Management Layer
* KNX Management Procedures
* cEMI Local Device Management

### EMI Support
* cEMI standard and extend L-Data
* cEMI Busmonitor 
* cEMI Device Management
* EMI1/2 standard L-Data 
* EMI1/2 Busmonitor

### Network Buffer
* State/command-based datapoint buffer to answer L_Data.reqs, and buffer incoming L_Data.ind updates


More Features, Tools, Examples
------------------------------

* [introduction](https://github.com/calimero-project/introduction) contains code examples for programming with Calimero.

* [calimero-tools](https://github.com/calimero-project/calimero-tools) contains command-line tools for KNX process communication, monitoring, and management.

* [calimero-gui](https://github.com/calimero-project/calimero-gui) contains a graphical user interface for process communication, monitoring, and management.

* [calimero-server](https://github.com/calimero-project/calimero-server) is the Calimero KNXnet/IP Server.

* [calimero-device](https://github.com/calimero-project/calimero-device) is the communication stack for a KNX device.

* [serial-native](https://github.com/calimero-project/serial-native) provides native libraries for serial port access (using JNI).

* [import-ets4-xml](https://github.com/calimero-project/import-ets4-xml) allows importing ETS XML KNX datapoints into Calimero XML.
