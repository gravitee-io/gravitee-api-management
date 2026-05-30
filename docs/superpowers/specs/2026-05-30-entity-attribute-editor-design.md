# Entity Attribute Editor (Gamma authz) — Design

Date: 2026-05-30
Module: `gravitee-gamma/gravitee-gamma-module-authz` (UI: `src/main/ui`)
Status: Approved design — ready for implementation plan

## Problem

Entity attributes are ABAC inputs: the PDP stages each entity's attribute bag into the
GAPL engine and `when {}` conditions read `principal.X` / `resource.X`. Today the admin UI
can set only display name, description, and parents. Attributes arrive almost exclusively
via catalog import; manually-created entities cannot get the attributes their ABAC policies
depend on. The backend already accepts arbitrary typed attributes
(`Map<String, Object>`), so this is a UI-only gap.

## Goal

Add a typed key/value attribute editor to the Create and Edit entity dialogs so operators
can author entity attributes that GAPL ABAC policies can evaluate — emitting only the
representations the PDP actually understands, so "saved == evaluable".

## Non-goals (explicit)

- No backend changes. `Map<String, Object>` already stores everything we need.
- **No type-safety guarantee across instances.** The schema is *derived* from stored
  attributes (`AuthzSchemaServiceImpl.typeByAttribute`), not authored. This editor
  guarantees a coherent type *within a single write*; it does NOT prevent two entities from
  giving the same attribute key different types. True cross-instance type safety requires an
  authored per-type schema — tracked separately as **Phase 2** and out of scope here.
- No editing of catalog-sourced attributes (they are owned by the catalog and overwritten
  on next sync).

## PDP serialization contract (the core constraint)

The PDP entity-staging path uses only `ValueConverter.attributesToValues` → `new Entity(...)`;
it never calls the engine's `EntityJsonParser`, so schema-typed coercion and the `__extn`
envelope are unreachable. Only `ValueConverter.toValue` governs what an attribute becomes:

| Editor type   | PDP `toValue` result        | Stored JSON form                  | Policy usage / note |
|---------------|-----------------------------|-----------------------------------|---------------------|
| String        | `ofString`                  | JSON string                       | direct |
| Integer       | `ofLong` (Integer/Long)     | JSON integer number               | safe for `>= 3` |
| Boolean       | `ofBool`                    | JSON boolean                      | direct |
| Set\<String\> | `ofSet` (always a Set)      | JSON array of strings             | duplicates collapse — warn |
| Decimal       | none — **Double is dropped**| JSON **string**                   | wrap in `decimal(...)` |
| Timestamp     | none                        | JSON **string**                   | wrap in `datetime(...)` |
| Duration      | none                        | JSON **string**                   | wrap in `duration(...)` |
| IP            | none                        | JSON **string**                   | wrap in `ip(...)` |
| CIDR          | none                        | JSON **string**                   | `ip(...)` / `isInRange` |
| Enum          | `StringValue`               | JSON **string**                   | constrained only by derived schema |

Two load-bearing rules derived from this:

1. **Never emit a JSON floating-point number.** PDP `toValue` has no Double branch → the
   attribute is silently dropped. "Decimal" therefore serializes as a String; numeric-looking
   attributes that need `>= / <` comparisons must be Integer (Long).
2. **Arrays always become Sets.** No List type exists in the engine; duplicates collapse. The
   editor labels this `Set<String>` and warns on duplicate members.

Six of the ten types (Decimal, Timestamp, Duration, IP, CIDR, Enum) serialize to plain
Strings and acquire typed semantics only via a GAPL function inside a condition. The editor's
"type" for these provides **format validation + a policy-authoring hint**, not engine-level
enforcement. This is documented in the UI (helper text naming the wrapping function) so
operators are not misled.

## Architecture

Two new units plus edits to two existing dialogs and the adapter.

### `attribute-codec.ts` (pure, no React) — single source of truth for types
- `type AttrType = 'string'|'integer'|'boolean'|'set'|'decimal'|'timestamp'|'duration'|'ip'|'cidr'|'enum'`
- `coerce(type, raw): { ok: true; value: AttrValue } | { ok: false; error: string }`
  - integer → JSON number, reject non-integer / overflow
  - boolean → `true|false`
  - set → `string[]` (dedup, warn surfaced separately)
  - decimal → validated decimal **string**
  - timestamp → validated ISO-8601 **string**
  - ip → validated IPv4/IPv6 **string**; cidr → validated CIDR **string**
  - duration → validated duration **string**; enum/string → string
  - **Never returns a JS number for decimal** (guards the Double-drop trap).
- `inferType(jsonValue): AttrType` — for seeding rows from existing attributes
  (boolean→boolean, integer number→integer, array→set, else string).
- `isReservedKey(key)` — blocks `_`-prefix (meta) and `id` / `type` (engine throws).
- `validateKey(key, existingKeys)` — pattern `^[a-zA-Z][a-zA-Z0-9_]*$`, no duplicates.
- Fully unit-tested in isolation; holds ALL knowledge of the PDP serialization contract.

### `AttributeEditor.tsx` (UI only) — rows of `key · type · value · remove`
- Props: `value: AttributeRow[]`, `onChange`, `readOnly?`, `keySuggestions` (from derived
  schema for this entity kind, with inferred types), `entityKindLabel`.
- Per-type value widget (text / integer input / toggle / chips for set / format-validated
  text with helper hint for temporal/ip/cidr/decimal/enum).
- Inline validation via `attribute-codec`; shows the policy-function hint for string-backed
  types; warns on set duplicates; distinguishes "remove row" (key absent) from empty string.
- Knows nothing about the API.

### Dialog integration
- `CreateEntityDialog.tsx` and `EditEntityDialog.tsx` hold `AttributeRow[]` state, render an
  "Attributes" section under parents, and merge coerced rows into the attribute map on submit.
- Catalog/APIM-sourced entities (`source !== 'local'`): editor rendered `readOnly` with a
  "from catalog" badge (consistent with existing edit/delete local-only gating).
- Submit reconstructs the FULL attribute map: editable rows + preserved meta keys + (for
  read-only catalog entities) untouched catalog attributes. Must not clobber unknown attrs.

### `entity-adapter.ts` fix (bundled)
- `toBackend` currently drops `_url` / `_proxyApiId` / `_syncedAt` (they are in `META_KEYS`
  but never re-emitted) → lost on every edit. Fix the round-trip as part of this work since
  the editor exercises the same path. Add a regression test.

## Data flow

1. Open dialog → `fromBackend` attrs → `AttributeRow[]` via `inferType`, keys/types hinted by
   `useSchema` for the entity kind.
2. Operator edits rows; `AttributeEditor` validates live via `attribute-codec`.
3. Submit → each row `coerce`d → typed JSON map merged with preserved/meta/catalog attrs →
   `toBackend` → `createEntity` / `updateEntity`.
4. After save, the schema is invalidated server-side, so the Schema viewer reflects new
   attributes (closes the loop).

## Error handling

- Invalid key/value blocks submit with inline messages (no silent drop).
- Decimal/temporal/ip/cidr format failures are surfaced at edit time.
- Backend errors surface in the existing dialog error alert; dialog stays open.
- Empty-string values are allowed but flagged (distinct from unset) because GAPL `==` and
  ordered comparisons treat absent vs empty differently (fail-closed on ordered/missing).

## Testing (TDD)

- `attribute-codec.test.ts` — coercion + rejection per type; **explicit test that decimal
  never yields a JS number**; set dedup; reserved-key and key-pattern rejection. (Core.)
- `AttributeEditor.test.tsx` — add/remove rows, change type re-coerces, block `_`/`id`/`type`,
  duplicate keys, catalog read-only, set duplicate warning, unset vs empty.
- `EditEntityDialog` / `CreateEntityDialog` tests — round-trip typed values end-to-end,
  preserve unknown attrs, and the `_url`/`_proxyApiId`/`_syncedAt` meta-key regression.
- `tsc --noEmit`, full `vitest`, `nx lint`, `nx format:check` all green before PR.

## Out of scope / follow-ups

- Phase 2: authored per-entity-type attribute schema (backend) → prescriptive types, policy
  validation against schema, schema-driven (not free-form) editor. Separate spec.
- A PDP `ValueConverter` decimal branch (would let Decimal be a real number). Until then
  Decimal stays string-only.
