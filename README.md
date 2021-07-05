[![Build Status](https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-repository-hazelcast/master)](https://ci.gravitee.io/job/gravitee-io/job/gravitee-repository-hazelcast/job/master/)

# Gravitee Hazelcast Repository

A repository implementation based on [Hazelcast](https://hazelcast.com/).

## Requirement

The minimum requirement is:
 * Maven3 
 * Jdk8

## Building

```
$ git clone https://github.com/gravitee-io/gravitee-repository-hazelcast.git
$ cd gravitee-repository-hazelcast
$ mvn clean package
```

## Installing

Unzip the gravitee-repository-hazelcast-x.y.z-SNAPSHOT.zip in the plugins directory.

## Configuration

repository.mongodb options : 

| Parameter                                        |   default  |
| ------------------------------------------------ | ---------: |
| configurationFile                                |  ${gravitee.home}/config/hazelcast.xml |