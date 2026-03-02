# Work Request: Step 002 — Core Domain Models

## Context

You are implementing step 002 of the **Zee Mode** feature for Gravitee APIM. Step 001 is complete — Maven dependencies for `json-schema-llm-java`, `json-schema-llm-engine`, and the `gravitee-api-definition-spec` V4 Java SDK are vendored in `lib/` and resolve correctly.

You are working on the `feat/zee-mode` branch in `~/workspace/Gravitee/gravitee-api-management`.

## Your Task

Read and execute the implementation described in:

- **Task file**: `tmp/feat-roadmap/002-core-domain-models.md`
- **Architecture overview**: `tmp/feat-roadmap/architecture-overview.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`

In summary: create the core domain model classes that live in the **domain layer** with zero infrastructure dependencies. These are the data contracts used by all subsequent steps.

## What to Create

All files go in `io.gravitee.apim.core.zee.model` inside the `gravitee-apim-rest-api-service` module.

### 1. ZeeResourceType (enum)

Maps enum values to SDK component class names. The V4 Java SDK at `io.gravitee.apim.definition.llm.v4` has 64 components — start with these key ones:

| Enum Value              | SDK Component Name       |
| ----------------------- | ------------------------ |
| `FLOW`                  | `"Flow"`                 |
| `PLAN`                  | `"Plan"`                 |
| `API`                   | `"Api"`                  |
| `ENDPOINT`              | `"Endpoint"`             |
| `ENTRYPOINT`            | `"Entrypoint"`           |
| `STEP`                  | `"Step"`                 |
| `ENDPOINT_GROUP`        | `"EndpointGroup"`        |
| `HTTP_LISTENER`         | `"HttpListener"`         |
| `SUBSCRIPTION_LISTENER` | `"SubscriptionListener"` |

Each enum value should have a `componentName()` accessor that returns the string.

### 2. ZeeRequest (record)

```java
public record ZeeRequest(
    ZeeResourceType resourceType,
    String prompt,
    List<FileContent> files,
    Map<String, Object> contextData  // apiId, envId, orgId, etc.
)
```

**Validation**: `resourceType` and `prompt` are required. `prompt` must not be blank. `files` defaults to empty list if null. `contextData` defaults to empty map if null. Use defensive copying (`List.copyOf`, `Map.copyOf`).

### 3. ZeeResult (record)

```java
public record ZeeResult(
    ZeeResourceType resourceType,
    JsonNode generated,       // Rehydrated Gravitee shape (Jackson JsonNode)
    ZeeMetadata metadata
)
```

### 4. ZeeMetadata (record)

```java
public record ZeeMetadata(
    String model,
    int tokensUsed,
    boolean ragContextUsed
)
```

### 5. FileContent (record)

```java
public record FileContent(
    String filename,
    String content,
    String mediaType
)
```

## Learnings from Step 001

- `ZeeConfiguration` was placed in `io.gravitee.apim.infra.zee` — this is the **infra** layer. The models you're creating go in `io.gravitee.apim.core.zee.model` — the **core/domain** layer. These packages likely don't exist yet, you'll need to create them.
- APIM has pre-existing Lombok compile errors when building rest-api-service standalone. This is expected — don't worry about them as long as your new code compiles cleanly.
- The APIM codebase uses Java 17. Use record types, but no Java 21+ features.

## Hard Constraints

- **ZERO infrastructure dependencies** — these are pure domain models. No Spring annotations, no Jackson annotations (except `JsonNode` which is a library type), no imports from `io.gravitee.apim.infra.*`
- **Immutable** — use records with defensive copies in compact constructors
- **Branch**: Work on `feat/zee-mode` — verify before committing
- **Do NOT modify** any files from step 001

## Acceptance Criteria

- [ ] All 5 classes compile in `io.gravitee.apim.core.zee.model`
- [ ] No infrastructure/Spring/framework dependencies in the package
- [ ] `ZeeRequest` validation rejects null `resourceType`, null/blank `prompt`
- [ ] `ZeeResourceType.componentName()` returns the correct SDK class name
- [ ] Unit tests for `ZeeRequest` validation (null prompt, blank prompt, null files → empty list, etc.)

## After Implementation

1. **Commit** with message: `feat(zee): add core domain models`
2. **Request review** from one external review agent (Dex, Gem, or Claw) — apply any feedback
3. **Output the following work summary**:

```markdown
## Work Summary: Step 002 — Core Domain Models

### What Was Done

- [files created/modified with full paths]

### Decisions Made

- [any choices not specified in the step file]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [blockers, surprises, or things affecting future steps]

### Test Results

- [unit test results, build output]

### Review Feedback Applied

- [what the reviewer flagged, how it was addressed]

### Ready for Next Step?

- [yes/no + any prerequisites for step 003]
```
