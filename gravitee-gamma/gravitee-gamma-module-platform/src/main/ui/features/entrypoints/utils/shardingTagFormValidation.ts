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

import type { ShardingTagRow } from '../types/entrypoint';

export const TAG_NAME_MAX = 64;
export const TAG_KEY_MAX = 64;

/**
 * Port of Classic `sanitizeKeyBase` — live sanitize while typing the key.
 * Alphanumeric + hyphens only; strips diacritics and special characters.
 */
export function slugifyTagKeyBase(key: string): string {
    return key
        .normalize('NFD')
        .replaceAll(/[\u0300-\u036f]+/g, '')
        .toLowerCase()
        .replaceAll(/[^a-z\d\s-]/g, '')
        .trim()
        .replaceAll(/[^a-z\d]+/g, '-');
}

/**
 * Port of Classic `sanitizeKeyFinal` — strip trailing hyphens for the immutable key.
 */
export function slugifyTagKeyFinal(key: string): string {
    let sanitized = slugifyTagKeyBase(key);
    while (sanitized.endsWith('-')) {
        sanitized = sanitized.slice(0, -1);
    }
    return sanitized;
}

export function getTagNameError(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) return null;
    if (trimmed.length > TAG_NAME_MAX) {
        return `Name must be at most ${TAG_NAME_MAX} characters`;
    }
    return null;
}

export function isTagNameValid(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= 1 && trimmed.length <= TAG_NAME_MAX;
}

export function isTagKeyValid(key: string): boolean {
    return key.length >= 1 && key.length <= TAG_KEY_MAX;
}

export function findDuplicateTagName(rows: ShardingTagRow[], name: string, excludeId?: string): ShardingTagRow | undefined {
    const normalized = name.trim().toLowerCase();
    if (!normalized) return undefined;
    return rows.find(row => row.id !== excludeId && row.name.trim().toLowerCase() === normalized);
}

export function findDuplicateTagKey(rows: ShardingTagRow[], key: string, excludeId?: string): ShardingTagRow | undefined {
    if (!key) return undefined;
    return rows.find(row => row.id !== excludeId && row.key === key);
}
