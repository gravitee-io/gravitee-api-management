# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Gravitee API Management (APIM) is an enterprise API management platform. This is a Java/TypeScript monorepo containing the API Gateway, Management API, Console UI, and Developer Portal UI.

## Architecture

**Core components:**
- **gravitee-apim-gateway** — The API Gateway (smart proxy applying policies to API traffic). Built on Vert.x (async/reactive). Entry point: `gateway-standalone`.
- **gravitee-apim-rest-api** — Management REST API backend (Spring-based). Has v1 and v2 API versions (`rest-api-management` and `rest-api-management-v2`). Entry point: `rest-api-standalone`.
- **gravitee-apim-console-webui** — Admin console UI (Angular 20, TypeScript).
- **gravitee-apim-portal-webui** — Developer portal UI (Angular 20 + legacy AngularJS 1.8.3).
- **gravitee-apim-portal-webui-next** — Modern developer portal UI rewrite (Angular 20).

**Data layer:**
- **gravitee-apim-repository** — Repository interfaces with implementations for MongoDB (primary), JDBC (PostgreSQL/MySQL/MariaDB/MSSQL), Elasticsearch, and Redis.
- **gravitee-apim-definition** — API definition models shared across components.
- **gravitee-apim-common** — Shared utilities.

**Plugin system:**
- **gravitee-apim-plugin** — Plugin infrastructure. 60+ bundled plugins for policies (rate limiting, JWT, OAuth2, API key, etc.), resources, reporters, fetchers, and notifiers.

**Testing modules:**
- **gravitee-apim-integration-tests** — Full stack integration tests (Testcontainers).
- **gravitee-apim-e2e** — Cypress/Jest E2E tests.
- **gravitee-apim-perf** — Performance tests.

Read .agent/rules/java.md for more information

## Prerequisites

- Java JDK 17+ (JDK 21 recommended; JDK 25 has Lombok issues)
- Maven 3.9+
- Node.js 22.12.x (see `.nvmrc` files; use `nvm use`)
- Yarn 4.1.1 (via `npx @yarnpkg/cli-dist@4.1.1` if Corepack unavailable)
- Docker

## Build Commands

### Backend (Java)

```bash
# Full build with tests
mvn clean install -T 2C

# Quick build (skip tests and validation)
mvn clean install -T 2C -DskipTests=true -Dskip.validation=true

# Build with all plugins (default + dev/community)
mvn clean install -T 2C -P bundle-default,bundle-dev

# Build a specific module (e.g., rest-api)
mvn clean install -pl gravitee-apim-rest-api -am -DskipTests

# Quick build via Taskfile
task build-quick
```

### Frontend

```bash
# Console UI
cd gravitee-apim-console-webui
yarn install && yarn build
yarn serve          # Dev server

# Portal UI
cd gravitee-apim-portal-webui
yarn install && yarn build
yarn serve          # Dev server
```

## Running Tests

### Java (JUnit 5 + Mockito + AssertJ)

```bash
# Run all tests for a module
mvn test -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service

# Run a single test class
mvn test -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -Dtest=MyTestClass

# Run a single test method
mvn test -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -Dtest=MyTestClass#myMethod

# Integration tests (requires high ulimit)
task integration-test
```

### Frontend (Jest)

```bash
# Console UI tests
cd gravitee-apim-console-webui
yarn test               # Run all tests
yarn test:auto          # Watch mode
yarn test:coverage      # With coverage

# Portal UI tests — same commands in respective directories
```

## Linting & Formatting

### Frontend

```bash
cd gravitee-apim-console-webui   # or portal-webui
yarn lint              # ESLint + Prettier check
yarn lint:fix          # Auto-fix all (ESLint + license headers + Prettier)
yarn lint:eslint:fix   # ESLint auto-fix only
yarn prettier:fix      # Prettier auto-fix only
```

### Java

- Prettier is configured for Java via Maven (printWidth: 140, tabWidth: 4).
- License headers are validated during build; skip with `-Dskip.validation=true`.

## Running Locally with Docker Compose

The default full-stack setup uses MongoDB:

```bash
cd docker/quick-setup/mongodb
docker compose up -d
```

**Default endpoints:**
- Gateway: http://localhost:8082
- Management API: http://localhost:8083
- Console UI: http://localhost:8084
- Portal UI: http://localhost:4100
- Default credentials: admin/admin

**Required ports:** 8082, 8083, 8084, 4100, 8025, 27017, 9200, 9300

Alternative Docker Compose setups exist in `docker/quick-setup/` for PostgreSQL, OpenSearch, Redis, Kafka, Keycloak, HTTPS, OpenTelemetry, and more.

## Key Conventions

- **Commit messages:** Follow [Conventional Commits](https://conventionalcommits.org/) (e.g., `feat:`, `fix:`, `chore:`).
- **Branch naming:** `issue/<issue-id>-description` off `master`.
- **Lombok:** Uses custom logger factory — `@CustomLog` generates `io.gravitee.node.logging.NodeLoggerFactory.getLogger(TYPE)` instead of standard SLF4J.
- **Reactive stack:** Gateway uses Vert.x + Project Reactor. Management API uses Spring Framework.
- **Maven profiles:** `main-modules`, `gateway-modules`, `rest-api-modules`, `definition-modules` for building subsets.
- **IntelliJ run configs:** Pre-configured in `.run/` directory (Gateway/Rest API with MongoDB or JDBC).

## Build Workaround for JDK 25

If Lombok fails on JDK 25, build inside a JDK 21 container:
```bash
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/home/maven -e MAVEN_CONFIG=/home/maven/.m2 \
  -v "$HOME/.m2":/home/maven/.m2 -v "$PWD":/workspace -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -pl <module-path> -am package -DskipTests -Dskip.validation=true
```
