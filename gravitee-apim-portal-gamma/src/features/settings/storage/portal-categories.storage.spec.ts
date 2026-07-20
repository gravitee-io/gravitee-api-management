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
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import {
    createPortalCategory,
    deleteCategoriesForPortal,
    getCategoriesByPortalId,
    setCategoryEnabled,
    setCategoryMappedApis,
} from './portal-categories.storage';

describe('portal-categories.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create and list categories for a portal', async () => {
        const category = await createPortalCategory('portal-1', {
            name: 'Payments',
            description: 'Payment APIs',
        });

        expect(category).toMatchObject({
            portalId: 'portal-1',
            name: 'Payments',
            description: 'Payment APIs',
            enabled: true,
            mappedApis: [],
        });

        const listed = await getCategoriesByPortalId('portal-1');
        expect(listed).toHaveLength(1);
        expect(listed[0]?.id).toBe(category.id);
    });

    it('should update enabled flag and mapped APIs', async () => {
        const category = await createPortalCategory('portal-1', { name: 'Billing' });

        await setCategoryMappedApis(category.id, [{ id: 'api-1', name: 'Invoices' }]);
        await setCategoryEnabled(category.id, false);

        const listed = await getCategoriesByPortalId('portal-1');
        expect(listed[0]).toMatchObject({
            enabled: false,
            mappedApis: [{ id: 'api-1', name: 'Invoices' }],
        });
    });

    it('should delete all categories for a portal', async () => {
        await createPortalCategory('portal-1', { name: 'A' });
        await createPortalCategory('portal-1', { name: 'B' });
        await createPortalCategory('portal-2', { name: 'Other' });

        await deleteCategoriesForPortal('portal-1');

        expect(await getCategoriesByPortalId('portal-1')).toEqual([]);
        expect(await getCategoriesByPortalId('portal-2')).toHaveLength(1);
    });
});
