# Work Request: Step 011 — Expand Resource Types

## Context

We are on `feat/zee-mode`.
The v0.1 pipeline for generating **Flows** is complete end-to-end! The Zee Widget is successfully integrated into the APIM Console UI.

Now we need to scale the architecture we built to support 4 additional resource types:

1. `PLAN`
2. `ENDPOINT`
3. `ENTRYPOINT`
4. `API`

## Workflow Overview

For each new resource type, you need to implement both the **Backend** RAG strategy and the **Frontend** adapter/card.

### Backend Tooling

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -DskipTests -Dskip.validation -am -T 2C
```

### Frontend Tooling

```bash
cd gravitee-apim-console-webui
nvm use
yarn test --testPathPattern="zee|adapter"
```

## Your Task

### Phase 1: Backend (Core & Infra)

1. **RAG Strategies**: Create `RagContextStrategy` implementations in `io.gravitee.apim.infra.zee.adapter` for the new types.
    - `PlanRagContextAdapter.java`: Use `PlanQueryService` or similar to fetch existing plans for the API. limit to top 5.
    - `EndpointRagContextAdapter.java`: Fetch existing endpoints.
    - `EntrypointRagContextAdapter.java`: Fetch existing entrypoints.
    - `ApiRagContextAdapter.java`: Fetch the current API's top-level details.
      *(Since these are infra adapters, they should be simple wrappers around existing APIM `*QueryService`s. Follow the pattern set by `FlowRagContextAdapter`.)\*

2. **Component Registry Verification**: The `GenerateResourceUseCase` maps the `ZeeResourceType` enum (e.g. `PLAN`) to the SDK component name (e.g. `Plan`). Ensure all 4 of the new resource SDK schemas actually exist and are registered in `LlmEngineServiceImpl`.

3. **Compile & Test**: Run the backend unit tests. No `GenerateResourceUseCase` changes should be necessary assuming the strategies are correctly wired via Spring `@ApplicationScoped`.

### Phase 2: Frontend (Console UI)

1. **Adapters**: Create `src/shared/components/zee/adapters/` adapters for each type.
    - `plan-adapter.ts`
    - `endpoint-adapter.ts`
    - `entrypoint-adapter.ts`
    - `api-adapter.ts`
      _(Each must map the raw APIM Definition shape to the specific V4 Management API save payload shape. Refer to existing Console UI code for the payload shapes, similar to what you did for the Flow in Step 010.)_

2. **Cards**: Create matching display cards in `zee-structured-view/` (e.g., `plan-card.component`, `endpoint-card.component`). Add them to the `zee-preview` template using the `[ngSwitch]="resourceType"` block.

3. **Wire up**: Integrate the `<zee-widget>` into the respective Console UI pages for creating Plans, Endpoints, Entrypoints, and APIs.
   _(Use `grep` to find the create/add pages in `gravitee-apim-console-webui/src/app/pages/api/v4/`.)_

### Phase 3: Testing

Write unit tests for the new backend RAG strategies, frontend adapters, and frontend cards.

## Acceptance Criteria

- [ ] 4 new RAG strategies implemented and tested in the backend.
- [ ] 4 new frontend adapters implemented and tested.
- [ ] 4 new frontend cards implemented and added to the preview component.
- [ ] Zee Widget integrated into the respective 4 creation pages in the Console UI.
- [ ] Both Backend (`mvn test`) and Frontend (`yarn test`) tests pass.

## Hard Constraints

- **Do not modify the Prompt Generation logic**: The prompt building and LLM calling in `GenerateResourceUseCase` is solid and provider-agnostic. RAG strategy implementations should just return formatted context strings.
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `feat(zee): add RAG, adapters, and UI integration for Plan, Endpoint, Entrypoint, and Api`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 011 — Expand Resource Types

### What Was Done

- [files created/modified]

### Decisions Made

- [where you integrated the widget for each of the 4 types]
- [what domain query services you used for the 4 RAG adapters]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [e.g., any SDK schema mismatches]

### Test Results

- [count of new backend & frontend tests added, and total pass output]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
