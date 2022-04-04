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

## Testing

By default, unit tests are run with a TestContainer version of Mongo 4.4.6, but sometimes it can be useful to run them against other version of Mongo.
To do so you can use the following commands:
- Mongo 3: `mvn clean install -DmongoVersion=3`
- Mongo 4: `mvn clean install -DmongoVersion=4`
- Mongo 5: `mvn clean install -DmongoVersion=5`

You can use the version of Mongo you want to test by using the docker image tag in the `-DmongoVersion` parameter. For example, for Mongo 4.4.4, you will use `-DmongoVersion=4.4.4` .

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