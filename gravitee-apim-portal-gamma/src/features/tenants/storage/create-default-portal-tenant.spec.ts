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
import { createDefaultPortalTenant } from './create-default-portal-tenant';
import { getTenantsByPortalId } from './portal-tenants.storage';

describe('createDefaultPortalTenant', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create an Acme tenant when the portal has none', async () => {
        const tenant = await createDefaultPortalTenant('portal-1');

        expect(tenant).toMatchObject({
            portalId: 'portal-1',
            name: 'Acme',
            hrid: 'acme',
            apiAccessMode: 'all',
            allowedApiIds: [],
        });

        const tenants = await getTenantsByPortalId('portal-1');
        expect(tenants).toHaveLength(1);
        expect(tenants[0]).toEqual(tenant);
    });

    it('should be idempotent when tenants already exist', async () => {
        const first = await createDefaultPortalTenant('portal-1');
        const second = await createDefaultPortalTenant('portal-1');

        expect(second).toBeUndefined();
        expect(await getTenantsByPortalId('portal-1')).toEqual([first]);
    });
});
