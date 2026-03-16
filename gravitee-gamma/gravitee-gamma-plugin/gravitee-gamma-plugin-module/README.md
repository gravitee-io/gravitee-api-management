# Gravitee Gamma Modules

## What is Gravitee Gamma?

Gravitee Gamma is the next-generation unified control plane for the Gravitee
platform. It delivers a modular, domain-oriented management experience covering:

- **API Management** — HTTP and TCP API lifecycle
- **Event Stream Management** — Kafka APIs and event governance
- **AI Agent Management** — AI Gateway, MCP, agents, AI Fabric
- **Authentication & Authorization** — Identity, access, and fine-grained authorization
- …and future product areas

APIM remains the runtime container. Gamma modules are standard Gravitee
plugins that plug into the APIM Management API, adding domain-specific backends
and frontends without modifying the core distribution.

## What is a Gamma Module?

A self-contained plugin that extends the Gravitee Management API with:

- **A backend** (optional) — Jersey/JAX-RS REST resources with full Spring context
- **A frontend** (optional) — React micro-frontend loaded via Module Federation
- **Spring integration** — Each module gets its own ApplicationContext as a
  child of the Management API context, with access to shared infrastructure beans
- **MongoDB access** — Modules can declare Spring Data repositories that
  reuse the management MongoDB connection (`managementMongoTemplate`)
- **Permission enforcement** — Modules reuse Gravitee's `@Permissions`
  annotation model for role-based access control

## Architecture

### Plugin Discovery

At startup, `GammaModuleHandler` discovers plugins of type `gamma-module`,
creates a Spring ApplicationContext per plugin, registers JAX-RS resource
classes, and serves frontend assets.

### REST Endpoints

```
GET  /gamma/modules/                              → list modules + manifests
GET  /gamma/modules/{id}/assets/{path}            → serve frontend assets
*    /gamma/modules/{id}/{path}                    → module backend (no env)
*    /gamma/modules/environments/{envId}/{id}/...  → module backend (env-scoped)
```

### Spring Context & Dependency Injection

Each module plugin gets its own child ApplicationContext. The plugin's
Spring configuration class (declared in plugin.properties via the main class's
package) can:

- Define @Bean methods
- Use @EnableMongoRepositories with mongoTemplateRef = "managementMongoTemplate"
- Inject beans from the parent context (Configuration, Environment, etc.)
- Use Spring Data MongoDB repositories with @Document models

Example configuration from a Gamma module:

```java
@Configuration
@EnableMongoRepositories(
    basePackages = "com.example.gamma.module.repository",
    mongoTemplateRef = "managementMongoTemplate"
)
public class MyModuleConfiguration {
    @Bean
    public MyService myService(MyRepository repo, Configuration config) {
        return new MyService(repo, config);
    }
}
```

### MongoDB Integration

Modules can define Spring Data MongoDB entities and repositories that
operate on the same management database used by APIM:

- `@Document` with SpEL prefix: `@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}my_collection")`
- `@Repository` interfaces extending `MongoRepository<T, ID>`
- `@CompoundIndex`, `@Indexed` (including TTL indexes)
- Dependencies (`spring-data-mongodb`, `mongodb-driver-sync`) are declared as
  provided scope — they come from the parent classloader

### Frontend (Module Federation)

The frontend architecture uses Rspack/Webpack Module Federation with React.
The Gamma host application is a lightweight shell that dynamically loads
module frontends as remotes at runtime.

Module frontends:
- Are compiled as independent Module Federation remotes
- Export a root React component mounted by the host
- Share core libraries (react, react-dom, react-router-dom) with the host
- Manage their own internal routing, state, and API calls
- Frontend assets are served by the backend via `GET /modules/{id}/assets/`

For a detailed walkthrough of the host application, module discovery, and
lazy-loading lifecycle, see
[`gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md`](../../gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md).

## Plugin Structure

A Gamma module ZIP:

```
gamma-module-example-1.0.0.zip
├── gamma-module-example-1.0.0.jar   # Backend
├── lib/                                 # Runtime dependencies
└── ui/                                  # Frontend assets (optional)
    ├── mf-manifest.json
    ├── remoteEntry.js
    └── ...
```

### plugin.properties

```properties
id=example
name=Example
type=gamma-module
version=${project.version}
description=An example Gamma module
class=com.example.gamma.module.ExampleModule
```

### Security & Permissions

Modules reuse Gravitee's permission model. Annotate JAX-RS methods:

```java
@POST
@Permissions({ @Permission(
    value = RolePermission.ENVIRONMENT_API,
    acls = { RolePermissionAction.CREATE }) })
public Response create(...) { ... }
```

Environment-scoped permissions require the env-scoped path:
`/gamma/modules/environments/{envId}/{pluginId}/...`

## Building

```bash
mvn clean install
mvn clean install -DskipTests -Dskip.validation=true  # fast
```
