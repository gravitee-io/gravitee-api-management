/**
 * Bidirectional adapter between the backend EntityResponse / EntityRequest
 * and the frontend EntityInstance used in all UI components.
 *
 * Canonical backend uid format — dotted: `<lowercase-kind>.<id>` (e.g.
 * `user.alice`, `group.engineering`, `mcpserver.flight-mcp`). SCIM-sourced
 * principals carry a 3-segment form `<kind>.<connector>.<slug>` (e.g.
 * `user.okta.alice`); for those the structured uid keeps the full tail
 * (`okta.alice`) as `id` so a fromBackend → toBackend round-trip preserves
 * the canonical entityId verbatim.
 *
 * Server-side validation rejects anything outside `[a-z0-9._-]+`
 * (see `EntityIdValidator` in `gravitee-apim-authorization-core`); the
 * legacy `Type::"id"` form this adapter used to produce was failing every
 * create with a 400 — that's bug #8 from the review, fixed here.
 *
 * Meta fields that have no backend column are stored inside `attributes`
 * under reserved underscore-prefixed keys: `_source`, `_principalProvider`,
 * `_displayName`, `_importedAt`. These are NEVER shown in the visible
 * attributes table.
 */
import type { AttrValue, EntityInstance, EntitySource } from '../app/features/policy-structure/entity-types';
import type { EntityRequest, EntityResponse } from './api/authz-api.types';

// ---------- Meta key constants ------------------------------------------------

export const META_KEYS = new Set([
    '_source',
    '_principalProvider',
    '_displayName',
    '_importedAt',
    '_kind',
    '_connector',
    '_url',
    '_syncedAt',
    '_provider',
    '_proxyApiId',
]);

/**
 * Maps a kind hint (lowercased — either the `_kind` attribute or the first
 * dotted segment of the entityId) to the structured uid type the UI's
 * categories table speaks. Returns undefined when the hint is missing or unknown.
 */
function kindToType(kind: unknown): string | undefined {
    if (typeof kind !== 'string') return undefined;
    const k = kind.toLowerCase();
    if (k === 'user') return 'User';
    if (k === 'group') return 'Group';
    if (k === 'serviceaccount' || k === 'service-account' || k === 'service_account') return 'ServiceAccount';
    if (k === 'agentidentity' || k === 'agent') return 'AgentIdentity';
    if (k === 'mcp' || k === 'mcpserver') return 'MCPServer';
    // proxy-api mirror + plain resources bucket into the schema's Resource type
    if (k === 'api' || k === 'resource') return 'Resource';
    // actions are managed on their own page; tag them so EntitiesPage can hide them
    if (k === 'action') return 'Action';
    return undefined;
}

/** Inverse of {@link kindToType}: structured uid type → lowercase backend kind segment. */
function typeToKind(type: string): string {
    const t = type.toLowerCase();
    // Camel-case structured types collapse into their lowercase canonical name.
    if (t === 'serviceaccount') return 'serviceaccount';
    if (t === 'agentidentity') return 'agentidentity';
    if (t === 'mcpserver') return 'mcp';
    return t;
}

// ---------- UID parsing / formatting ------------------------------------------

/**
 * Parse a backend uid string into a structured `{ type, id }` pair.
 *
 * Accepts both layouts the platform has seen:
 * - canonical dotted: `user.alice`, `group.engineering`, `user.okta.alice`
 *   (kind inferred from first segment; id is everything after the first dot)
 * - legacy quoted/unquoted: `User::alice`, `User::"alice"` — tolerated for
 *   any in-flight test fixture, but the platform's EntityIdValidator would
 *   reject this on save, so it never actually round-trips at runtime.
 *
 * Returns `{type: 'Unknown', id: <whole input>}` when no kind can be inferred —
 * the caller is expected to consult `_kind` in attributes for the override.
 */
export function parseEntityUid(uid: string): { type: string; id: string } {
    const sep = uid.indexOf('::');
    if (sep >= 0) {
        // Legacy Type::"id" form. Kept for backwards-compat with tests and any
        // pre-existing fixture; production data is dotted because the backend
        // validator rejects this layout on write.
        const type = uid.slice(0, sep);
        const rawId = uid.slice(sep + 2);
        const id = rawId.startsWith('"') && rawId.endsWith('"') ? rawId.slice(1, -1).replace(/\\"/g, '"') : rawId;
        return { type, id };
    }

    // Canonical dotted form: first segment hints kind, rest is the id.
    const dot = uid.indexOf('.');
    if (dot > 0) {
        const inferred = kindToType(uid.slice(0, dot));
        if (inferred) {
            return { type: inferred, id: uid.slice(dot + 1) };
        }
    }

    return { type: 'Unknown', id: uid };
}

/**
 * Format a structured uid back to the canonical backend form
 * `<lowercase-kind>.<id>`. The id is passed through unchanged because the
 * server-side validator (`[a-z0-9._-]+`) wouldn't have accepted invalid
 * characters on the way in either; we deliberately do NOT silently re-slugify
 * here so a corrupted id surfaces as a 400 from the backend rather than a
 * silent collision.
 */
export function formatEntityUid(u: { type: string; id: string }): string {
    return `${typeToKind(u.type)}.${u.id}`;
}

// ---------- fromBackend -------------------------------------------------------

/**
 * Convert a backend EntityResponse to the richer frontend EntityInstance.
 *
 * - Splits reserved meta keys out of attributes.
 * - Parses parents from string form to structured objects.
 * - For dotted entityIds, strips the kind prefix from the structured `id`
 *   when an explicit `_kind` attribute matches the prefix — keeps round-trip
 *   stable for both local (`user.alice`) and SCIM-sourced
 *   (`user.okta.alice`) layouts.
 */
export function fromBackend(e: EntityResponse): EntityInstance {
    // Separate meta from regular attributes.
    const attrs: Record<string, AttrValue> = {};
    let source: EntitySource = 'local';
    let principalProvider: string | undefined;
    let displayName: string | undefined;
    let importedAt: string | undefined;
    let kindOverride: string | undefined;

    for (const [k, v] of Object.entries(e.attributes)) {
        if (k === '_source') {
            source = v as string as EntitySource;
        } else if (k === '_principalProvider' || k === '_provider' || k === '_connector') {
            principalProvider = v as string;
        } else if (k === '_displayName') {
            displayName = v as string;
        } else if (k === '_importedAt' || k === '_syncedAt') {
            importedAt = v as string;
        } else if (k === '_kind') {
            kindOverride = kindToType(v);
        } else if (k === '_url' || k === '_proxyApiId') {
            // meta — drop from visible attrs
        } else {
            // Only carry over values that are valid AttrValue primitives.
            if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') {
                attrs[k] = v;
            }
        }
    }

    const uid = resolveUid(e.uid, kindOverride);

    const parents = e.parents.map(p => {
        const parsed = parseEntityUid(p);
        if (parsed.type !== 'Unknown') return parsed;
        const segs = p.split('.');
        if (segs.length >= 2) {
            const inferred = kindToType(segs[0]);
            if (inferred) return { type: inferred, id: p };
        }
        return parsed;
    });

    return {
        uid,
        displayName,
        attrs,
        parents,
        source,
        principalProvider,
        importedAt,
        _backendId: e.id,
        createdAt: e.createdAt,
        updatedAt: e.updatedAt,
    };
}

/**
 * Resolve the structured uid using the optional `_kind` attribute as the
 * source of truth for the type. Strips a matching dotted prefix from the
 * entityId so `user.alice` with `_kind=user` resolves to
 * `{type: 'User', id: 'alice'}` (round-trippable) rather than
 * `{type: 'User', id: 'user.alice'}` (which would re-wrap on save).
 */
function resolveUid(rawUid: string, kindOverride: string | undefined): { type: string; id: string } {
    const parsedFromUid = parseEntityUid(rawUid);
    if (!kindOverride) {
        return parsedFromUid;
    }
    const expectedPrefix = typeToKind(kindOverride) + '.';
    const id = rawUid.startsWith(expectedPrefix) ? rawUid.slice(expectedPrefix.length) : rawUid;
    return { type: kindOverride, id };
}

// ---------- toBackend ---------------------------------------------------------

/**
 * Convert a frontend EntityInstance back to an EntityRequest for POST/PUT.
 * Packs meta fields back into `attributes` under reserved underscore keys.
 */
export function toBackend(e: EntityInstance): EntityRequest {
    const uid = formatEntityUid(e.uid);

    const attributes: Record<string, unknown> = { ...e.attrs };

    // Only store meta keys that have non-default values.
    if (e.source && e.source !== 'local') {
        attributes['_source'] = e.source;
    }
    if (e.principalProvider) {
        attributes['_principalProvider'] = e.principalProvider;
    }
    if (e.displayName) {
        attributes['_displayName'] = e.displayName;
    }
    if (e.importedAt) {
        attributes['_importedAt'] = e.importedAt;
    }

    const parents = e.parents.map(p => formatEntityUid(p));

    return { uid, attributes, parents };
}

// ---------- Helpers -----------------------------------------------------------

/** Stable key for a ui entity (used as React key and lookup). */
export function entityKeyOf(uid: { type: string; id: string }): string {
    return `${uid.type}::${uid.id}`;
}
