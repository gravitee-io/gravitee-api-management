[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-standalone)](http://build.gravitee.io/jenkins/view/Tous/job/gravitee-standalone/)


# Gravitee.IO - Standalone Gateway

Standalone Gravitee.IO gateway


## Requirements

Ensure you have JDK 8 (or newer) and Maven 3.2.2 installed, then run:

```
$ java -version
$ maven -version
``` 

## Building

```
$ git clone https://github.com/gravitee-io/gravitee-standalone.git
$ cd gravitee-standalone
$ mvn clean package
```

## Available distributions

 * Zip 
 * RPM (RedHat 5-6)
 
## Starting and stopping Gravitee.IO Gateway

### Running from ZIP

```
$ cd gravitee-standalone-distribution/zip/target/
$ unzip gravitee-standalone-*-SNAPSHOT.zip -d gravitee-gateway
$ cd gravitee-gateway
$ ./bin/gravitee
```

The gateway has been correctly started and is listening on port 8082 by default when you see this message in your console :
```
22:16:26.766 [gravitee] INFO  i.g.g.standalone.node.DefaultNode - Gateway [lolcaohst - 1.0.0-SNAPSHOT] started in 76 ms.
```

### Running from RPM

```
$ cd gravitee-standalone-distribution/rpm/target/rpm/gravitee-gateway/RPMS/noarch
$ sudo rpm -i gravitee-gateway-*.noarch.rpm 
$ sudo /etc/init.d/gravitee-gateway start
```

## License
Gravitee.IO is released under the Apache License v2.0.
Please have a look to [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0) for a full version of the license.