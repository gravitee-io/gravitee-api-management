[![Build Status](https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-repository-mongodb/master)](https://ci.gravitee.io/job/gravitee-io/job/gravitee-repository-mongodb/job/master/)

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
| writeConcern                                     |      1     |
| wtimeout                                         |    0       |
| journal                                          |            |
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
| keystorePassword                                 |            |
| keystore                                         |            |
| keyPassword                                      |            |

NB: writeConcern possible value are 1,2,3... (the number of node) or 'majority' 