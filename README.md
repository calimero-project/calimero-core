Calimero-core Library
=====================

Calimero for Java ME Embedded. 

Provided under the terms of GPL, version 2, with the Classpath Exception.

__In the current snapshot, the unit tests do not compile! It is best to just exclude them from builds.__

Overview
--------

Java ME Embedded is usually referred to with the Internet of Things (IoT). For an overview of Java ME Embedded, see [the Oracle Java ME Embedded Overview](http://www.oracle.com/technetwork/java/embedded/javame/embed-me/overview/index.html).

The Calimero ME library is based on the Calimero-core library for Java SE 8 with the compact1 profile.
Due to the restricted Java API and the resource-constrained execution environment of Java ME target platforms, 
the Calimero API also requires minor adaptations. 


Dependencies
------------
* Java ME configuration: CLDC 1.8
* Java ME extensions:
* Security configuration

	- Required permissions:

		* java.util.PropertyPermission "*" "read"
		* java.lang.RuntimePermission "*"
		* javax.microedition.io.DatagramProtocolPermission "datagram://:*"
		* javax.microedition.io.DatagramProtocolPermission "datagram://*:*"
		* javax.microedition.io.MulticastProtocolPermission "multicast://*:*"
		* javax.microedition.io.SocketProtocolPermission "socket://localhost:*"
		* javax.microedition.io.AccessPointPermission "property"
		* javax.microedition.io.AccessPointPermission "manage"
		* javax.microedition.io.CommProtocolPermission "comm:*"

	- Optional permissions: you might make any permission optional if the corresponding functionality is not used

* Development: Oracle Java ME Embedded 8.0, or later
* Logging framework (the initial port uses a cut-down slf4j, similar to the Java 8 compact1 version)


Development
-----------

Currently, the Java ME Embedded 8 SDK is only available for Microsoft Windows from the Oracle site. Hence, for development, an installation of Windows 7 or newer is required.

`Maven` supports compiling Calimero against the Java ME Embedded libraries, assuming the default SDK installation directory. For packaging and distribution, an IDE or the terminal is required. Both the Eclipse and Netbeans IDE provide the necessary plugins. (In my opinion, the current integration of the user dialogs and the device emulator is realized better in Netbeans.)

For compilation only, the logging dependency for slf4j is sufficient. However, the pre-compiled slf4j versions will not load on embedded systems using Java ME Embedded.
