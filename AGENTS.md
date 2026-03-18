# AGENTS.md

## Preamble
This file is intentionally written in English.
All process, implementation, and build guidance in this repository should be documented and maintained in English.
Do not use tables in documentation descriptions in this repository because IDE rendering is unreliable; use headings and bullet lists instead.
When documenting a topic, prefer a hierarchy of Markdown headings with short prose blocks. Use bullet lists as supporting structure, not as the primary way to describe longer topics.

## Source of Truth
Read `CONTRIBUTING.adoc`, especially the section `AI Agent Context (Docker Compose Full Stack)`.
When this file and `CONTRIBUTING.adoc` differ, align with `CONTRIBUTING.adoc` and update this file.

## Repository Topology
### Stack Types
This repository is a mixed stack monorepo:
- Maven multi-module backend and platform modules (Java)
- Nx workspace at repository root for Angular applications/libraries
- Independent Angular project for legacy Portal (`gravitee-apim-portal-webui`)

### Main Module Families
Main module families:
- Java: `gravitee-apim-rest-api`, `gravitee-apim-gateway`, `gravitee-apim-common`, `gravitee-apim-repository`, `gravitee-apim-plugin`, `gravitee-apim-reporter`, `gravitee-apim-distribution`, ...
- Angular (Nx): `gravitee-apim-console-webui` (`console`), `gravitee-apim-portal-webui-next` (`portal-next`), `gravitee-apim-webui-libs`
- Angular (standalone): `gravitee-apim-portal-webui`

## Implementation Conventions
This section describes the main logical layers visible in the current codebase. Treat it as the entry point for implementation conventions. The structure below should be refined over time with explicit rules per layer instead of mixing all conventions into one flat list.

### Purpose
Use this section to explain how code is divided into logical layers, what responsibility belongs to each layer, and where new behavior should be implemented. Prefer extending the relevant layer subsection instead of adding generic repository-wide advice elsewhere.

### Backend Layers
The backend is not limited to `repository`, `service`, and `resource`. The current codebase shows a broader layered structure spread across `gravitee-apim-rest-api`, `gravitee-apim-repository`, and related modules. Use the layer map below to understand where code belongs and how dependencies should flow.

#### Layer Overview
The main backend layers are:
- API Contract Layer
- Resource Layer
- Security Layer
- Use Case Layer
- Service Layer
- Mapping and Transformation Layer
- Model Layer
- Repository Layer
- Background and Integration Services Layer

#### API Contract Layer
This layer contains OpenAPI definitions and contract-first REST descriptions. It defines the public API surface, payload shapes, pagination contracts, filters, and endpoint-level semantics.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest`

##### Typical locations
- `src/main/resources/openapi/*.yaml`
- `src/main/resources/portal-openapi.yaml`

##### Main dependencies
- This layer is the outermost contract description layer.
- It describes the HTTP surface exposed by the resource layer.
- It should not contain business orchestration logic.

#### Resource Layer
This layer exposes REST endpoints in management, portal, automation, and related REST modules. Resources receive HTTP input, perform request-level validation and authorization orchestration, and delegate business behavior to lower layers instead of implementing complex domain rules inline.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest`

##### Typical locations
- `src/main/java/**/resource/**`

##### Main dependencies
- Resource Layer depends on Use Case Layer in the current delivery model.
- Resource Layer may still depend directly on Service Layer in legacy areas.
- Resource Layer depends on Mapping and Transformation Layer for request and response conversion.
- Resource Layer is described by the API Contract Layer.

#### Security Layer
The codebase contains dedicated security modules and request-level security components. This layer is responsible for authentication, authorization integration, and request protection concerns around exposed APIs.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-security`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-security`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-security`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-security`

##### Main dependencies
- Security Layer is used by the Resource Layer.
- Security Layer may depend on Service Layer for authorization and identity-related checks.

#### Use Case Layer
The backend contains an explicit use-case-oriented layer under packages such as `io.gravitee.apim.core.*.use_case`. These classes appear where the codebase models focused business operations as application actions instead of exposing all behavior directly through broad service classes.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-kafka-explorer`

##### Typical locations
- `src/main/java/io/gravitee/apim/core/**/use_case/**`
- `src/main/java/io/gravitee/rest/api/kafkaexplorer/domain/use_case/**`

##### Main dependencies
- Use Case Layer depends on Service Layer.
- Use Case Layer may depend on Mapping and Transformation Layer when application-level conversion is needed.
- Use Case Layer must not depend on Resource Layer.
- Use Case Layer is the preferred application-layer entry point between resources and services.

#### Service Layer
The service layer remains a major part of the backend, especially in `gravitee-apim-rest-api-service`. It coordinates business operations, access control, validation, cross-repository orchestration, notifications, and integration with surrounding infrastructure.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service`

##### Typical locations
- `src/main/java/io/gravitee/rest/api/service/**`
- `src/main/java/io/gravitee/rest/api/service/v4/**`
- `src/main/java/io/gravitee/rest/api/service/configuration/**`

##### Main dependencies
- Service Layer depends on Repository Layer for persistence access.
- Service Layer depends on Model Layer.
- Service Layer may depend on Mapping and Transformation Layer.
- Service Layer must not depend on Resource Layer.
- Service Layer may be orchestrated directly by legacy resources, but the preferred model is orchestration through the Use Case Layer.

#### Mapping and Transformation Layer
The codebase includes dedicated mapper packages and transformation helpers. This layer translates between repository objects, domain objects, API models, and transport-specific payloads.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-service`

##### Typical locations
- `src/main/java/**/mapper/**`
- `src/main/java/**/jackson/**`

##### Main dependencies
- Mapping and Transformation Layer is used by Resource Layer, Use Case Layer, and Service Layer.
- It may depend on Model Layer.
- It must not become a hidden orchestration layer for business workflows.

#### Model Layer
The backend uses multiple model forms, including API models, service-level entities, and repository representations. This layer carries the data structures used by contracts, orchestration, and persistence boundaries.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-model`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest`
- `gravitee-apim-repository`

##### Main dependencies
- Model Layer is shared across Resource Layer, Use Case Layer, Service Layer, Mapping and Transformation Layer, and Repository Layer.
- Keep model types separated by boundary when possible instead of collapsing all forms into one shared object.

#### Repository Layer
Repository APIs and their implementations live in repository-focused modules and adapters. This layer is responsible for persistence access and storage-specific querying, not for assembling higher-level business workflows.

##### Main modules
- `gravitee-apim-repository/gravitee-apim-repository-api`
- `gravitee-apim-repository/gravitee-apim-repository-jdbc`
- `gravitee-apim-repository/gravitee-apim-repository-mongodb`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-repository`

##### Typical locations
- `src/main/java/**/repository/**`
- `src/main/java/**/api/*Repository.java`

##### Main dependencies
- Repository Layer depends on persistence adapters and repository model types.
- Repository Layer must not depend on Resource Layer or Use Case Layer.
- Repository Layer serves the Service Layer.

#### Background and Integration Services Layer
The codebase also includes scheduled services, automation modules, integration controllers, fetchers, and sync-oriented components. These modules support asynchronous, batch, or integration-driven behaviors that do not fit the synchronous request/response flow of REST resources.

##### Main modules
- `gravitee-apim-rest-api/gravitee-apim-rest-api-services`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-integration-controller`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-fetcher`
- `gravitee-apim-rest-api/gravitee-apim-rest-api-automation`

##### Main dependencies
- Background and Integration Services Layer usually depends on Service Layer.
- Some modules may also expose their own Resource Layer or Use Case Layer when they provide dedicated APIs.
- This layer must not push business orchestration back into unrelated REST resources.

### Backend Dependency Direction
The preferred backend dependency direction is:
- `API Contract Layer -> Resource Layer -> Use Case Layer -> Service Layer -> Repository Layer`

Supporting dependencies:
- `Security Layer -> Resource Layer`
- `Security Layer -> Service Layer`
- `Mapping and Transformation Layer` is a supporting layer used by `Resource Layer`, `Use Case Layer`, and `Service Layer`
- `Model Layer` is a supporting layer used across the backend boundaries
- `Background and Integration Services Layer -> Service Layer`

### Backend Evolution Rule
For new backend delivery, use the Onion-style flow:
- `Resource -> UseCase -> Service -> Repository`

Direct `Resource -> Service -> Repository` still exists in legacy areas and should be treated as legacy.

When changing a legacy area:
- do not deepen the spaghetti pattern by adding more orchestration directly in the resource
- for meaningful new behavior, prefer introducing or extending a dedicated `UseCase`
- keep the resource focused on transport concerns
- treat mixed resource implementations as transitional, not as the target template for new endpoints

### Frontend Layer Map
The frontend is also broader than only `services` and `components`. Across Console, Portal Next, and the legacy Portal, the codebase shows a recurring structure of feature areas, reusable UI building blocks, API-facing services, and shared infrastructure.

#### Feature Pages and Screens
Feature-specific folders under `management`, `portal`, `app`, and `pages` represent routed screens, page-level containers, and feature composition points. This is where user-facing flows are assembled.

#### Reusable Components Layer
Reusable components live under shared component directories and feature-local `components` directories. This layer provides UI building blocks that can be composed by pages and larger feature views.

#### Frontend Service Layer
Frontend services exist in `services`, `services-ngx`, feature-local service folders, and generated SDK-style modules. They handle HTTP calls, orchestration of frontend state transitions, and communication with backend APIs.

#### Frontend Model Layer
The frontend uses `entities`, models, enums, and generated API types to represent application data. This layer defines the shapes consumed by pages, components, and services.

#### Shared Infrastructure Layer
Shared frontend infrastructure includes guards, resolvers, interceptors, validators, pipes, and utilities. These elements support routing, request processing, data preparation, and cross-cutting UI behavior.

#### SDK and Generated Client Layer
At least part of the frontend stack uses generated or semi-generated API client layers, for example in the legacy portal SDK and management API model definitions. This layer separates transport contracts from higher-level feature code.

### Layer Rules Structure
As this section evolves, define conventions under the corresponding layer heading instead of appending generic bullets at the end of the file.

#### Backend Rules by Layer
Use the following subsections as anchors for future backend rules:
- `#### API Contract Layer Rules`
- `#### Resource Layer Rules`
- `#### Security Layer Rules`
- `#### Use Case Layer Rules`
- `#### Service Layer Rules`
- `#### Mapping and Transformation Layer Rules`
- `#### Model Layer Rules`
- `#### Repository Layer Rules`
- `#### Background and Integration Services Layer Rules`

#### Repository Layer Rules
The repository layer in this codebase is an adapter layer over multiple data sources. It is not a purely generic CRUD abstraction.

##### Supported Repository Families
The repository modules show several repository families:
- management repositories over relational and document storage, mainly in `gravitee-apim-repository-jdbc` and `gravitee-apim-repository-mongodb`
- analytics and logs repositories in Elasticsearch modules
- rate limit and key-value style repositories in Redis-oriented modules
- no-op repositories used as alternative adapters

This means repository behavior depends on the storage family. Do not assume that all repositories have the same capabilities, pagination model, or query cost.

##### Repository API Shape
At API level, many management repositories extend `CrudRepository<T, ID>` or `FindAllRepository<T>`, but in practice they also expose domain-specific query methods.

Typical repository responsibilities include:
- single-entity CRUD
- collection reads by business reference, environment, role, status, or type
- paginated search for selected aggregates
- streamed search for large result sets
- bulk delete operations scoped by reference or environment
- persistence of aggregate children stored in separate tables or collections

Do not reduce the repository layer description to only `create`, `read`, `update`, and `delete`. Query-oriented methods are a normal part of this layer.

##### Querying, Filtering, and Pagination
Filtering and pagination are partly implemented in repositories and partly in higher layers, depending on the storage adapter and repository contract.

Repository-level querying already exists and should remain there when it is storage-driven:
- repositories such as `ApiRepository` expose criteria-based search APIs
- repository contracts may accept criteria objects, sortable objects, pageable objects, or field filters
- repository implementations may perform filtering, sorting, or pagination directly in SQL, Mongo queries, or storage-native APIs

Higher layers may still perform additional filtering or pagination when:
- a unified view is assembled from multiple repositories
- a legacy API endpoint paginates after loading a collection
- storage-specific limitations make exact repository-level pagination impractical

Do not move storage-native filtering logic upward without a strong reason. If the selection can be expressed naturally and efficiently in the storage adapter, keep it in the repository layer.

##### Batch and Aggregate Persistence
Repository implementations are not limited to writing one element at a time.

The current codebase already uses:
- single-entity `create`, `update`, and `delete`
- bulk delete methods returning deleted ids
- JDBC `batchUpdate` for child collections and aggregate substructures
- Mongo bulk-oriented utilities and batched processing for large sets

This is important because some repositories persist aggregates that span multiple tables or documents. In those cases, repository implementations are expected to persist related children, tags, groups, hooks, properties, or similar substructures as part of the repository operation.

##### Exception Handling
Repository APIs commonly declare `TechnicalException`, and the repository API also defines repository-specific exception types such as `RepositoryException`, `DuplicateKeyException`, and `AnalyticsException`.

In practice:
- JDBC repositories usually catch storage exceptions, log them, and wrap them in `TechnicalException`
- Mongo repositories often rely on underlying repository calls directly for simple operations and add explicit wrapping where needed for custom operations
- some repositories may still throw `IllegalStateException` for invalid update preconditions, such as updating a missing aggregate or null input

Repository implementations should convert storage-specific failures into repository-level technical exceptions instead of leaking storage-driver details through higher layers.

##### Cache Expectations
There is no evidence that management repositories rely on a general repository-layer cache as a primary design rule.

What exists today is narrower:
- specific repository probes classify some repository families as cacheable for health-check purposes
- individual implementations may use small local caches or storage-engine cache-friendly patterns

Do not assume a built-in cache exists for a repository operation unless the implementation clearly provides it. Service and use-case logic should treat repository calls as real I/O unless documented otherwise.

##### Source-of-Truth Rule
Repositories are storage adapters and query entry points. They are not the place for business orchestration across unrelated aggregates.

Use repositories for:
- storage-native selection
- aggregate persistence
- reference-based lookup
- storage-aware batch operations

Do not use repositories for:
- cross-domain orchestration that belongs in services or use cases
- transport-level response shaping
- policy decisions that require application-layer coordination

##### Testing Strategy
Repository testing in this project is already structured as layered repository contract testing.

The current testing model includes:
- shared repository tests in `gravitee-apim-repository-test`
- adapter-specific tests in JDBC, Mongo, Elasticsearch, Redis, and no-op modules
- repository-provider tests for adapter wiring
- focused internal tests for custom Mongo implementations and JDBC helper behavior

The shared repository tests act as behavioral contracts for repository APIs. Storage-specific modules are expected to satisfy those contracts.

When adding or changing repository behavior:
- update the shared repository contract test when the repository API behavior changes
- add or update adapter-specific tests when the change depends on JDBC, Mongo, Elasticsearch, Redis, or other storage-specific behavior
- cover custom query methods, bulk deletes, and aggregate child persistence paths, not only simple CRUD

##### Design Guidance for New Repository Work
When implementing new repository behavior:
- start from the repository API contract, not from one adapter implementation
- decide explicitly whether the operation belongs in management, analytics, log, rate-limit, distributed-sync, or key-value repository families
- keep storage-aware filtering and pagination in the repository if the storage can express them well
- expose dedicated query methods when the domain needs them instead of forcing callers to `findAll` and filter in memory
- model bulk delete or batch persistence explicitly when the operation is aggregate-scoped
- preserve parity between JDBC and Mongo management adapters when the repository contract is shared across them

#### Frontend Rules by Layer
Use the following subsections as anchors for future frontend rules:
- `#### Feature Pages and Screens Rules`
- `#### Reusable Components Layer Rules`
- `#### Frontend Service Layer Rules`
- `#### Frontend Model Layer Rules`
- `#### Shared Infrastructure Layer Rules`
- `#### SDK and Generated Client Layer Rules`

### Scope Note
This is an initial structure based on the layers visible in the current codebase. It is expected to be refined as we document implementation rules more precisely for each layer.

## Environment Baseline
### Runtime and Tool Versions
- Java: JDK 17+
- Maven: 3.9+
- Node.js: 22.12.x
- Yarn: 4.1.1
- Docker + Docker Compose for full-stack runtime

## Build and Run Guide by Module Type

### Java Modules (Maven)
Important: Java formatting is validated during Maven `validate` with `prettier-maven-plugin`.
Before Java build, run:
- `mvn prettier:write`

Then build:
- `mvn clean install -T 2C`

Fast local build option:
- `mvn clean install -T 2C -DskipTests=true -Dskip.validation=true`

Notes:
- `-Dskip.validation=true` skips prettier/license validation checks.
- Use it only when explicitly needed.

### Angular Nx Workspace (Console + Portal Next)
From repository root:
- Install: `yarn install`
- Console: `yarn console:serve`, `yarn console:build`, `yarn console:test`
- Portal Next: `yarn portal-next:serve`, `yarn portal-next:build`, `yarn portal-next:test`
- Global lint/format: `yarn lint`, `yarn prettier`, `yarn prettier:fix`

### Angular Legacy Portal (Independent Project)
From `gravitee-apim-portal-webui`:
- Install: `yarn install`
- Serve: `yarn serve`
- Build: `yarn build` or `yarn build:prod`
- Test: `yarn test`

## Command Matrix

### Java (All Modules)
- Working directory: repo root
- Install: n/a
- Format: `mvn prettier:write`
- Lint/validation: `mvn -DskipTests install`
- Test: `mvn test`
- Build: `mvn clean install -T 2C`
- Run/serve: use Docker compose flow from `CONTRIBUTING.adoc`

### Java (Single Module)
- Working directory: repo root
- Install: n/a
- Format: `mvn -pl <module> -am prettier:write`
- Lint/validation: `mvn -pl <module> -am -DskipTests install`
- Test: `mvn -pl <module> -am test`
- Build: `mvn -pl <module> -am clean install`
- Run/serve: module-specific runtime setup

### Nx Workspace (All Apps and Libraries)
- Working directory: repo root
- Install: `yarn install`
- Format: `yarn prettier:fix`
- Lint: `yarn lint`
- Test: `yarn test`
- Build: `yarn build`
- Run/serve: n/a

### Console (Nx App `console`)
- Working directory: repo root
- Install: `yarn install`
- Format: `yarn prettier:fix`
- Lint: `yarn lint` or `nx lint console`
- Test: `yarn console:test`
- Build: `yarn console:build`
- Run/serve: `yarn console:serve`

### Portal Next (Nx App `portal-next`)
- Working directory: repo root
- Install: `yarn install`
- Format: `yarn prettier:fix`
- Lint: `yarn lint` or `nx lint portal-next`
- Test: `yarn portal-next:test`
- Build: `yarn portal-next:build`
- Run/serve: `yarn portal-next:serve`

### Legacy Portal (`gravitee-apim-portal-webui`)
- Working directory: `gravitee-apim-portal-webui`
- Install: `yarn install`
- Format: `yarn prettier:fix`
- Lint: `yarn lint`
- Test: `yarn test`
- Build: `yarn build` or `yarn build:prod`
- Run/serve: `yarn serve`

### Notes
- For Java changes, run `mvn prettier:write` before build to avoid validation failures.
- For faster local Java builds when appropriate: `-DskipTests=true -Dskip.validation=true`.
- For Nx projects, prefer targeted commands (`yarn console:test`, `yarn portal-next:test`) when the scope is limited.

## Frontend Test Execution Guidance
### Preferred Targets
- Prefer `nx` project targets for frontend tests. Use:
  - `yarn console:test`
  - `yarn portal-next:test`

### Single Spec Execution
- For a single spec file, prefer positional `testFile` with Nx:
  - `yarn nx test console <file>.spec.ts --runInBand`
  - `yarn nx test portal-next <file>.spec.ts --runInBand`

### Focused Debugging
- For focused debugging, use `--runInBand` and optionally `-t "<test name regex>"`.
- Avoid relying on root-level broad test commands for local iteration (`yarn test`) when only one frontend app is impacted.
- Use direct `jest` invocation only as a fallback/debug path; standard workflow should stay on Nx targets for CI parity.
- If `jest` prints known config warnings (for example around `testTimeout`/`reporters`) but suites pass, treat them as non-blocking unless they start failing CI.

## Frontend Task Completion
After finishing any frontend implementation task, always run formatting and lint auto-fixes for the impacted frontend module before finalizing.

### Nx Modules
- Console: `yarn nx format:write --projects=console` then `yarn nx lint console --fix`
- Portal Next: `yarn nx format:write --projects=portal-next` then `yarn nx lint portal-next --fix`

### Legacy Portal Module
- In `gravitee-apim-portal-webui`: `yarn prettier:fix` then `yarn lint:fix`

### Notes
- Keep the scope module-specific; do not run global fixes unless the task touches multiple frontend modules.
- If frontend work spans multiple modules, run the same sequence for each touched module.

## Requirements Management Plan
Epic documentation structure, requirements-documentation rules, story conventions, decision/meeting artifacts, and implementation-plan rules are maintained in `features/RequirementsManagementPlan.md`.

When working on anything under `features/`, follow that document as the source of truth for:
- Epic directory structure
- required requirements sections
- story file content
- decision and meeting note conventions
- implementation plan and implementation task conventions
- readiness rules for starting implementation

## Change Scope Rules
- Deliver the smallest end-to-end change that solves the problem.
- Do not mix unrelated refactors with functional work.
- Keep contracts backward-compatible unless scope explicitly includes contract changes.
- For UI changes, include accessibility and error/empty states in scope.

## Testing Expectations
- Add or update tests for every new branch/guard/behavior.
- Cover negative paths, not only happy paths.
- Frontend: update Jest/harness tests where behavior is enforced.
- Backend: run focused module tests and impacted integration tests when behavior crosses module boundaries.

## Quality Gates
Before finalizing changes, run checks relevant to touched modules.

### Recommended Minimum Set
- Java changes: `mvn prettier:write` then module build/tests
- Nx Angular changes: targeted `yarn <app>:test` + lint/format checks
- Legacy portal changes: `yarn lint` and `yarn test` in `gravitee-apim-portal-webui`

## Documentation and Traceability
- Update documentation when behavior, workflows, or operational commands change.
- Keep file-level and PR-level change summaries concise and reproducible.
- Record known assumptions, constraints, and follow-up items.

## Definition of Done
A change is done when all conditions are met:
- Functional requirements are implemented.
- Relevant tests are implemented/updated and passing.
- Required quality gates pass for affected modules.
- Implementation plan in `features/<epic-jira-id>-<short-slug>/<epic-jira-id>-implementation-plan.md` is aligned with delivered scope.

## Additional Topics That Should Be Maintained in This File
As the project evolves, keep these topics updated:
- Module ownership or maintainer map
- CI/CD parity commands for local verification
- Performance/regression test entrypoints for critical flows
- Release-sensitive workflows (distribution packaging, Docker image build order)
- Known build pitfalls and workarounds (for example JDK-specific constraints)

## Repository-Local Skills
This repository may define additional workflow skills under `features/_skills/`.

### Repository-Local Skill Lookup Rules
- Treat `features/_skills/<skill-name>/SKILL.md` as the canonical location for a repository-local skill.
- If the user refers to a repository-local skill by name, for example `story-clarifier`, check `features/_skills/<skill-name>/SKILL.md` first.
- If the skill exists, use its workflow guidance for the current request.
- If the skill does not exist, state that briefly and continue with the best fallback approach.

### Prompt Usage Convention
- Users may refer to repository-local skills by short name instead of full path.
- Example: `Use story-clarifier for APIM-12280`
- Example: `Continue with story-clarifier`
- Example: `Apply story-clarifier to this story`

### Scope Note
- This convention is a repository-local workflow rule.
- It does not automatically register repository-local skills as globally installed Codex skills.
- It exists so short skill names can be used reliably inside this repository context.
