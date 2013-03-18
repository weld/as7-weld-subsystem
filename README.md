AS7 Weld Subsystem
==================

This is a JBoss AS 7 subsystem that provides integration with Weld 2.

The installation script provided in the /installer directory works on an existing JBoss AS 7 installation
and allows it to be updated with Weld 2. The script updates everything necessary for Weld 2 to run on JBoss AS 7.
Namely it updates the following modules: 
* cdi-api
* weld-api
* weld-core
* weld-spi
* as7-weld-subsystem

Installation
------------

Set the $JBOSS_HOME environment property to point to JBoss EAP 6.1 Alpha (JBoss AS 7.2.0.Final) or later

> export JBOSS_HOME=/opt/jboss/jboss-eap-6.1

Run the build

> mvn clean install -Pupdate-jboss-as

You may specify the version of Weld explicitly

> mvn clean install -Pupdate-jboss-as -Dweld.version=2.0.0-SNAPSHOT
