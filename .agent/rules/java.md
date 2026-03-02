---
description: Java coding standards, Vert.x patterns, and Maven build rules
trigger: always_on
---

# Java Architecture Rules

## Naming Conventions

Enforced by ArchUnit tests in `gravitee-apim-rest-api-service/src/test/java/architecture/`:

| Type            | Interface        | Implementation       |
| --------------- | ---------------- | -------------------- |
| Use Cases       | -                | `*UseCase`           |
| CRUD Services   | `*CrudService`   | `*CrudServiceImpl`   |
| Domain Services | `*DomainService` | `*DomainServiceImpl` |

## Package Structure (Onion Architecture)

```
io.gravitee.apim.core.{domain}
├── model/           # Domain entities
├── usecase/         # Application services/use cases
├── crud_service/    # CRUD service interfaces
├── query_service/   # Query service interfaces
└── domain_service/  # Domain service interfaces

io.gravitee.apim.infra.{domain}
├── crud_service/    # CRUD implementations
├── adapter/         # External adapters
└── ...
```

**Rules:**

- Domain layer has NO dependencies on infrastructure
- Use cases orchestrate domain services
- Infrastructure adapters implement domain interfaces

## Vert.x & Non-Blocking (Critical!)

> ⚠️ **The Gateway uses Vert.x event loops. NEVER block the main thread.**

### Key Patterns

- Use `Verticle` pattern for async services (healthcheck, sync, discovery)
- All I/O must be async: `Future`, `Promise`, RxJava, reactive streams
- Policy execution must be non-blocking
- Use `-Dvertx.options.maxEventLoopExecuteTime=1000000000000` for debugging

### Common Verticles

- `EndpointHealthcheckVerticle` - Health checks
- `EndpointDiscoveryVerticle` - Service discovery
- `DebugHttpProtocolVerticle` - Debug mode

### Anti-Patterns (AVOID)

```java
// ❌ BLOCKING - will freeze event loop
Thread.sleep(1000);
someBlockingHttpCall();
file.readSync();

// ✅ NON-BLOCKING
vertx.setTimer(1000, id -> { ... });
webClient.get(...).send().onSuccess(resp -> { ... });
vertx.fileSystem().readFile(...).onSuccess(buffer -> { ... });
```

## Build Commands

| Task                    | Command                                                 |
| ----------------------- | ------------------------------------------------------- |
| Quick build (no tests)  | `mvn clean install -DskipTests -Dskip.validation -T 2C` |
| Full build              | `mvn clean install -T 2C`                               |
| Dev bundle with plugins | `mvn clean install -T 2C -P bundle-default,bundle-dev`  |
| Prettier format         | `mvn prettier:write`                                    |
| License headers         | `mvn license:format`                                    |
| Integration tests       | See `Taskfile.dist.yml` → `task integration-test`       |

## Code Style

- Prettier: 140 print width, 4-space tabs
- Apache 2.0 license headers required
- Use `@author` Javadoc tags

## Main Classes

| Component | Main Class                                              | Module                                        |
| --------- | ------------------------------------------------------- | --------------------------------------------- |
| Gateway   | `io.gravitee.gateway.standalone.GatewayContainer`       | `gravitee-apim-gateway-standalone-container`  |
| REST API  | `io.gravitee.rest.api.standalone.GraviteeApisContainer` | `gravitee-apim-rest-api-standalone-container` |

## Environment Variables

Common env vars for local development:

```bash
# MongoDB (default for local)
gravitee_management_mongodb_uri=mongodb://localhost:27017/gravitee

# Elasticsearch
gravitee_reporters_elasticsearch_endpoints_0=http://localhost:9200

# JDBC (PostgreSQL alternative)
GRAVITEE_MANAGEMENT_TYPE=jdbc
GRAVITEE_MANAGEMENT_JDBC_URL=jdbc:postgresql://localhost:5432/gravitee
```
