# AI Product — Build & Architecture Guide (engineering reference)

> Purpose: a practical "how this is built and how to change it fast" guide. Read §1–§3 for the model,
> then jump to §6 **Recipes** and §7 **Gotchas** to make a change in a single attempt.
>
> Companion docs (same folder): `AI_PRODUCT_ARCHITECTURE.md` (vision), `AI_PRODUCT_ROADMAP.md` (phases).
> Living status board: the Cursor canvas `ai-product-progress.canvas.tsx`.

---

## 1. The one mental model (memorize this)

- An **AI Product is an APIM API Product** with a `type` discriminator. The wire/referenceType stays
  `API_PRODUCT` (Decision **D1** — "rename the concept, freeze the wire"). AI behaviour branches on `type`.
- An AI Product is a **box of assets**. Every capability — LLM proxy, MCP proxy, agent, tool, stream — is an
  **asset** = `{ type, config }`. Kind-specific settings live in the asset's **`config`** (a JSON string),
  never in per-kind tables.
  - LLM proxy asset → `config` = `{ models: [...] }`
  - (future) MCP asset → `config` = `{ tools, acl }`; agent → `{ agentCard }`; etc.
- **Governance is asset-agnostic.** Plan / key / per-user budget / rate-limit / 429 hang off the **product plan +
  subscription**, governing whatever assets are bundled. Token budget for LLM, request rate-limit for any,
  cost budget for any priced asset.

**Golden rule for new capabilities ("add anything, never rebuild"):** a new asset kind = new enum value +
its sync service (reads its asset `config`) + attach flow + portal card. **No new tables, no core rebuild.**

---

## 2. Repos in play (multi-root workspace)

| Repo | Role |
|------|------|
| `gravitee-api-management` | ~90% of the feature: core, repository (JDBC+Mongo+liquibase), gateway, MAPI v2 |
| `gravitee-plugins/gravitee-gamma-module-aim` | the **AIM ("Agent Management") console** — React 19 + Rsbuild + Module Federation, Graphene design system, Vitest. This is where the AI Product UI lives. |
| `gravitee-reactor-llm-proxy` | the embedded LLM proxy reactor + endpoint/entrypoint connectors (the model-routing engine) |
| `gravitee-plugins/gravitee-policy-token-ratelimit`, `…-ratelimit` | per-user token / request budgets (P1.4–1.5) |

Build toolchain: **JDK 21**, Maven 3.9, Yarn 4 (Corepack). Node 22.12.x.

---

## 3. Layered architecture & where each concern lives

```
AIM Console (React, gamma-module-aim)  ──HTTP──►  MAPI v2 (JAX-RS resources)
                                                      │ uses
                                                      ▼
                                              Core use cases (@UseCase)
                                                      │ orchestrate
                                                      ▼
                                       Domain services (@DomainService)
                                                      │ via ports
                                                      ▼
                                  CRUD/Query ports → infra adapters (MapStruct) → repository (JDBC/Mongo)
                                                      │ deploy event
                                                      ▼
                                          Gateway (reactors, plan flow chain) ── LLM/MCP reactors
```

Key packages (in `gravitee-apim-rest-api/gravitee-apim-rest-api-service/.../core/api_product/`):
`model/`, `use_case/`, `domain_service/`, `crud_service/`, `query_service/`. Infra adapters in
`…/infra/adapter/ApiProductAdapter.java` and `…/infra/crud_service/api_product/`.

---

## 4. What exists today (the shared `ApiProduct` surface)

Shared `ApiProduct` (repo + core) carries, beyond the generic API-Product fields:
- `type` : `ApiProductType { AI_PRODUCT }` — nullable; `null` = classic API Product (backward compatible).
- `assets` : `List<ApiProductAsset/Ref>` — each `{ id, assetId, type:ApiProductAssetType, pathSuffix, configJson, enabled }`.
  - `ApiProductAssetType { DEFAULT_LLM, LLM_PROXY, MCP_PROXY, AGENT }` (open-ended).
  - **No `role`** — default-vs-attached is derived from `type` (`DEFAULT_LLM` = the embedded default).

Tables (additive, liquibase `v4_12_0/`): `20_add_type_to_api_products`, `21_add_api_product_assets_table`.
LLM model catalog is **not** a table — it lives in the `DEFAULT_LLM` asset's `configJson` (typed by `LlmAssetConfig`/`LlmModel`).

---

## 5. Feature status (Phase 1 = LLM is the POC scope)

Done & verified (unit/compile; repo contract tests run in CI with DB containers):
- `type` discriminator end-to-end (repo→core→MAPI), `_search` type filter (Lucene), AIM list/create/filter.
- `api_product_assets` typed table.
- **Default LLM Proxy auto-provisioning** on create (`ProvisionDefaultLlmProxyDomainService`).
- **Model catalog**: `AiProductModelCatalogDomainService` + add/get/delete use cases + `SyncDefaultLlmModelsDomainService`
  (writes the catalog into the embedded proxy endpoint config) + MAPI `/api-products/{id}/models`.

Pending: AIM Models UI; **P1.3–1.5 product plan + per-user token budget → 429** (headline); Dev Portal; P2 (dollars,
assignments, wizard); P3 (MCP/agents/streams as assets); P4 (`AiProductReactor`). Nothing committed; runtime behaviours
(routing, 429) need a live gateway to prove.

---

## 6. Recipes (the fast path)

### 6.1 Add a field to `ApiProduct` (additive, non-breaking)
1. Repo model `gravitee-apim-repository-api/.../model/ApiProduct.java` — add field.
2. JDBC `gravitee-apim-repository-jdbc/.../JdbcApiProductRepository.java` — add `.addColumn(...)` in `buildOrm()` (scalar)
   OR a child-table store/load (collection, see 6.3).
3. Liquibase: new `…/liquibase/changelogs/v4_12_0/NN_*.yml` + register in `…/liquibase/master.yml`.
4. Mongo `…/internal/model/ApiProductMongo.java` — add field (same name → MapStruct auto-maps).
5. Repo contract test `gravitee-apim-repository-test/.../ApiProductRepositoryTest.java` — round-trip assertion.
6. Core model `…/core/api_product/model/ApiProduct.java` — add field. MapStruct `ApiProductAdapter` auto-maps by name.
7. MAPI: see 6.4 (remember the **dual-spec gotcha**).

### 6.2 Add a new asset kind (MCP / agent / tool / stream)
1. Add enum value to **both** `ApiProductAssetType` (repo + core).
2. Define its typed config POJO in `core/api_product/model/` (like `LlmAssetConfig`) ↔ stored in `asset.configJson`.
3. Write a `Sync<Kind>DomainService` reading that asset config → the component's runtime config (mirror
   `SyncDefaultLlmModelsDomainService`).
4. Attach flow: a use case to add the asset (mirror provisioning/catalog use cases).
5. AIM: a card/screen for the kind. **No new tables.**

### 6.3 Add a child collection table (like `api_product_assets`)
Mirror the assets implementation in `JdbcApiProductRepository`: `store<X>` (batch insert, generate UUID id),
`get<X>` (rowmapper), `enrich<X>` (called in every list method next to `enrichWithTags`), wire into
`create/update/findById/delete`. Mongo: embedded `List<XMongo>` on `ApiProductMongo`. Core + adapter auto-map.

### 6.4 Add / change a MAPI v2 endpoint or DTO (API-first)
1. Edit OpenAPI in `gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-api-products.yaml`
   (schemas + optionally paths; paths are doc-only — `generateApis=false`).
2. Compile the module so `openapi-generator` regenerates DTOs into `target/generated-sources/openapi`.
3. Implement/........ the JAX-RS resource; register sub-resources in `ApiProductResource` via
   `resourceContext.getResource(XResource.class)` under `@Path("/x")`.
4. Map generated DTO ↔ core model (manual or MapStruct).
5. **Automation API check** (AGENTS rule): if the field is surfaced for GitOps, mirror it in the Automation API spec.

---

## 7. Gotchas (these cost the most time — check them first)

1. **`ApiProduct` is defined in TWO OpenAPI specs.** Both `openapi-api-products.yaml` and `openapi-apis.yaml`
   define `ApiProduct` (and `ApiProductType`), generated into the **same** Java model package — last execution wins.
   When you add a field to the `ApiProduct` DTO you must edit it in **both** specs, or it silently won't appear.
2. **`POST /api-products/_search` uses Lucene, not `ApiProductCriteria`.** To filter the search endpoint you must
   touch the index (`IndexableApiProductDocumentTransformer`) + searcher (`ApiProductDocumentSearcher`) +
   `ApiProductSearchQueryServiceImpl` (`queryBuilder.addFilter(...)`), not the repository criteria.
3. **MapStruct**: maps by field name; enums map by constant name; cross-package same-name types are disambiguated
   with FQNs in generated code. Adding a same-named field on both sides → auto-mapped, no adapter edit.
4. **Build**: `mvn -q -DskipTests -Dskip.validation=true -am -pl <module> compile` (JDK 21). Add
   `-Dmaven.compiler.useIncrementalCompilation=false` to avoid "NoSuchFileException … .class / Please remove"
   incremental-compile corruption. If `clean` fails with "Failed to delete target", `chmod -R u+rwx <module>/target && rm -rf <module>/target`.
5. **`-am` test runs** need `-Dsurefire.failIfNoSpecifiedTests=false` (upstream modules have no matching test).
6. **Repository contract tests** (`*RepositoryTest`) need DB containers — they run in CI, not via a bare `mvn test` here.
7. **AIM is a separate repo** (`gravitee-plugins/gravitee-gamma-module-aim`). UI = Graphene components
   (`@gravitee/graphene-core`), data via `lib/api/management-api-client.ts` (MAPI v2) + react-query; tests = Vitest
   (`yarn vitest run <pattern>`). Routes in `config/routes.ts`, nav in `config/navigation.ts`, registered in `app/AppRoutes.tsx`.
8. **Provisioning a V4 API** from a domain service: build `NewHttpApi` → `ApiModelFactory.fromNewHttpApi` →
   `CreateApiDomainService.create(...)` (NOT raw `ApiCrudService.create`, which skips audit/index/flows/owner).
   LLM proxy = `ApiType.LLM_PROXY`, entrypoint/endpoint connector type `"llm-proxy"`.
9. **LLM proxy endpoint config schema**: one endpoint per `provider+target+authentication`; models are the
   endpoint's `models[]` (`{name, aliases, inputPrice, outputPrice}`); auth `{type:"API_KEY", headerName, apiKey}`;
   HTTP tuning goes in `endpointGroup.sharedConfiguration`. Clients call `{groupName}:{model}`.

---

## 8. Key file index

| Concern | Path |
|---|---|
| Core AI Product model/use cases/domain services | `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/api_product/` |
| `ProvisionDefaultLlmProxyDomainService` | `…/core/api_product/domain_service/` |
| Model catalog (`AiProductModelCatalogDomainService`, `SyncDefaultLlmModelsDomainService`, `LlmAssetConfig/LlmModel`) | `…/core/api_product/domain_service/` + `…/model/` |
| Repo model + JDBC + Mongo | `gravitee-apim-repository/gravitee-apim-repository-{api,jdbc,mongodb}/…/ApiProduct*` |
| Liquibase | `gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs/v4_12_0/` + `master.yml` |
| MapStruct adapter | `…/infra/adapter/ApiProductAdapter.java` |
| MAPI v2 resources | `gravitee-apim-rest-api-management-v2-rest/.../resource/api_product/` |
| MAPI v2 OpenAPI | `gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-api-products.yaml` (+ `openapi-apis.yaml`) |
| Lucene index/searcher | `…/service/impl/search/lucene/transformer|searcher/ApiProduct*` |
| AIM AI Products UI | `gravitee-gamma-module-aim/src/main/ui/app/features/secure/{pages,components,hooks}/` + `lib/api/ai-product.*` |
| Verify scripts | `gravitee-api-management/ai-poc-demo/verify/` |

---

## 9. Decisions log
- **D1**: visible concept = "AI Product"; wire stays `API_PRODUCT` + `type` discriminator. No new reference type.
- **Generic-first**: assets + per-asset `configJson`; **no per-kind tables** (the `api_product_models` table was
  built then reverted in favour of the `DEFAULT_LLM` asset config).
- **Asset `role` removed**: redundant with asset `type` (`DEFAULT_LLM` is the default).
- **Additive-only**: every DB/DTO/event change is a nullable column / parallel table / optional field.

---

## 10. Resuming after the stash
The AI Product code changes are stashed (see the repo READMEs / `git stash list`):
- `gravitee-api-management`: `git stash list` → pop the "AI Product WIP" entry (scoped to `gravitee-apim-repository` + `gravitee-apim-rest-api`).
- `gravitee-gamma-module-aim`: pop the "AI Product WIP" entry (scoped to `src/main/ui`).
- `gravitee-reactor-llm-proxy`: the API-Product reactor wiring is a separate pre-existing uncommitted change (commit to a branch).
This guide, the verify scripts, and the canvas are **not** stashed and remain available.
