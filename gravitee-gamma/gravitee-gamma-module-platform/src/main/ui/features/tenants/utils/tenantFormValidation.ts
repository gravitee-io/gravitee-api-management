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

import type { Tenant } from '../types/tenant';

/** Backend `@Size(max = 40)` on `NewTenantEntity` name and key. */
export const TENANT_NAME_MAX = 40;
export const TENANT_KEY_MAX = 40;
/** Classic org-settings UI limit; backend has no description max. */
export const TENANT_DESCRIPTION_MAX = 160;

/**
 * Port of Classic `sanitizeKeyBase` — live sanitize while typing the key.
 * Alphanumeric + hyphens only; strips diacritics and special characters.
 */
export function slugifyTenantKeyBase(key: string): string {
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
export function slugifyTenantKeyFinal(key: string): string {
    let sanitized = slugifyTenantKeyBase(key);
    while (sanitized.endsWith('-')) {
        sanitized = sanitized.slice(0, -1);
    }
    return sanitized;
}

export function tenantKeyFromName(name: string): string {
    return slugifyTenantKeyFinal(name).slice(0, TENANT_KEY_MAX);
}

export function getTenantNameError(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) return null;
    if (trimmed.length > TENANT_NAME_MAX) {
        return `Name must be at most ${TENANT_NAME_MAX} characters`;
    }
    return null;
}

export function isTenantNameValid(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= 1 && trimmed.length <= TENANT_NAME_MAX;
}

export function isTenantKeyValid(key: string): boolean {
    const finalized = slugifyTenantKeyFinal(key);
    return finalized.length >= 1 && finalized.length <= TENANT_KEY_MAX;
}

export function getTenantDescriptionError(description: string): string | null {
    if (description.length > TENANT_DESCRIPTION_MAX) {
        return `Description must be at most ${TENANT_DESCRIPTION_MAX} characters`;
    }
    return null;
}

export function isTenantDescriptionValid(description: string): boolean {
    return description.length <= TENANT_DESCRIPTION_MAX;
}

export function findDuplicateTenantKey(rows: Tenant[], key: string, excludeId?: string): Tenant | undefined {
    if (!key) return undefined;
    return rows.find(row => row.id !== excludeId && row.key === key);
}
