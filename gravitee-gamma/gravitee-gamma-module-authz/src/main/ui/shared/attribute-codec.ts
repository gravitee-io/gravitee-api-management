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
const IPV6_RE =
    /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:(:[0-9a-fA-F]{1,4}){1,6}|:((:[0-9a-fA-F]{1,4}){1,7}|:))$/;

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
