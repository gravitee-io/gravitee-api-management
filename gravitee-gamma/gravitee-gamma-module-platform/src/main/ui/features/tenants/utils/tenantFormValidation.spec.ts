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

import {
    findDuplicateTenantKey,
    getTenantDescriptionError,
    getTenantNameError,
    isTenantDescriptionValid,
    isTenantKeyValid,
    isTenantNameValid,
    slugifyTenantKeyBase,
    slugifyTenantKeyFinal,
    TENANT_DESCRIPTION_MAX,
    TENANT_KEY_MAX,
    TENANT_NAME_MAX,
    tenantKeyFromName,
} from './tenantFormValidation';
import type { Tenant } from '../types/tenant';

const STUB_TENANTS: Tenant[] = [
    { id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia gateway cluster' },
    { id: 't-2', key: 'eu-west', name: 'EU West', description: 'Frankfurt gateway cluster' },
];

describe('tenantFormValidation', () => {
    it('uses backend @Size(max = 40) for name and key', () => {
        expect(TENANT_NAME_MAX).toBe(40);
        expect(TENANT_KEY_MAX).toBe(40);
        expect(TENANT_DESCRIPTION_MAX).toBe(160);
    });

    describe('slugifyTenantKeyBase', () => {
        it('converts a simple name to a lowercase hyphenated key', () => {
            expect(slugifyTenantKeyBase('My Tenant Key')).toBe('my-tenant-key');
        });

        it('strips diacritics and special characters', () => {
            expect(slugifyTenantKeyBase('Tênant Spécîal @#$ Nàme!')).toBe('tenant-special-name');
        });
    });

    describe('slugifyTenantKeyFinal', () => {
        it('strips trailing hyphens from the base slug', () => {
            expect(slugifyTenantKeyFinal('My Tenant Key---')).toBe('my-tenant-key');
        });
    });

    describe('tenantKeyFromName', () => {
        it('generates a key from a display name', () => {
            expect(tenantKeyFromName('AP South')).toBe('ap-south');
        });

        it('truncates generated keys to the API max', () => {
            const longName = 'A '.repeat(TENANT_KEY_MAX);
            expect(tenantKeyFromName(longName).length).toBeLessThanOrEqual(TENANT_KEY_MAX);
        });
    });

    describe('getTenantNameError', () => {
        it('returns null for empty or whitespace-only names', () => {
            expect(getTenantNameError('')).toBeNull();
            expect(getTenantNameError('   ')).toBeNull();
        });

        it('returns an error when name exceeds max length', () => {
            const longName = 'a'.repeat(TENANT_NAME_MAX + 1);
            expect(getTenantNameError(longName)).toBe(`Name must be at most ${TENANT_NAME_MAX} characters`);
        });
    });

    describe('isTenantNameValid', () => {
        it('returns false for empty names and true within max length', () => {
            expect(isTenantNameValid('')).toBe(false);
            expect(isTenantNameValid('US East')).toBe(true);
            expect(isTenantNameValid('a'.repeat(TENANT_NAME_MAX + 1))).toBe(false);
        });
    });

    describe('isTenantKeyValid', () => {
        it('requires a non-empty key within max length', () => {
            expect(isTenantKeyValid('')).toBe(false);
            expect(isTenantKeyValid('us-east')).toBe(true);
            expect(isTenantKeyValid('a'.repeat(TENANT_KEY_MAX + 1))).toBe(false);
        });

        it('rejects keys that sanitize to empty (hyphen-only)', () => {
            expect(isTenantKeyValid('-')).toBe(false);
            expect(isTenantKeyValid('---')).toBe(false);
        });
    });

    describe('description limits', () => {
        it('allows empty description and rejects over max length', () => {
            expect(isTenantDescriptionValid('')).toBe(true);
            expect(getTenantDescriptionError('')).toBeNull();
            expect(isTenantDescriptionValid('a'.repeat(TENANT_DESCRIPTION_MAX))).toBe(true);
            expect(isTenantDescriptionValid('a'.repeat(TENANT_DESCRIPTION_MAX + 1))).toBe(false);
            expect(getTenantDescriptionError('a'.repeat(TENANT_DESCRIPTION_MAX + 1))).toBe(
                `Description must be at most ${TENANT_DESCRIPTION_MAX} characters`,
            );
        });
    });

    describe('findDuplicateTenantKey', () => {
        it('finds a duplicate by exact key match', () => {
            expect(findDuplicateTenantKey(STUB_TENANTS, 'us-east')?.id).toBe('t-1');
            expect(findDuplicateTenantKey(STUB_TENANTS, 'us-east', 't-1')).toBeUndefined();
            expect(findDuplicateTenantKey(STUB_TENANTS, '')).toBeUndefined();
        });
    });
});
