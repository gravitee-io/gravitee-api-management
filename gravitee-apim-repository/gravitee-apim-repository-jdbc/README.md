[![CircleCI](https://circleci.com/gh/gravitee-io/gravitee-repository-jdbc.svg?style=shield)](https://circleci.com/gh/gravitee-io/gravitee-repository-jdbc)

# Gravitee JDBC Repository

JDBC repository implementation that supports MySQL, MariaDB and PostgreSQL.

## Requirements

The minimum requirements are:
 * Maven3 
 * Jdk8

To use Gravitee.io snapshots, you need to declare the following repository in your maven settings:
`https://oss.sonatype.org/content/repositories/snapshots`

## Building

```shell
cd gravitee-apim-repository/gravitee-apim-repository-jdbc
mvn clean package
```

## Testing

By default, unit tests are run with en embedded PostgreSQL, but sometimes it can be useful to run them against another database.
To do so, TestContainer has been set up, and you can use the following commands: 
 - MariaDB: `mvn clean install -DjdbcType=mariadb-tc`
 - MySQL: `mvn clean install -DjdbcType=mysql-tc`
 - PostgreSQL: `mvn clean install -DjdbcType=postgresql-tc`
 - SQLServer: `mvn clean install -DjdbcType=sqlserver-tc`

You can also run tests against other embedded databases:
- MariaDB: `mvn clean install -DjdbcType=mariadb-te`
- MySQL: `mvn clean install -DjdbcType=mysql-te`

## Installing

Unzip the gravitee-repository-jdbc-x.y.z-SNAPSHOT.zip in the gravitee home directory.

## Configuration

repository.jdbc options : 

| Parameter                                        |   default  |
| ------------------------------------------------ | ---------: |
| ....                                             |  ......... |
