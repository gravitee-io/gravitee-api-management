# Authz Entity Type Contract — Design Spec

**Date:** 2026-06-04
**Status:** Draft v2 (contract + phasing; editable schema is now Phase 0)
**Scope:** Gamma authz module (`gravitee-gamma-module-authz`) + PDP wire contract. The editable schema lives **only in the authz module — never synced to the gateway** (which is already the case today).

## Problem

One concept — "what type is this entity" — is encoded in **six** inconsistent places. The authoritative field (`entityType`) is dropped on the frontend, so the UI reconstructs the type by parsing strings. The reconstruction can disagree with what the PDP actually stores/matches.

| # | representation | where | values | role today |
|---|---|---|---|---|
| ① | `kind` (enum column) | domain, REST, Mongo, `?kind=` filter | `PRINCIPAL` / `RESOURCE` | coarse bucket |
| ② | `_kind` (attribute) | `attributes`, **principals only** | `user`,`group`,`role`,`agent-identity` | granular principal type (side channel) |
| ③ | `entityType` (String column) | domain, REST, wire→PDP | **always `Principal`/`Resource`** (default, never set) | authoritative engine `Type` — but dead |
| ④ | uid prefix | `entityId`, **resources only** | `mcp.`,`api.`,`agent.`,`model.`,`mcptool.` | granular resource type (side channel) |
| ⑤ | registry `uiType` | `entity-kind-registry.ts` (FE) | `MCPServer`,`API`,`Model`,… | FE guessing map |
| ⑥ | generated schema type | `AuthzSchemaServiceImpl.typeNameFor` | `capitalise(prefix)` → `Mcp` | schema gen (readonly) |

### Engine plane (why the type string matters)

- PDP entity UID = `<entityType>::"<entityId>"`. Default when `entityType` null = `Principal`/`Resource`.
- PEP at eval sends `subjectType::"subjectId"` where `subjectType` is **PEP policy config** (operator-set), `subjectId` is an EL expression (e.g. JWT `sub`).
- **Equality match** works iff policy **token** type == PEP `subjectType`. Independent of the stored entity's `entityType`.
- **Attribute / group conditions** resolve against the entity store, keyed by `entityType::"entityId"`. Here a token↔store type mismatch silently fails.

So today's granular FE tokens are "correct" only by coincidence of operator config; condition-based policies on synced principals (stored as `Principal::`) can silently miss.

### Grounding facts (verified)

- The schema is **already not synced to the gateway** — publisher emits only `PUBLISH_AUTHZ_POLICY` / `PUBLISH_AUTHZ_ENTITY`. No schema event. The PDP evaluates with entities + policies only.
- The schema is currently **generated on read** from entities/policies (`AuthzSchemaServiceImpl.build` → `typeNameFor` = `capitalise(prefix)`) and cached in memory. `AuthzSchemaAdminApi` exposes only `currentGaplSchema` + `invalidate` — **read-only, no save**.
- **No backend GAPL parser exists.** `parseGaplSchema` is frontend-only (TS).
- `feat/authz-typed-entity-type` laid the `entityType` plumbing (REST → domain → command → publisher → wire) but **nothing populates it granularly** (sync passes `null`, UI has no field, FE still parses).

## Phasing

**Phase 0 — Editable schema (module-only).** Make the schema a stored, user-editable free-text GAPL document. This becomes the authoritative type catalog + principal/resource classification, replacing the prefix-based generator.

**Phase 1 — entityType population + backfill (backend).** Set granular `entityType` on every write path; backfill existing rows.

**Phase 2 — Frontend consumption.** Read `entityType`; delete the parsing tower.

**Phase 3 — (out of repo) PEP `subjectType` / deployed-policy alignment.** Breaking match-plane coordination.

---

## Phase 0 — Editable schema

**Model: the schema is an independent, opt-in document with its own CRUD. There is NO generation.** Entities and policies work with or without a schema. A schema exists only when a user creates one; absent otherwise. `currentGaplSchema` is verified to be read by exactly one caller (the REST `GET`); generation is therefore safe to delete.

### S1 — Storage
A stored document per environment (Mongo `authz_schemas`, `_id = environmentId`, `schemaText`, `updatedAt`). Single doc per env, upsert by `_id`.

### S2 — No generation, no seed
The generator (`build` / `typeNameFor` / `renderAttributes` / cache) is **deleted**. There is nothing to seed: a domain with no stored schema simply has no schema (the FE already renders the "No schema defined yet" empty state via `useSchema`'s 404→notFound). This is a deliberate behaviour change — domains lose the auto-generated read-only view and start blank until they create a schema.

### S3 — REST surface (full CRUD)
- `GET …/schema` → stored schema, or `404`/empty when none.
- `PUT …/schema` → create/update.
- `DELETE …/schema` → remove (back to "no schema").
Port `AuthzSchemaAdminApi` → `getSchema(env)`, `saveSchema(env, text)`, `deleteSchema(env)`. The old `currentGaplSchema` and `invalidate` are removed.

### S4 — Remove generation plumbing (full cleanup)
`invalidate` is now dead: its 10 call sites in `AuthzEntityServiceImpl` (×4) / `AuthzPolicyServiceImpl` (×6) are deleted, and since `schemaService` was injected into those two services **only** to call `invalidate`, that dependency is removed from their constructors (and all call sites + Spring wiring updated). `AuthzSchemaServiceImpl` loses its `AuthzEntityRepository` / `AuthzPolicyRepository` deps; it becomes a thin wrapper over `AuthzSchemaRepository`.

### S5 — Validation
- **Frontend** validates syntax with the existing `parseGaplSchema` before save; blank is rejected client-side.
- **Backend** light only (`@NotNull`, stores as-is). A full backend GAPL parser is deferred (none exists today).

### S6 — UI
`SchemaPage` gains create/edit/delete: empty state offers "Create schema" (opens an empty editable Monaco buffer); existing schema offers Edit / Delete. Save/Create disabled while `schemaDiagnostics(draft)` is non-empty.

### S7 — Consistency (deferred)
Free-text can drift from entities (a type removed while entities use it). Deferred — Phase 0 does not warn or block. (Was a "warn" item; dropped from Phase 0 scope to keep it minimal.)

---

## Target contract (Phases 1–2, built on the schema)

### C1 — `entityType` (③) is the single source of truth for type
Granular `entityType` is authoritative. `_kind` (②) and uid prefix (④) demoted to non-authoritative. The FE reads `entityType`; stops deriving type from uid/`_kind`.

### C2 — Canonical taxonomy and naming (kill the 6 spellings)
Canonical names (one spelling each):
- **Principal types:** `User`, `Group`, `Role`, `ServiceAccount`, `AgentIdentity`
- **Resource types:** `MCPServer`, `MCPTool`, `Model`, `Agent`, `API`, `Event`, `Action`
- **Umbrella (legacy / "Other"):** `Principal`, `Resource`

All producers align: FE token = `entityType::"entityId"` verbatim (no prefix stripping, no `groupForSegment`); registry = migration map + FE labels only. The **valid-type set is owned by the schema** (Phase 0; user-authored, no longer generated); the registry is FE convenience for built-ins + migration. `entityType` stays a String. (The old prefix-capitalising generator `typeNameFor` is deleted in Phase 0, so the `Mcp` spelling disappears with it.)

### C3 — `kind` (①) stays, redefined as coarse scoping only
Explicit column set at create; used for list/fetch scoping (`?kind=PRINCIPAL`) and `defaultEntityType()` fallback. **Not** a semantic constraint: the PDP store is keyed by `entityType::"entityId"`, so an `Agent::"x"` is one entry usable as principal **or** resource across policies — A2A needs no duplicate; `kind` only picks the management-list tab. Classification for the create dropdown comes from the schema `appliesTo` (principal vs resource sets).

### C4 — Write paths populate granular `entityType`
| path | today | target |
|---|---|---|
| AM sync | `entityType=null`, `_kind=…` | `entityType = map(_kind)`; stop writing `_kind` (keep as transitional read-fallback) |
| UI create | no field | FE sends `entityType` from the create dropdown (schema-derived); "Other" → umbrella |
| Catalog import | `entityId="mcp.weather"`, `entityType=null` | `entityType = map(prefix)`; `entityId` unchanged |

### C5 — `entityId` never rewritten; resources keep their prefix
`mcp.weather` → `MCPServer::"mcp.weather"`. Principals bare → `User::"alice"`. No id migration, no forced policy-text rewrite for ids.

### C6 — Backfill rule: only when `entityType` is empty
Set already → leave (idempotent). Empty → derive: principals from `_kind`, resources from prefix, else `kind` umbrella.

### C7 — Frontend: read, don't parse
`adaptEntityResponse` carries `entityType` (+ `kind`) into `EntityResponse`. `toChipOption` → `group = entityType`; token = `entityType::"entityId"`. Removed: `parseEntityUid`-as-type, `resolveEntityRef`/`_kind`, `fallbackGroup` (PR #17287), `groupForSegment`. Picker principal/resource classification from schema `appliesTo` (+ registry fallback for built-ins).

## Out of scope (this initiative)
- **PEP `subjectType` / deployed-policy migration** (API flow config, out of repo). **Breaking change to the match plane** — granular tokens require `subjectType` alignment on every deployed flow. Captured as risk; owner/sequencing TBD.
- **Full backend GAPL parser** (S5) — deferred hardening.
- **Schema↔entity consistency** (S7) — deferred; Phase 0 neither warns nor blocks.

## Risks & open questions
1. **Breaking match plane** — needs an inventory of deployed flows/policies + `subjectType` before any granular cutover.
2. **Existing policy text** — policies authored via the old FE (umbrella or `MCP::"weather"`) won't match `MCPServer::"mcp.weather"`. Count what exists; rewrite vs accept-break (likely low volume).
3. **Schema seed fidelity** — the generated seed inherits today's prefix-derived names; the seed step must emit canonical names (C2), not `capitalise(prefix)`.
4. **`_kind` removal timing** — keep as read-fallback through Phase 1 backfill; remove after.
5. **Schema not validating policies at runtime** — confirmed the PDP doesn't ingest a schema; so the editable schema is an authoring/validation aid in the module, not a runtime artifact. (Implication: schema correctness is advisory until/unless the PDP starts consuming it.)

## Components affected
- **Phase 0:** new `AuthzSchemaRepository` + Mongo doc; `AuthzSchemaServiceImpl` (store/read, retire generation, seed); `AuthzSchemaAdminApi` (+save); REST schema resource (+PUT); FE `SchemaPage` (editable Monaco) + schema API client.
- **Phase 1:** `SyncAmUsersUseCase`, `AuthzEntityServiceImpl`, `CreateOrReplaceAuthzEntityCommand`, backfill (Mongo migration), `AuthzEntityRequest` (entityType present); wire/PDP already carries `entityType`.
- **Phase 2 (FE):** `authz-api.service.ts` (`CanonicalEntity` + `adaptEntityResponse`), `authz-api.types.ts` (`EntityResponse`), `entity-adapter.ts`, `useEntityOptions.ts`, `resource-options.ts`, `CreateEntityRequest`, create form.

## Testing
- **Phase 0:** save→read round-trip; seed from generated for a fresh domain; FE parse diagnostics block save on invalid GAPL; consistency warnings (non-blocking) surface drift.
- **Phase 1:** backfill idempotency (set-once), principal-from-`_kind`, resource-from-prefix, umbrella fallback.
- **Phase 2:** `toChipOption` reads `entityType`; token round-trips as `entityType::"entityId"`; pickers classify via schema `appliesTo`; removed heuristics gone (existing picker tests adjusted).
- **Naming:** one canonical spelling end-to-end (token == schema type == store UID).
