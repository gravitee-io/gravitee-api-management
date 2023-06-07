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
One tag only per operation.

### Mappers
Mapper classes follow the pattern: `[MyObject]Mapper`, where [MyObject] is the name of the object in the rest layer.<br>
Ex:
 - `ApiMapper`
 - `PlanMapper`
 - etc.

By default, every mapper methods will be called `map`. <br>
Ex:
 - `map(ApiEntity apiEntity)`
 - `map(PlanEntity planEntity)`
 - etc.

#### Special cases:
If there is no ambiguity when creating a map method, the method can be simply named `map`.<br>
But there are many cases where there is an ambiguity, so we need to use explicit names for the methods.<br>
The rules belows have to be applied only if there is an ambiguity.

1. When mapping collections, the method will be suffixed with `ToSet`or `ToList`, depending on the input or output.<br>
   Ex:
   ```java
   @Mapper
   public interface SubscriptionMapper {
       List<Subscription> map(List<SubscriptionEntity> subscriptionEntities);
       Set<Subscription> mapToSet(List<SubscriptionEntity> subscriptionEntities);
       Set<Subscription> map(Set<SubscriptionEntity> subscriptionEntities);
       List<Subscription> mapToList(Set<SubscriptionEntity> subscriptionEntities);
   }
   ```

2. When the target object is related to a V2/V4 definition, the method will be suffixed with `ToV2`/`ToV4`.<br>
   Ex:
   ```java
   @Mapper
   public interface ResourceMapper {
       io.gravitee.definition.model.v4.resource.Resource mapToV4(Resource resource);
       io.gravitee.definition.model.plugins.resources.Resource mapToV2(Resource resource);
   }
   ```

3. When the mapped Object is not the same as the mapper name, the name of this object should be added in the method name.<br>
   Ex:
   ```java
   @Mapper
   public interface SubscriptionMapper {
       SubscriptionStatus mapSubscriptionStatus(io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus subscriptionStatus);
       List<SubscriptionStatus> mapSubscriptionStatusList(Collection<io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus> subscriptionStatus);}
   ```
4. A default implementation annotated with `@Named(xxx)` used in `@Mapping(qualifiedByName="xxx")` should be named as the annotation value.<br>
   Tha value of the annotation should be explicit but no particular rule.<br>
   Ex:
   ```java
   @Mapper
   public interface ListenerMapper {
       @Named("pathMappingsToPattern")
       default Map<String, Pattern> pathMappingsToPattern(List<String> pathMappings) {
           if (Objects.isNull(pathMappings)) {
               return null;
           }
           return pathMappings.stream().collect(Collectors.toMap(pathMapping -> pathMapping, pathMapping -> Pattern.compile(pathMapping)));
       }
   }
   ``` 
   
5. When none of the previous rules can be applied, the method should be named as much explicit as possible.<br>
   Ex:
   ```java
   @Mapper
   public interface ResponseTemplateMapper {
       Map<String, ResponseTemplate> mapToApiModel(Map<String, io.gravitee.definition.model.ResponseTemplate> responseTemplate);
       default Map<String, Map<String, ResponseTemplate>> mapResponseTemplateToApiModel(Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> value) {
           ...
       }
   }
   ```

6. Special mappings like Object <=> String are not concerned by the previous rules.<br>
The only rule to follow is to use explicit names for the methods.
