# Continuation: Reduce Per-Component Cost in Zee Frontend

I'm working in `gravitee-api-management` at `~/workspace/Gravitee/gravitee-api-management`. Branch: `feat/zee-mode`. Tech stack: Angular 19 + Java 17 + `json-schema-llm` SDK.

## Next Up

Reduce the per-component cost of adding new Zee resource types. Currently, each resource type requires 6 touch points (adapter, card component [ts+html+scss], preview switch case, module registration, host page wiring). The goal is to eliminate redundant layers.

## Key Context

- **The `json-schema-llm` SDK already validates and structures LLM output** against JSON Schema on the backend. The data arrives at the frontend pre-validated. The frontend adapters (`adapters/*.ts`) re-map this data field-by-field with defensive coercion — this is redundant norfmalization.
- **Adapters can likely be eliminated entirely.** The `transform()` logic duplicates what the SDK already guarantees. Known quirks (like `target` → `configuration` promotion) can be solved upstream via JSON Patch on the schema itself.
- **Structured view cards** (`zee-structured-view/*-card.component.*`) are bespoke per resource type. The `zee-preview` already has a JSON tab fallback. Consider whether a single generic card component that introspects the data shape could replace all 5.
- **Shared helpers are already extracted** into `adapters/helpers.ts` (8 functions: `asString`, `asBoolean`, `asNumber`, `asOptionalString`, `asArray`, `asStringArray`, `asRecord`, `parseConfig`). If adapters go away, helpers go too.

## Roadmap & Context Docs

All located in `tmp/feat-roadmap/`:

- `README.md` — project overview
- `architecture-overview.md` — system architecture
- `json-schema-llm-integration.md` — how the SDK integrates
- `frontend-widget.md` — frontend Zee module design
- `agent-backend.md` — backend service design
- `001-018` .md files — numbered issue tracker (015 complete, 016-018 open)

## Current State (clean, all pushed)

```
4f9806c5a4 refactor(ai): extract shared helpers from zee adapters
1c2e67adec feat(ai): improve zee widget keyboard interactions and auto-focus
746b7128d3 feat(ai): support update mode and improve V4 SDK integration
```

## The Exploration

1. **Kill the adapters** — Stop calling `adapter.transform()` in `zee-widget`. Pass `generated` data straight through. The host page `(accepted)` handler receives SDK-structured JSON directly.
2. **Kill the bespoke cards** — Replace 5 `*-card.component` files with a single generic preview component that renders any JSON object with reasonable formatting (key-value pairs, nested objects as expandable sections, arrays as lists).
3. **Simplify `zee.model.ts`** — `ZeeResourceAdapter` interface may become unnecessary if adapters are removed.
4. **Measure the cost** — After each cut, verify the UI still works. The JSON tab in preview is the safety net.

## What NOT to Do

- Don't touch the backend (`GenerateResourceUseCase`, `LlmEngineServiceImpl`, `ZeeResource`) — it's stable.
- Don't remove `helpers.ts` until adapters are confirmed dead — it's the fallback if we need to keep lightweight adapters.
- Don't start on 016/017/018 yet — this refactor makes them cheaper to implement.
