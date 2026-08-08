---
name: Gamma Host Application
layer: local
dirs: [gravitee-gamma/gravitee-gamma-rest-api]
description: How gravitee-gamma-rest-api deviates from the shared gamma Clean Architecture rules - host layout, port naming, Spring wiring, resource registration, and when code belongs here vs in a module plugin
---

# Gamma Host Application

This module hosts the **Gamma host application** (Jersey app mounted at `/gamma` by the apim standalone container) plus any **global, cross-module REST resources** that every gamma module's UI calls (e.g. the trace explorer). The shared Gamma Clean Architecture rules apply; this rule carries only where the host deviates from them, referencing their section numbers.

## Layout (deviates from §1)

The package root is `io.gravitee.gamma.rest`, and three things differ from the module layout:

- **Single REST root** at `io.gravitee.gamma.rest.resources` (note: plural). Existing host resources (`GammaRootResource`, `GammaModulesResource`, `GammaUIResource`) sit at the root of that package and handle infrastructure routing (`/` routing, `/modules/{pluginId}/...` plugin dispatch, `/ui/bootstrap` and asset serving); `GammaModuleApplication` is the Jersey bootstrap, untouched. New per-domain resources nest under `resources/<domain>/` with their `dto/` and `exception/` subpackages — keeps every JAX-RS class discoverable from the same place and avoids the resource/resources singular/plural confusion.
- **Ports live per domain**: `core/<domain>/port/repository/` and `core/<domain>/port/service_provider/` (the module layout keeps `core/port/` beside the domains).
- **Spring wiring lives in `infra/config/`** — one `@Configuration` class per domain (see the wiring section below).

## Naming (adds to §2)

- Service-provider ports take the `Port` suffix (or a contextual name) in `core.<domain>.port.service_provider`.
- **Port naming clarification**: a port that fronts the platform's own repository SPI (e.g. `TracingRepository` from `gravitee-apim-repository-api`) lives under `port/service_provider/` — from this module's perspective the SPI is an external service we consume, not a data store we own. Reserve `port/repository/` for ports onto data we'd persist ourselves.
- **ArchUnit rule (inherited from the apim modules)**: any class whose simple name ends with `Adapter` must reside under `infra.adapter..`. Even port-impl-style adapters (`XxxPortAdapter`) live in `infra/adapter/` to satisfy this. If you want a non-`Adapter` location, name the class differently (e.g. `RepositoryBackedXxxPort` in `infra/service_provider/<domain>/`).

## Spring wiring (adds to §6)

Adapters here may also use static `toCoreModel(...)` / `toRepository(...)` methods — both the static form and the MapStruct singleton are accepted. Each domain's `infra/config/<DomainName>Configuration.java`:

1. Uses `@ComponentScan` with an `@UseCase`-filtered include to pick up the domain's use cases — the rest-api parent context doesn't scan `io.gravitee.gamma.rest.*` by default, so the per-domain config has to opt in explicitly.
2. Declares `@Bean` factories for the adapter port impls — these aren't Spring components on their own.
3. Is `@Import`ed from `StandaloneConfiguration` so its beans land in the parent rest-api context (the Jersey `/gamma` app inherits beans from the parent).

When several sibling sub-domains share an umbrella domain (e.g. traces/filters/logs/analytics/dashboards all under Observability, mounted under `/observability/*` by `GammaRootResource`), add a beanless aggregator `Gamma<Umbrella>Configuration` that only `@Import`s the sub-domain configs (see `GammaObservabilityConfiguration`), and `@Import` that single aggregator from `StandaloneConfiguration` instead of every sub-domain config individually. Each sub-domain still owns its own config file — the aggregator doesn't replace step 3 above, it just gives `StandaloneConfiguration` one entry point per umbrella domain instead of one per sub-domain.

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
}
```

## Exceptions (adds to §8)

- Map domain exceptions to HTTP status codes in the JAX-RS layer, or rely on the apim management-rest exception mappers, which already cover the base types.

## REST registration (refines §9)

New global resources must be:

1. Registered in `GammaModuleApplication` (`register(MyResource.class);`).
2. Mounted at a stable URL by `GammaRootResource` — preferably under a top-level namespace like `/observability/<domain>` so the URL is module-agnostic.

## Testing (adapts §10)

- **Port contract tests** replace the module rule's repository contract tests: for each port (repository or service provider), define an abstract `XxxPortContractTest` every implementation must honor — an in-memory variant for domain tests and a real-backend variant (Mongo, ES via Testcontainers, ...) for integration verification.
- REST tests extend `AbstractResourceTest` in the `io.gravitee.gamma.rest.resource` test tree — provides a Spring context plus the mocked services.
- The ArchUnit suite additionally enforces that all classes ending in `Adapter` reside under `infra.adapter..`.

## 11. When to add code here vs in a gamma module plugin

- **Here (gamma-rest-api)**: cross-cutting / global resources that any gamma module's UI calls — observability (traces, logs, metrics), tenant-wide settings, host-level routing. The data being served isn't owned by a specific gamma module.
- **In a gamma module plugin (`gravitee-gamma-module-*`)**: domain-specific endpoints owned by one gamma module's product surface (LLM router CRUD, MCP proxy management, API products, ...). The data IS owned by that module.

If unsure: prefer the module plugin. Promoting to the host requires coordinating with every module's UI and is harder to evolve once shipped.
