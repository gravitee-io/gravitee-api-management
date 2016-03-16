[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-repository-test)](http://build.gravitee.io/jenkins/view/Tous/job/gravitee-repository-test/)

# Gravitee Test Repository

This repository is used to ensure repository test coverage and to provide a minimal set of tests on each repository implementation.

## Requirement

The minimum requirement is :
 * Maven3 
 * Jdk8

For user gravitee snapshot, You need the declare the following repository in you maven settings :

https://oss.sonatype.org/content/repositories/snapshots


## Building

```
$ git clone https://github.com/gravitee-io/gravitee-repository-test.git
$ cd gravitee-repository-test
$ mvn clean package
```

## How to use on your own implementation?

Add this dependency in scope test:

    <dependency>
        <groupId>io.gravitee.repository</groupId>
        <artifactId>gravitee-repository-test</artifactId>
        <version>${gravitee-repository-test.version}</version>
        <classifier>tests</classifier>
        <scope>test</scope>
    </dependency>

Then add a plugin in the build section:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>${maven-dependency-plugin.version}</version>
        <executions>
            <execution>
                <id>unpack-repository-tests</id>
                <phase>test-compile</phase>
                <goals>
                    <goal>unpack-dependencies</goal>
                </goals>
                <configuration>
                    <includeGroupIds>io.gravitee.repository</includeGroupIds>
                    <includeArtifactIds>gravitee-repository-test</includeArtifactIds>
                    <includeClassifiers>tests</includeClassifiers>
                    <outputDirectory>${project.build.directory}/test-classes</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

And finally add some test configuration & data initializer:

The test configuration & data initializer class must contains 'Test' to be loaded. For example : 'MongoTestRepositoryConfiguration' & 'MongoTestRepositoryInitializer'.
The data initializer must implement io.gravitee.repository.config.TestRepositoryInitializer with a setUp and tearDown methods which are executed around each test to isolate them.
