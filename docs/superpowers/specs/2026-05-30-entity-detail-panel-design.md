# Entity Detail Panel + Entities page parity (Gamma authz) — Design

Date: 2026-05-30
Module: `gravitee-gamma/gravitee-gamma-module-authz` (UI: `src/main/ui`)
Reference: staging `https://gamma-photon-party.vercel.app/authorization/entities`
Status: Approved design — ready for implementation plan

## Problem

Our Entities page shows a flat table with edit/delete actions only. The staging
reference adds a rich **entity detail panel** (opened by clicking a row) plus
table/KPI/settings affordances. We want the Entities page — and especially the
entity preview — to closely match staging.

## Goal

Bring the Entities page to parity with the staging reference:

1. **Entity detail panel** (right Sheet, opened on row click) with four tabs:
   Overview, Relationships, Policies, GAPL shape (canonical JSON + Copy).
2. **Table parity:** Policies column, Relationships column, copy-to-clipboard on
   Entity ID, Source as a badge.
3. **KPI parity:** add a fifth tile, "Policy-Linked".
4. **Settings:** a gear menu on the page header with a single action
   "Open entities.json" (export the current collection as JSON).

## Non-goals (explicit)

- No backend changes. All data comes from existing services/hooks.
- **No "Reset to seeded entities".** That is a staging-demo affordance with no
  equivalent in the real backend; intentionally omitted.
- **No hard cross-module navigation.** Staging's header links ("Manage in Agent
  Management", deep catalog links) are simplified to informational source +
  provenance display, since those routes are out of this module's scope.
- The panel is a read-only preview. Editing remains via the existing
  `EditEntityDialog`, reachable from an "Edit" button in the panel header for
  `local` entities only.

## Data sources (no new endpoints)

| Need | Source |
| --- | --- |
| Attributes (Name/Type/Value) | `entity.attrs` + `inferType` (from `attribute-codec`*) |
| Provenance (source, catalog origin, imported/created/updated) | `entity.source/importedAt/createdAt/updatedAt` |
| GAPL shape JSON | `entity.uid` (`{type,id}`), `entity.attrs`, `entity.parents` |
| Parents | `entity.parents` (labels resolved from the full entity list) |
| "Referenced by" | `useAllEntities` — entities whose `parents` include this uid |
| "contains N \<type\>" | reverse children grouped by `uid.type` |
| Policies | `usePolicies(env)` — policies where `entityId === formatEntityUid(uid)` |
| "Policy-Linked" KPI | count of entities with ≥ 1 matching policy |

\* `attribute-codec.ts` ships in the (separate, open) attribute-editor PR. If that
has not merged when this is implemented, add a minimal local `inferType` helper
(boolean→Boolean, integer number→Integer, array→Set, else String) in the new
`entity-gapl-shape.ts` rather than depending on the other branch. This keeps the
plan self-contained.

## Architecture (Approach A)

A presentational Sheet shell + small per-tab components + pure data builders.

### Pure modules (no React, fully unit-tested)
- **`entity-gapl-shape.ts`** —
  - `buildGaplShape(entity): { uid: { type: string; id: string }; attrs: Record<string, unknown>; parents: string[] }`
  - `toGaplJson(entity): string` — `JSON.stringify(buildGaplShape(entity), null, 2)`.
  - The canonical document the PDP evaluates: uid + attrs + parents (parents as
    canonical dotted strings via `formatEntityUid`).
- **`entities-json.ts`** —
  - `buildEntitiesJson(entities: EntityInstance[]): string` — pretty-printed JSON
    array of each entity's GAPL shape. Used by the settings export.
- **`entity-relationships.ts`** —
  - `referencedBy(entity, all): EntityInstance[]` — entities listing this uid as a parent.
  - `childrenByType(entity, all): { type: string; count: number }[]` — grouped reverse children.
  - `policiesFor(entity, policies): AuthzPolicy[]` — `entityId === formatEntityUid(uid)`.

### Presentational components
- **`EntityDetailSheet.tsx`** — shell: header (type badge + source badge, title =
  displayName, uid + copy button, timestamp, chips `N attrs · N parents · N
  referenced by · N policies`), Tabs (Overview / Relationships / Policies / GAPL
  shape), an "Edit" button (only `source === 'local'`) that opens
  `EditEntityDialog`, and close. Receives `entity`, `allEntities`, `policies`,
  `onEdit`, `onOpenChange`.
- **`EntityOverviewTab.tsx`** — attributes table (Name/Type/Value) + Provenance
  rows. Empty state "No attributes." when none.
- **`EntityRelationshipsTab.tsx`** — Parents list + "Referenced by" list +
  "contains" chips. Empty state when isolated.
- **`EntityPoliciesTab.tsx`** — list of matching policies (name + status badge);
  empty state "No policies reference this entity."
- **`EntityGaplShapeTab.tsx`** — code block of `toGaplJson(entity)` + "Copy JSON"
  button (clipboard with graceful fallback).

### Page integration (`EntitiesPage.tsx`)
- Row click opens `EntityDetailSheet` (state `viewing: EntityInstance | null`).
  Existing edit/delete row actions remain (their clicks must `stopPropagation` so
  they don't also open the panel).
- New columns: **Relationships** ("in \<parent\>" + "contains N \<type\>" chips) and
  **Policies** (count). Copy icon next to Entity ID. Source rendered as a `Badge`.
- Fifth KPI tile "Policy-Linked" = entities with ≥ 1 policy.
- Settings: a `DropdownMenu` triggered by a gear `Button` in the page header,
  single item "Open entities.json" → `buildEntitiesJson(all)` → open in a new tab
  via a `Blob` object URL (revoked after open).

## Data flow

1. EntitiesPage already loads principals + resources via `useAllEntities`, and now
   also `usePolicies(env)`. It passes the full entity list + policies down.
2. Row click → `setViewing(entity)` → `EntityDetailSheet` renders; tabs compute
   their data from the passed-in lists via the pure helpers (no extra fetches).
3. "Edit" in the panel → opens existing `EditEntityDialog` for local entities.
4. Settings → builds JSON client-side and opens it in a new tab.

## Error handling / edge cases

- Empty states for every tab (no attrs / no relationships / no policies).
- `Copy JSON` and the uid copy: use `navigator.clipboard.writeText` guarded by a
  feature check; on failure show a transient "Copy failed" without throwing.
- Read-only entities: no Edit button; show source badge + provenance; never mutate.
- Row-action clicks (edit/delete pencils) call `stopPropagation` so the row's
  open-panel handler does not also fire.
- Reverse-relationship and policy matching are O(n) over the already-loaded lists —
  fine for expected sizes; computed in `useMemo`.
- `Policies` data: if `usePolicies` errors or is loading, the Policies tab/column
  show a neutral state (— / "Loading…"), never block the rest of the panel.

## Testing (TDD)

- `entity-gapl-shape.test.ts` — shape correctness (uid/attrs/parents), JSON string
  formatting, parents serialized as canonical dotted strings.
- `entities-json.test.ts` — array export, stable shape.
- `entity-relationships.test.ts` — referencedBy, childrenByType grouping,
  policiesFor matching (incl. no-match and GLOBAL policies excluded).
- `EntityDetailSheet.test.tsx` — header chips counts, tab switching, Edit button
  present only for local, absent for catalog.
- Per-tab tests — Overview (attrs table + provenance + empty), Relationships
  (parents + referenced-by), Policies (match + empty), GaplShape (JSON rendered +
  Copy invokes clipboard).
- `EntitiesPage` additions — row click opens the panel; Policies/Relationships
  columns render; Entity ID copy; Source badge; Policy-Linked KPI; settings menu
  builds entities.json (assert blob/open invoked).
- `tsc --noEmit`, full `vitest`, `nx lint`, `nx format:check` green before PR.

## Out of scope / follow-ups

- Cross-module deep links (catalog item / Agent Management) once those routes exist.
- "Reset to seeded entities" (no real-backend equivalent).
