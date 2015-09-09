[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-repository-mongodb)](http://build.gravitee.io/jenkins/view/Tous/job/gravitee-repository-mongodb/)

# Gravitee Mongo Repository

Mongo repository based on MongoDB

## Requirement

The minimum requirement is :
 * Maven3 
 * Jdk8

For user gravitee snapshot, You need the declare the following repository in you maven settings :

https://oss.sonatype.org/content/repositories/snapshots


## Building

```
$ git clone https://github.com/gravitee-io/gravitee-repository-mongodb.git
$ cd gravitee-repository-mongodb
$ mvn clean package
```

## Installing

Unzip the gravitee-repository-mongodb-1.0.0-SNAPSHOT.zip in the gravitee home directory.
 


## Configuration

repository.mongodb options : 

| Parameter                                        |   default  |
| ------------------------------------------------ | ---------: |
| host                                             |  localhost |
| port                                             |      9200  |
| username                                         |            |
| password                                         |            |
| connectionPerHost                                |            |
| connectTimeout                                   |            |
| maxWaitTime                                      |            |
| socketTimeout                                    |            |
| socketKeepAlive                                  |            |
| maxConnectionLifeTime                            |            |
| maxConnectionIdleTime                            |            |
| minHeartbeatFrequency                            |            |
| description                                      |            |
| heartbeatConnectTimeout                          |            |
| heartbeatFrequency 	                           |            |
| heartbeatsocketTimeout                           |            |
| localThreshold 	                               |            |
| minConnectionsPerHost                            |            |
| sslEnabled 		                               |            |
| threadsAllowedToBlockForConnectionMultiplier     |            |
| cursorFinalizerEnabled                           |            |