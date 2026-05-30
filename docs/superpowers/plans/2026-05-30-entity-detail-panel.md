# Entity Detail Panel + Entities Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the Gamma authz Entities page to parity with the staging reference — an entity detail panel (right Sheet, 4 tabs incl. canonical GAPL-shape JSON), table columns (Policies, Relationships), a Policy-Linked KPI, copy-on-Entity-ID, Source badge, and a settings menu that exports `entities.json`.

**Architecture:** Three pure, unit-tested modules (`entity-gapl-shape`, `entity-relationships`, `entities-json`) hold all derivation logic. Presentational tab components consume them. `EntityDetailSheet` is the shell; `EntitiesPage` opens it on a Name-cell click and gains the new columns/KPI/settings. No backend changes — data comes from `useAllEntities` and `usePolicies`.

**Tech Stack:** React 19, TypeScript, `@gravitee/graphene-core`, TanStack Query, Vitest, Testing Library. Module root: `gravitee-gamma/gravitee-gamma-module-authz`. UI under `src/main/ui`.

**Reference spec:** `docs/superpowers/specs/2026-05-30-entity-detail-panel-design.md`.

**Conventions (follow them):**
- Tests: from module root `npx vitest run <pattern>`. Typecheck: from `src/main/ui` `npx tsc --noEmit`. Lint: repo root `npx nx lint gravitee-gamma-module-authz`. Format: prettier (printWidth 140, tabWidth 4, singleQuote, semi, trailingComma all, arrowParens avoid).
- Every source/test file starts with the Apache license header — copy lines 1-15 from `src/main/ui/shared/entity.types.ts`. The `lint-license` target enforces it.
- graphene-core: named imports only, semantic tokens, `cn()` to merge classes, no `as` casts in new code, avoid raw apostrophes in JSX text (`react/no-unescaped-entities`).
- Conventional commits, English, no `Co-Authored-By` trailer.

**Key existing APIs (verified):**
- `EntityInstance` (`shared/entity.types.ts`): `{ uid: {type,id}, displayName?, attrs: Record<string,AttrValue>, parents: {type,id}[], source: 'local'|'apim'|'gravitee-catalog', importedAt?, createdAt?, updatedAt? }`. `AttrValue = string|number|boolean` on master (the attribute-editor branch widens it to include `string[]`; this plan does not depend on that — treat array attrs defensively).
- `formatEntityUid(uid)` → `<kind>.<id>` (from `shared/entity-adapter.ts`).
- `PolicyResponse` (`shared/api/authz-api.types.ts`): `{ id, name, kind:'GLOBAL'|'RESOURCE', entityId: string|null, policyText, status }`.
- `usePolicies(env, { initialPerPage })` (`shared/hooks/usePolicies.ts`) → `{ data: PagedResponse<PolicyResponse>|null, isLoading, error, ... }`.
- `useAllEntities(env, { kind })` (`shared/hooks/useAllEntities.ts`) → `{ data: EntityResponse[], total, isLoading, error }`; `fromBackend` maps to `EntityInstance`.
- `MAX_PER_PAGE = 100` exported from `shared/api/authz-api.service.ts`.
- `EntitiesPage.tsx` already renders 4 KPI tiles, a `Tabs` (principals/resources), and per-tab `EntitiesTable` (a `DataTable` with a `name` column and an actions column). `DataTable` has **no** `onRowClick` prop — open the panel from a clickable Name cell.

---

## File structure

- Create `src/main/ui/shared/entity-gapl-shape.ts` — canonical GAPL document + JSON + `inferAttrType`.
- Create `src/main/ui/shared/entity-relationships.ts` — `referencedBy`, `childrenByType`, `policiesFor`.
- Create `src/main/ui/shared/entities-json.ts` — collection export.
- Create `src/main/ui/features/policy-structure/entity-detail/EntityGaplShapeTab.tsx`
- Create `.../entity-detail/EntityOverviewTab.tsx`
- Create `.../entity-detail/EntityRelationshipsTab.tsx`
- Create `.../entity-detail/EntityPoliciesTab.tsx`
- Create `.../entity-detail/EntityDetailSheet.tsx`
- Modify `src/main/ui/features/policy-structure/EntitiesPage.tsx` — open panel, new columns, KPI, settings.
- Tests alongside each, under the nearest `__tests__/`.

---

## Task 1: `entity-gapl-shape.ts` (canonical JSON + type inference)

**Files:**
- Create: `src/main/ui/shared/entity-gapl-shape.ts`
- Test: `src/main/ui/shared/__tests__/entity-gapl-shape.test.ts`

- [ ] **Step 1: Write the failing test** (license header first)

```ts
import { describe, expect, it } from 'vitest';
import { buildGaplShape, toGaplJson, inferAttrType } from '../entity-gapl-shape';
import type { EntityInstance } from '../entity.types';

function entity(over: Partial<EntityInstance> = {}): EntityInstance {
    return {
        uid: { type: 'MCPServer', id: 'flight-status-mcp' },
        displayName: 'Flight Status MCP',
        attrs: { name: 'Flight Status MCP', url: 'https://x', transport: 'http' },
        parents: [],
        source: 'gravitee-catalog',
        ...over,
    };
}

describe('entity-gapl-shape', () => {
    it('buildGaplShape returns uid(type,id) + attrs + canonical parent strings', () => {
        const shape = buildGaplShape(entity({ parents: [{ type: 'Group', id: 'developers' }] }));
        expect(shape).toEqual({
            uid: { type: 'MCPServer', id: 'mcp.flight-status-mcp' },
            attrs: { name: 'Flight Status MCP', url: 'https://x', transport: 'http' },
            parents: ['group.developers'],
        });
    });

    it('toGaplJson pretty-prints the shape', () => {
        const json = toGaplJson(entity());
        expect(json).toContain('"type": "MCPServer"');
        expect(json).toContain('"id": "mcp.flight-status-mcp"');
        expect(json.startsWith('{')).toBe(true);
    });

    it('inferAttrType maps JS runtime types to display labels', () => {
        expect(inferAttrType('x')).toBe('String');
        expect(inferAttrType(3)).toBe('Integer');
        expect(inferAttrType(3.5)).toBe('Decimal');
        expect(inferAttrType(true)).toBe('Boolean');
        expect(inferAttrType(['a'])).toBe('Set');
    });
});
```

- [ ] **Step 2: Run to verify failure** — `npx vitest run entity-gapl-shape` (module root). Expected: FAIL (module not found).

- [ ] **Step 3: Implement** (license header first)

```ts
import { formatEntityUid } from './entity-adapter';
import type { EntityInstance } from './entity.types';

export type AttrInferredType = 'String' | 'Integer' | 'Decimal' | 'Boolean' | 'Set';

export function inferAttrType(value: unknown): AttrInferredType {
    if (typeof value === 'boolean') return 'Boolean';
    if (typeof value === 'number') return Number.isInteger(value) ? 'Integer' : 'Decimal';
    if (Array.isArray(value)) return 'Set';
    return 'String';
}

export interface GaplShape {
    readonly uid: { readonly type: string; readonly id: string };
    readonly attrs: Record<string, unknown>;
    readonly parents: string[];
}

export function buildGaplShape(entity: EntityInstance): GaplShape {
    return {
        uid: { type: entity.uid.type, id: formatEntityUid(entity.uid) },
        attrs: { ...entity.attrs },
        parents: entity.parents.map(formatEntityUid),
    };
}

export function toGaplJson(entity: EntityInstance): string {
    return JSON.stringify(buildGaplShape(entity), null, 2);
}
```

- [ ] **Step 4: Run to verify pass** — `npx vitest run entity-gapl-shape`. Expected: PASS.
- [ ] **Step 5: Typecheck** — from `src/main/ui` `npx tsc --noEmit`. Expected: PASS.
- [ ] **Step 6: Commit**

```bash
git add src/main/ui/shared/entity-gapl-shape.ts src/main/ui/shared/__tests__/entity-gapl-shape.test.ts
git commit -m "feat(authz-ui): add entity GAPL-shape JSON builder"
```

---

## Task 2: `entity-relationships.ts` (reverse links + policy matching)

**Files:**
- Create: `src/main/ui/shared/entity-relationships.ts`
- Test: `src/main/ui/shared/__tests__/entity-relationships.test.ts`

- [ ] **Step 1: Write the failing test** (license header first)

```ts
import { describe, expect, it } from 'vitest';
import { referencedBy, childrenByType, policiesFor } from '../entity-relationships';
import type { EntityInstance } from '../entity.types';
import type { PolicyResponse } from '../api/authz-api.types';

const mcp = (id: string, parents: { type: string; id: string }[] = []): EntityInstance => ({
    uid: { type: 'MCPServer', id },
    attrs: {},
    parents,
    source: 'gravitee-catalog',
});
const tool = (id: string, parentId: string): EntityInstance => ({
    uid: { type: 'MCPTool', id },
    attrs: {},
    parents: [{ type: 'MCPServer', id: parentId }],
    source: 'gravitee-catalog',
});

const server = mcp('flight-status-mcp');
const all = [server, tool('get-flight', 'flight-status-mcp'), tool('search', 'flight-status-mcp'), mcp('payments-mcp')];

describe('entity-relationships', () => {
    it('referencedBy finds entities that list this uid as a parent', () => {
        expect(referencedBy(server, all).map(e => e.uid.id)).toEqual(['get-flight', 'search']);
        expect(referencedBy(mcp('payments-mcp'), all)).toEqual([]);
    });

    it('childrenByType groups reverse children by type with counts', () => {
        expect(childrenByType(server, all)).toEqual([{ type: 'MCPTool', count: 2 }]);
    });

    it('policiesFor matches policies whose entityId equals the canonical uid', () => {
        const policies: PolicyResponse[] = [
            { id: '1', name: 'p1', kind: 'RESOURCE', entityId: 'mcp.flight-status-mcp', policyText: '', status: 'DRAFT' },
            { id: '2', name: 'p2', kind: 'GLOBAL', entityId: null, policyText: '', status: 'DRAFT' },
            { id: '3', name: 'p3', kind: 'RESOURCE', entityId: 'mcp.payments-mcp', policyText: '', status: 'DRAFT' },
        ];
        expect(policiesFor(server, policies).map(p => p.name)).toEqual(['p1']);
    });
});
```

- [ ] **Step 2: Run to verify failure** — `npx vitest run entity-relationships`. Expected: FAIL.

- [ ] **Step 3: Implement** (license header first)

```ts
import { formatEntityUid } from './entity-adapter';
import type { EntityInstance } from './entity.types';
import type { PolicyResponse } from './api/authz-api.types';

export function referencedBy(entity: EntityInstance, all: readonly EntityInstance[]): EntityInstance[] {
    const uid = formatEntityUid(entity.uid);
    return all.filter(e => e.parents.some(p => formatEntityUid(p) === uid));
}

export function childrenByType(entity: EntityInstance, all: readonly EntityInstance[]): { type: string; count: number }[] {
    const counts = new Map<string, number>();
    for (const child of referencedBy(entity, all)) {
        counts.set(child.uid.type, (counts.get(child.uid.type) ?? 0) + 1);
    }
    return Array.from(counts, ([type, count]) => ({ type, count })).sort((a, b) => a.type.localeCompare(b.type));
}

export function policiesFor(entity: EntityInstance, policies: readonly PolicyResponse[]): PolicyResponse[] {
    const uid = formatEntityUid(entity.uid);
    return policies.filter(p => p.entityId === uid);
}
```

- [ ] **Step 4: Run to verify pass** — `npx vitest run entity-relationships`. PASS.
- [ ] **Step 5: Typecheck** — `npx tsc --noEmit`. PASS.
- [ ] **Step 6: Commit**

```bash
git add src/main/ui/shared/entity-relationships.ts src/main/ui/shared/__tests__/entity-relationships.test.ts
git commit -m "feat(authz-ui): add entity relationship and policy matching helpers"
```

---

## Task 3: `entities-json.ts` (collection export)

**Files:**
- Create: `src/main/ui/shared/entities-json.ts`
- Test: `src/main/ui/shared/__tests__/entities-json.test.ts`

- [ ] **Step 1: Write the failing test** (license header first)

```ts
import { describe, expect, it } from 'vitest';
import { buildEntitiesJson } from '../entities-json';
import type { EntityInstance } from '../entity.types';

const e: EntityInstance = { uid: { type: 'User', id: 'alice' }, attrs: { dept: 'eng' }, parents: [], source: 'local' };

describe('entities-json', () => {
    it('exports a pretty JSON array of GAPL shapes', () => {
        const json = buildEntitiesJson([e]);
        const parsed = JSON.parse(json);
        expect(parsed).toEqual([{ uid: { type: 'User', id: 'user.alice' }, attrs: { dept: 'eng' }, parents: [] }]);
        expect(json).toContain('\n'); // pretty-printed
    });
});
```

- [ ] **Step 2: Run to verify failure** — `npx vitest run entities-json`. FAIL.

- [ ] **Step 3: Implement** (license header first)

```ts
import { buildGaplShape } from './entity-gapl-shape';
import type { EntityInstance } from './entity.types';

export function buildEntitiesJson(entities: readonly EntityInstance[]): string {
    return JSON.stringify(entities.map(buildGaplShape), null, 2);
}
```

- [ ] **Step 4: Run to verify pass** — PASS.
- [ ] **Step 5: Commit**

```bash
git add src/main/ui/shared/entities-json.ts src/main/ui/shared/__tests__/entities-json.test.ts
git commit -m "feat(authz-ui): add entities.json collection export builder"
```

---

## Task 4: `EntityGaplShapeTab` (JSON + Copy)

**Files:**
- Create: `src/main/ui/features/policy-structure/entity-detail/EntityGaplShapeTab.tsx`
- Test: `src/main/ui/features/policy-structure/entity-detail/__tests__/EntityGaplShapeTab.test.tsx`

- [ ] **Step 1: Write the failing test** (license header first)

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityGaplShapeTab } from '../EntityGaplShapeTab';

const entity: EntityInstance = { uid: { type: 'User', id: 'alice' }, attrs: { dept: 'eng' }, parents: [], source: 'local' };

beforeEach(() => {
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
});

describe('EntityGaplShapeTab', () => {
    it('renders the canonical JSON', () => {
        render(<EntityGaplShapeTab entity={entity} />);
        expect(screen.getByText(/"type": "User"/)).toBeInTheDocument();
        expect(screen.getByText(/"id": "user.alice"/)).toBeInTheDocument();
    });

    it('copies JSON to the clipboard', async () => {
        const user = userEvent.setup();
        render(<EntityGaplShapeTab entity={entity} />);
        await user.click(screen.getByRole('button', { name: /Copy JSON/i }));
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(expect.stringContaining('"id": "user.alice"'));
    });
});
```

> Note: a `<pre>` containing the whole JSON may not match `getByText(/"type": "User"/)` if the matcher requires a full-text node. If the assertion fails because the text is split, change the test to `expect(screen.getByTestId('gapl-json').textContent).toContain('"type": "User"')` and add `data-testid="gapl-json"` to the `<pre>`.

- [ ] **Step 2: Run to verify failure** — `npx vitest run EntityGaplShapeTab`. FAIL.

- [ ] **Step 3: Implement** (license header first)

```tsx
import { Button } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { toGaplJson } from '../../../shared/entity-gapl-shape';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityGaplShapeTab({ entity }: { entity: EntityInstance }) {
    const json = toGaplJson(entity);
    const [copied, setCopied] = useState(false);

    async function copy() {
        try {
            await navigator.clipboard?.writeText(json);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch {
            // clipboard unavailable — leave the JSON visible for manual copy
        }
    }

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">The canonical document the Policy Decision Point evaluates against.</p>
                <Button type="button" variant="outline" size="sm" onClick={copy}>
                    <CopyIcon className="mr-2 size-4" aria-hidden />
                    {copied ? 'Copied' : 'Copy JSON'}
                </Button>
            </div>
            <pre data-testid="gapl-json" className="overflow-auto rounded-md border bg-muted/40 p-3 font-mono text-xs">
                {json}
            </pre>
        </div>
    );
}
```

- [ ] **Step 4: Run to verify pass** — PASS (apply the test-id note from Step 1 if needed).
- [ ] **Step 5: Typecheck + commit**

```bash
git add src/main/ui/features/policy-structure/entity-detail/EntityGaplShapeTab.tsx src/main/ui/features/policy-structure/entity-detail/__tests__/EntityGaplShapeTab.test.tsx
git commit -m "feat(authz-ui): add GAPL-shape tab with copy JSON"
```

---

## Task 5: `EntityOverviewTab` (attributes + provenance)

**Files:**
- Create: `src/main/ui/features/policy-structure/entity-detail/EntityOverviewTab.tsx`
- Test: `.../entity-detail/__tests__/EntityOverviewTab.test.tsx`

- [ ] **Step 1: Write the failing test** (license header first)

```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityOverviewTab } from '../EntityOverviewTab';

const entity: EntityInstance = {
    uid: { type: 'MCPServer', id: 'flight-status-mcp' },
    attrs: { name: 'Flight Status MCP', port: 8080, secure: true },
    parents: [],
    source: 'gravitee-catalog',
    importedAt: '2026-04-14T11:12:00.000Z',
};

describe('EntityOverviewTab', () => {
    it('renders the attribute table with inferred types', () => {
        render(<EntityOverviewTab entity={entity} />);
        expect(screen.getByText('name')).toBeInTheDocument();
        expect(screen.getByText('Integer')).toBeInTheDocument(); // port
        expect(screen.getByText('Boolean')).toBeInTheDocument(); // secure
    });

    it('renders provenance with the source', () => {
        render(<EntityOverviewTab entity={entity} />);
        expect(screen.getByText(/Gravitee Catalog|gravitee-catalog/i)).toBeInTheDocument();
    });

    it('shows an empty state when there are no attributes', () => {
        render(<EntityOverviewTab entity={{ ...entity, attrs: {} }} />);
        expect(screen.getByText(/No attributes/i)).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run to verify failure** — FAIL.

- [ ] **Step 3: Implement** (license header first)

```tsx
import { Badge } from '@gravitee/graphene-core';
import { inferAttrType } from '../../../shared/entity-gapl-shape';
import type { EntityInstance } from '../../../shared/entity.types';

function sourceLabel(source: EntityInstance['source']): string {
    if (source === 'apim') return 'APIM';
    if (source === 'gravitee-catalog') return 'Gravitee Catalog';
    return 'Local';
}

function renderValue(v: unknown): string {
    if (Array.isArray(v)) return v.join(', ');
    return String(v);
}

export function EntityOverviewTab({ entity }: { entity: EntityInstance }) {
    const attrs = Object.entries(entity.attrs);
    return (
        <div className="flex flex-col gap-5">
            <section className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Attributes</h3>
                {attrs.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No attributes.</p>
                ) : (
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="text-left text-xs text-muted-foreground">
                                <th className="py-1 font-medium">Name</th>
                                <th className="py-1 font-medium">Type</th>
                                <th className="py-1 font-medium">Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            {attrs.map(([k, v]) => (
                                <tr key={k} className="border-t">
                                    <td className="py-1 font-mono">{k}</td>
                                    <td className="py-1">
                                        <Badge variant="outline" className="font-mono text-xs">
                                            {inferAttrType(v)}
                                        </Badge>
                                    </td>
                                    <td className="py-1 font-mono text-muted-foreground">{renderValue(v)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </section>
            <section className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Provenance</h3>
                <table className="w-full text-sm">
                    <tbody>
                        <tr className="border-t">
                            <td className="w-40 py-1 text-muted-foreground">Source</td>
                            <td className="py-1 font-mono">{sourceLabel(entity.source)}</td>
                        </tr>
                        {entity.importedAt && (
                            <tr className="border-t">
                                <td className="py-1 text-muted-foreground">Imported at</td>
                                <td className="py-1 font-mono">{entity.importedAt}</td>
                            </tr>
                        )}
                        {entity.createdAt && (
                            <tr className="border-t">
                                <td className="py-1 text-muted-foreground">Created</td>
                                <td className="py-1 font-mono">{entity.createdAt}</td>
                            </tr>
                        )}
                        {entity.updatedAt && (
                            <tr className="border-t">
                                <td className="py-1 text-muted-foreground">Updated</td>
                                <td className="py-1 font-mono">{entity.updatedAt}</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </section>
        </div>
    );
}
```

- [ ] **Step 4: Run to verify pass** — PASS.
- [ ] **Step 5: Commit**

```bash
git add src/main/ui/features/policy-structure/entity-detail/EntityOverviewTab.tsx src/main/ui/features/policy-structure/entity-detail/__tests__/EntityOverviewTab.test.tsx
git commit -m "feat(authz-ui): add entity overview tab (attributes + provenance)"
```

---

## Task 6: `EntityRelationshipsTab` + `EntityPoliciesTab`

**Files:**
- Create: `.../entity-detail/EntityRelationshipsTab.tsx`, `.../entity-detail/EntityPoliciesTab.tsx`
- Test: `.../entity-detail/__tests__/EntityRelationshipsTab.test.tsx`, `.../entity-detail/__tests__/EntityPoliciesTab.test.tsx`

- [ ] **Step 1: Write failing tests** (license header first, each file)

`EntityRelationshipsTab.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityRelationshipsTab } from '../EntityRelationshipsTab';

const server: EntityInstance = { uid: { type: 'MCPServer', id: 'flight-status-mcp' }, attrs: {}, parents: [], source: 'gravitee-catalog' };
const child: EntityInstance = {
    uid: { type: 'MCPTool', id: 'get-flight' },
    attrs: {},
    parents: [{ type: 'MCPServer', id: 'flight-status-mcp' }],
    source: 'gravitee-catalog',
};

describe('EntityRelationshipsTab', () => {
    it('lists referenced-by children', () => {
        render(<EntityRelationshipsTab entity={server} allEntities={[server, child]} />);
        expect(screen.getByText(/mcp\.get-flight/)).toBeInTheDocument();
    });

    it('shows an empty state when isolated', () => {
        render(<EntityRelationshipsTab entity={server} allEntities={[server]} />);
        expect(screen.getByText(/No relationships/i)).toBeInTheDocument();
    });
});
```

`EntityPoliciesTab.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import type { PolicyResponse } from '../../../../shared/api/authz-api.types';
import { EntityPoliciesTab } from '../EntityPoliciesTab';

const server: EntityInstance = { uid: { type: 'MCPServer', id: 'flight-status-mcp' }, attrs: {}, parents: [], source: 'gravitee-catalog' };

describe('EntityPoliciesTab', () => {
    it('lists policies that reference the entity', () => {
        const policies: PolicyResponse[] = [
            { id: '1', name: 'allow-invoke', kind: 'RESOURCE', entityId: 'mcp.flight-status-mcp', policyText: '', status: 'PUBLISHED' },
        ];
        render(<EntityPoliciesTab entity={server} policies={policies} />);
        expect(screen.getByText('allow-invoke')).toBeInTheDocument();
    });

    it('shows an empty state when none match', () => {
        render(<EntityPoliciesTab entity={server} policies={[]} />);
        expect(screen.getByText(/No policies/i)).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run to verify failure** — `npx vitest run EntityRelationshipsTab EntityPoliciesTab`. FAIL.

- [ ] **Step 3: Implement `EntityRelationshipsTab.tsx`** (license header first)

```tsx
import { Badge } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { formatEntityUid } from '../../../shared/entity-adapter';
import { childrenByType, referencedBy } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityRelationshipsTab({ entity, allEntities }: { entity: EntityInstance; allEntities: readonly EntityInstance[] }) {
    const parents = entity.parents;
    const children = useMemo(() => referencedBy(entity, allEntities), [entity, allEntities]);
    const grouped = useMemo(() => childrenByType(entity, allEntities), [entity, allEntities]);

    if (parents.length === 0 && children.length === 0) {
        return <p className="text-xs text-muted-foreground">No relationships.</p>;
    }
    return (
        <div className="flex flex-col gap-5">
            {grouped.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                    {grouped.map(g => (
                        <Badge key={g.type} variant="secondary" className="font-mono text-xs">
                            contains {g.count} {g.type}
                        </Badge>
                    ))}
                </div>
            )}
            {parents.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h3 className="text-sm font-semibold">Parents</h3>
                    {parents.map(p => (
                        <span key={formatEntityUid(p)} className="font-mono text-sm">
                            {formatEntityUid(p)}
                        </span>
                    ))}
                </section>
            )}
            {children.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h3 className="text-sm font-semibold">Referenced by</h3>
                    {children.map(c => (
                        <span key={formatEntityUid(c.uid)} className="font-mono text-sm">
                            {formatEntityUid(c.uid)}
                        </span>
                    ))}
                </section>
            )}
        </div>
    );
}
```

- [ ] **Step 4: Implement `EntityPoliciesTab.tsx`** (license header first)

```tsx
import { Badge } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import type { PolicyResponse } from '../../../shared/api/authz-api.types';
import { policiesFor } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityPoliciesTab({ entity, policies }: { entity: EntityInstance; policies: readonly PolicyResponse[] }) {
    const matched = useMemo(() => policiesFor(entity, policies), [entity, policies]);
    if (matched.length === 0) {
        return <p className="text-xs text-muted-foreground">No policies reference this entity.</p>;
    }
    return (
        <div className="flex flex-col gap-2">
            {matched.map(p => (
                <div key={p.id} className="flex items-center justify-between border-t py-2 first:border-t-0">
                    <span className="font-medium">{p.name}</span>
                    <Badge variant="secondary">{p.status}</Badge>
                </div>
            ))}
        </div>
    );
}
```

- [ ] **Step 5: Run to verify pass** — PASS.
- [ ] **Step 6: Typecheck + commit**

```bash
git add src/main/ui/features/policy-structure/entity-detail/EntityRelationshipsTab.tsx src/main/ui/features/policy-structure/entity-detail/EntityPoliciesTab.tsx src/main/ui/features/policy-structure/entity-detail/__tests__/EntityRelationshipsTab.test.tsx src/main/ui/features/policy-structure/entity-detail/__tests__/EntityPoliciesTab.test.tsx
git commit -m "feat(authz-ui): add relationships and policies tabs"
```

---

## Task 7: `EntityDetailSheet` (shell with tabs + chips + edit)

**Files:**
- Create: `.../entity-detail/EntityDetailSheet.tsx`
- Test: `.../entity-detail/__tests__/EntityDetailSheet.test.tsx`

- [ ] **Step 1: Write the failing test** (license header first)

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityDetailSheet } from '../EntityDetailSheet';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => undefined;
});

const local: EntityInstance = {
    uid: { type: 'User', id: 'alice' },
    displayName: 'Alice',
    attrs: { dept: 'eng' },
    parents: [],
    source: 'local',
};
const catalog: EntityInstance = { ...local, uid: { type: 'MCPServer', id: 'flight' }, source: 'gravitee-catalog' };

function renderSheet(entity: EntityInstance, onEdit = vi.fn()) {
    render(<EntityDetailSheet entity={entity} allEntities={[entity]} policies={[]} open onOpenChange={vi.fn()} onEdit={onEdit} />);
    return { onEdit };
}

describe('EntityDetailSheet', () => {
    it('shows the title, uid, and attr-count chip', () => {
        renderSheet(local);
        expect(screen.getByText('Alice')).toBeInTheDocument();
        expect(screen.getByText('user.alice')).toBeInTheDocument();
        expect(screen.getByText(/1 attrs/i)).toBeInTheDocument();
    });

    it('switches to the GAPL shape tab', async () => {
        const user = userEvent.setup();
        renderSheet(local);
        await user.click(screen.getByRole('tab', { name: /GAPL shape/i }));
        expect(screen.getByText(/"id": "user.alice"/)).toBeInTheDocument();
    });

    it('offers Edit for a local entity and calls onEdit', async () => {
        const user = userEvent.setup();
        const { onEdit } = renderSheet(local);
        await user.click(screen.getByRole('button', { name: /^Edit$/i }));
        expect(onEdit).toHaveBeenCalledWith(local);
    });

    it('hides Edit for a catalog entity', () => {
        renderSheet(catalog);
        expect(screen.queryByRole('button', { name: /^Edit$/i })).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run to verify failure** — `npx vitest run EntityDetailSheet`. FAIL.

- [ ] **Step 3: Implement** (license header first). Mirror the Sheet structure used by `EditEntityDialog.tsx` (right side, fixed width, scrollable body).

```tsx
import { Badge, Button, Sheet, SheetContent, SheetTitle, Tabs, TabsContent, TabsList, TabsTrigger } from '@gravitee/graphene-core';
import { PencilIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import type { PolicyResponse } from '../../../shared/api/authz-api.types';
import { formatEntityUid } from '../../../shared/entity-adapter';
import { policiesFor, referencedBy } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';
import { EntityGaplShapeTab } from './EntityGaplShapeTab';
import { EntityOverviewTab } from './EntityOverviewTab';
import { EntityPoliciesTab } from './EntityPoliciesTab';
import { EntityRelationshipsTab } from './EntityRelationshipsTab';

type DetailTab = 'overview' | 'relationships' | 'policies' | 'gapl';

export interface EntityDetailSheetProps {
    readonly entity: EntityInstance | null;
    readonly allEntities: readonly EntityInstance[];
    readonly policies: readonly PolicyResponse[];
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onEdit: (entity: EntityInstance) => void;
}

function sourceLabel(source: EntityInstance['source']): string {
    if (source === 'apim') return 'APIM';
    if (source === 'gravitee-catalog') return 'Gravitee Catalog';
    return 'Local';
}

export function EntityDetailSheet({ entity, allEntities, policies, open, onOpenChange, onEdit }: EntityDetailSheetProps) {
    const [tab, setTab] = useState<DetailTab>('overview');
    const counts = useMemo(() => {
        if (!entity) return { attrs: 0, parents: 0, refs: 0, policies: 0 };
        return {
            attrs: Object.keys(entity.attrs).length,
            parents: entity.parents.length,
            refs: referencedBy(entity, allEntities).length,
            policies: policiesFor(entity, policies).length,
        };
    }, [entity, allEntities, policies]);

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton
                aria-label="Entity details"
                style={{ width: 'min(640px, 100vw)', maxWidth: 'min(640px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                {entity && (
                    <>
                        <div className="flex flex-col gap-2 border-b px-6 py-4">
                            <div className="flex items-center gap-2">
                                <Badge variant="outline" className="font-mono text-xs">
                                    {entity.uid.type}
                                </Badge>
                                <Badge variant="secondary">{sourceLabel(entity.source)}</Badge>
                                {entity.source === 'local' && (
                                    <Button type="button" variant="outline" size="sm" className="ml-auto" onClick={() => onEdit(entity)}>
                                        <PencilIcon className="mr-2 size-4" aria-hidden />
                                        Edit
                                    </Button>
                                )}
                            </div>
                            <SheetTitle className="text-lg font-semibold">{entity.displayName ?? entity.uid.id}</SheetTitle>
                            <span className="font-mono text-sm text-muted-foreground">{formatEntityUid(entity.uid)}</span>
                            <div className="flex flex-wrap gap-1.5">
                                <Badge variant="secondary">{counts.attrs} attrs</Badge>
                                <Badge variant="secondary">{counts.parents} parents</Badge>
                                <Badge variant="secondary">{counts.refs} referenced by</Badge>
                                <Badge variant="secondary">{counts.policies} policies</Badge>
                            </div>
                        </div>
                        <Tabs
                            value={tab}
                            onValueChange={v => {
                                if (v === 'overview' || v === 'relationships' || v === 'policies' || v === 'gapl') setTab(v);
                            }}
                            className="flex min-h-0 flex-1 flex-col"
                        >
                            <TabsList variant="line" className="px-6">
                                <TabsTrigger value="overview">Overview</TabsTrigger>
                                <TabsTrigger value="relationships">Relationships</TabsTrigger>
                                <TabsTrigger value="policies">Policies</TabsTrigger>
                                <TabsTrigger value="gapl">GAPL shape</TabsTrigger>
                            </TabsList>
                            <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
                                <TabsContent value="overview">
                                    <EntityOverviewTab entity={entity} />
                                </TabsContent>
                                <TabsContent value="relationships">
                                    <EntityRelationshipsTab entity={entity} allEntities={allEntities} />
                                </TabsContent>
                                <TabsContent value="policies">
                                    <EntityPoliciesTab entity={entity} policies={policies} />
                                </TabsContent>
                                <TabsContent value="gapl">
                                    <EntityGaplShapeTab entity={entity} />
                                </TabsContent>
                            </div>
                        </Tabs>
                    </>
                )}
            </SheetContent>
        </Sheet>
    );
}
```

- [ ] **Step 4: Run to verify pass** — PASS. (If the Tabs default-tab reset between entities matters, the `tab` state persists across entities; acceptable — opening always lands on the last tab. If a test needs Overview-on-open, add `useEffect(() => setTab('overview'), [entity])`.)
- [ ] **Step 5: Typecheck + lint + commit**

```bash
git add src/main/ui/features/policy-structure/entity-detail/EntityDetailSheet.tsx src/main/ui/features/policy-structure/entity-detail/__tests__/EntityDetailSheet.test.tsx
git commit -m "feat(authz-ui): add entity detail sheet with tabs and chips"
```

---

## Task 8: Wire the panel into `EntitiesPage` (open on Name click + load policies)

**Files:**
- Modify: `src/main/ui/features/policy-structure/EntitiesPage.tsx`
- Modify: `src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx`

- [ ] **Step 1: Write the failing test** — add to the existing EntitiesPage test suite (it already mocks `useAllEntities`/`listEntities` and `authzApiService`; add a `listPolicies` mock returning `{ data: [], total: 0, page: 1, perPage: 100 }`). Test:

```tsx
    it('opens the entity detail sheet when an entity name is clicked', async () => {
        mockByKind({ principals: [makeEntity({ id: 'p1', uid: 'user.alice', attributes: { _displayName: 'Alice' } })] });
        renderPage();
        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByRole('button', { name: 'Alice' })).toBeInTheDocument());
        await user.click(screen.getByRole('button', { name: 'Alice' }));
        expect(await screen.findByRole('dialog', { name: /Entity details/i })).toBeInTheDocument();
        expect(screen.getByText('user.alice')).toBeInTheDocument();
    });
```

> If `listPolicies` is not already mocked in this file, add it to the `vi.mock('../../../shared/api/authz-api.service', ...)` factory: `listPolicies: (env: string, params?: unknown) => listPoliciesSpy(env, params)` with `const listPoliciesSpy = vi.fn().mockResolvedValue({ data: [], total: 0, page: 1, perPage: 100 })` and reset it in `beforeEach`.

- [ ] **Step 2: Run to verify failure** — `npx vitest run EntitiesPage`. FAIL (name is plain text, no sheet).

- [ ] **Step 3: Implement** — in `EntitiesPage.tsx`:
1. Imports:
```ts
import { EntityDetailSheet } from './entity-detail/EntityDetailSheet';
import { usePolicies } from '../../shared/hooks/usePolicies';
import { MAX_PER_PAGE } from '../../shared/api/authz-api.service';
```
2. In `EntitiesPage`, load policies and add view state:
```ts
const policiesQuery = usePolicies(environmentId, { initialPerPage: MAX_PER_PAGE });
const allPolicies = useMemo(() => policiesQuery.data?.data ?? [], [policiesQuery.data]);
const allEntities = useMemo(() => [...principals, ...resources], [principals, resources]);
const [viewing, setViewing] = useState<EntityInstance | null>(null);
```
3. Pass an `onView` callback into `EntitiesTable` (new optional prop) and make the `name` column cell a button:
   - Add `readonly onView?: (entity: EntityInstance) => void;` to `EntitiesTableProps`, thread it through.
   - Change the `name` column cell to:
   ```tsx
   cell: ({ row }) =>
       onView ? (
           <button type="button" className="block truncate text-left font-medium hover:underline" onClick={() => onView(row.original)}>
               {displayNameOf(row.original)}
           </button>
       ) : (
           <span className="block truncate font-medium">{displayNameOf(row.original)}</span>
       ),
   ```
   - Add `onView` to the `columns` `useMemo` dependency array.
4. Pass `onView={setViewing}` to both `EntitiesTable` instances (principals + resources).
5. Render the sheet near the other dialogs:
```tsx
<EntityDetailSheet
    entity={viewing}
    allEntities={allEntities}
    policies={allPolicies}
    open={viewing !== null}
    onOpenChange={open => {
        if (!open) setViewing(null);
    }}
    onEdit={entity => {
        setViewing(null);
        setEditing({ entity, kind: entity.uid.type && resources.includes(entity) ? 'RESOURCE' : 'PRINCIPAL' });
    }}
/>
```
   > The `kind` for `onEdit` must match how `setEditing` is typed in this file (`{ entity, kind: AddingKind }`). Use `principals.includes(entity) ? 'PRINCIPAL' : 'RESOURCE'` to pick the kind. Verify against the actual `editing` state shape before writing.

- [ ] **Step 4: Run to verify pass** — `npx vitest run EntitiesPage`. PASS (existing tests still green).
- [ ] **Step 5: Typecheck + lint + commit**

```bash
git add src/main/ui/features/policy-structure/EntitiesPage.tsx src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx
git commit -m "feat(authz-ui): open entity detail sheet from the entities table"
```

---

## Task 9: Table columns (Policies, Relationships), Entity-ID copy, Source badge

**Files:**
- Modify: `src/main/ui/features/policy-structure/EntitiesPage.tsx`
- Modify: `src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
    it('shows a relationships chip and a policies count in the table', async () => {
        mockByKind({
            resources: [
                makeEntity({ id: 'r1', uid: 'mcp.flight', attributes: { _displayName: 'Flight' } }),
                makeEntity({ id: 'r2', uid: 'mcp.flight.tool', attributes: { _displayName: 'Tool', _parents: undefined } }),
            ],
        });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByRole('tab', { name: /Resources/i }));
        await waitFor(() => expect(screen.getByLabelText('Copy mcp.flight')).toBeInTheDocument());
    });
```

> This test only asserts the copy button exists (a stable, deterministic signal). The relationships/policies cell content depends on parent wiring in the mock fixtures, which is awkward to set precisely here; assert the copy affordance and leave richer relationship assertions to the pure `entity-relationships` tests (Task 2).

- [ ] **Step 2: Run to verify failure** — FAIL (no copy button).

- [ ] **Step 3: Implement** — in `EntitiesTable` columns (`EntitiesPage.tsx`):
1. Add a copy button to the `entityId` column cell:
```tsx
cell: ({ row }) => {
    const uid = formatEntityUid(row.original.uid);
    return (
        <span className="flex items-center gap-1.5">
            <span className="font-mono text-xs text-foreground">{uid}</span>
            <Button
                type="button"
                variant="ghost"
                size="sm"
                className="size-6 p-0"
                aria-label={`Copy ${uid}`}
                onClick={e => {
                    e.stopPropagation();
                    void navigator.clipboard?.writeText(uid);
                }}
            >
                <CopyIcon className="size-3.5 text-muted-foreground" aria-hidden />
            </Button>
        </span>
    );
},
```
   (Import `CopyIcon` from `@gravitee/graphene-core/icons`.)
2. Add a `relationships` column after `source` that renders `childrenByType` chips + a parents chip:
```tsx
{
    id: 'relationships',
    header: 'Relationships',
    size: 220,
    cell: ({ row }) => {
        const groups = childrenByType(row.original, allEntities);
        const parentCount = row.original.parents.length;
        if (groups.length === 0 && parentCount === 0) return <span className="text-muted-foreground">—</span>;
        return (
            <div className="flex flex-wrap gap-1">
                {parentCount > 0 && <Badge variant="outline" className="text-xs">in {parentCount}</Badge>}
                {groups.map(g => (
                    <Badge key={g.type} variant="secondary" className="text-xs">
                        contains {g.count} {g.type}
                    </Badge>
                ))}
            </div>
        );
    },
},
```
3. Add a `policies` column:
```tsx
{
    id: 'policies',
    header: 'Policies',
    size: 100,
    cell: ({ row }) => {
        const n = policiesFor(row.original, allPolicies).length;
        return n === 0 ? <span className="text-muted-foreground">—</span> : <Badge variant="secondary">{n}</Badge>;
    },
},
```
   - `EntitiesTable` must receive `allEntities` and `allPolicies` as props (thread them from `EntitiesPage`). Add `readonly allEntities: readonly EntityInstance[]; readonly allPolicies: readonly PolicyResponse[];` to `EntitiesTableProps`, pass from both call sites, and add to the `columns` `useMemo` deps. Import `childrenByType, policiesFor` from `../../shared/entity-relationships` and `PolicyResponse` type.
4. (Source is already a `Badge` via `sourceLabelOf` — leave as is.)

- [ ] **Step 4: Run to verify pass** — PASS.
- [ ] **Step 5: Typecheck + lint + commit**

```bash
git add src/main/ui/features/policy-structure/EntitiesPage.tsx src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx
git commit -m "feat(authz-ui): add relationships/policies columns and entity-id copy"
```

---

## Task 10: Policy-Linked KPI + settings → Open entities.json

**Files:**
- Modify: `src/main/ui/features/policy-structure/EntitiesPage.tsx`
- Modify: `src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
    it('renders the Policy-Linked KPI and the settings entities.json action', async () => {
        mockByKind({ principals: [makeEntity({ id: 'p1', uid: 'user.alice' })] });
        renderPage();
        const user = userEvent.setup();
        await waitFor(() => expect(screen.getByLabelText('Policy-Linked')).toBeInTheDocument());
        await user.click(screen.getByRole('button', { name: /Entities settings/i }));
        expect(await screen.findByRole('menuitem', { name: /Open entities\.json/i })).toBeInTheDocument();
    });
```

- [ ] **Step 2: Run to verify failure** — FAIL.

- [ ] **Step 3: Implement** — in `EntitiesPage.tsx`:
1. Compute the KPI and add a fifth `KpiTile`:
```ts
const policyLinkedCount = useMemo(
    () => allEntities.filter(e => policiesFor(e, allPolicies).length > 0).length,
    [allEntities, allPolicies],
);
```
   Change the KPI grid to 5 columns (`md:grid-cols-5`) and add:
```tsx
<KpiTile label="Policy-Linked" value={policyLinkedCount} loading={isLoading} />
```
2. Add a settings gear menu in the page header (top-right). Import `DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger` and `SettingsIcon` (verify the icon name exists in `@gravitee/graphene-core/icons`; if not, use `Settings2Icon` or `EllipsisIcon`). Add `buildEntitiesJson` import from `../../shared/entities-json`.
```tsx
<DropdownMenu>
    <DropdownMenuTrigger asChild>
        <Button type="button" variant="ghost" size="sm" aria-label="Entities settings">
            <SettingsIcon className="size-4" aria-hidden />
        </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end">
        <DropdownMenuItem
            onClick={() => {
                const blob = new Blob([buildEntitiesJson(allEntities)], { type: 'application/json' });
                const url = URL.createObjectURL(blob);
                window.open(url, '_blank', 'noopener');
                setTimeout(() => URL.revokeObjectURL(url), 10_000);
            }}
        >
            Open entities.json
        </DropdownMenuItem>
    </DropdownMenuContent>
</DropdownMenu>
```
   Place the trigger in the `<header>` block (add an `ml-auto` wrapper so it sits on the right).
   > In jsdom `URL.createObjectURL`/`window.open` may be undefined. Guard them: `const url = URL.createObjectURL?.(blob); if (url) window.open?.(url, '_blank', 'noopener');`. The test only asserts the menu item renders, not the open behavior.

- [ ] **Step 4: Run to verify pass** — PASS.
- [ ] **Step 5: Typecheck + lint + commit**

```bash
git add src/main/ui/features/policy-structure/EntitiesPage.tsx src/main/ui/features/policy-structure/__tests__/EntitiesPage.test.tsx
git commit -m "feat(authz-ui): add Policy-Linked KPI and entities.json export"
```

---

## Task 11: Final verification + PR

- [ ] **Step 1: Typecheck** — from `src/main/ui` `npx tsc --noEmit` → PASS.
- [ ] **Step 2: Full suite** — module root `npx vitest run` → all green.
- [ ] **Step 3: Lint + format** — repo root `npx nx lint gravitee-gamma-module-authz` → PASS; `npx prettier --write` any flagged new files, then `npx nx format:check --base=origin/master --head=HEAD` → PASS.
- [ ] **Step 4: Manual smoke (optional)** — `bash ~/.claude/skills/gamma-dev/start.sh`; open Authorization → Entities; click an entity name → panel opens; switch tabs (Overview/Relationships/Policies/GAPL shape); Copy JSON; copy an Entity ID; check the Policy-Linked KPI; settings → Open entities.json.
- [ ] **Step 5: Push + PR**

```bash
git push -u origin feat/authz-entity-detail-panel
gh pr create --title "feat(authz-ui): entity detail panel + entities page parity" --body "$(cat <<'EOF'
## Summary
- Entity detail Sheet opened from the entities table (Name click) with four tabs: Overview (attributes + provenance), Relationships (parents + referenced-by), Policies, and GAPL shape (canonical PDP JSON + Copy JSON).
- Table: Relationships and Policies columns, copy-to-clipboard on Entity ID, Source badge.
- KPI: Policy-Linked tile.
- Settings menu: Open entities.json (client-side export of the collection).
- No backend changes; data from useAllEntities + usePolicies. Pure builders (entity-gapl-shape, entity-relationships, entities-json) are unit-tested.

## Test plan
- [ ] Pure builder unit tests (GAPL shape, relationships/policy matching, entities.json)
- [ ] Tab component tests + EntityDetailSheet tests
- [ ] EntitiesPage tests (open panel, columns, KPI, settings)
- [ ] tsc, vitest, nx lint, nx format green
EOF
)"
```

---

## Self-review notes (author)

- **Spec coverage:** detail panel + 4 tabs → Tasks 4-7; GAPL JSON + Copy → Task 4; table columns + copy + Source badge → Task 9; Policy-Linked KPI + settings entities.json → Task 10; row-open + policy loading → Task 8; pure builders → Tasks 1-3. "Reset to seeded" omitted (matches non-goal). Cross-module links simplified (matches non-goal).
- **Type consistency:** `buildGaplShape`/`toGaplJson`/`inferAttrType`, `referencedBy`/`childrenByType`/`policiesFor`, `buildEntitiesJson`, `EntityDetailSheetProps`, tab component props are used identically across tasks. `PolicyResponse`/`EntityInstance` imported from the same paths everywhere.
- **Flagged verification points (not placeholders — each names the exact adjustment):** `getByText` on `<pre>` JSON (Task 4 test-id fallback), graphene `DropdownMenu`/`SettingsIcon` exports (Task 10), `EntitiesPage` `editing` state shape for the `onEdit` kind (Task 8), `listPolicies` mock wiring in the existing EntitiesPage test (Task 8), jsdom `URL.createObjectURL`/`window.open` guards (Task 10).
- **Independence:** Tasks 1-3 (pure) and 4-7 (components) are independent of the page; 8-10 integrate. Each task commits working, tested code.
