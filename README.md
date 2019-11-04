# Liferay Portal DB Setup core [![Build Status](https://travis-ci.org/ableneo/liferay-db-setup-core.svg?branch=master)](https://travis-ci.org/ableneo/liferay-db-setup-core/branches) [![Gitter chat](https://badges.gitter.im/ableneo/liferay-db-setup-core.png)](https://gitter.im/ableneo/liferay-db-setup-core) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=liferay-db-setup-core-2_x&metric=alert_status)](https://sonarcloud.io/dashboard?id=liferay-db-setup-core-2_x)
Library that allows to setup a number of [Liferay][3] artifacts in the DB. It uses xml and [Liferay APIs][4] to add all configured artifacts.

# Motivation
We use [Liferay][3] as an application building platform.

In the approach pages, portlets, content and permissions serve as a building block of an application with consistent portal UX. It's all easy and fun unless you need to move through environments or track changes. Which you always need to track. Suddenly it becomes a problem that a very important part of your application resides in database.

The library helps to fix what need to be fixed across environments while allowing to use as much of a portal flexibility as needed.

# Usage
## Liferay Portal requirements
The code is compatible with **Liferay Portal DXP/7.x**. We maintain a [separate branch][1] for **Liferay Portal EE/CE 6.2.x**.

## Maven project setup
We didn't publish binary yet so you'll need to build the jar yourself. Here are the steps to do it:

1. Download sources.
1. Install Gradle
1. run <code>gradle build</code>
1. install generated jar in Your Liferay deploy folder

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
```gradle
Include-Resource: @db-setup-core-2.2.0-SNAPSHOT.jar
```
the name of the file is the same that you will find inside the maven/gradle repository.

## Contributing
Want/need to hack on db-setup-core? See our [super short contributing guide](CONTRIBUTING.md) for information on building, testing and contributing changes.

They are probably not perfect, please let me know if anything feels wrong or incomplete.

# Roadmap
* reference documentation
* more tests
* how-to guides

# Changelog

## 2.3.0-SNAPSHOT

### Features & bug fixes

* OSGI export files generated
* instance import feature added

## 2.2.0-SNAPSHOT

### Features & bug fixes
* it's possible to use more than one company id per configuration file, the configuration will be applied to all listed companies
* tag names in configuration follow unified naming convention: word-word
* run-as-user renamed to run-as-user-email to be explicit about expected value
* added missing documentation to few xml elements
* setup xsd provides a version attribute

### Refactorings & project changes
* configured sonar analysis on each commit
* configured maven test / coverage runner
* maven project structure has changed to single-module
* companyId, groupId and runAsUserId are set in Setup class and propagated to all involved Utils with SetupConfigurationThreadLocal context class
* improved MarshallUtil performance
* introduced unit tests 
* most of the problems reported by sonar are fixed
* improved logging

## 2.1.4

### Features & bug fixes
* Added resource class name when creating ADT 
* Fix user expando assignement
* Allow add categories and summaries to articles

[1]: https://github.com/ableneo/liferay-db-setup-core/tree/1.x
[2]: src/main/java/com/ableneo/liferay/portal/setup/LiferaySetup.java
[3]: https://github.com/liferay
[4]: https://docs.liferay.com/portal/7.0/javadocs/portal-kernel
