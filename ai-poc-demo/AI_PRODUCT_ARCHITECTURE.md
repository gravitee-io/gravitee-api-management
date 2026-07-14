# AI Product — Ground-Zero Architecture

> A governed "AI workspace" on Gravitee: an admin creates an AI Product, adds models / proxies /
> MCP tools, sets plans + per-user budgets, and developers self-serve from the Dev Portal with their
> own keys. Goal: the governance + multi-modality of an API platform with the model-routing ergonomics
> of LiteLLM / Portkey — and more, because it sits inside a full API gateway.

---

## 0. The one insight that shapes everything

From reactor research (LLM Proxy, MCP, V4 reactor, API Product):

- **The LLM Proxy reactor is already a model router.** `LLMInvoker.findTargetModel()` maps a requested
  model → provider/endpoint by name/alias/prefix priority; `/models` aggregates the catalog across all
  endpoint groups; the endpoint already emits `llmproxy.usage.sent/received.token` + cost + OTel GenAI spans.
- **API Product is NOT a gateway reactor.** Component APIs are the reactors; the product *layers* plan
  security into each component's `HttpSecurityChain` and (new this project) a product-plan **flow chain**
  for per-user `token-ratelimit`. `SubscriptionProcessor` sets `ATTR_API_PRODUCT`.
- **One reactor can serve many paths**; each MCP component needs its own path.

**Therefore:** we get the LiteLLM/Portkey core *for free* by provisioning **one embedded LLM Proxy** per
AI Product and managing its **model catalog** from the product. No new reactor, no router to build in
Phase 1. The AI Product is the **provisioning + governance + self-service** layer on top.

---

## 1. Vision & differentiation

An **AI Product** bundles, under one governed unit with one consumer experience:

| Capability | Backed by | Status |
|---|---|---|
| OpenAI-compatible endpoint, many models, multi-provider routing, failover | embedded **LLM Proxy** reactor | exists |
| Per-model pricing + token/cost metering + GenAI traces | LLM Proxy endpoint | exists |
| MCP tools / servers (tools/call, resources, prompts) + per-tool ACL | **MCP Proxy** reactor + `mcp-acl` | exists |
| Per-user **API key**, **token budget**, **rate limit** | product plan + `token-ratelimit` dynamicLimit (metadata) | **built this project** |
| Self-service catalog → subscribe → approve → key | Dev Portal + portal-rest | **built this project** |
| Any gateway policy on AI traffic (redaction, threat-prot, transforms, JWT/OAuth2) | V4 policy chain | exists |
| RBAC, groups, audit, approval workflows | APIM core | exists |

**Why it beats LiteLLM/Portkey:** they are LLM-only proxies. We add **heterogeneous bundling**
(LLM + MCP tools + future agents) under **one product, one plan, one key, one budget**, with a real
**developer portal**, full **policy chain**, **provider-key hiding**, and **enterprise governance**
(RBAC/groups/approvals/audit/OTel) — inside the same gateway already running the org's APIs.

---

## 2. Phases (roadmap)

**Phase 1 — Self-contained LLM product (no new reactor).**
Auto-create a default embedded LLM Proxy with the product; manage its model catalog from the product UI;
product plans + per-user budgets (done); Dev Portal self-serve (done); live usage via response headers.
→ Ships the LiteLLM/Portkey replacement with full governance. Lowest risk.

**Phase 2 — Heterogeneous components.**
Attach MCP proxies/tools as typed components under the product (own paths, shared plan/auth/budget);
typed `components[]` model (backward-compatible with `apiIds`); per-component config + enable flags.

**Phase 3 — Unified product entrypoint.**
A stable product base path (`/ai/{product-hrid}/…`) with a thin path-router (or a dedicated
`AiProductReactor`) presenting ONE base URL that dispatches to components by sub-path. Optional; only
once Phases 1–2 are proven, since it's the deepest gateway change.

**Phase 4 — Intelligence & cost.**
Cross-provider fallback policy, semantic/model routing, prompt caching, budget alerts, a real usage
dashboard (counter-store/analytics), guardrail policies (PII redaction, jailbreak detection).

---

## 3. Components — how different things attach, and the entrypoint model

### 3.1 Component types (typed bundle)
Refactor the flat `apiIds: Set<String>` into a typed list (keeping `apiIds` as a derived view):

```
ProductComponent {
  id            // underlying V4 API id (LLM_PROXY or MCP_PROXY), or null for pure config
  type          // DEFAULT_LLM | LLM_PROXY | MCP_PROXY | MCP_TOOLS | (future) AGENT/TOOL
  role          // DEFAULT (the embedded engine) | ATTACHED
  pathSuffix    // sub-path under the product (e.g. "", "mcp/jira")
  config        // per-type JSON (model catalog ref, ACL, etc.)
  enabled
}
```

### 3.2 "Add a model" = manage the default LLM Proxy's catalog
The product owns **one DEFAULT_LLM component** = a hidden `LLM_PROXY` API auto-created at product creation.
"Add model from catalog" writes a `Model{name, provider, target, auth(apiKey), inputPrice, outputPrice,
aliases, governance}` entry into that proxy's endpoint config (grouped by provider). The LLM Proxy reactor
then routes by model name and meters tokens. → One endpoint, many models, many providers. No new code on
the gateway; we drive it through existing LLM Proxy config + a model-catalog UI.

### 3.3 "Add an LLM proxy / MCP proxy / tools" = attach a component
Attach an existing (or product-created) `LLM_PROXY` / `MCP_PROXY` API as an `ATTACHED` component. It keeps
its own reactor + path but is **secured by the product's plan** (plan security already injected per-API via
`ApiProductPlanPolicyManager`) and shares the **product subscription + budget**.

### 3.4 Entrypoint strategy
- **Phase 1/2:** convention-based namespacing — default LLM at `/<base>` (e.g. `/ai/<product-hrid>`),
  MCP components at `/<base>/mcp/<name>`. Each component is its own reactor/acceptor; the product plan
  unifies auth + budget. This needs **no new reactor**.
- **Phase 3:** a product-level reactor/path-router for a single clean base URL (deepest change; deferred).

---

## 4. Plans

- **One published plan per reset window** (per minute/hour/day/week/month) — bounded to ≤5 regardless of
  user count, because the per-user *limit* is metadata and only the *period* is plan-level (the
  `token-ratelimit` policy has `dynamicLimit` but no dynamic period). Auto-managed; admin never hand-builds.
- Security types: **API_KEY** (default, key+curl UX), JWT, OAuth2, mTLS — all supported by the security chain.
- Validation **MANUAL** by default: every subscription lands PENDING so the admin sets the user's budget at
  approval. Approval ensures the right per-window plan exists and **transfers** the subscription onto it.
- Plans carry the per-user policy **flows** (`token-ratelimit` + `rate-limit`), persisted to the flow store
  and **embedded into the deploy event** (fixed this project — was the root cause of non-enforcement).

## 5. Policies, users, keys, budgets

- **Per-user budget**: `token-ratelimit` with
  `dynamicLimit = {#subscription.metadata['tokenLimit'] ?: <fallback>}`, counter keyed per-subscription
  (verified in policy bytecode: key = api∶subscription∶plan), so users never share a budget.
- **Per-user rate**: `rate-limit` with the same metadata-driven dynamicLimit.
- **Unique key per user**: portal subscribe uses `api_key_mode: EXCLUSIVE` on a user-owned application →
  each subscriber gets their own key tied to their own budget; per-product revoke.
- **Admin lifecycle** (Subscribers screen): Approve (+ set budget/rate/window) → Edit limits → **Revoke**
  (closes the subscription → key 401s). All via api-product subscription endpoints.
- **Live usage**: every response returns `X-Token-Rate-Limit-Remaining` / `-Limit` / `-Reset`. Real-time
  per-call balance today; aggregate dashboard = Phase 4 (counter-store/analytics).
- **MCP access**: `mcp-acl` policy gates tools/resources/prompts per plan/condition.
- Any other policy (redaction, threat protection, JWT) composes in the same chain.

## 6. Gateway + DB refactor — backward-compatible

**DB (additive only):**
- `api_products.type` discriminator (AI_PRODUCT) — already added via migration.
- New `api_product_components` (product_id, component_id, type, role, path_suffix, config_json) + Mongo array;
  keep `apiIds` as a derived/synced view so existing rows + code keep working.
- `embedded_llm_proxy_api_id` on the product (nullable); legacy products = null.
- (Phase 3) optional `entrypoint_base_path`.

**Gateway (additive only):**
- Deploy event payload already carries plans **with flows** (fixed). Extend `ReactableApiProduct` +
  `ApiProductMapper` to also carry typed `components[]` — deserialize gracefully when absent (old events).
- Default LLM proxy is a normal `LLM_PROXY` API → uses the existing reactor; no gateway change to route models.
- Phase 3 only: register an `AiProductReactor` via `ReactorFactory.canCreate(type==AI_PRODUCT && hasBasePath)`.

**Non-breaking guarantees:** every change is an *added* nullable field / parallel table / new optional event
property. Existing API Products (apiIds-based), plans, subscriptions, and the standard API reactor path are
untouched. `getApi()==null` guards already protect non-API_PRODUCT subscriptions (portal 500 fix).

## 7. Management API & Dev Portal

- **MAPI v2**: extend `api_product` resources — model-catalog CRUD on the default proxy
  (`/api-products/{id}/models`), components CRUD (`/api-products/{id}/components`), keep plans/subscriptions.
- **Console (Gamma)**: product Overview, Components (add LLM/MCP), **Models** (add from catalog + price),
  Plans (window), Subscribers (approve/edit/revoke + budget/window).
- **Dev Portal**: catalog → product detail (models, endpoint, snippets) → subscribe (EXCLUSIVE key) →
  pending → on approval the consumption view (key, endpoint, per-proxy models, cURL/Python/JS).

## 8. Risks / open decisions

1. **Embedded proxy ownership** — auto-created hidden API: lifecycle coupling (delete product ⇒ delete proxy),
   visibility (`allowedInApiProducts=true`, hidden from normal API list).
2. **Model secrets** — provider API keys live in the embedded proxy endpoint config; use secret refs / encryption.
3. **Entrypoint unification (Phase 3)** — single base path vs. per-component paths: only worth the reactor work
   once multi-component products are common.
4. **Usage dashboard** — needs the rate-limit counter store (Redis) or analytics pipeline; scope in Phase 4.
5. **Quotas across components** — one budget for LLM+MCP, or per-component sub-budgets? Start with one; revisit.

## 9. Sequencing (concrete next steps)

1. `ProductComponent` typed model + `api_product_components` table (additive) + derived `apiIds`.
2. Auto-create the **default embedded LLM Proxy** on AI Product create; `embedded_llm_proxy_api_id`.
3. **Model catalog** MAPI + Console UI writing to the embedded proxy endpoint config.
4. Carry `components[]` in the deploy event + `ReactableApiProduct` (graceful when absent).
5. Portal: surface the product's single LLM endpoint + models from the embedded proxy.
6. (Phase 2) MCP component attach + `mcp-acl` per plan.
7. (Phase 3) evaluate the unified `AiProductReactor`.
</content>
</invoke>
