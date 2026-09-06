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
    canDeleteOrResetMetadata,
    displayMetadataValue,
    getMetadataValuePlaceholder,
    isInheritedGlobal,
    isMetadataValueValid,
    isResettableMetadata,
    toMetadataSortBy,
} from './apiMetadata';
import type { ApiMetadata } from '../types/metadata';

const API_ONLY: ApiMetadata = { key: 'team', name: 'Team', format: 'STRING', value: 'Platform' };
const GLOBAL_INHERITED: ApiMetadata = {
    key: 'support-email',
    name: 'Support Email',
    format: 'MAIL',
    defaultValue: 'help@example.com',
};
const GLOBAL_OVERRIDE: ApiMetadata = {
    key: 'support-email',
    name: 'Support Email',
    format: 'MAIL',
    value: 'api@example.com',
    defaultValue: 'help@example.com',
};

describe('apiMetadata helpers', () => {
    describe('displayMetadataValue', () => {
        it('returns the API value when set', () => {
            expect(displayMetadataValue(API_ONLY)).toBe('Platform');
            expect(displayMetadataValue(GLOBAL_OVERRIDE)).toBe('api@example.com');
        });

        it('falls back to the global default when the API value is missing', () => {
            expect(displayMetadataValue(GLOBAL_INHERITED)).toBe('help@example.com');
        });
    });

    describe('inheritance flags', () => {
        it('marks rows with a defaultValue as inherited global metadata', () => {
            expect(isInheritedGlobal(API_ONLY)).toBe(false);
            expect(isInheritedGlobal(GLOBAL_INHERITED)).toBe(true);
            expect(isInheritedGlobal(GLOBAL_OVERRIDE)).toBe(true);
        });

        it('allows delete only when an API-level value exists', () => {
            expect(canDeleteOrResetMetadata(API_ONLY)).toBe(true);
            expect(canDeleteOrResetMetadata(GLOBAL_INHERITED)).toBe(false);
            expect(canDeleteOrResetMetadata(GLOBAL_OVERRIDE)).toBe(true);
        });

        it('treats inherited rows with an override as resettable', () => {
            expect(isResettableMetadata(API_ONLY)).toBe(false);
            expect(isResettableMetadata(GLOBAL_INHERITED)).toBe(false);
            expect(isResettableMetadata(GLOBAL_OVERRIDE)).toBe(true);
        });
    });

    describe('toMetadataSortBy', () => {
        it('serializes ascending and descending sort for the v2 query param', () => {
            expect(toMetadataSortBy([{ id: 'name', desc: false }])).toBe('name');
            expect(toMetadataSortBy([{ id: 'key', desc: true }])).toBe('-key');
        });

        it('returns undefined when no sort is active', () => {
            expect(toMetadataSortBy([])).toBeUndefined();
            expect(toMetadataSortBy(undefined)).toBeUndefined();
        });
    });

    describe('getMetadataValuePlaceholder', () => {
        it('returns format examples for numeric, mail, and url values', () => {
            expect(getMetadataValuePlaceholder('NUMERIC')).toBe('e.g. 123');
            expect(getMetadataValuePlaceholder('MAIL')).toBe('e.g. john@doe.com');
            expect(getMetadataValuePlaceholder('URL')).toBe('e.g. https://gravitee.io');
        });

        it('omits a placeholder for string, boolean, and date values', () => {
            expect(getMetadataValuePlaceholder('STRING')).toBeUndefined();
            expect(getMetadataValuePlaceholder('BOOLEAN')).toBeUndefined();
            expect(getMetadataValuePlaceholder('DATE')).toBeUndefined();
        });
    });

    describe('isMetadataValueValid', () => {
        it('accepts boolean literals only', () => {
            expect(isMetadataValueValid('BOOLEAN', 'true')).toBe(true);
            expect(isMetadataValueValid('BOOLEAN', 'false')).toBe(true);
            expect(isMetadataValueValid('BOOLEAN', 'yes')).toBe(false);
        });

        it('rejects empty non-boolean values', () => {
            expect(isMetadataValueValid('STRING', '')).toBe(false);
            expect(isMetadataValueValid('NUMERIC', '3')).toBe(true);
            expect(isMetadataValueValid('NUMERIC', 'nope')).toBe(false);
        });
    });
});
