# Graviteeio - APIM - EL

This module contains a `Schema` class used to generate a JSON Schema describing which objects 
are available when using expression language in APIM.

This can be useful e.g. to implement intellisense in a U.I.

## The schema file

The `schema.json` file is generated at build time and can be later retrieved using the 
`Schema.FILE` property.

## Adding properties to the schema file

To make sure only relevant properties are adding to the schema, only properties annotated with the jackson `@JsonProperty` 
annotation will be taken into consideration.

This means that to be reflected into the resulting schema file, evaluable properties (or getters) **must** be annotated accordingly.


## Example

### Dependency

```xml
<!-- EL Schema -->
<dependency>
    <groupId>io.gravitee.apim</groupId>
    <artifactId>gravitee-apim-el</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Reading the schema file from within your dependent module

```java
Schema.class.getResourceAsStream(Schema.FILE);
```