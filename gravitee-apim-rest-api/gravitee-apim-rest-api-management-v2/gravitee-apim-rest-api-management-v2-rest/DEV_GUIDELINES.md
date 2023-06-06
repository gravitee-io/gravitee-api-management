# Development guidelines for the Management API v2

## Introduction
With this new version of APIM Management API, we have started to apply Gravitee API Guidelines on the design part of the API.

This document describes the guidelines to follow when implementing the API.

## Structure of the project
In the main java package, can be found:
 - `exceptionMapper`: a package containing the exception mappers of Jersey.
 - `filter`: a package containing Jersey filters.
 - `mapper`: a package containing the MapStruct mappers to convert entities from the rest layer to the service layer and vice versa.
 - `resource`: a package containing all the API resources. Resources are organized with the "1 tag/1 package" rule. A tag in the OpenApi specification corresponds to a package in the resource package.

    **Exceptions**: 
   - OpenApiResource: responsible for the display of the OpenApi specification.
   - `param`: contains classes that represents some common query parameters.
 - `security`: a package containing the security layer of the API. It contains the authentication and authorization filters.
 - `spring`: a package containing the Spring configuration of the API.
 - `utils`: a package containing some utility classes.
 - `GraviteeManagementV2Application`: represents the main class of the API.

In the main resources folder, can be found:
 - `logback.xml`: the logback configuration file.
 - `openapi/management-openapi-v2.yaml`: the OpenApi specification of the API.
 - `openapi/index.html`: the OpenApi documentation page, powered by stoplight/elements specification.

In the test java package, can be found:
 - `fixtures`: a package containing the fixtures of the tests. They are helpers to create entities.
 - `security`: a package containing the tests of the security layer of the API.

## Development guidelines
### OpenAPI specification
Tags are sorted by name.<br>
Paths are sorted by tag and by name.<br>
Inside a path, operations are sorted by this order:
 - get
 - post
 - put
 - delete

Description of the operations must be written in Markdown format.<br>
Must contain the needed permissions <br>
