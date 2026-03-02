# Work Request: Step 004 — RAG Context Strategy

## Context

You are implementing step 004 of **Zee Mode** for Gravitee APIM. Steps 001-003 are complete and green:

- Maven deps vendored (`json-schema-llm-engine`, V4 SDK, V2 SDK in `lib/`)
- Domain models in `io.gravitee.apim.core.zee.model`
- `LlmEngineService` interface + `LlmEngineServiceImpl` in `io.gravitee.apim.infra.zee`
- **26/26 unit tests passing**

You are working on the `feat/zee-mode` branch in `~/workspace/Gravitee/gravitee-api-management`.

## ⚠️ Critical: Build Command

The module cannot compile standalone. **Always use this exact command**:

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service \
  -DskipTests -Dskip.validation -am -T 2C
```

And for tests:

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn test -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service \
  -Dtest="ZeeRequestTest,ZeeResourceTypeTest,LlmEngineServiceImplTest,RagContextStrategyTest,FlowRagContextAdapterTest,NoOpRagStrategyTest" \
  -DfailIfNoTests=false -Dskip.validation -am
```

## Your Task

Read and execute:

- **Task file**: `tmp/feat-roadmap/004-rag-context-flow-strategy.md`
- **RAG strategy design**: `tmp/feat-roadmap/rag-strategy.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`

In summary: create the `RagContextStrategy` domain interface (core layer) and two implementations: `FlowRagContextAdapter` (infra layer) and `NoOpRagStrategy`.

## What to Create

### 1. Domain Interface (Core Layer)

```
io.gravitee.apim.core.zee.domain_service/
└── RagContextStrategy.java
```

```java
public interface RagContextStrategy {
    ZeeResourceType resourceType();
    String retrieveContext(String envId, String orgId, Map<String, Object> contextData);
}
```

### 2. NoOpRagStrategy

`NoOpRagStrategy` can live in the **core layer** (no infra deps — it's just a fallback):

```java
public class NoOpRagStrategy implements RagContextStrategy {
    public static final NoOpRagStrategy INSTANCE = new NoOpRagStrategy();

    @Override
    public ZeeResourceType resourceType() { return null; } // not registered for any type

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        return "";
    }
}
```

### 3. FlowRagContextAdapter (Infra Layer)

```
io.gravitee.apim.infra.zee.adapter/
└── FlowRagContextAdapter.java
```

Before writing, **search the codebase** to find the correct existing services to inject:

```bash
# Find flow query services
grep -r "interface.*FlowCrud\|interface.*FlowQuery" \
  gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" -l

# Find policy/plugin services
grep -r "interface.*PolicyQuery\|interface.*PolicyService\|PluginService" \
  gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" -l

# Find existing usage patterns of flow queries
grep -r "findByApi\|findAll.*flow\|getFlows" \
  gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" -l | head -10
```

Use whatever interfaces you find — the exact method names will differ from the docs. **Don't invent service interfaces that don't exist.** If a needed service doesn't exist as a domain interface, inject the infra-layer service directly as a fallback (and note it in your work summary for future cleanup).

**Context to retrieve** (top 5 of each, formatted as markdown):

- Existing flows for the given API (use `apiId` from `contextData`)
- Available policy plugins (names + descriptions)

**Output format** — plain markdown the LLM will read:

```
### Existing Flows
- My Rate Limit Flow: Limits requests to the /api/v2/* path
- Auth Flow: Validates JWT tokens

### Available Policies
- rate-limit: Enforce request rate limits
- transform-headers: Add/remove HTTP headers
```

**Tenant scoping**: All queries must use `envId` and `orgId`. The domain query services handle this automatically via their method signatures — just pass them through.

**Graceful degradation**: Wrap each retrieval in try/catch. If a query fails, log a warning and return whatever context was gathered up to that point. Never throw from `retrieveContext`.

## Hard Constraints

- **`RagContextStrategy` interface in core layer** — no infra deps
- **`NoOpRagStrategy` in core layer** — no infra deps
- **`FlowRagContextAdapter` in infra layer** — can inject Spring beans and APIM services
- **Use existing domain query services** — never write raw MongoDB queries
- **Max 5 flows, max 20 policies** in context output
- **Branch**: `feat/zee-mode`

## Acceptance Criteria

- [ ] Build passes with the command above — **BUILD SUCCESS**
- [ ] `RagContextStrategy` interface in `io.gravitee.apim.core.zee.domain_service`
- [ ] `NoOpRagStrategy` returns empty string, no errors
- [ ] `FlowRagContextAdapter` retrieves flows + policies via existing APIM services
- [ ] All queries tenant-scoped
- [ ] Unit test for `NoOpRagStrategy` (trivial but explicit)
- [ ] Unit test for `FlowRagContextAdapter` — mock the injected services, verify formatted output
- [ ] All **26 existing tests + new tests** pass via Maven

## After Implementation

1. **Commit**: `feat(zee): add RAG context strategy for Flow resources`
2. **Request external review** from Dex, Gem, or Claw — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 004 — RAG Context Strategy

### What Was Done

- [files created with full paths]
- [which existing APIM services were found and used]

### Decisions Made

- [what services you found vs what the docs suggested]
- [any deviations in how context is gathered]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [any missing domain interfaces, service API surprises]

### Test Results

- [total tests passing — should be 26 + new tests]
- [build command output]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
