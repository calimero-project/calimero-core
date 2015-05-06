Calimero-core Library
=====================

Calimero for Java ME Embedded. 

Provided under the terms of GPL, version 2, with the Classpath Exception.

__As a side-effect of porting the source code, current snapshots might not compile!__

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
	- Optional permissions:

* Development: Oracle Java ME Embedded 8.0, or later
* Logging framework (the initial port uses a cut-down slf4j, similar to the Java 8 compact1 version)


Development
-----------
 ...