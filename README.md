# Liferay Portal DB Setup core [![Build Status](https://travis-ci.org/ableneo/liferay-db-setup-core.svg?branch=master)](https://travis-ci.org/ableneo/liferay-db-setup-core) [![Gitter chat](https://badges.gitter.im/ableneo/liferay-db-setup-core.png)](https://gitter.im/ableneo/liferay-db-setup-core) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=liferay-db-setup-core-2_x&metric=alert_status)](https://sonarcloud.io/dashboard?id=liferay-db-setup-core-2_x)
Library that allows to setup a number of Liferay artifacts in the DB. It uses xml and Liferay APIs to add all configured artifacts.

# Usage
## Setup
We didn't publish binary yet so you'll need to build the jar yourself. Here are the steps to do it:

1. Download sources.
1. Install Maven 3.x.
1. run <code>mvn clean install</code>
1. grab jar from <code>./target</code> or use as a dependency in your maven project
```xml
<dependency>
    <groupId>com.ableneo.liferay</groupId>
    <artifactId>db-setup-core</artifactId>
    <version>2.2.0-SNAPSHOT</version>
</dependency>
```

## Integration
Run <code>com.ableneo.liferay.portal.setup.LiferaySetup#setup(java.io.File)</code> with following xml configuration:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<setup xmlns="http://www.ableneo.com/liferay/setup">
    <configuration>
        <run-as-user-email>test@liferay.com</run-as-user-email>
    </configuration>
 
    <!--
    This will add new custom field that can be used in theme to control if ads should display on
    particular page.
    -->
    <custom-fields>
        <field name="showAds" type="boolean" class-name="com.liferay.portal.model.Layout">
            <role-permission role-name="Guest" permission="view"/>
        </field>
    </custom-fields>
</setup>
```
When adding the library to a liferay OSGI module it will be necessary to specify the dependency into the bnd file:
```java
Include-Resource: @db-setup-core-2.2.0-SNAPSHOT.jar
```
the name of the file is the same that you will find inside the maven/gradle repository.

# Compatibility
* Version 2.x.x: Liferay Portal DXP/7.x
* Version 1.x.x: Liferay Portal EE/CE 6.2.x

