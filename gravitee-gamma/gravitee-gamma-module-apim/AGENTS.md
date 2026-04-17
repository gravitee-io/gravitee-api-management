# Coding Guidelines for gravitee-gamma-module-apim

This module follows **Clean Architecture** (Hexagonal) for all backend code. These rules mirror the patterns established in `gravitee-apim-rest-api-service` under `io.gravitee.apim.core` / `io.gravitee.apim.infra`, using shared annotations and base classes from `gravitee-apim-common`.

## 1. Package Layout

Each domain within the module follows this structure:

```
io.gravitee.gamma.module.apim/
├── rest/                              
│   ├── resource/                      # @RestController 
│   ├── exception/                     # Exception handlers
├── core/                              # Business logic (framework-free)
│   ├── <domain>/
│   │   ├── model/                     # Domain entities, value objects
│   │   ├── use_case/                  # One class per user/system action
│   │   ├── domain_service/            # Reusable cross-use-case business logic
│   │   └── exception/                 # Domain-specific exceptions
│   ├── port/
│   │   ├── repository/                # DB interface
│   │   └── service_provider/          # Optional provider interfaces
└── infra/                             # Framework & persistence wiring
    ├── repository/<domain>/           # Repository implementations (Spring @Repository)
    ├── service_provider/              # Provider implementations (Spring @Component)
    └── adapter/                       # Adapters (repo model <-> core model)
```

## 2. Naming Conventions

| Artifact             | Suffix                | Annotation           | Package                        |
|----------------------|-----------------------|----------------------|--------------------------------|
| Use case class       | `UseCase`             | `@UseCase`           | `core.<domain>.use_case`       |
| Domain service class | `DomainService`       | `@DomainService`     | `core.<domain>.domain_service` |
| Repository interface | `Repository`          | —                    | `core.<domain>.repository`     |
| Repository impl      | `MongoRepository` | `@Repository`        | `infra.repository.<domain>`    |
| Domain exception     | context-specific name | extends base exception | `core.<domain>.exception`      |
| Adapter              | `Adapter`             | —             | `infra.adapter`                |

## 3. Use Case Rules

- Annotate with `@UseCase` (annotation from gravitee-apim-common — provides `@Transactional`).
- **One use case = one user/system action.**
- IMPORTANT: Use cases **CANNOT** depend on other use cases.
- Allowed dependencies: `Repository`, `DomainService`, `ServiceProvider` only.
- Define `Input` and `Output` as Java **records** (inner classes of the use case).
- Single `public Output execute(Input input)` method.
- Constructor injection (explicit or `@RequiredArgsConstructor`).

Example:

```java
@UseCase
@RequiredArgsConstructor
public class GetClusterUseCase {

    private final ClusterRepository clusterRepository;

    public record Input(String clusterId, String environmentId) {}
    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        var cluster = clusterRepository.get(input.clusterId());
        return new Output(cluster);
    }
}
```

## 4. Domain Service Rules

- Annotate with `@DomainService` (annotation from gravitee-apim-common).
- Contains reusable business logic shared across multiple use cases.
- **Framework-agnostic** — no Spring dependencies, no dependencies to infra packages.
- Can be called by use cases and other domain services.
- A DomainService is a class not an interface. If it needs to use a Legacy service, it should call a LegacyService wrapper from port package.


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

- Generate adapters to convert between repository models and core domain models (Anticorruption Layer).
- Adapter pattern: `XxxAdapter.INSTANCE.toCoreModel(...)` / `.toRepository(...)`.

Example with MapStruct (but any other mapper library or in-house implementation works):

```java
@Mapper
public interface ClusterAdapter {
    ClusterAdapter INSTANCE = Mappers.getMapper(ClusterAdapter.class);

    Cluster toCoreModel(io.gravitee.repository.management.model.Cluster source);
    io.gravitee.repository.management.model.Cluster toRepository(Cluster source);
}
```

## 7. Domain Models

- Domain behavior methods are preferred on model classes. We apply POO principles to domain models. We avoid anemic models
- Separate input classes for creation/update: `NewXxxCommand`, `UpdateXxxCommand`.

## 8. Exception Hierarchy

- Use base exceptions from gravitee-apim-common: `NotFoundDomainException`, `ValidationDomainException`, `TechnicalDomainException` in `core.exception`.
- Domain exceptions extend these bases with descriptive names (e.g., `ClusterNotFoundException`).

## 9. REST Layer Conventions

- JAX-RS: `@Path`, `@GET`/`@POST`/etc., `@Produces(MediaType.APPLICATION_JSON)`.
- Inject with `@Inject` (Jakarta), not `@Autowired`.
- **REST resources call use cases** — never Repository directly.
- Register a parent resource in `HttpApiManagementModule`. Each sub-resource should be referenced in that parent resource.
- Use records for simple response DTOs.
- For resource creating stuff, the id should come from the request instead of the usecase generating one.


## 10. Testing

### Domain Tests

- **Fixtures-first**: every `// Given` goes through fixtures — never direct entity instantiation or raw repository calls. This keeps the in-memory object graph consistent and shields tests from signature refactors.
- **Fixture tree**: a `RootFixture` exposes sub-fixtures per sub-domain as **public fields** (`rootFixture.apiFixture`, `rootFixture.planFixture`, `rootFixture.subscriptionFixture`, ...). Fields are public and non-final so a test can swap in a specific implementation and rebuild the graph when needed.
- **One sub-fixture, one responsibility**: each sub-fixture owns its use-cases and in-memory repositories, and exposes `givenXxx(...)` methods that return the persisted entity, ready to feed the `// When`.
- **Lambda customization**: `givenXxx` methods accept a `UnaryOperator<XxxBuilder>` to override defaults without bloating the API surface. Defaults must produce a valid entity with no argument.

  ```java
  public class ApiFixture {
      public final CreateApiUseCase createApiUseCase;
      public final ApiCrudServiceInMemory apiCrudService;

      public Api givenAnApi() {
          return givenAnApi(builder -> builder);
      }

      public Api givenAnApi(UnaryOperator<Api.ApiBuilder> customizer) {
          Api.ApiBuilder defaults = Api.builder()
              .id(UUID.randomUUID().toString())
              .name("default-api")
              .version("1.0");
          return createApiUseCase.execute(customizer.apply(defaults).build());
      }
  }

  // Usage in a test
  var api = rootFixture.apiFixture.givenAnApi(a -> a.name("my-api"));
  ```

- **Cross-domain scenarios**: a `givenAnApiWithAPlan()` orchestrates multiple sub-domains. Placement rule:
    - Scenario stays **within one** sub-domain → method on the sub-fixture (`apiFixture.givenAnApiWithMetadata(...)`).
    - Scenario **crosses** sub-domains → method on `RootFixture` (`rootFixture.givenAnApiWithAPlan()`), delegating to sub-fixtures.
    - When cross-domain scenarios grow (> ~5 per theme), extract them into dedicated `XxxScenarios` classes (e.g. `SubscriptionScenarios`) wired into `RootFixture` — prevents `RootFixture` from becoming a god-object.
- **Test structure**: `// Given` / `// When` / `// Then`, snake_case method names, `@DisplayNameGeneration`, no mocks — fixtures provide real instances wired to in-memory repositories.

### Repository Tests

- **Shared contract**: for each repository, define an abstract `XxxRepositoryContractTest` that describes the expected behavior (CRUD, queries, sorting, pagination, edge cases such as missing entity / duplicate / null field). This is the **contract** every implementation must honor.
- **Dual implementation by default**: each contract is executed by at least two concrete suites — `XxxMongoRepositoryTest extends XxxRepositoryContractTest` (Testcontainers) and `XxxInMemoryRepositoryTest extends XxxRepositoryContractTest`. Domain fixtures rely on the in-memory variant, so any Mongo/in-memory divergence would silently break domain tests — the contract pins that equivalence.
- **No implementation-specific tests** unless justified (e.g. Mongo indexes): all functional behavior lives in the contract.

### REST tests

- Extend `AbstractResourceTest` for the Jersey test harness.

### Architecture tests (ArchUnit)

- Create ArchUnit tests enforcing:
    - Naming conventions (suffixes match table in section 2).
    - Use cases reside in `use_case` packages.
    - Use cases do not depend on other use cases.
    - Core layer only depends on allowed packages (section 5).
    - Enforce this dependency direction: REST -> core -> infra.
