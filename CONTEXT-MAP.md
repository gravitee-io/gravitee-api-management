# Context Map

Gravitee APIM is an API/Event/Stream/Agent Management platform. Customer-facing contexts are being documented one at a time; see below for what's covered so far.

## Contexts

- [Gateway](./gravitee-apim-gateway/CONTEXT.md) — runtime that customer traffic hits first; deploys API Definitions as Reactables and applies Governance (plugins/policies) to requests/responses
- [Rest API](./gravitee-apim-rest-api/CONTEXT.md) — control-plane REST API; owns CRUD lifecycle for Api, Plan, Subscription, Application, API Product, Group, Role, Environment, Organization, and platform-ops (License, Cockpit, Alert, Audit, API Score, Cluster, Instance, Integration)
- [Console UI](./gravitee-apim-console-webui/CONTEXT.md) — admin frontend; owns no domain concepts of its own, pure consumer of Rest API's Management API
- [Portal Next](./gravitee-apim-portal-webui-next/CONTEXT.md) — developer-portal frontend for API consumers; owns Portal Navigation Item, Portal Configuration, Application Invitation; otherwise consumes Rest API's Portal API

## Relationships

- **Rest API → Gateway**: an Api's config is synced down and built into an API Definition, which the Gateway deploys as a Reactable
- **Rest API → Gateway**: Plan, Subscription, Application, Organization, and API Product are created/updated/deleted in Rest API; Gateway only reads and enforces them against traffic
- **Rest API → Console UI**: Console UI is a thin client over the Management API — no independent domain model
- **Rest API → Portal Next**: Portal Next is a thin client over the Portal API, plus a handful of portal-owned presentation concepts (navigation tree, portal config, application invitations)
