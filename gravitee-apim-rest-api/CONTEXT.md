# Rest API

Gravitee APIM's control-plane REST API. Owns the CRUD lifecycle for APIs, Plans, Subscriptions, Applications, API Products, Groups, Roles, Environments, Organizations, and platform-operational concerns (License, Cockpit, Alert, Audit, API Score, Cluster, Instance, Integration). Two surfaces exist: Management API (admin-facing, backs console-ui) and Portal API (consumer-facing, backs portal-ui/portal-next).

## Language

**Management API**:
The admin-facing REST surface — full CRUD over APIs, Plans, Subscriptions, Applications, etc. Canonical version is v2; v1 is out of scope for this glossary.
_Avoid_: Console API, admin API.

**Portal API**:
The consumer-facing REST surface — read-only over APIs/Plans, plus self-service actions (subscribe, manage own Applications). No admin CRUD.
_Avoid_: Developer API.

**Organization**:
The top-level tenant. Owns Environments and Roles. Also owns Organization-level Flows, shared across every API in every Environment beneath it.
_Avoid_: Tenant, account.

**Environment**:
Scopes a set of APIs within an Organization. Sits between Organization and Api in the ownership hierarchy: Organization → Environment → Api.

**Api**:
The CRUD-managed entity representing a customer API — has a `lifecycleState` (CREATED, PUBLISHED, UNPUBLISHED, DEPRECATED, ARCHIVED) and owns a set of Plans. This is the source Gateway's API Definition is built from.
_Avoid_: API (bare) when the Gateway-side API Definition/Reactable is meant instead — see gateway/CONTEXT.md.

**Group**:
A collection of Users and/or Applications, used to scope permissions and ownership — e.g. primary ownership of an Api or Application.
_Avoid_: Team.

**Role**:
A named, scope-bound bundle of CRUD-style permissions assignable to a user. Scopes: API, APPLICATION, GROUP, ENVIRONMENT, ORGANIZATION, PLATFORM, INTEGRATION, CLUSTER, EXPLORER, API_PRODUCT, AI_WORKSPACE. Distinct from Group: a Group is *who* (a collection of Users/Applications); a Role is *what they can do* within a scope.

**Application**:
The client-side entity end users' software authenticates as. Holds Subscriptions, an owner, OAuth client credentials, and belongs to Groups.

**Plan**:
An access-and-consumption-limit layer on top of an Api — authN method, rate limits/quotas, subscription validation mode. Has a `PlanStatus` (STAGING, PUBLISHED, CLOSED, DEPRECATED). Not a billing construct — no price/currency/invoice fields exist anywhere on it; monetization, if any, is external to APIM.

**Subscription**:
An Application's grant of access to a Plan. Has a `SubscriptionStatus` (PENDING, REJECTED, ACCEPTED, CLOSED, PAUSED, RESUMED) — the mechanism an Api publisher uses to control who can consume the Api, under what conditions, at what access level.

**API Product**:
An environment-level resource bundling multiple Apis under one subscription model — its own Plans (API Key, JWT, or mTLS only) and Subscriptions, separate from the plans/subscriptions of the Apis it contains. What a customer productizes a group of Apis into for business/revenue purposes. Like Plan, carries no billing/pricing field itself — monetization is realized externally, not implemented by APIM. An Api can belong to multiple API Products; the Gateway evaluates the Product's plan/subscription before the Api's own for traffic routed through a Product.
_Avoid_: API bundle, product (bare).

## Platform & operations

Concerns beyond the Api/Plan/Subscription core — still CRUD-owned here, surfaced to operators through console-ui.

**License**:
The tier (`oss` or an Enterprise tier) plus the set of unlocked feature flags (e.g. audit trail, alert engine, clustering, API Products) governing which capabilities are available on an installation.
_Avoid_: plan (bare — unrelated to the Plan entity), tier (bare).

**Cockpit**:
Gravitee's separate hosted SaaS control-plane ("Gravitee Cloud") that a self-hosted installation can register/sync with. Cross-environment Api Promotion is keyed by Cockpit IDs.

**Installation**:
A self-hosted APIM deployment's own identity record — holds the Cockpit URL and sync status.

**Alert**:
A trigger that watches a condition (with severity and dampening) and fires notifications on a schedule. Monitoring/notification — distinct from Governance (gateway's per-request policy enforcement).

**Audit**:
An immutable log entry recording a change — who changed what, when, and the patch applied. A record, not a live managed entity.

**API Score**:
A lint/quality-scoring evaluation of an Api against a Ruleset. Design-time only — distinct from Governance, which is gateway runtime policy enforcement.
_Avoid_: API Quality (superseded name).

**Cluster**:
A registered external messaging infrastructure asset (e.g. a Kafka cluster) APIs connect through — distinct from an Api or Application.

**Instance**:
A running Gateway node's operational record (hostname, IP, version, heartbeat). The infra/ops view of a deployed Gateway — not the Reactable/traffic-handling concept itself (see gateway/CONTEXT.md).

**Integration**:
A registered connection to a third-party gateway or broker, used for federating externally-managed Apis into APIM.
