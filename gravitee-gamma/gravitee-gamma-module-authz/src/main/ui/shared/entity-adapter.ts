/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { EntityRequest, EntityResponse } from './api/authz-api.types';
import { kindToUiType, uiTypeToKind } from './entity-kind-registry';
import type { AttrValue, EntityInstance, EntitySource } from './entity.types';

export const META_KEYS = new Set(['_source', '_displayName', '_importedAt', '_kind', '_url', '_syncedAt', '_proxyApiId']);

/**
 * Parse a canonical dotted backend uid (`<kind>.<id>`, e.g. `user.alice`,
 * `user.okta.alice`) into a structured `{ type, id }` pair: kind is inferred
 * from the first segment, id is everything after the first dot.
 *
 * Returns `{type: 'Unknown', id: <whole input>}` when no kind can be inferred —
 * the caller is expected to consult `_kind` in attributes for the override.
 */
export function parseEntityUid(uid: string): { type: string; id: string } {
    const dot = uid.indexOf('.');
    if (dot > 0) {
        const inferred = kindToUiType(uid.slice(0, dot));
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
    return `${uiTypeToKind(u.type)}.${u.id}`;
}

/**
 * Convert a backend EntityResponse to the richer frontend EntityInstance.
 *
 * - Splits reserved meta keys out of attributes.
 * - Parses parents from string form to structured objects.
 * - For dotted entityIds, strips the kind prefix from the structured `id`
 *   when an explicit `_kind` attribute matches the prefix — keeps round-trip
 *   stable for both single-segment (`user.alice`) and multi-segment
 *   (`mcp.flight-api.search`) ids.
 */
export function fromBackend(e: EntityResponse): EntityInstance {
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

    const uid = resolveUid(e.uid, kindOverride);

    const parents = e.parents.map(parseEntityUid);

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
    const expectedPrefix = uiTypeToKind(kindOverride) + '.';
    const id = rawUid.startsWith(expectedPrefix) ? rawUid.slice(expectedPrefix.length) : rawUid;
    return { type: kindOverride, id };
}

/**
 * Convert a frontend EntityInstance back to an EntityRequest for POST/PUT.
 * Packs meta fields back into `attributes` under reserved underscore keys.
 */
export function toBackend(e: EntityInstance): EntityRequest {
    const uid = formatEntityUid(e.uid);

    const attributes: Record<string, unknown> = { ...(e.reservedMeta ?? {}), ...e.attrs };

    // Surface uid.type back as `_kind` so a fromBackend → toBackend round-trip
    // preserves the explicit kind that fromBackend may have consumed off the
    // wire. Without this, kind information is silently dropped on the next PUT.
    attributes['_kind'] = uiTypeToKind(e.uid.type);
    if (e.source && e.source !== 'local') {
        attributes['_source'] = e.source;
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

export function entityKeyOf(uid: { type: string; id: string }): string {
    return `${uid.type}::${uid.id}`;
}
