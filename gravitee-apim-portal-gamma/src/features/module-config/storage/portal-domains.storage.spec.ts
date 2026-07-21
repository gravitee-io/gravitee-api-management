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
    createPortalDomain,
    deletePortalDomain,
    listPortalDomains,
    updatePortalDomain,
} from './portal-domains.storage';

describe('portal-domains.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create and list domains', async () => {
        const domain = await createPortalDomain({
            hostname: 'Developers.Example.COM',
            portalId: 'portal-1',
            primary: true,
        });

        expect(domain).toMatchObject({
            hostname: 'developers.example.com',
            portalId: 'portal-1',
            primary: true,
            status: 'Pending',
        });

        const listed = await listPortalDomains();
        expect(listed).toHaveLength(1);
        expect(listed[0]?.id).toBe(domain.id);
    });

    it('should clear previous primary when setting a new primary for the same portal', async () => {
        const first = await createPortalDomain({
            hostname: 'old.example.com',
            portalId: 'portal-1',
            primary: true,
        });
        const second = await createPortalDomain({
            hostname: 'new.example.com',
            portalId: 'portal-1',
            primary: true,
        });

        const listed = await listPortalDomains();
        expect(listed.find(item => item.id === first.id)?.primary).toBe(false);
        expect(listed.find(item => item.id === second.id)?.primary).toBe(true);
    });

    it('should update and delete domains', async () => {
        const domain = await createPortalDomain({
            hostname: 'dev.example.com',
            portalId: 'portal-1',
        });

        await updatePortalDomain(domain.id, { status: 'Active', primary: true });
        const updated = (await listPortalDomains())[0];
        expect(updated).toMatchObject({
            status: 'Active',
            primary: true,
        });

        await deletePortalDomain(domain.id);
        expect(await listPortalDomains()).toEqual([]);
    });
});
