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
import { getTenantsByPortalId } from '../../tenants/storage/portal-tenants.storage';
import { clearPortalsDatabase } from './portals.storage.test-utils';
import { createPortalFromTemplate } from './create-portal';

describe('create-portal', () => {
    beforeEach(async () => {
        Object.defineProperty(globalThis, 'crypto', {
            value: { randomUUID: () => 'portal-new-1' },
            configurable: true,
        });
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create a default Acme tenant for a new blank portal', async () => {
        const portal = await createPortalFromTemplate('blank');

        const tenants = await getTenantsByPortalId(portal.id);
        expect(tenants).toHaveLength(1);
        expect(tenants[0]).toMatchObject({
            portalId: portal.id,
            name: 'Acme',
            hrid: 'acme',
        });
    });
});
