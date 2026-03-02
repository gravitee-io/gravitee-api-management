---
trigger: always_on
---

# Gravitee APIM General Workspace Rules

Multi-module Java Maven project with Angular frontends and Vert.x-based Gateway.

## Core Principles

- **Java 17+** with Maven multi-module builds
- **Vert.x event loop** in Gateway - never block main thread
- **Onion Architecture** with strict naming conventions
- **Angular 19** for Console UI, **yarn 4.1.1** package manager
- **MongoDB + Elasticsearch** for persistence/analytics

## Quick Commands

| Task           | Command                                                                                                                                            |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| Build (fast)   | `mvn clean install -DskipTests -Dskip.validation -T 2C`                                                                                            |
| Start infra    | `docker compose -f docker/quick-setup/mongodb/docker-compose.yml up mongodb elasticsearch -d`                                                      |
| Start Gateway  | `cd gravitee-apim-gateway && mvn clean compile exec:java -Pdev -pl gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-container`    |
| Start REST API | `cd gravitee-apim-rest-api && mvn clean compile exec:java -Pdev -pl gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-container` |
| Console UI     | `cd gravitee-apim-console-webui && yarn && yarn serve`                                                                                             |

## Domain Rules

- [Java Architecture](rules/java.md) - Naming conventions, Vert.x patterns, build commands
- [Frontend](rules/frontend.md) - Angular, yarn, testing
- [Plugins](rules/plugins.md) - Policy/resource development

## Key Ports

| Service       | Port  | URL                   |
| ------------- | ----- | --------------------- |
| MongoDB       | 27017 | -                     |
| Elasticsearch | 9200  | -                     |
| Gateway       | 8082  | http://localhost:8082 |
| REST API      | 8083  | http://localhost:8083 |
| Console UI    | 4000  | http://localhost:4000 |
| Portal UI     | 4100  | http://localhost:4100 |

Default credentials: `admin` / `admin`
