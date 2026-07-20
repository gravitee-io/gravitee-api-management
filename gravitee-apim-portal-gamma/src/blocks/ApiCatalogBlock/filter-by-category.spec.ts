/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    filterEntriesByCategory,
    getCategoryNamesForApi,
    getEnabledCategoriesForFilter,
} from './filter-by-category';
import type { PortalCategory } from '../../features/settings/types';

describe('filterEntriesByCategory', () => {
    const categories: PortalCategory[] = [
        {
            id: 'cat-payments',
            portalId: 'portal-1',
            name: 'Payments',
            description: '',
            createdAt: 1,
            enabled: true,
            mappedApis: [
                { id: 'api-1', name: 'Orders' },
                { id: 'api-2', name: 'Checkout' },
            ],
        },
        {
            id: 'cat-disabled',
            portalId: 'portal-1',
            name: 'Disabled',
            description: '',
            createdAt: 2,
            enabled: false,
            mappedApis: [{ id: 'api-1', name: 'Orders' }],
        },
    ];

    const entries = [
        { apiId: 'api-1', navItemId: 'nav-1' },
        { apiId: 'api-2', navItemId: 'nav-2' },
        { apiId: 'api-3', navItemId: 'nav-3' },
    ];

    it('should return all entries when no category is selected', () => {
        expect(filterEntriesByCategory(entries, categories, '')).toEqual(entries);
        expect(filterEntriesByCategory(entries, categories, '   ')).toEqual(entries);
    });

    it('should intersect published APIs with category.mappedApis', () => {
        expect(filterEntriesByCategory(entries, categories, 'cat-payments')).toEqual([
            { apiId: 'api-1', navItemId: 'nav-1' },
            { apiId: 'api-2', navItemId: 'nav-2' },
        ]);
    });

    it('should return empty when category is unknown or disabled', () => {
        expect(filterEntriesByCategory(entries, categories, 'missing')).toEqual([]);
        expect(filterEntriesByCategory(entries, categories, 'cat-disabled')).toEqual([]);
    });
});

describe('getEnabledCategoriesForFilter', () => {
    it('should only include enabled categories', () => {
        const categories: PortalCategory[] = [
            {
                id: 'a',
                portalId: 'p',
                name: 'A',
                description: '',
                createdAt: 1,
                enabled: true,
                mappedApis: [],
            },
            {
                id: 'b',
                portalId: 'p',
                name: 'B',
                description: '',
                createdAt: 2,
                enabled: false,
                mappedApis: [],
            },
        ];

        expect(getEnabledCategoriesForFilter(categories).map(c => c.id)).toEqual(['a']);
    });
});

describe('getCategoryNamesForApi', () => {
    it('should return enabled category names mapped to the API', () => {
        const categories: PortalCategory[] = [
            {
                id: 'a',
                portalId: 'p',
                name: 'Payments',
                description: '',
                createdAt: 1,
                enabled: true,
                mappedApis: [{ id: 'api-1', name: 'Orders' }],
            },
            {
                id: 'b',
                portalId: 'p',
                name: 'Hidden',
                description: '',
                createdAt: 2,
                enabled: false,
                mappedApis: [{ id: 'api-1', name: 'Orders' }],
            },
            {
                id: 'c',
                portalId: 'p',
                name: 'Billing',
                description: '',
                createdAt: 3,
                enabled: true,
                mappedApis: [{ id: 'api-2', name: 'Invoices' }],
            },
        ];

        expect(getCategoryNamesForApi('api-1', categories)).toEqual(['Payments']);
        expect(getCategoryNamesForApi('api-2', categories)).toEqual(['Billing']);
        expect(getCategoryNamesForApi('api-3', categories)).toEqual([]);
    });
});
