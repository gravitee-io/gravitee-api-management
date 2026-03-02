# Work Request: Step 007 — Backend Integration Test

## Context

Zee Mode steps 001-006 are complete on `feat/zee-mode`. The full backend pipeline is wired up:

- Maven dependencies configured
- Core domain models (ZeeRequest, ZeeResult, etc.)
- LlmEngineService (Azure OpenAI integration)
- RagContextStrategies (Flow RAG)
- GenerateResourceUseCase orchestrator
- ZeeResource JAX-RS endpoint (`/environments/{envId}/ai/generate`)

Currently **55/55 unit tests** are passing.

## ⚠️ Critical: Build Command

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service,gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest \
  -DskipTests -Dskip.validation -am -T 2C
```

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/007-backend-integration-test.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`

## What to Do

The primary goal of Step 007 is to **verify the backend end-to-end** before we hand off to the frontend.

### 1. Identify Missing Test Coverage

Look through the tests implemented in Steps 002-006. Are there any critical paths missing?

- Step 005 already tested `GenerateResourceUseCase` with mocks.
- Step 006 already tested `ZeeResourceTest` with mocks.

If the unit tests are solid, your main job is to document and potentially automate the **live integration test**.

### 2. Live `curl` Test Procedure

The ultimate proof is a live HTTP call against a running local APIM Gateway/REST API that hits Azure OpenAI and returns a generated Flow.

**Create a test script or documentation in `/tmp/feat-roadmap/scripts/test-zee-endpoint.sh` (or similarly named file) that:**

1. Explains how to start the APIM REST API locally with the required `OPENAI_API_URL` and `OPENAI_API_KEY` environment variables.
2. Provides the exact `curl` command (or an equivalent script) to hit `/environments/{envId}/ai/generate` with a multipart payload (JSON request + dummy file).
3. Notes what the expected successful JSON output structure looks like.

_Note: Since you can't easily start the full APIM stack and get a valid Console JWT within your sandbox environment, you are creating the **procedure** for the user (D) to run._

### 3. Polish any Backend Stragglers

If you noticed any minor backend issues or missing tests during your review in Step 1, fix them now. For example, Step 006 skipped testing the rate-limiting logic because of the `System.currentTimeMillis()` dependency. You can extract a `Clock` interface if you want to be thorough, or just leave it as is if it's acceptable for a prototype.

## Acceptance Criteria

- [ ] A reproducible test script/procedure is created for end-to-end verification.
- [ ] No regressions in existing functionality (55 tests pass).
- [ ] Build passes with modified command — **BUILD SUCCESS**

## Hard Constraints

- **API Layer ONLY**: Do not modify files in the core or infra layers created in steps 001-006 unless fixing a bug discovered during integration testing.
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `test(zee): add backend integration test procedure`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 007 — Backend Integration Test

### What Was Done

- [files created/modified]
- [description of the integration test procedure created]

### Architecture Compliance

- [confirm: No layer violations introduced]

### Decisions Made

- [any missing tests you decided to fill in (or skip)]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [surprises or things affecting future steps]

### Test Results

- [total count — must be ≥ 55 + new tests, all passing]
- [build command output: BUILD SUCCESS]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
