# 002: Core Domain Models

> **Common docs**: [architecture-overview.md](./architecture-overview.md) · [agent-backend.md](./agent-backend.md)

## Objective

Create the core domain model classes: `ZeeResourceType` enum, `ZeeRequest`, `ZeeResult`, `ZeeMetadata`, `FileContent`. These live in the domain layer with zero infrastructure dependencies.

## Package

```
io.gravitee.apim.core.zee.model/
├── ZeeResourceType.java
├── ZeeRequest.java
├── ZeeResult.java
├── ZeeMetadata.java
└── FileContent.java
```

## Implementation Detail

### ZeeResourceType (enum)

Maps to the `gravitee-api-definition-spec` Java SDK component class names. The enum value names should match the SDK class names exactly (e.g., `FLOW` → `"Flow"`, `ENDPOINT_GROUP` → `"EndpointGroup"`).

```java
public enum ZeeResourceType {
    FLOW("Flow"),
    PLAN("Plan"),
    API("Api"),
    ENDPOINT("Endpoint"),
    ENTRYPOINT("Entrypoint"),
    STEP("Step"),
    ENDPOINT_GROUP("EndpointGroup"),
    HTTP_LISTENER("HttpListener"),
    SUBSCRIPTION_LISTENER("SubscriptionListener");

    private final String componentName;

    ZeeResourceType(String componentName) {
        this.componentName = componentName;
    }

    public String componentName() { return componentName; }
}
```

### ZeeRequest (record)

```java
public record ZeeRequest(
    ZeeResourceType resourceType,
    String prompt,
    List<FileContent> files,
    Map<String, Object> contextData
) {
    public ZeeRequest {
        Objects.requireNonNull(resourceType, "resourceType is required");
        Objects.requireNonNull(prompt, "prompt is required");
        if (prompt.isBlank()) throw new IllegalArgumentException("prompt cannot be blank");
        files = files != null ? List.copyOf(files) : List.of();
        contextData = contextData != null ? Map.copyOf(contextData) : Map.of();
    }
}
```

### ZeeResult, ZeeMetadata, FileContent (records)

Immutable records. See full signatures in [agent-backend.md](./agent-backend.md).

## Acceptance Criteria

- [ ] All classes compile with no infrastructure dependencies
- [ ] `ZeeRequest` validation rejects null/blank prompts
- [ ] `ZeeResourceType` maps correctly to SDK component names
- [ ] Unit tests for ZeeRequest validation

## Commit Message

```
feat(zee): add core domain models

Add ZeeResourceType enum, ZeeRequest, ZeeResult, ZeeMetadata, and
FileContent domain records in io.gravitee.apim.core.zee.model package.
```
