# Gravitee Management REST API

Administrative Rest API to manage the Gateway

[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-management-rest-api)](http://build.gravitee.io/jenkins/view/Tous/job/gravitee-management-rest-api/)

## Building

```
$ git clone https://github.com/gravitee-io/gravitee-management-rest-api.git
$ cd gravitee-management-rest-api/war
$ mvn -Djetty.port=8083 jetty:run -Dgravitee.home=/|gravite-home-path|/ -Dgravitee.conf=/|gravite-conf-path|/gravitee.yml

...
<app starts and listens on port 8083>
```
The application can be package and deploy as web application (war file)
```
$ git clone https://github.com/gravitee-io/gravitee-management-rest-api.git
$ cd gravitee-management-rest-api/war
$ mvn package
...
<app war file can be find inside target/ folder>
<deploy it inside web container to get ready>
```
and also as a executable jar (embedded web application)
```
$ git clone https://github.com/gravitee-io/gravitee-management-rest-api.git
$ cd gravitee-management-rest-api/standalone
$ mvn package
...
<app jar file can be find inside target/ folder>
<you can start the application with the following command line>
$ java -jar target/<app_name>-executable.jar -Dgravitee.home=/|gravite-home-path|/ -Dgravitee.conf=/|gravite-conf-path|/gravitee.yml
...
<app starts and listens on port 8083>
```
## Application configuration file

A minimal configuration file (gravitee.yml) :

```
node:
  home: ${gravitee.home}

plugins:
  registry:
    path: ${gravitee.home}/plugins

repository:
  type: mongodb
  mongodb:
    dbname: gravitee
    host: localhost
    port: 27017

#  type: jpa
#  jpa:
#    hibernateDialect: org.hibernate.dialect.PostgreSQL9Dialect
#    driverClassName: org.postgresql.Driver
#    url: jdbc:postgresql://localhost/gravitee
#    username: user
#    password: password

security:
  type: basic-auth
  authentication-manager:
    authentication-providers:
      size: 3
      authentication-provider-1:
        type: ldap
        embedded: true
        role-mapping: true		
        # if role-mapping is true set the following role mapper LDAP values
        role-mapper: {
                ROLE_DEVELOPERS: ROLE_USER,
                ROLE_MEMBERS: ROLE_USER,
                ROLE_OWNERS: ROLE_ADMIN
               }
        user-dn-patterns: uid={0},ou=people
        group-search-base: ou=groups
        context-source-base: dc=gravitee,dc=io
        # if embedded is false set the following values 
        # context-source-username: test
        # context-source-password: test
        # context-source-url: ldap://localhost:389/dc=gravitee,dc=io
      authentication-provider-2:
        type: memory
        users:
          size: 2
          user-1:
            username: user
            password: password
            roles: USER
          user-2:
            username: admin
            password: admin
            roles: ADMIN
      authentication-provider-3:
        type: gravitee
        password-encoding: true
```
Subfolders (plugins, apis, ...) must be created before start the application.

LDAP Embedded Server :

User accounts : see gravitee-io-management-rest-api-ldap-test.ldif file to get your credentials
