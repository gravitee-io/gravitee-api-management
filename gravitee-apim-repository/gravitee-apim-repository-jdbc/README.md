# Gravitee JDBC Repository

JDBC repository implementation that supports MySQL, MariaDB, PostgreSQL and Microsoft SQLServer.

## Requirements

The minimum requirements are:
 * Maven3 
 * Jdk11

To use Gravitee.io snapshots, you need to declare the following repository in your maven settings:
`https://oss.sonatype.org/content/repositories/snapshots`

## Building

```shell
cd gravitee-apim-repository/gravitee-apim-repository-jdbc
mvn clean package
```

## Testing

By default, unit tests are run with a TestContainer version of PostgreSQL, but sometimes it can be useful to run them against another database.
To do so you can use the following commands: 
 - MariaDB (10.3.6): `mvn clean install -DjdbcType=mariadb`
 - MySQL (5.7.22): `mvn clean install -DjdbcType=mysql`
 - PostgreSQL (9.6.12): `mvn clean install -DjdbcType=postgresql`
 - SQLServer (2017-CU12): `mvn clean install -DjdbcType=sqlserver`

You can also use a specific version of the databsase like:
- MariaDB: `mvn clean install -DjdbcType=mariadb~10.4.24`
- MySQL: `mvn clean install -DjdbcType=mysql~8.0.28`
- PostgreSQL: `mvn clean install -DjdbcType=postgresql~13.6`


## Installing

* Copy the gravitee-apim-repository-jdbc-x.y.z-SNAPSHOT.zip in `${gravitee.home}/plugins` directory.
* Add your JDBC Driver in `${gravitee.home}/plugins/ext/repository-jdbc`

## Configuration

repository.jdbc options : 

| Parameter                                        |   default  |
| ------------------------------------------------ | ---------: |
| ....                                             |  ......... |
