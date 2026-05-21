# Gamma Module - Authorization

## UI Development

This module exposes a React Module Federation remote named `authz` at `./App`, mounted by the host Gravitee shell. UI source lives under `src/main/ui/`.

### Run standalone

    yarn install
    yarn serve

Opens on http://localhost:3003.

### Build (produces `target/classes/ui/` for Maven packaging)

    yarn build

### Test

    yarn test

### Module-Federation consumer

When loaded by the Gravitee shell, the exposed remote is:

- Name: `authz`
- Expose key: `./App`
- Shared singletons: `react`, `react-dom`, `react-router-dom`, `@gravitee/graphene`

### Module pages

#### Schema page (`/authz/schema`)

The Schema page provides a split-pane view for authoring GAPL schema files stored in the backend (`GET / PUT / DELETE /environments/{envId}/schema`).

**Left pane — Outline.** Entities are grouped by category (Principals, MCP, APIs, Agents, LLMs, Events, Custom) and actions are listed in a separate section below. Each row shows the entity or action name plus attribute/parent counts. Clicking a row scrolls the Monaco editor to the corresponding line via `editor.revealLineInCenter`. Categories and grouping logic are driven by `getEntityCategoryId` from `entity-types.ts` — entities with names not present in the known-types list fall into the Custom bucket.

**Right pane — Monaco editor.** A live editable buffer using the GAPL language definition (`gapl-language.ts`). The outline updates in real time as you type, using a tolerant outline-only parser (`gapl-parser.ts`) that extracts `entityDef` and `actionDef` constructs without blocking on malformed syntax.

**Stat row.** Four pills show entity count, action count, distinct-category count, and diagnostic count. The diagnostics pill switches to a destructive badge style when the local parser detects unbalanced braces or other soft errors; the server-side GAPL engine is still the authoritative validator on Save.

**Save / Reset / Delete.** Save is disabled when the buffer is unchanged. Reset restores the last saved text (with a confirm). Delete removes the backend document and pre-fills the starter template.

### Mocked features

Some pages (Playground, Runtime, Lineage, Runtime PDPs, Dashboard KPIs, Integrations) are backed by in-memory mocks in `src/main/ui/lib/mocks/` until the matching backend endpoints exist. Each mock preserves the same return shape a real API service would use, so swapping to live data is a single-import change.
