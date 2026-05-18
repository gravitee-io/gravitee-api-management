/**
 * Parsing and validation logic for bulk JSON principal import.
 * Ported from prototype principal-import.ts, adapted to use real EntityInstance
 * types (no mock data).
 */
import type { EntityInstance } from './entity-types';

// ---------- Types -------------------------------------------------------------

export interface ParsedPrincipal {
    /** Zero-based index in the original input array. */
    index: number;
    /** Raw parsed object (for display). */
    raw: unknown;
    /** The resulting entity if valid. */
    entity?: Omit<EntityInstance, '_backendId' | 'createdAt' | 'updatedAt'>;
    /** Validation error if invalid. */
    error?: string;
}

export interface ParseResult {
    /** Whether parsing the top-level JSON succeeded. */
    ok: boolean;
    /** Parse-level error (e.g. malformed JSON, not an array). */
    parseError?: string;
    /** Per-item results. */
    items: ParsedPrincipal[];
    /** Indices of items that are duplicates of a previous item in the same batch. */
    duplicateIndices: Set<number>;
}

// ---------- Validation --------------------------------------------------------

const PRINCIPAL_TYPES = new Set(['User', 'Group', 'ServiceAccount', 'AgentIdentity']);

function validateItem(raw: unknown, index: number): ParsedPrincipal {
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
        return { index, raw, error: 'Expected a plain object.' };
    }
    const obj = raw as Record<string, unknown>;

    // uid: { type, id } — required
    if (!obj.uid || typeof obj.uid !== 'object' || Array.isArray(obj.uid)) {
        return { index, raw, error: 'Missing required field "uid" (must be an object { type, id }).' };
    }
    const uid = obj.uid as Record<string, unknown>;
    if (typeof uid.type !== 'string' || !uid.type) {
        return { index, raw, error: '"uid.type" must be a non-empty string.' };
    }
    if (typeof uid.id !== 'string' || !uid.id) {
        return { index, raw, error: '"uid.id" must be a non-empty string.' };
    }
    if (!PRINCIPAL_TYPES.has(uid.type)) {
        return {
            index,
            raw,
            error: `"uid.type" must be one of: ${Array.from(PRINCIPAL_TYPES).join(', ')}. Got "${uid.type}".`,
        };
    }

    // attrs: optional Record<string, primitive>
    const attrs: Record<string, string | number | boolean> = {};
    if (obj.attrs !== undefined) {
        if (!obj.attrs || typeof obj.attrs !== 'object' || Array.isArray(obj.attrs)) {
            return { index, raw, error: '"attrs" must be a plain object if provided.' };
        }
        for (const [k, v] of Object.entries(obj.attrs as Record<string, unknown>)) {
            if (typeof v !== 'string' && typeof v !== 'number' && typeof v !== 'boolean') {
                return { index, raw, error: `attrs["${k}"] must be a string, number, or boolean.` };
            }
            attrs[k] = v;
        }
    }

    // parents: optional Array<{ type, id }> (same format as uid)
    const parents: Array<{ type: string; id: string }> = [];
    if (obj.parents !== undefined) {
        if (!Array.isArray(obj.parents)) {
            return { index, raw, error: '"parents" must be an array if provided.' };
        }
        for (let i = 0; i < obj.parents.length; i++) {
            const p = obj.parents[i] as Record<string, unknown>;
            if (!p || typeof p !== 'object' || Array.isArray(p)) {
                return { index, raw, error: `parents[${i}] must be an object { type, id }.` };
            }
            if (typeof p.type !== 'string' || !p.type || typeof p.id !== 'string' || !p.id) {
                return { index, raw, error: `parents[${i}] must have non-empty string "type" and "id".` };
            }
            parents.push({ type: p.type as string, id: p.id as string });
        }
    }

    // source: optional EntitySource
    const source = typeof obj.source === 'string' ? (obj.source as EntityInstance['source']) : 'local';
    const displayName = typeof obj.displayName === 'string' ? obj.displayName : undefined;

    return {
        index,
        raw,
        entity: {
            uid: { type: uid.type as string, id: uid.id as string },
            attrs,
            parents,
            source,
            displayName,
        },
    };
}

// ---------- Top-level parser --------------------------------------------------

/**
 * Parse a JSON string that should contain an array of principal objects.
 * Returns per-item validation results and detects duplicates by uid.
 */
export function parsePrincipalJson(jsonText: string): ParseResult {
    let parsed: unknown;
    try {
        parsed = JSON.parse(jsonText);
    } catch (e) {
        return {
            ok: false,
            parseError: `Invalid JSON: ${e instanceof Error ? e.message : String(e)}`,
            items: [],
            duplicateIndices: new Set(),
        };
    }

    if (!Array.isArray(parsed)) {
        return {
            ok: false,
            parseError: 'Expected a JSON array at the top level.',
            items: [],
            duplicateIndices: new Set(),
        };
    }

    const items: ParsedPrincipal[] = parsed.map((item, i) => validateItem(item, i));

    // Detect duplicates within the batch by uid key.
    const seenKeys = new Set<string>();
    const duplicateIndices = new Set<number>();
    for (const item of items) {
        if (!item.entity) continue;
        const key = `${item.entity.uid.type}::${item.entity.uid.id}`;
        if (seenKeys.has(key)) {
            duplicateIndices.add(item.index);
        } else {
            seenKeys.add(key);
        }
    }

    return { ok: true, items, duplicateIndices };
}
