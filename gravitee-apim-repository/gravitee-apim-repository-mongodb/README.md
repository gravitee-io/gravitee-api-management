[![CircleCI](https://circleci.com/gh/gravitee-io/gravitee-repository-mongodb.svg?style=shield)](https://circleci.com/gh/gravitee-io/gravitee-repository-mongodb)

# Gravitee Mongo Repository

Mongo repository based on MongoDB

## Requirements

The minimum requirements are:
* Maven3
* Jdk8

To use Gravitee.io snapshots, you need to declare the following repository in your maven settings:
`https://oss.sonatype.org/content/repositories/snapshots`

## Building

```shell
cd gravitee-apim-repository/gravitee-apim-repository-mongodb
mvn clean package
```

## Installing

Unzip the gravitee-repository-mongodb-x.y.z-SNAPSHOT.zip in the gravitee home directory.
 


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