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
