AS7 Weld Subsystem
==================

This is a JBoss AS subsystem that provides integration with Weld 2.

The installation script provided in the /installer directory works on an existing JBoss AS installation
and allows it to be updated with Weld 2. The script updates everything necessary for Weld 2 to run on JBoss AS.
Namely it updates the following modules: 
* cdi-api
* weld-api
* weld-core
* weld-spi
* as7-weld-subsystem

Installation
------------

Set the $JBOSS_HOME environment property to point to JBoss AS 8.x or later

> export JBOSS_HOME=/opt/jboss/jboss-as-8.0.0.Alpha1-SNAPSHOT

Run the build

> mvn clean install -Pupdate-jboss-as

You may specify the version of Weld explicitly

> mvn clean install -Pupdate-jboss-as -Dweld.version=2.0.0-SNAPSHOT
