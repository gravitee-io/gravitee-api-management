# Entity Attribute Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a typed key/value attribute editor to the Create/Edit entity dialogs in the Gamma authz UI so operators can author entity attributes that GAPL ABAC policies evaluate — emitting only representations the PDP understands.

**Architecture:** A pure `attribute-codec.ts` module owns all type knowledge (validation + coercion to the PDP-safe JSON form). A presentational `AttributeEditor.tsx` renders `key · type · value · remove` rows and validates via the codec. Both Create and Edit dialogs hold `AttributeRow[]` state and merge coerced rows into the attribute map on submit. The `entity-adapter` is extended to round-trip arrays (Sets) and previously-dropped meta keys.

**Tech Stack:** React 19, TypeScript, `@gravitee/graphene-core`, TanStack Query, Vitest, Testing Library. Module root: `gravitee-gamma/gravitee-gamma-module-authz`. UI under `src/main/ui`. Run commands from the module root unless noted.

**Reference spec:** `docs/superpowers/specs/2026-05-30-entity-attribute-editor-design.md` — especially the PDP serialization table.

**Key invariants (from the spec):**

- Never emit a JSON floating-point number — PDP `ValueConverter.toValue` has no Double branch and silently drops it. `decimal` serializes as a **string**.
- A JSON array always becomes a **Set** in the engine (dedup). Label it `Set<String>`, warn on duplicates.
- `decimal/timestamp/duration/ip/cidr/enum` are stored as **strings**; they only get typed via a GAPL function (`decimal()/datetime()/duration()/ip()`) inside a condition.

**Conventions in this codebase (follow them):**

- Test runner: from module root `npx vitest run <pattern>`. Typecheck: from `src/main/ui` `npx tsc --noEmit`. Lint: from repo root `npx nx lint gravitee-gamma-module-authz`. Format: `npx prettier --check <files>` (printWidth 140, tabWidth 4, singleQuote, semi, trailingComma all, arrowParens avoid).
- Every source file starts with the Apache license header (copy it from any sibling file, e.g. `entity.types.ts` lines 1-15). The lint target `lint-license` enforces it.
- graphene-core: named imports only, semantic tokens, `cn()` to merge classes, no `as` casts in new code.
- Commit style: conventional commits, English, no `Co-Authored-By` trailer.

---

## File structure

- Create `src/main/ui/shared/attribute-codec.ts` — pure type/validation/coercion logic. No React.
- Create `src/main/ui/shared/__tests__/attribute-codec.test.ts` — exhaustive codec tests.
- Create `src/main/ui/features/policy-structure/AttributeEditor.tsx` — presentational rows component.
- Create `src/main/ui/features/policy-structure/__tests__/AttributeEditor.test.tsx`.
- Modify `src/main/ui/shared/entity.types.ts` — extend `AttrValue` with `string[]`, add `reservedMeta`.
- Modify `src/main/ui/shared/entity-adapter.ts` — round-trip arrays + preserve unmapped meta keys.
- Modify `src/main/ui/shared/__tests__/entity-adapter.test.ts` — round-trip regressions (create file if absent).
- Modify `src/main/ui/features/policy-structure/EditEntityDialog.tsx` — embed editor, seed/merge attrs.
- Modify `src/main/ui/features/policy-structure/CreateEntityDialog.tsx` — embed editor, merge attrs.
- Modify the two dialogs' test files to cover the new attribute round-trip.

---

## Task 1: Extend `AttrValue` and `EntityInstance`

**Files:**

- Modify: `src/main/ui/shared/entity.types.ts`

- [ ] **Step 1: Widen `AttrValue` and add `reservedMeta`**

Replace line 16 and the `EntityInstance` interface:

```ts
export type AttrValue = string | number | boolean | string[];
```

Add a field to `EntityInstance` (after `attrs`):

```ts
export interface EntityInstance {
    uid: { type: string; id: string };
    displayName?: string;
    attrs: Record<string, AttrValue>;
    /** Reserved `_`-prefixed meta keys not mapped to structured fields, preserved verbatim for round-trip. */
    reservedMeta?: Record<string, unknown>;
    parents: Array<{ type: string; id: string }>;
    source: EntitySource;
    importedAt?: string;
    _backendId?: string;
    createdAt?: string;
    updatedAt?: string;
}
```

- [ ] **Step 2: Typecheck**

Run (from `src/main/ui`): `npx tsc --noEmit`
Expected: PASS (widening `AttrValue` may surface a couple of call sites that assume scalar — none expected yet; if any appear, they are addressed in later tasks. If `tsc` fails here, note the file and continue; Task 2 touches the adapter that consumes this).

- [ ] **Step 3: Commit**

```bash
git add src/main/ui/shared/entity.types.ts
git commit -m "feat(authz-ui): widen AttrValue to sets and add reservedMeta"
```

---

## Task 2: Round-trip arrays + preserve unmapped meta keys (bundled bug fix)

Today `fromBackend` drops `_url`/`_proxyApiId` and never keeps arrays; `toBackend` re-emits only a fixed set of meta keys, so `_url`/`_proxyApiId` are lost on every edit. Fix both: stash unmapped `_`-keys in `reservedMeta`, keep arrays of strings in `attrs`, and re-emit `reservedMeta` first on save.

**Files:**

- Modify: `src/main/ui/shared/entity-adapter.ts`
- Test: `src/main/ui/shared/__tests__/entity-adapter.test.ts` (create if missing)

- [ ] **Step 1: Write failing tests**

Create/append `src/main/ui/shared/__tests__/entity-adapter.test.ts` (include the license header from `entity.types.ts` lines 1-15):

```ts
import { describe, expect, it } from 'vitest';
import { fromBackend, toBackend } from '../entity-adapter';
import type { EntityResponse } from '../api/authz-api.types';

function res(attributes: Record<string, unknown>): EntityResponse {
    return {
        id: 'e1',
        environmentId: 'DEFAULT',
        uid: 'user.alice',
        attributes,
        parents: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-02T00:00:00Z',
    };
}

describe('entity-adapter round-trip', () => {
    it('preserves unmapped meta keys (_url, _proxyApiId) across fromBackend → toBackend', () => {
        const entity = fromBackend(res({ _displayName: 'Alice', _url: 'https://x', _proxyApiId: 'api-7', dept: 'eng' }));
        const back = toBackend(entity);
        expect(back.attributes._url).toBe('https://x');
        expect(back.attributes._proxyApiId).toBe('api-7');
        expect(back.attributes._displayName).toBe('Alice');
        expect(back.attributes.dept).toBe('eng');
    });

    it('keeps string-array attributes (sets) on the visible attrs and round-trips them', () => {
        const entity = fromBackend(res({ _displayName: 'Alice', regions: ['us', 'eu'] }));
        expect(entity.attrs.regions).toEqual(['us', 'eu']);
        const back = toBackend(entity);
        expect(back.attributes.regions).toEqual(['us', 'eu']);
    });

    it('does not leak reservedMeta into visible attrs', () => {
        const entity = fromBackend(res({ _url: 'https://x', dept: 'eng' }));
        expect('_url' in entity.attrs).toBe(false);
        expect(entity.reservedMeta?._url).toBe('https://x');
    });
});
```

- [ ] **Step 2: Run to verify failure**

Run (module root): `npx vitest run entity-adapter`
Expected: FAIL — `_url`/`_proxyApiId` are `undefined` on `back.attributes`; `regions` is not kept.

- [ ] **Step 3: Update `fromBackend` to stash unmapped meta + keep arrays**

In `entity-adapter.ts`, replace the loop body in `fromBackend` (lines 65-87) with:

```ts
const attrs: Record<string, AttrValue> = {};
const reservedMeta: Record<string, unknown> = {};
let source: EntitySource = 'local';
let displayName: string | undefined;
let importedAt: string | undefined;
let kindOverride: string | undefined;

for (const [k, v] of Object.entries(e.attributes)) {
    if (k === '_source') {
        source = v as string as EntitySource;
    } else if (k === '_displayName') {
        displayName = v as string;
    } else if (k === '_importedAt' || k === '_syncedAt') {
        importedAt = v as string;
    } else if (k === '_kind') {
        kindOverride = kindToUiType(v);
    } else if (k.startsWith('_')) {
        // Unmapped reserved meta (e.g. _url, _proxyApiId) — keep verbatim for round-trip.
        reservedMeta[k] = v;
    } else if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') {
        attrs[k] = v;
    } else if (Array.isArray(v) && v.every(x => typeof x === 'string')) {
        attrs[k] = v as string[];
    }
}
```

Then add `reservedMeta` to the returned object (after `attrs`):

```ts
return {
    uid,
    displayName,
    attrs,
    reservedMeta: Object.keys(reservedMeta).length > 0 ? reservedMeta : undefined,
    parents,
    source,
    importedAt,
    _backendId: e.id,
    createdAt: e.createdAt,
    updatedAt: e.updatedAt,
};
```

- [ ] **Step 4: Update `toBackend` to re-emit reservedMeta first**

In `toBackend` (line 130), change the attribute seed so reserved meta is restored before structured meta and visible attrs:

```ts
const attributes: Record<string, unknown> = { ...(e.reservedMeta ?? {}), ...e.attrs };
```

Leave the rest of `toBackend` (the `_kind`/`_source`/`_displayName`/`_importedAt` assignments) unchanged.

- [ ] **Step 5: Run tests to verify pass**

Run (module root): `npx vitest run entity-adapter`
Expected: PASS (3 new tests green).

- [ ] **Step 6: Typecheck + full module tests**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS
Run (module root): `npx vitest run` → all green (no regressions in existing adapter/dialog tests).

- [ ] **Step 7: Commit**

```bash
git add src/main/ui/shared/entity-adapter.ts src/main/ui/shared/__tests__/entity-adapter.test.ts
git commit -m "fix(authz-ui): preserve unmapped meta keys and set attributes on entity round-trip"
```

---

## Task 3: `attribute-codec.ts` — the typed core

This module is the single source of truth for the PDP serialization contract. It is pure (no React) and fully unit-tested.

**Files:**

- Create: `src/main/ui/shared/attribute-codec.ts`
- Test: `src/main/ui/shared/__tests__/attribute-codec.test.ts`

- [ ] **Step 1: Write failing tests**

Create `src/main/ui/shared/__tests__/attribute-codec.test.ts` (license header first):

```ts
import { describe, expect, it } from 'vitest';
import { coerce, inferType, isReservedKey, validateKey, policyFnHint } from '../attribute-codec';

describe('attribute-codec coerce', () => {
    it('integer → JS number, rejects non-integers and decimals', () => {
        expect(coerce('integer', '3')).toEqual({ ok: true, value: 3 });
        expect(coerce('integer', '-12')).toEqual({ ok: true, value: -12 });
        expect(coerce('integer', '3.5').ok).toBe(false);
        expect(coerce('integer', 'abc').ok).toBe(false);
    });

    it('decimal → STRING, never a JS number (PDP drops Doubles)', () => {
        const r = coerce('decimal', '3.5');
        expect(r).toEqual({ ok: true, value: '3.5' });
        expect(typeof (r as { value: unknown }).value).toBe('string');
        expect(coerce('decimal', 'x').ok).toBe(false);
    });

    it('boolean → true/false only', () => {
        expect(coerce('boolean', 'true')).toEqual({ ok: true, value: true });
        expect(coerce('boolean', 'FALSE')).toEqual({ ok: true, value: false });
        expect(coerce('boolean', 'yes').ok).toBe(false);
    });

    it('set → deduped string array, warns on duplicates', () => {
        const r = coerce('set', ['us', 'eu', 'us']);
        expect(r).toMatchObject({ ok: true, value: ['us', 'eu'] });
        expect((r as { warning?: string }).warning).toMatch(/collapsed/i);
    });

    it('string/enum pass through verbatim', () => {
        expect(coerce('string', 'engineering')).toEqual({ ok: true, value: 'engineering' });
        expect(coerce('enum', 'gold')).toEqual({ ok: true, value: 'gold' });
    });

    it('timestamp validates ISO-8601 and stores a string', () => {
        expect(coerce('timestamp', '2026-05-30T12:00:00Z')).toEqual({ ok: true, value: '2026-05-30T12:00:00Z' });
        expect(coerce('timestamp', '30/05/2026').ok).toBe(false);
    });

    it('ip validates IPv4/IPv6 and stores a string', () => {
        expect(coerce('ip', '10.0.0.1')).toEqual({ ok: true, value: '10.0.0.1' });
        expect(coerce('ip', '::1')).toEqual({ ok: true, value: '::1' });
        expect(coerce('ip', '999.1.1.1').ok).toBe(false);
    });

    it('cidr validates a network/mask and stores a string', () => {
        expect(coerce('cidr', '10.0.0.0/24')).toEqual({ ok: true, value: '10.0.0.0/24' });
        expect(coerce('cidr', '10.0.0.0').ok).toBe(false);
    });

    it('duration validates a unit form and stores a string', () => {
        expect(coerce('duration', '30s')).toEqual({ ok: true, value: '30s' });
        expect(coerce('duration', '5m')).toEqual({ ok: true, value: '5m' });
        expect(coerce('duration', 'soon').ok).toBe(false);
    });
});

describe('attribute-codec keys', () => {
    it('isReservedKey blocks underscore-prefix and id/type', () => {
        expect(isReservedKey('_kind')).toBe(true);
        expect(isReservedKey('id')).toBe(true);
        expect(isReservedKey('type')).toBe(true);
        expect(isReservedKey('department')).toBe(false);
    });

    it('validateKey enforces pattern, reserved, and duplicates', () => {
        expect(validateKey('dept', [])).toBeNull();
        expect(validateKey('', [])).toMatch(/required/i);
        expect(validateKey('1bad', [])).toMatch(/match/i);
        expect(validateKey('_x', [])).toMatch(/reserved/i);
        expect(validateKey('dept', ['dept'])).toMatch(/duplicate/i);
    });
});

describe('attribute-codec inferType + hints', () => {
    it('infers type from a stored value', () => {
        expect(inferType(true)).toBe('boolean');
        expect(inferType(7)).toBe('integer');
        expect(inferType(['a', 'b'])).toBe('set');
        expect(inferType('hello')).toBe('string');
    });

    it('exposes a policy function hint for string-backed types', () => {
        expect(policyFnHint('ip')).toBe('ip(...)');
        expect(policyFnHint('timestamp')).toBe('datetime(...)');
        expect(policyFnHint('string')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run to verify failure**

Run (module root): `npx vitest run attribute-codec`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `attribute-codec.ts`**

Create `src/main/ui/shared/attribute-codec.ts` (license header first, then):

```ts
import type { AttrValue } from './entity.types';

export type AttrType = 'string' | 'integer' | 'boolean' | 'set' | 'decimal' | 'timestamp' | 'duration' | 'ip' | 'cidr' | 'enum';

export const ATTR_TYPES: readonly AttrType[] = [
    'string',
    'integer',
    'boolean',
    'set',
    'decimal',
    'timestamp',
    'duration',
    'ip',
    'cidr',
    'enum',
];

export const ATTR_TYPE_LABELS: Record<AttrType, string> = {
    string: 'String',
    integer: 'Integer',
    boolean: 'Boolean',
    set: 'Set<String>',
    decimal: 'Decimal',
    timestamp: 'Timestamp',
    duration: 'Duration',
    ip: 'IP',
    cidr: 'CIDR',
    enum: 'Enum',
};

// String-backed types only acquire their typed meaning via a GAPL function inside a condition.
const POLICY_FN_HINTS: Partial<Record<AttrType, string>> = {
    decimal: 'decimal(...)',
    timestamp: 'datetime(...)',
    duration: 'duration(...)',
    ip: 'ip(...)',
    cidr: 'ip(...)',
};

export function policyFnHint(type: AttrType): string | undefined {
    return POLICY_FN_HINTS[type];
}

export type CoerceResult = { ok: true; value: AttrValue; warning?: string } | { ok: false; error: string };

const KEY_RE = /^[a-zA-Z][a-zA-Z0-9_]*$/;
const INT_RE = /^-?\d+$/;
const DECIMAL_RE = /^-?\d+(\.\d+)?$/;
const ISO_TS_RE = /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})?)?$/;
const DURATION_RE = /^\d+(ns|us|ms|s|m|h|d)$/;
const IPV4_RE = /^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$/;
const IPV6_RE = /^(([0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{0,4}|::1|::)$/;

function isIp(s: string): boolean {
    return IPV4_RE.test(s) || IPV6_RE.test(s);
}

export function isReservedKey(key: string): boolean {
    return key.startsWith('_') || key === 'id' || key === 'type';
}

export function validateKey(key: string, existingKeys: readonly string[]): string | null {
    const k = key.trim();
    if (k.length === 0) return 'Key is required.';
    if (isReservedKey(k)) return 'Keys starting with "_" and the keys "id"/"type" are reserved.';
    if (!KEY_RE.test(k)) return 'Key must match [a-zA-Z][a-zA-Z0-9_]* (letter first, no dots/spaces).';
    if (existingKeys.includes(k)) return 'Duplicate key.';
    return null;
}

export function coerce(type: AttrType, raw: string | readonly string[]): CoerceResult {
    if (type === 'set') {
        const arr = (Array.isArray(raw) ? raw : [raw as string]).map(x => x.trim()).filter(x => x.length > 0);
        const unique = Array.from(new Set(arr));
        const warning = unique.length < arr.length ? 'Duplicate members were collapsed (stored as a Set).' : undefined;
        return { ok: true, value: unique, warning };
    }
    const s = (typeof raw === 'string' ? raw : (raw[0] ?? '')).trim();
    switch (type) {
        case 'string':
        case 'enum':
            return { ok: true, value: typeof raw === 'string' ? raw : (raw[0] ?? '') };
        case 'integer': {
            if (!INT_RE.test(s)) return { ok: false, error: 'Must be a whole number (decimals are dropped by the engine — use Decimal).' };
            const n = Number(s);
            if (!Number.isSafeInteger(n)) return { ok: false, error: 'Integer is out of the safe range.' };
            return { ok: true, value: n };
        }
        case 'boolean': {
            const low = s.toLowerCase();
            if (low === 'true') return { ok: true, value: true };
            if (low === 'false') return { ok: true, value: false };
            return { ok: false, error: 'Must be true or false.' };
        }
        case 'decimal':
            // Stored as a STRING: PDP toValue has no Double branch and would drop a JS number.
            return DECIMAL_RE.test(s)
                ? { ok: true, value: s }
                : { ok: false, error: 'Must be a decimal number; use decimal(...) in policies.' };
        case 'timestamp':
            return ISO_TS_RE.test(s)
                ? { ok: true, value: s }
                : { ok: false, error: 'Must be ISO-8601 (e.g. 2026-05-30T12:00:00Z); wrap with datetime(...).' };
        case 'duration':
            return DURATION_RE.test(s)
                ? { ok: true, value: s }
                : { ok: false, error: 'Must be a duration like 30s, 5m, 2h; wrap with duration(...).' };
        case 'ip':
            return isIp(s) ? { ok: true, value: s } : { ok: false, error: 'Must be a valid IPv4/IPv6 address; wrap with ip(...).' };
        case 'cidr': {
            const [addr, mask, extra] = s.split('/');
            const m = Number(mask);
            const ok = extra === undefined && mask !== undefined && Number.isInteger(m) && m >= 0 && m <= 128 && isIp(addr);
            return ok ? { ok: true, value: s } : { ok: false, error: 'Must be CIDR like 10.0.0.0/24.' };
        }
    }
}

export function inferType(value: AttrValue): AttrType {
    if (typeof value === 'boolean') return 'boolean';
    if (typeof value === 'number') return 'integer';
    if (Array.isArray(value)) return 'set';
    return 'string';
}

/** Render a stored value back into the editor's raw input form. */
export function toRaw(value: AttrValue): string | string[] {
    if (Array.isArray(value)) return value;
    return String(value);
}
```

> Note: the `DURATION_RE` accepts Go-style unit durations (`30s`, `5m`, `2h`). If the engine's `duration()` parser expects ISO-8601 (`PT30S`) instead, change only `DURATION_RE` and the matching test in Step 1 — the rest is unaffected.

- [ ] **Step 4: Run tests to verify pass**

Run (module root): `npx vitest run attribute-codec`
Expected: PASS (all codec tests green).

- [ ] **Step 5: Typecheck**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/ui/shared/attribute-codec.ts src/main/ui/shared/__tests__/attribute-codec.test.ts
git commit -m "feat(authz-ui): add attribute-codec with PDP-safe type coercion"
```

---

## Task 4: `AttributeEditor` component

Presentational. Owns no API. Receives rows + `onChange`; validates via the codec; renders a value widget per type; supports read-only (catalog) mode.

**Files:**

- Create: `src/main/ui/features/policy-structure/AttributeEditor.tsx`
- Test: `src/main/ui/features/policy-structure/__tests__/AttributeEditor.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `__tests__/AttributeEditor.test.tsx` (license header first):

```tsx
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { AttributeEditor, type AttributeRow } from '../AttributeEditor';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => undefined;
});

function Harness({ initial = [] as AttributeRow[], readOnly = false }: { initial?: AttributeRow[]; readOnly?: boolean }) {
    const [rows, setRows] = useState<AttributeRow[]>(initial);
    return <AttributeEditor value={rows} onChange={setRows} readOnly={readOnly} keySuggestions={[]} />;
}

const row = (over: Partial<AttributeRow> = {}): AttributeRow => ({ id: 'r1', key: 'dept', type: 'string', raw: 'eng', ...over });

describe('AttributeEditor', () => {
    it('adds a blank row when "Add attribute" is clicked', async () => {
        const user = userEvent.setup();
        render(<Harness />);
        await user.click(screen.getByRole('button', { name: /Add attribute/i }));
        expect(screen.getByLabelText(/Attribute key/i)).toBeInTheDocument();
    });

    it('shows a reserved-key error for an underscore key', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row({ key: '' })]} />);
        await user.type(screen.getByLabelText(/Attribute key/i), '_secret');
        expect(screen.getByText(/reserved/i)).toBeInTheDocument();
    });

    it('shows an integer error for a non-numeric value', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row({ key: 'clearance', type: 'integer', raw: '' })]} />);
        await user.type(screen.getByLabelText(/Attribute value/i), 'abc');
        expect(screen.getByText(/whole number/i)).toBeInTheDocument();
    });

    it('shows the policy-function hint for string-backed types', () => {
        render(<Harness initial={[row({ key: 'srcIp', type: 'ip', raw: '10.0.0.1' })]} />);
        expect(screen.getByText(/ip\(\.\.\.\)/)).toBeInTheDocument();
    });

    it('removes a row', async () => {
        const user = userEvent.setup();
        render(<Harness initial={[row()]} />);
        await user.click(screen.getByRole('button', { name: /Remove dept/i }));
        expect(screen.queryByLabelText(/Attribute key/i)).not.toBeInTheDocument();
    });

    it('renders read-only with a catalog badge and no add button', () => {
        render(<Harness initial={[row()]} readOnly />);
        expect(screen.getByText('eng')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Add attribute/i })).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/Attribute value/i)).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run to verify failure**

Run (module root): `npx vitest run AttributeEditor`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `AttributeEditor.tsx`**

Create `AttributeEditor.tsx` (license header first, then):

```tsx
import {
    Badge,
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    cn,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';
import { ATTR_TYPES, ATTR_TYPE_LABELS, coerce, policyFnHint, validateKey, type AttrType } from '../../shared/attribute-codec';

export interface AttributeRow {
    readonly id: string;
    key: string;
    type: AttrType;
    /** Raw editor input: string for scalar types, string[] for sets. */
    raw: string | string[];
}

export interface AttributeEditorProps {
    readonly value: AttributeRow[];
    readonly onChange: (rows: AttributeRow[]) => void;
    readonly readOnly?: boolean;
    readonly keySuggestions: readonly string[];
}

let rowSeq = 0;
export function newAttributeRow(): AttributeRow {
    rowSeq += 1;
    return { id: `attr-${rowSeq}-${Date.now()}`, key: '', type: 'string', raw: '' };
}

export function AttributeEditor({ value, onChange, readOnly = false, keySuggestions }: AttributeEditorProps) {
    const listId = useId();

    function patch(id: string, next: Partial<AttributeRow>) {
        onChange(value.map(r => (r.id === id ? { ...r, ...next } : r)));
    }
    function remove(id: string) {
        onChange(value.filter(r => r.id !== id));
    }

    if (readOnly) {
        return (
            <div className="flex flex-col gap-2">
                {value.length === 0 && <p className="text-xs text-muted-foreground">No attributes.</p>}
                {value.map(r => (
                    <div key={r.id} className="flex items-center gap-2 text-sm">
                        <span className="font-mono">{r.key}</span>
                        <Badge variant="outline" className="font-mono text-xs">
                            {ATTR_TYPE_LABELS[r.type]}
                        </Badge>
                        <span className="truncate text-muted-foreground">{Array.isArray(r.raw) ? r.raw.join(', ') : r.raw}</span>
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-3">
            <datalist id={listId}>
                {keySuggestions.map(k => (
                    <option key={k} value={k} />
                ))}
            </datalist>
            {value.map(r => {
                const keyError = validateKey(
                    r.key,
                    value.filter(o => o.id !== r.id).map(o => o.key),
                );
                const coerced = coerce(r.type, r.raw);
                const valueError = !coerced.ok ? coerced.error : null;
                const warning = coerced.ok ? coerced.warning : undefined;
                const hint = policyFnHint(r.type);
                return (
                    <div key={r.id} className="flex flex-col gap-1 rounded-md border p-2">
                        <div className="flex flex-wrap items-start gap-2">
                            <Input
                                value={r.key}
                                onChange={e => patch(r.id, { key: e.target.value })}
                                placeholder="key"
                                aria-label="Attribute key"
                                list={listId}
                                aria-invalid={keyError !== null}
                                className="w-40 font-mono"
                            />
                            <Select value={r.type} onValueChange={t => patch(r.id, { type: t as AttrType })}>
                                <SelectTrigger aria-label="Attribute type" className="w-36">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {ATTR_TYPES.map(t => (
                                        <SelectItem key={t} value={t}>
                                            {ATTR_TYPE_LABELS[t]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <AttributeValueInput row={r} onRawChange={raw => patch(r.id, { raw })} invalid={valueError !== null} />
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => remove(r.id)}
                                aria-label={`Remove ${r.key || 'attribute'}`}
                                title="Remove"
                            >
                                <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                            </Button>
                        </div>
                        {keyError && <p className="text-xs text-destructive">{keyError}</p>}
                        {valueError && <p className="text-xs text-destructive">{valueError}</p>}
                        {warning && <p className="text-xs text-warning">{warning}</p>}
                        {hint && !valueError && (
                            <p className="text-xs text-muted-foreground">
                                Stored as text; reference with <span className="font-mono">{hint}</span> in policies.
                            </p>
                        )}
                    </div>
                );
            })}
            <div>
                <Button type="button" variant="outline" size="sm" onClick={() => onChange([...value, newAttributeRow()])}>
                    <PlusIcon className="mr-2 size-4" aria-hidden />
                    Add attribute
                </Button>
            </div>
        </div>
    );
}

function AttributeValueInput({
    row,
    onRawChange,
    invalid,
}: {
    row: AttributeRow;
    onRawChange: (raw: string | string[]) => void;
    invalid: boolean;
}) {
    if (row.type === 'boolean') {
        const checked = row.raw === 'true' || row.raw === true.toString();
        return (
            <div className="flex h-9 items-center gap-2">
                <Switch checked={checked} onCheckedChange={c => onRawChange(c ? 'true' : 'false')} aria-label="Attribute value" />
                <span className="text-sm text-muted-foreground">{checked ? 'true' : 'false'}</span>
            </div>
        );
    }
    if (row.type === 'set') {
        const text = Array.isArray(row.raw) ? row.raw.join(', ') : row.raw;
        return (
            <Input
                value={text}
                onChange={e => onRawChange(e.target.value.split(',').map(s => s.trim()))}
                placeholder="comma,separated"
                aria-label="Attribute value"
                aria-invalid={invalid}
                className={cn('min-w-40 flex-1 font-mono')}
            />
        );
    }
    return (
        <Input
            value={Array.isArray(row.raw) ? row.raw.join(',') : row.raw}
            onChange={e => onRawChange(e.target.value)}
            placeholder="value"
            aria-label="Attribute value"
            aria-invalid={invalid}
            className="min-w-40 flex-1 font-mono"
        />
    );
}
```

> If `Switch` is not exported by `@gravitee/graphene-core`, replace the boolean branch with a `Select` of `true`/`false` (named imports already in this file). Verify with: `npx tsc --noEmit`.

- [ ] **Step 4: Run tests to verify pass**

Run (module root): `npx vitest run AttributeEditor`
Expected: PASS (6 tests green).

- [ ] **Step 5: Typecheck + lint**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS
Run (repo root): `npx nx lint gravitee-gamma-module-authz` → PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/ui/features/policy-structure/AttributeEditor.tsx src/main/ui/features/policy-structure/__tests__/AttributeEditor.test.tsx
git commit -m "feat(authz-ui): add typed AttributeEditor component"
```

---

## Task 5: Integrate into `EditEntityDialog`

Add a helper to convert between `AttributeRow[]` and the backend attribute map, seed rows on open, render the editor (read-only for non-local), and merge coerced rows on submit while preserving meta + unknown attrs.

**Files:**

- Create: `src/main/ui/features/policy-structure/attribute-rows.ts` (shared row↔map helpers)
- Test: `src/main/ui/features/policy-structure/__tests__/attribute-rows.test.ts`
- Modify: `src/main/ui/features/policy-structure/EditEntityDialog.tsx`
- Modify: `src/main/ui/features/policy-structure/__tests__/EditEntityDialog.test.tsx`

- [ ] **Step 1: Write failing test for the row↔map helpers**

Create `__tests__/attribute-rows.test.ts` (license header first):

```ts
import { describe, expect, it } from 'vitest';
import { rowsFromAttrs, attrsFromRows } from '../attribute-rows';

describe('attribute-rows', () => {
    it('rowsFromAttrs seeds rows with inferred types, skipping description/_-keys', () => {
        const rows = rowsFromAttrs({ department: 'eng', clearance: 3, active: true, regions: ['us', 'eu'], description: 'x' });
        const byKey = Object.fromEntries(rows.map(r => [r.key, r]));
        expect(byKey.department).toMatchObject({ type: 'string', raw: 'eng' });
        expect(byKey.clearance).toMatchObject({ type: 'integer', raw: '3' });
        expect(byKey.active).toMatchObject({ type: 'boolean', raw: 'true' });
        expect(byKey.regions).toMatchObject({ type: 'set', raw: ['us', 'eu'] });
        expect(byKey.description).toBeUndefined();
    });

    it('attrsFromRows coerces and returns { attributes, error }', () => {
        const good = attrsFromRows([
            { id: 'a', key: 'clearance', type: 'integer', raw: '3' },
            { id: 'b', key: 'regions', type: 'set', raw: ['us', 'us'] },
        ]);
        expect(good.error).toBeNull();
        expect(good.attributes).toEqual({ clearance: 3, regions: ['us'] });

        const bad = attrsFromRows([{ id: 'a', key: 'clearance', type: 'integer', raw: 'x' }]);
        expect(bad.error).toMatch(/clearance/);
    });

    it('attrsFromRows rejects an invalid key', () => {
        const bad = attrsFromRows([{ id: 'a', key: '1bad', type: 'string', raw: 'x' }]);
        expect(bad.error).toMatch(/1bad|key/i);
    });
});
```

- [ ] **Step 2: Run to verify failure**

Run (module root): `npx vitest run attribute-rows`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `attribute-rows.ts`**

Create `attribute-rows.ts` (license header first, then):

```ts
import { coerce, inferType, validateKey, toRaw, type AttrType } from '../../shared/attribute-codec';
import type { AttrValue } from '../../shared/entity.types';
import { newAttributeRow, type AttributeRow } from './AttributeEditor';

/** Keys the dialogs manage outside the attribute editor (own fields) — never shown as rows. */
const EXCLUDED_KEYS = new Set(['description']);

export function rowsFromAttrs(attrs: Record<string, AttrValue>): AttributeRow[] {
    return Object.entries(attrs)
        .filter(([k]) => !k.startsWith('_') && !EXCLUDED_KEYS.has(k))
        .map(([key, v]) => {
            const type: AttrType = inferType(v);
            const base = newAttributeRow();
            return { ...base, key, type, raw: toRaw(v) };
        });
}

export interface AttrsFromRowsResult {
    readonly attributes: Record<string, AttrValue>;
    readonly error: string | null;
}

export function attrsFromRows(rows: readonly AttributeRow[]): AttrsFromRowsResult {
    const attributes: Record<string, AttrValue> = {};
    const seenKeys: string[] = [];
    for (const r of rows) {
        const keyError = validateKey(r.key, seenKeys);
        if (keyError) return { attributes: {}, error: `Attribute "${r.key || '(empty)'}": ${keyError}` };
        const coerced = coerce(r.type, r.raw);
        if (!coerced.ok) return { attributes: {}, error: `Attribute "${r.key}": ${coerced.error}` };
        attributes[r.key] = coerced.value;
        seenKeys.push(r.key);
    }
    return { attributes, error: null };
}
```

- [ ] **Step 4: Run to verify pass**

Run (module root): `npx vitest run attribute-rows`
Expected: PASS.

- [ ] **Step 5: Write the failing dialog integration test**

Append to `__tests__/EditEntityDialog.test.tsx` a test inside the existing `describe('EditEntityDialog', ...)` block. The existing `aliceEntity()` has `attrs: { department: 'engineering', email: 'alice@example.io', description: 'Eng lead' }`. Add:

```tsx
it('edits an attribute value and sends the typed map (preserving others)', async () => {
    const user = userEvent.setup();
    renderDialog();

    // department is an existing string attribute → edit it via the attribute editor.
    const deptValue = screen.getAllByLabelText(/Attribute value/i).find(el => (el as HTMLInputElement).value === 'engineering');
    expect(deptValue).toBeTruthy();
    await user.clear(deptValue as HTMLElement);
    await user.type(deptValue as HTMLElement, 'platform');
    await user.click(screen.getByRole('button', { name: /Save changes/i }));

    await waitFor(() => expect(updateEntitySpy).toHaveBeenCalledTimes(1));
    const attrs = (updateEntitySpy.mock.calls[0][2] as { attributes: Record<string, unknown> }).attributes;
    expect(attrs.department).toBe('platform');
    expect(attrs.email).toBe('alice@example.io');
    expect(attrs._displayName).toBe('Alice');
});
```

- [ ] **Step 6: Run to verify failure**

Run (module root): `npx vitest run EditEntityDialog`
Expected: FAIL — no "Attribute value" inputs yet (editor not embedded).

- [ ] **Step 7: Embed the editor in `EditEntityDialog`**

In `EditEntityDialog.tsx`:

1. Add imports:

```ts
import { AttributeEditor, type AttributeRow } from './AttributeEditor';
import { attrsFromRows, rowsFromAttrs } from './attribute-rows';
import { useEntities } from '../../shared/hooks/useEntities';
```

(If `useEntities` is already imported for parents, do not duplicate.)

2. Add state next to the other `useState` hooks:

```ts
const [attrRows, setAttrRows] = useState<AttributeRow[]>([]);
```

3. In the re-seed `useEffect([open, entity])`, after the existing seeds, add:

```ts
setAttrRows(rowsFromAttrs(entity.attrs));
```

4. Replace the submit attribute construction. The current `handleSubmit` builds `attributes` from `toBackend(entity).attributes` then overrides `_displayName`/`description`. Change it to also merge the editor rows and to fail closed on a bad row:

```ts
const rowsResult = attrsFromRows(attrRows);
if (rowsResult.error) {
    setSubmitError(rowsResult.error);
    setSubmitting(false);
    return;
}
const attributes: Record<string, unknown> = { ...toBackend(entity).attributes };
// Drop existing editable (non-meta, non-description) attrs, then apply the editor's set.
for (const k of Object.keys(attributes)) {
    if (!k.startsWith('_') && k !== 'description') delete attributes[k];
}
Object.assign(attributes, rowsResult.attributes);
attributes._displayName = displayName.trim();
const trimmedDescription = description.trim();
if (trimmedDescription) attributes.description = trimmedDescription;
else delete attributes.description;
```

Keep the rest of `handleSubmit` (the `updateEntity` call, toast, error handling) unchanged.

5. Render the editor in the form, after the Parents block:

```tsx
<div className="flex flex-col gap-1.5">
    <Label>
        Attributes <span className="text-xs text-muted-foreground">(optional)</span>
    </Label>
    <AttributeEditor value={attrRows} onChange={setAttrRows} readOnly={entity?.source !== 'local'} keySuggestions={[]} />
    {entity?.source !== 'local' && <p className="text-xs text-muted-foreground">Attributes are managed by the source and are read-only.</p>}
</div>
```

- [ ] **Step 8: Run to verify pass**

Run (module root): `npx vitest run EditEntityDialog attribute-rows`
Expected: PASS (new + existing EditEntityDialog tests green).

- [ ] **Step 9: Typecheck**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS

- [ ] **Step 10: Commit**

```bash
git add src/main/ui/features/policy-structure/attribute-rows.ts src/main/ui/features/policy-structure/__tests__/attribute-rows.test.ts src/main/ui/features/policy-structure/EditEntityDialog.tsx src/main/ui/features/policy-structure/__tests__/EditEntityDialog.test.tsx
git commit -m "feat(authz-ui): edit entity attributes via typed editor"
```

---

## Task 6: Integrate into `CreateEntityDialog`

**Files:**

- Modify: `src/main/ui/features/policy-structure/CreateEntityDialog.tsx`
- Modify: `src/main/ui/features/policy-structure/__tests__/CreateEntityDialog.test.tsx`

- [ ] **Step 1: Write the failing test**

Append to the existing `CreateEntityDialog` test describe a test that fills name + slug, adds an attribute, and asserts the create payload. Match the existing test file's render helper and mocks (it mocks `authzApiService.createEntity` and `getEntity`). Add:

```tsx
it('includes a typed attribute in the create payload', async () => {
    const user = userEvent.setup();
    renderDialog('PRINCIPAL'); // existing helper; adjust name to match the file

    await user.type(screen.getByLabelText(/Display name/i), 'Alice');
    // slug auto-derives to "alice"
    await user.click(screen.getByRole('button', { name: /Add attribute/i }));
    await user.type(screen.getByLabelText(/Attribute key/i), 'clearance');
    await user.selectOptions(screen.getByLabelText(/Attribute type/i), 'integer'); // if Select is native; else open + click 'Integer'
    await user.type(screen.getByLabelText(/Attribute value/i), '3');
    await user.click(screen.getByRole('button', { name: /Create Principal/i }));

    await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
    const payload = createEntitySpy.mock.calls[0][1] as { attributes: Record<string, unknown> };
    expect(payload.attributes.clearance).toBe(3);
    expect(payload.attributes._displayName).toBe('Alice');
});
```

> The graphene `Select` is not a native `<select>`, so `selectOptions` will not work. Instead open it and click the option: `await user.click(screen.getByLabelText(/Attribute type/i)); await user.click(screen.getByRole('option', { name: 'Integer' }));`. Use whichever the existing tests in this file already use for the Type select, to stay consistent.

- [ ] **Step 2: Run to verify failure**

Run (module root): `npx vitest run CreateEntityDialog`
Expected: FAIL — no attribute editor present.

- [ ] **Step 3: Embed the editor in `CreateEntityDialog`**

1. Add imports:

```ts
import { AttributeEditor, type AttributeRow } from './AttributeEditor';
import { attrsFromRows } from './attribute-rows';
```

2. Add state with the other hooks:

```ts
const [attrRows, setAttrRows] = useState<AttributeRow[]>([]);
```

3. In the reset `useEffect(!open)`, add: `setAttrRows([]);`

4. In `handleSubmit`, replace the attribute construction (lines 184-188) with:

```ts
const rowsResult = attrsFromRows(attrRows);
if (rowsResult.error) {
    setSubmitError(rowsResult.error);
    setSubmitting(false);
    return;
}
const trimmedDescription = description.trim();
const attributes: Record<string, unknown> = {
    ...rowsResult.attributes,
    _displayName: displayName.trim(),
};
if (trimmedDescription) attributes.description = trimmedDescription;
```

5. Render the editor after the Parents block (before the closing `</div>` of the scroll area):

```tsx
<div className="flex flex-col gap-1.5">
    <Label>
        Attributes <span className="text-xs text-muted-foreground">(optional)</span>
    </Label>
    <AttributeEditor value={attrRows} onChange={setAttrRows} keySuggestions={[]} />
</div>
```

- [ ] **Step 4: Run to verify pass**

Run (module root): `npx vitest run CreateEntityDialog`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/ui/features/policy-structure/CreateEntityDialog.tsx src/main/ui/features/policy-structure/__tests__/CreateEntityDialog.test.tsx
git commit -m "feat(authz-ui): set entity attributes when creating an entity"
```

---

## Task 7: Wire key suggestions from the derived schema (optional polish)

Give the editor real key/type hints from the schema viewer's derived attributes for the entity's kind, so operators reuse existing names.

**Files:**

- Modify: `src/main/ui/features/policy-structure/EditEntityDialog.tsx` and `CreateEntityDialog.tsx`

- [ ] **Step 1: Derive suggestions from `useSchema`**

In each dialog, read the schema and compute the attribute-key list for the current entity kind:

```ts
import { useSchema } from '../../shared/hooks/useSchema';
import { parseGaplSchema } from '../../shared/gapl-parser';
// ...
const { schema } = useSchema(environmentId);
const keySuggestions = useMemo(() => {
    const parsed = parseGaplSchema(schema?.schemaText ?? '');
    const names = new Set<string>();
    for (const ent of parsed.entities) for (const a of ent.attributes) if (!a.name.startsWith('_')) names.add(a.name);
    return Array.from(names).sort();
}, [schema?.schemaText]);
```

Pass `keySuggestions={keySuggestions}` to `<AttributeEditor>` in both dialogs.

> Verify the `useSchema` return shape and `parseGaplSchema` export against `src/main/ui/shared/hooks/useSchema.ts` and `src/main/ui/shared/gapl-parser.ts` (used by `SchemaPage.tsx`). If `ParsedEntity.attributes` items are not `{ name }`, adjust the accessor to match the parser's type.

- [ ] **Step 2: Typecheck + tests**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS
Run (module root): `npx vitest run` → all green.

- [ ] **Step 3: Commit**

```bash
git add src/main/ui/features/policy-structure/EditEntityDialog.tsx src/main/ui/features/policy-structure/CreateEntityDialog.tsx
git commit -m "feat(authz-ui): suggest attribute keys from the derived schema"
```

---

## Task 8: Full verification + PR

- [ ] **Step 1: Typecheck**

Run (`src/main/ui`): `npx tsc --noEmit` → PASS

- [ ] **Step 2: Full test suite**

Run (module root): `npx vitest run` → all green.

- [ ] **Step 3: Lint + format**

Run (repo root): `npx nx lint gravitee-gamma-module-authz` → PASS
Run (repo root): `npx prettier --check "gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/shared/attribute-codec.ts" "gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/AttributeEditor.tsx" "gravitee-gamma/gravitee-gamma-module-authz/src/main/ui/features/policy-structure/attribute-rows.ts"` → clean (run `--write` then re-check if needed).
Run (repo root): `npx nx format:check --base=origin/master --head=HEAD` → PASS

- [ ] **Step 4: Manual smoke (optional, via gamma-dev)**

Bring up the stack (`bash ~/.claude/skills/gamma-dev/start.sh`), open the Authorization module → Entities → Add principal, add an `integer` attribute `clearance=3`, save, confirm it appears; edit it, confirm the value round-trips; open a catalog resource and confirm attributes are read-only.

- [ ] **Step 5: Push + open PR**

```bash
git push -u origin feat/authz-entity-attribute-editor
gh pr create --title "feat(authz-ui): typed entity attribute editor" --body "$(cat <<'EOF'
## Summary
- Add a typed key/value attribute editor to the Create/Edit entity dialogs.
- Coerce values to the representations the PDP understands (Integer→Long, Set from array, decimal/timestamp/duration/ip/cidr/enum as strings); never emit a JSON Double (PDP drops it).
- Catalog-sourced entities show attributes read-only.
- Fix entity-adapter round-trip: preserve `_url`/`_proxyApiId` and string-set attributes.

## Test plan
- [ ] attribute-codec unit tests (coercion/validation per type)
- [ ] AttributeEditor component tests
- [ ] attribute-rows + dialog round-trip tests
- [ ] tsc, vitest, nx lint, nx format all green

See `docs/superpowers/specs/2026-05-30-entity-attribute-editor-design.md`. Phase 2 (authored schema for true cross-instance type safety) is out of scope.
EOF
)"
```

---

## Self-review notes (author)

- **Spec coverage:** PDP serialization table → Task 3 codec (every type). Never-emit-Double → codec decimal-as-string + explicit test (Task 3 Step 1). Array→Set + dedup warning → codec `set` + test. Reserved/meta keys → `isReservedKey` + editor block (Task 3/4). Catalog read-only → Task 4 readOnly + Task 5 wiring. Meta-key round-trip bug → Task 2. Editor in both dialogs → Tasks 5 & 6. Schema-derived key hints → Task 7. Phase-2 disclaimer → carried into the spec, not implemented (correct).
- **Type consistency:** `AttrType`, `AttributeRow`, `coerce`, `validateKey`, `inferType`, `toRaw`, `newAttributeRow`, `rowsFromAttrs`, `attrsFromRows` names are used identically across tasks.
- **Known verification points flagged inline (not placeholders):** graphene `Switch` export (Task 4), graphene `Select` interaction style in tests (Task 6), `useSchema`/`ParsedEntity.attributes` shape (Task 7), engine `duration()` format vs `DURATION_RE` (Task 3). Each names the exact line to adjust if the assumption is wrong.
