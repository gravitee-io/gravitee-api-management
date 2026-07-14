# AI Product — Engineering Roadmap & Execution Guide

> **Audience:** engineering (you). This is the build guide — phase map, requirements, concrete
> steps with file locations, and a **continuous, automatic verification strategy** so we know we're on
> track at every step (tests ship *with* each step, never bolted on at the end).
>
> **Companion docs:** `AI_PRODUCT_ARCHITECTURE.md` (design rationale, block diagram, change map),
> `AI_PRODUCT_ROADMAP_PM.pdf` (vision deck for Product Managers).
>
> **Canonical source of truth:** the AI Products PRD (Problem / Vision / Solution / R1–R5 / Decisions).
> This roadmap is the implementation projection of that PRD.

---

## 0. How to read this doc

- **Phase map** (§3) is the high-level sequence and what each phase *delivers*.
- **Continuous verification** (§4) is the "automatic something" — how every step proves itself and how
  results stay green. **Read this before writing any code.**
- **Phases 0–4** (§5–§9) are the work. Each phase is a table: `Step → What → Where (files) →
  Automated test (command) → Done when`. Every step has a test that runs in CI; nothing is "tested later".
- **Appendix** (§10) is the test-command cheat sheet and file index.

---

## 1. Guiding principles

1. **Rename the concept, freeze the wire.** Visible concept = **AI Product** everywhere (Console, Portal,
   docs). Internal `referenceType` stays **`API_PRODUCT`** (Decision **D1**). AI behavior branches on the
   product `type` and on **asset types** — never on a new reference type. → zero breakage to plans/subs/
   gateway security chain.
2. **Control plane is reactor-agnostic.** Tables, MAPI, Console, Portal, policies-as-data, limit
   resolution, provisioning, ledger are identical whether or not we build `AiProductReactor`. Build them
   first; they are never at risk from the reactor decision.
3. **One stable seam decouples dispatch.** Everything upstream depends on the `AiProductEntrypoint` +
   `PlanResolution` contract (§5). Reactor-first vs reactor-last is then a one-layer implementation swap.
4. **Additive only.** Every DB/gateway change is a nullable column / parallel table / optional event
   property. Existing API Products, plans, subscriptions, and reactor paths keep working (compat surface).
5. **Extensibility contract — "add anything, never rebuild":** a new asset kind = enum value + asset row +
   registry key + portal card; a new budget dimension = metadata key + flow; a new AI policy = flow entry.
6. **Test ships with the step.** A step is not "done" until its automated test is green in CI and the
   verification matrix row is checked. No end-of-phase test crunch.

---

## 2. Requirements (from PRD) — traceability anchor

| ID | Requirement (abridged) | Primary phase |
|----|------------------------|---------------|
| **R1** | AI Product replaces API Product as the visible packaging concept; existing API Product records stay compatible | P0–P1 |
| **R2** | Each AI Product gets one **Default LLM Proxy**; admins pick models from the catalog; proxy exposes endpoint, hides creds, enforces limits | P1 (+P3 broaden assets) |
| **R3** | Both admin journeys: packaged **Wizard** (App+Plan+Sub+Key) **and** existing group/app/subscription flow | P2 |
| **R4** | Product-level **and** assignment-level controls: dollar budget, token rate limit, request rate limit | P1 (token/req) + P2 (dollars + assignment) |
| **R5** | Portal shows assigned AI Products: app context, API key guidance, endpoint, headers, models, limits, snippets | P1 |

---

## 3. Phase map (overview)

| Phase | Theme | Delivers | Reactor? | Exit gate |
|-------|-------|----------|----------|-----------|
| **P0** | Foundations & seam | typed-asset tables, `type` cutover, the `AiProductEntrypoint`/`PlanResolution` contract, CI test harness | no | seam contract + tables green in CI |
| **P1** | Shippable AI Product | Default LLM Proxy, model catalog, product + per-user token/request limits, generated Portal page | no (reuse LLM_PROXY reactor) | end-to-end demo: 200 / 429 / 401 / 404 automated |
| **P2** | Assignments & dollars | assignment-level limits, group reconciliation, packaged Wizard, **dollar budget** (pricing→ledger→`cost-ratelimit`) | no | per-user $ + token + assignment limits enforced, all automated |
| **P3** | Broaden assets | MCP_PROXY / MCP_ROUTER / A2A / AGENT / EVENT_STREAM / KAFKA_TOPIC assets, registry resolve-by-typed-asset, `mcp-acl` | no | multi-asset product subscribes + governs under one plan, automated |
| **P4** | Unify & intelligence | `AiProductReactor` (single base path), model fallback / semantic routing / prompt cache, full usage dashboard | **yes** | unified base URL + product-level policy pass, automated |

**Reactor timing (Decision open):** built in **P4** by default. If the architect team mandates
*reactor-first* (single immutable base URL / one-pass product policies / one shared auth+budget at runtime),
the **control plane in P0–P3 is unaffected**; only the gateway *mechanism* swaps behind the seam
(injection → reactor). See `AI_PRODUCT_ARCHITECTURE.md` §"Reactor timing impact".

---

## 4. Continuous verification — the "automatic something"

> Goal: at any commit we can answer *"are we still on track?"* automatically, without a human re-running
> a checklist. Three mechanisms, all in version control:

### 4.1 Test ships with each step (the rule)

Every step's **Definition of Done** includes a named automated test at the right layer of the pyramid:

```
        ▲  E2E smoke (curl assertions: 200/429/401/404)   ── ai-poc-demo/verify/*.sh  (gateway live)
       ╱ ╲ Integration (gateway flow + MAPI resource)      ── *IntegrationTest, *ResourceTest
      ╱   ╲ Unit (use cases, domain services, mappers,      ── JUnit (Java) / Jest (Angular)
     ╱_____╲      Angular components/services)
```

**No step is merged without its test.** If a step has no test, it is not done.

### 4.2 Living verification matrix (kept in this doc)

Each phase has a **Verification Matrix** table at its end. As a step lands, flip its row to ✅ with the
test name. The matrix is the at-a-glance "are we on track" board — reviewed every standup. A red/empty row
in a completed phase is a release blocker.

### 4.3 Automated gates (scripts + CI)

| Gate | What it runs | When | Fails the build if |
|------|--------------|------|--------------------|
| `ai-poc-demo/verify/unit.sh` | all touched-module unit tests (`mvn -pl … test`, `nx test …`, Jest) | every commit / pre-push | any unit test red |
| `ai-poc-demo/verify/integration.sh` | MAPI resource tests + gateway flow integration tests | every PR | any integration test red |
| `ai-poc-demo/verify/smoke.sh` | spins fixtures, calls the gateway, **asserts 200 / 429 / 401 / 404** and budget headers | per phase exit + nightly | any assertion fails (exit ≠ 0) |
| `.github/workflows/ai-product.yml` (or local act) | unit → integration → smoke in sequence | PR + main | any stage red |

`smoke.sh` is the heartbeat: it provisions a product + two users (e.g. Bob 250 / Alice 2000 tokens),
fires real requests, and **exits non-zero** unless Bob hits 429 while Alice still gets 200, a bad key gets
401, and a bad path gets 404. This is the single command that says "the whole thing still works."

### 4.4 Definition of Done (applies to every step)

```
[ ] code + test in same PR
[ ] unit.sh green     [ ] integration.sh green (if step touches MAPI/gateway)
[ ] verification-matrix row flipped to ✅ with test name
[ ] additive-only check: no breaking change to existing API Product / plan / subscription paths
[ ] (phase exit only) smoke.sh green
```

---

## 5. Phase 0 — Foundations & seam *(no user-visible feature; de-risks everything after)*

**Why first:** lays the typed-asset data model, the naming cutover, and the **one abstraction** that makes
the reactor decision a swap. Also stands up the CI harness so every later step is automatically verified.

| # | Step | Where (module / files) | Automated test | Done when |
|---|------|------------------------|----------------|-----------|
| 0.1 | CI harness + smoke skeleton | `ai-poc-demo/verify/{unit,integration,smoke}.sh`, `.github/workflows/ai-product.yml` | the scripts run & report (green on empty) | scripts exist, CI runs them on PR |
| 0.2 | `type` discriminator end-to-end (already partly done) — confirm core model, JDBC/Mongo, liquibase, OpenAPI, mapper | `gravitee-apim-repository`, `…/core/api_product/model`, liquibase `*_add_type_to_api_products` | repository CRUD test asserts `type` round-trips | `type=AI_PRODUCT` persists & reads on JDBC+Mongo |
| 0.3 | **`api_product_assets`** table (typed) + `AiProductAssetRef` model; `apiIds` becomes a **derived view** (type=API) | repository (new table + migration), core model | repository test: write typed assets, read back `apiIds` derived | typed assets persist; legacy `apiIds` still resolves |
| 0.4 | **Seam contract**: `AiProductEntrypoint` (resolve public URL) + `PlanResolution` (plans for an asset ref) interfaces; P0 impl = convention paths + existing registry | gateway (`…/handlers/api/.../apiproduct/…`), interface + default impl | unit test: contract returns convention URL + resolves plan by `API:<id>` | contract compiles, default impl passes unit test |
| 0.5 | Naming cutover scaffolding (labels/strings only; no model fork) — Console + Portal show "AI Product" | `gravitee-gamma/...module-apim` labels, `portal-webui-next` i18n | component test asserts heading text "AI Product" | UI reads "AI Product"; API Product records still load |

**Phase 0 Verification Matrix**

| Req | Check | Test | Status |
|-----|-------|------|--------|
| R1 | `type` round-trips JDBC+Mongo | `ApiProductRepositoryTest#typeRoundTrip` | ☐ |
| R1 | typed assets persist; `apiIds` derived | `ApiProductAssetsRepositoryTest` | ☐ |
| (arch) | seam resolves convention URL + plan-by-asset | `AiProductEntrypointTest` | ☐ |
| R1 | UI shows "AI Product" | gamma + portal component specs | ☐ |
| (ops) | CI runs unit→integration→smoke | `ai-product.yml` dry run | ☐ |

---

## 6. Phase 1 — Shippable AI Product *(the LiteLLM/Portkey replacement, fully governed)*

**Delivers:** create an AI Product → it auto-provisions a Default LLM Proxy → admin picks catalog models →
publishes a plan with a reset window → a Dev-Portal user subscribes → admin approves + sets a per-user
**token + request** budget → the user gets a key + endpoint + models + snippets and starts calling, with
**per-user 429 enforcement**.

| # | Step | Where (module / files) | Automated test | Done when |
|---|------|------------------------|----------------|-----------|
| 1.1 | **Default LLM Proxy provisioning** on AI Product create (asset `role=DEFAULT`) | core `ProvisionDefaultLlmProxyDomainService`, `CreateApiProductUseCase` | unit: create product ⇒ one DEFAULT LLM_PROXY asset | creating an AI Product yields a hidden LLM_PROXY asset |
| 1.2 | **Model catalog**: `api_product_models` table + `/api-products/{id}/models` CRUD; sync models → proxy endpoint config at deploy | repository, mgmt-v2-rest `ApiProductModelsResource`, `SyncModelCatalogDomainService` | resource test (add model → 201) + unit (sync writes proxy config) | models persist & appear in proxy config on deploy |
| 1.3 | **Product plan + window** (reuse `ensurePlanForWindow`): one published API_KEY plan carrying `token-ratelimit` + `rate-limit` flows | core plan use cases, gamma `services/aiProduct.ts` | MAPI test: publish plan ⇒ flows persisted; gateway test: deploy event **embeds flows** | published plan; deploy event carries flows (regression-guarded) |
| 1.4 | **Effective per-user limit → subscription metadata** at approval (`ResolveEffectiveLimits`, token+request) | core `ResolveEffectiveLimitsDomainService`, approve use case | unit: approve(limit=N) ⇒ `metadata.tokenLimit=N` | approval writes effective limit to metadata |
| 1.5 | **Gateway enforcement** (reuse): product-plan flow chain runs `token-ratelimit dynamicLimit={#subscription.metadata['tokenLimit']}` per-subscription | gateway `ApiProductPlanFlowResolver` (built) — integration only | **gateway integration test**: user A 429 after budget, user B still 200 | per-user 429 enforced in integration test |
| 1.6 | **Portal catalog** (read-only) + **generated getting-started page** (app, key, endpoint, headers, models, limits, snippets) | portal-rest `ApiProductsResource`, `portal-webui-next` catalog + detail | portal resource test (list/get/plans) + Jest component specs | non-member user browses, subscribes, sees key+snippets |
| 1.7 | **Subscribe flow** (PENDING → approve) — reuse portal subscribe (resolves API_PRODUCT from plan) | `portal-webui-next` subscribe wizard, portal-rest `SubscriptionsResource` | Jest e2e-ish spec: subscribe ⇒ PENDING; approve ⇒ consumption view | subscribe lands PENDING; approval unlocks consumption |
| 1.8 | **Smoke**: provision → 2 users → 200 / **429** / 401 / 404 + budget headers | `ai-poc-demo/verify/smoke.sh` | the smoke script itself (asserting) | `smoke.sh` exits 0 with all four codes asserted |

**Phase 1 Verification Matrix**

| Req | Check | Test | Status |
|-----|-------|------|--------|
| R2 | create ⇒ Default LLM Proxy asset | `ProvisionDefaultLlmProxyTest` | ☐ |
| R2 | model add + sync to proxy config | `ApiProductModelsResourceTest` + `SyncModelCatalogTest` | ☐ |
| R4 | plan publish embeds flows in deploy event | `DeployApiProductDomainServiceTest` (flows non-null) | ☐ |
| R4 | approval writes effective limit | `ResolveEffectiveLimitsTest` | ☐ |
| R4 | **per-user 429** enforced | `ApiProductTokenLimitIntegrationTest` | ☐ |
| R5 | portal catalog + getting-started page | portal-rest `ApiProductsResourceTest` + Jest specs | ☐ |
| R1/R5 | end-to-end 200/429/401/404 | `smoke.sh` | ☐ |

---

## 7. Phase 2 — Assignments & dollars

**Delivers:** product-level **and** assignment-level controls (narrow a user/group), eager **group
reconciliation**, the packaged **Wizard**, and first-class **dollar budgets**.

| # | Step | Where (module / files) | Automated test | Done when |
|---|------|------------------------|----------------|-----------|
| 2.1 | **`api_product_assignments`** table (user/group → product + assignment limits) | repository + core model | repository test: assignment round-trip | assignments persist |
| 2.2 | **Effective-limit resolution** = product ∩ assignment ∩ plan window → metadata | core `ResolveEffectiveLimitsDomainService` (extend) | unit: product=1000, assignment=250 ⇒ effective=250 | narrowest wins, written to metadata |
| 2.3 | **Group reconciliation**: add user to assigned group ⇒ ensure App + Sub + Key + portal access | core `ReconcileGroupAccessDomainService` | unit: add member ⇒ App+Sub+Key created idempotently | re-running reconcile is a no-op (idempotent) |
| 2.4 | **Packaged Wizard** (Journey A): one call creates/updates App+Plan+Sub+Key | mgmt-v2-rest wizard resource, gamma wizard UI | resource test: wizard call ⇒ 4 records; gamma component spec | wizard provisions all four in one call |
| 2.5 | **Model pricing** on `api_product_models` (input/output price) | repository + models resource | resource test: price persists | pricing stored per model |
| 2.6 | **Spend ledger** (`api_product_spend_ledger` or counter store) + per-subscription spend accrual | core `SpendLedgerService`, gateway hook on token metering | unit (accrual math) + integration (spend grows per call) | spend accrues per call, keyed per subscription |
| 2.7 | **`cost-ratelimit` policy** (NEW): block when spend ≥ budget; reset window; exhaustion behavior | policy plugin + product-plan flow | **gateway integration**: budget exceeded ⇒ blocked + alert event | dollar budget enforced at gateway |
| 2.8 | **Usage/budget reporting** (token + $ + 429s) surfaced | MAPI `/api-products/{id}/usage`, Console Usage, Portal My Usage | resource test + component specs | admin & dev see usage + remaining budget |

**Phase 2 Verification Matrix**

| Req | Check | Test | Status |
|-----|-------|------|--------|
| R4 | assignment narrows product limit | `ResolveEffectiveLimitsTest#narrowest` | ☐ |
| R3 | group add reconciles access (idempotent) | `ReconcileGroupAccessTest` | ☐ |
| R3 | wizard creates App+Plan+Sub+Key | `AiProductWizardResourceTest` | ☐ |
| R4 | dollar budget enforced | `ApiProductCostLimitIntegrationTest` | ☐ |
| R4/R5 | usage + remaining budget visible | `usage` resource + UI specs | ☐ |
| (all) | smoke extends with $ assertion | `smoke.sh` (+budget-exhaustion case) | ☐ |

---

## 8. Phase 3 — Broaden assets

**Delivers:** the AI Product packages **heterogeneous** assets (MCP, A2A, agents, streams, topics) under one
plan/auth/budget; the gateway resolves plans **by typed asset reference**.

| # | Step | Where (module / files) | Automated test | Done when |
|---|------|------------------------|----------------|-----------|
| 3.1 | Asset types beyond LLM: `MCP_PROXY`, `MCP_ROUTER`, `A2A_PROXY`, `AGENT`, `EVENT_STREAM`, `KAFKA_TOPIC` (enum + `config_json`) | core model + repository (no schema change — enum + JSON) | unit: each type persists + validates | all asset types persist & validate |
| 3.2 | **Registry resolve-by-typed-asset**: `ApiProductRegistry` keys plans by `<TYPE>:<id>` | gateway registry + `PlanResolution` impl (seam) | integration: MCP request resolves `MCP_PROXY:<id>` plan | typed-asset request resolves the product plan |
| 3.3 | Attach MCP component (own path, shared product plan/budget) + `mcp-acl` per plan | gamma assets UI, gateway flow | integration: tool allowed/denied by plan ACL | per-tool ACL enforced under product plan |
| 3.4 | Deploy event carries `assets[]` (graceful-absent) | gateway sync/mapper, `ReactableApiProduct` | mapper test: old event (no assets) still deploys | new + old events both deploy |
| 3.5 | Portal renders mixed assets on the getting-started page | `portal-webui-next` detail | component spec: LLM + MCP cards render | portal shows all `portalVisible` assets |

**Phase 3 Verification Matrix**

| Req | Check | Test | Status |
|-----|-------|------|--------|
| R2 | all asset types persist | `ApiProductAssetTypeTest` | ☐ |
| R2 | registry resolves by typed asset | `RegistryTypedAssetIntegrationTest` | ☐ |
| R2 | MCP under product plan + ACL | `McpUnderProductIntegrationTest` | ☐ |
| R1 | old deploy event still works | `ApiProductMapperBackcompatTest` | ☐ |

---

## 9. Phase 4 — Unify & intelligence *(reactor + advanced; may move earlier if architects mandate)*

**Delivers:** one product base URL via `AiProductReactor`; product-level policy pass once across all assets;
model fallback / semantic routing / prompt cache; full usage dashboard.

| # | Step | Where (module / files) | Automated test | Done when |
|---|------|------------------------|----------------|-----------|
| 4.1 | `AiProductReactor` (`ReactorFactory.canCreate(type==AI_PRODUCT && hasBasePath)`); seam impl B | gateway reactor | integration: `/ai/{product}/…` dispatches to assets | one base path routes to all assets |
| 4.2 | Product-level flow chain runs **once** (guardrails/redaction across LLM+MCP) | gateway reactor flow chain | integration: product policy applied to LLM + MCP | one policy pass covers all assets |
| 4.3 | Dual-acceptor transition (old per-component paths + new base path) | gateway sync | integration: old key on old path still 200 | zero-downtime URL cutover |
| 4.4 | Model fallback / semantic routing / prompt cache (policies) | policy plugins + flows | integration per policy | each policy verified |
| 4.5 | Full usage dashboard (analytics aggregation) | MAPI analytics + Console/Portal | resource + UI specs | dashboard renders product×user×model |

**Phase 4 Verification Matrix**

| Req | Check | Test | Status |
|-----|-------|------|--------|
| (arch) | single base path dispatches | `AiProductReactorIntegrationTest` | ☐ |
| (arch) | product policy runs once across assets | `ProductPolicyPassIntegrationTest` | ☐ |
| R1 | old paths still work (dual acceptor) | `DualAcceptorBackcompatTest` | ☐ |

---

## 10. Appendix

### 10.1 Test-command cheat sheet

| Layer | Command |
|-------|---------|
| Java unit (one module) | `mvn -q -pl <module> test` (avoid `-am` to dodge the license check) |
| Java integration (gateway) | `mvn -q -pl gravitee-apim-integration-tests test -Dtest=ApiProduct*` |
| MAPI resource tests | `mvn -q -pl gravitee-apim-rest-api/...-management-v2-rest test` / `...-portal-rest test` |
| Console (Gamma) | `nx test gravitee-gamma-module-apim` |
| Portal (Angular) | `npx jest <spec>` in `gravitee-apim-portal-webui-next` |
| E2E smoke | `ai-poc-demo/verify/smoke.sh` |
| Full gate | `ai-poc-demo/verify/unit.sh && ai-poc-demo/verify/integration.sh && ai-poc-demo/verify/smoke.sh` |

### 10.2 Key file index (where things live)

| Concern | Path |
|---------|------|
| AI Product core model / use cases | `gravitee-apim-rest-api/.../core/api_product/` |
| Deploy (embeds plans+flows) | `core/api_product/domain_service/DeployApiProductDomainService.java` |
| Plan flow chain (gateway) | `gravitee-apim-gateway/.../handlers/api/v4/flow/resolver/ApiProductPlanFlowResolver.java` |
| Product registry / reactable | gateway `ReactableApiProduct`, `ApiProductRegistry` |
| Security chain injection | `ApiProductPlanPolicyManager`, `HttpSecurityChain`, `SubscriptionProcessor` |
| MAPI v2 api-product resources | `gravitee-apim-rest-api/...-management-v2-rest/.../resource/api_product/` |
| Portal catalog (new) | `gravitee-apim-rest-api/...-portal-rest/.../resource/` |
| Console (Gamma) AI Products | `gravitee-gamma/gravitee-gamma-module-apim/.../features/ai-products/` |
| Portal UI (Angular) | `gravitee-apim-portal-webui-next/src/.../dashboard/ai-product*` |
| Repository tables / migrations | `gravitee-apim-repository/` (JDBC liquibase + Mongo) |
| Verification scripts | `ai-poc-demo/verify/` |

### 10.3 Definition of Done (copy into each PR)

```
[ ] code + test in same PR        [ ] unit.sh green
[ ] integration.sh green (if MAPI/gateway touched)
[ ] verification-matrix row ✅ with test name
[ ] additive-only: no breaking change to API Product / plan / subscription
[ ] (phase exit) smoke.sh green
```

---

*Roadmap is the implementation projection of the AI Products PRD. Keep the verification matrices current —
they are the automatic "are we on track" signal.*
