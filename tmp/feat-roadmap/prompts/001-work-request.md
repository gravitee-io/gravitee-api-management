# Work Request: Step 001 — Project Setup & Maven Dependencies

## Context

You are implementing step 001 of the **Zee Mode** feature for Gravitee APIM. Zee Mode adds AI-assisted resource creation — users describe resources in natural language and get schema-valid Gravitee API definitions back via LLM structured output.

You are working on the `feat/zee-mode` branch in `~/workspace/Gravitee/gravitee-api-management`. The branch has been pushed to remote and contains a roadmap at `tmp/feat-roadmap/`.

## Your Task

Read and execute the implementation described in:

- **Task file**: `tmp/feat-roadmap/001-project-setup-dependencies.md`
- **SDK integration guide**: `tmp/feat-roadmap/json-schema-llm-integration.md`
- **Architecture overview**: `tmp/feat-roadmap/architecture-overview.md`

In summary: set up the Maven dependencies so the APIM REST API can import `json-schema-llm-java` and the `gravitee-api-definition-spec` Java SDK. Add configuration properties for Azure OpenAI.

## Key Details

1. **Dependencies are vendored locally** (not on Maven Central). You need to copy JARs into a `lib/` directory. The exact pattern is already working in `~/workspace/Gravitee/gravitee-mcp-dynamic/` — check their `lib/`, `.m2-local/`, and `pom.xml` (lines 76-113) for the proven setup.

2. **Source repos for the JARs**:
    - `json-schema-llm-java`: `~/workspace/dotslashderek/jsonschema-llm/` — you may need to build it first (`mvn install -DskipTests`)
    - `gravitee-api-definition-spec` Java SDK: `~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/`

3. **Configuration**: Azure OpenAI env vars are `OPENAI_API_URL` (full URL including deployment + api-version) and `OPENAI_API_KEY`. The URL is already complete — no need to construct deployment/version separately.

4. **Where to add the dependency**: `gravitee-apim-rest-api/gravitee-apim-rest-api-service/pom.xml` is the most likely place, but check the existing module structure.

## Hard Constraints

- **Do NOT add LangChain4J** — we're going direct with `json-schema-llm`'s `HttpTransport`
- **Follow existing APIM patterns** for configuration properties (check how other features configure their settings in `gravitee.yml`)
- **Do NOT modify any existing code** in this step — only add new files and dependencies
- **Branch**: Work on `feat/zee-mode` — verify before committing

## Acceptance Criteria

- [ ] `json-schema-llm-java` JAR is vendored in `lib/` with proper Maven repo layout
- [ ] Local file-based repository is configured in the POM
- [ ] `mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -DskipTests` succeeds with the new dependency
- [ ] Configuration class reads `OPENAI_API_URL` and `OPENAI_API_KEY` from environment
- [ ] No existing modules or tests break

## After Implementation

1. **Commit** with message: `feat(zee): add json-schema-llm and GAD spec SDK dependencies`
2. **Request review** from one external review agent (Dex, Gem, or Claw) — apply any feedback
3. **Output the following work summary** so the orchestrator can validate and generate the next step:

```markdown
## Work Summary: Step 001 — Project Setup & Maven Dependencies

### What Was Done

- [files created/modified with full paths]

### Decisions Made

- [any choices not specified in the step file]

### Deviations from Plan

- [anything different from the step file — and why]

### Issues Discovered

- [blockers, surprises, or things affecting future steps]

### Test Results

- [build output, any test runs]

### Review Feedback Applied

- [what the reviewer flagged, how it was addressed]

### Ready for Next Step?

- [yes/no + any prerequisites for step 002]
```
