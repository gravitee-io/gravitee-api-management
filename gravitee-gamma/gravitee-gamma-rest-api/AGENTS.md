# Coding Guidelines for gravitee-gamma-rest-api

This module hosts the **Gamma host application** (Jersey app mounted at `/gamma` by the apim standalone container) plus any **global, cross-module REST resources** that every gamma module's UI calls (e.g. the trace explorer). It follows **Clean Architecture** (Hexagonal) for any new business logic added here. These rules mirror the patterns established in `gravitee-apim-rest-api-service` under `io.gravitee.apim.core` / `io.gravitee.apim.infra` and the per-module gamma AGENTS guides (`gravitee-gamma-module-apim`, `gravitee-gamma-module-platform`), adapted to the gamma-host package layout.

## 1. Package Layout

Each domain within the module follows this structure:

```
io.gravitee.gamma.rest/
├── GammaModuleApplication              # Jersey app bootstrap (host-level, untouched)
│
├── resources/                          # ALL JAX-RS resources live here
│   ├── GammaRootResource               # `/` — routes to /modules + /observability/* etc.
│   ├── GammaModulesResource            # `/modules/{pluginId}/...` plugin-dispatch
│   ├── GammaUIResource                 # `/ui/bootstrap`, asset serving
│   └── <domain>/                       # per-domain global resources (e.g. tracing/)
│       ├── XxxResource                 # JAX-RS endpoints
│       ├── dto/                        # Request / response DTO records
│       └── exception/                  # Per-domain exception mappers (if any)
│
├── core/                               # Business logic (framework-free), per-domain
│   └── <domain>/                       # e.g. tracing/
│       ├── model/                      # Domain entities, value objects
│       ├── use_case/                   # One class per user/system action (@UseCase)
│       ├── domain_service/             # Reusable cross-use-case business logic
│       ├── exception/                  # Domain-specific exceptions
│       └── port/
│           ├── repository/             # Ports onto a data store (DB / index)
│           └── service_provider/       # Ports onto an external service / SPI
│
└── infra/                              # Framework & persistence wiring
    ├── adapter/                        # Port impls + model converters (Anticorruption Layer)
    ├── repository/<domain>/            # Repository implementations (Spring @Repository)
    ├── service_provider/<domain>/      # Service-provider port implementations
    └── config/                         # @Configuration classes — one per domain
```

**Single REST root** at `io.gravitee.gamma.rest.resources` (note: plural). Existing host
resources (`GammaRootResource`, `GammaModulesResource`, `GammaUIResource`) sit at the root of
that package and handle infrastructure routing. New per-domain resources nest under
`resources/<domain>/` — keeps every JAX-RS class discoverable from the same place and avoids
the resource/resources singular/plural confusion.

## 2. Naming Conventions

| Artifact             | Suffix                | Annotation             | Package                              |
| -------------------- | --------------------- | ---------------------- | ------------------------------------ |
| Use case class       | `UseCase`             | `@UseCase`             | `core.<domain>.use_case`             |
| Domain service class | `DomainService`       | `@DomainService`       | `core.<domain>.domain_service`       |
| Repository interface | `Repository`          | —                      | `core.<domain>.port.repository`      |
| Service provider port | `Port` (or contextual) | —                    | `core.<domain>.port.service_provider` |
| Repository impl      | `MongoRepository`     | `@Repository`          | `infra.repository.<domain>`          |
| Adapter              | `Adapter`             | —                      | `infra.adapter`                      |
| Domain exception     | context-specific name | extends base exception | `core.<domain>.exception`            |

Notes specific to this module:

- **Port naming clarification**: a port that fronts the platform's own repository SPI (e.g. `TracingRepository` from `gravitee-apim-repository-api`) lives under `port/service_provider/` — from this module's perspective the SPI is an external service we consume, not a data store we own. Reserve `port/repository/` for ports onto data we'd persist ourselves.
- **ArchUnit rule (inherited from the apim modules)**: any class whose simple name ends with `Adapter` must reside under `infra.adapter..`. Even port-impl-style adapters (`XxxPortAdapter`) live in `infra/adapter/` to satisfy this. If you want a non-`Adapter` location, name the class differently (e.g. `RepositoryBackedXxxPort` in `infra/service_provider/<domain>/`).

## 3. Use Case Rules

- Annotate with `@UseCase` (annotation from gravitee-apim-common — provides `@Transactional` and marks the class as a Spring component).
- **One use case = one user/system action.**
- IMPORTANT: Use cases **CANNOT** depend on other use cases.
- Allowed dependencies: `Repository`, `DomainService`, `ServiceProvider` only.
- Define `Input` and `Output` as Java **records** (inner classes of the use case).
- Single `public Output execute(Input input)` method.
- Constructor injection (explicit or `@RequiredArgsConstructor` / `@AllArgsConstructor`).

Example:

```java
@UseCase
@AllArgsConstructor
public class GetTraceDetailUseCase {

    private final TracingPort tracingPort;
    private final OtelLogPort otelLogPort;

    public record Input(String organizationId, String environmentId, String module, String apiId, String traceId) {}
    public record Output(Optional<TraceDetail> trace) {}

    public Output execute(Input input) {
        // …
    }
}
```

## 4. Domain Service Rules

- Annotate with `@DomainService` (annotation from gravitee-apim-common).
- Contains reusable business logic shared across multiple use cases.
- **Framework-agnostic** — no Spring dependencies, no dependencies to infra packages.
- Can be called by use cases and other domain services.
- A DomainService is a class not an interface. If it needs to use a Legacy service, it should call a LegacyService wrapper from the port package.

## 5. Core Layer Independence

The `core` package must **not** depend on Spring, infrastructure, or external framework libraries.

Allowed dependencies:

- `java.*`, `jakarta.*`
- `lombok.*`
- `com.fasterxml.*` (Jackson)
- `org.slf4j.*`
- `io.gravitee.definition.*`, `io.gravitee.common.*`

Only exception: `@UseCase` uses `@Transactional` (accepted trade-off).

## 6. Infrastructure Layer

- Generate adapters to convert between repository / SPI models and core domain models (Anticorruption Layer).
- Adapter pattern: static `toCoreModel(...)` / `toRepository(...)` methods, or the `XxxAdapter.INSTANCE.toCoreModel(...)` singleton with MapStruct — both are accepted.
- **Spring wiring** lives in `infra/config/<DomainName>Configuration.java`, one per domain. Each domain's config:
    1. Uses `@ComponentScan` with an `@UseCase`-filtered include to pick up the domain's use cases — the rest-api parent context doesn't scan `io.gravitee.gamma.rest.*` by default, so the per-domain config has to opt in explicitly.
    2. Declares `@Bean` factories for the adapter port impls — these aren't Spring components on their own.
    3. Is `@Import`ed from `StandaloneConfiguration` so its beans land in the parent rest-api context (the Jersey `/gamma` app inherits beans from the parent).

Example:

```java
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.tracing",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class)
)
public class GammaTracingConfiguration {

    @Bean
    public TracingPort tracingPort(TracingRepository tracingRepository) {
        return new TracingPortAdapter(tracingRepository);
    }
    // …
}
```

## 7. Domain Models

- Domain behavior methods are preferred on model classes. We apply POO principles to domain models. We avoid anemic models.
- Separate input classes for creation / update: `NewXxxCommand`, `UpdateXxxCommand`.

## 8. Exception Hierarchy

- Use base exceptions from gravitee-apim-common: `NotFoundDomainException`, `ValidationDomainException`, `TechnicalDomainException` in `core.<domain>.exception`.
- Domain exceptions extend these bases with descriptive names (e.g. `TraceNotFoundException`).
- Map domain exceptions to HTTP status codes in the JAX-RS layer (or rely on the apim management-rest exception mappers, which already cover the base types).

## 9. REST Layer Conventions

- JAX-RS: `@Path`, `@GET` / `@POST` / etc., `@Produces(MediaType.APPLICATION_JSON)`.
- Inject with `@Inject` (Jakarta), not `@Autowired`.
- **REST resources call use cases** — never repositories directly.
- New global resources must be:
    1. Registered in `GammaModuleApplication` (`register(MyResource.class);`).
    2. Mounted at a stable URL by `GammaRootResource` — preferably under a top-level namespace like `/observability/<domain>` so the URL is module-agnostic.
- Use records for simple response DTOs.
- For resource creation endpoints, the id should come from the request rather than being generated by the use case.

## 10. Testing

### Domain Tests

- **Fixtures-first**: every `// Given` goes through fixtures — never direct entity instantiation or raw repository calls. This keeps the in-memory object graph consistent and shields tests from signature refactors.
- **Fixture tree**: a `RootFixture` exposes sub-fixtures per domain as **public fields**. Fields are public and non-final so a test can swap in a specific implementation and rebuild the graph when needed.
- **One sub-fixture, one responsibility**: each sub-fixture owns its use cases and in-memory repositories / port impls, and exposes `givenXxx(...)` methods that return the persisted entity, ready to feed the `// When`.
- **Lambda customization**: `givenXxx` methods accept a `UnaryOperator<XxxBuilder>` to override defaults without bloating the API surface. Defaults must produce a valid entity with no argument.
- **Test structure**: `// Given` / `// When` / `// Then`, snake_case method names, `@DisplayNameGeneration`, no mocks — fixtures provide real instances wired to in-memory repositories.

### Port Tests

- **Shared contract**: for each port (repository or service provider), define an abstract `XxxPortContractTest` that describes the expected behaviour. Every implementation must honor it.
- **Dual implementation by default**: ship an in-memory variant for domain tests and a real-backend variant (Mongo, ES via Testcontainers, …) for integration verification.

### REST Tests

- Extend `AbstractResourceTest` (in `io.gravitee.gamma.rest.resource` test-tree) for the Jersey test harness — provides a Spring context plus the mocked services.

### Architecture Tests (ArchUnit)

- Create ArchUnit tests enforcing:
    - Naming conventions (suffixes match the table in §2).
    - Use cases reside in `use_case` packages.
    - Use cases do not depend on other use cases.
    - Core layer only depends on allowed packages (§5).
    - Dependency direction: REST → core → infra.
    - All classes ending in `Adapter` reside under `infra.adapter..`.

## 11. When to add code here vs in a gamma module plugin

- **Here (gamma-rest-api)**: cross-cutting / global resources that any gamma module's UI calls — observability (traces, logs, metrics), tenant-wide settings, host-level routing. The data being served isn't owned by a specific gamma module.
- **In a gamma module plugin (`gravitee-gamma-module-*`)**: domain-specific endpoints owned by one gamma module's product surface (LLM router CRUD, MCP proxy management, API products, …). The data IS owned by that module.

If unsure: prefer the module plugin. Promoting to the host requires coordinating with every module's UI and is harder to evolve once shipped.
