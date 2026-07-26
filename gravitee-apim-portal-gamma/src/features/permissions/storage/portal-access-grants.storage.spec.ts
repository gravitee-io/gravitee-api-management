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
    createPortalAccessGrant,
    deletePortalAccessGrant,
    getGrantsByGroupId,
    getGrantsByScopeId,
    setNavigationOverride,
    updatePortalAccessGrant,
} from './portal-access-grants.storage';
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';

const GROUP_ID = 'group-backend-devs';
const TENANT_ID = 'tenant-acme';

describe('portal-access-grants.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create a grant and read it back by group and by scope', async () => {
        const grant = await createPortalAccessGrant({
            groupId: GROUP_ID,
            tenantId: TENANT_ID,
            scopeType: 'API',
            scopeId: 'api-payments',
            access: 'VIEW',
        });

        expect(grant).toMatchObject({ scopeType: 'API', scopeId: 'api-payments', access: 'VIEW', overrides: [] });
        expect(await getGrantsByGroupId(GROUP_ID)).toEqual([grant]);
        expect(await getGrantsByScopeId('api-payments')).toEqual([grant]);
    });

    it('should keep the default plan only while auto-provisioning a consume grant', async () => {
        const grant = await createPortalAccessGrant({
            groupId: GROUP_ID,
            tenantId: TENANT_ID,
            scopeType: 'API',
            scopeId: 'api-payments',
            access: 'CONSUME',
            provisioning: 'AUTO',
            defaultPlanId: 'plan-payments-key',
        });

        expect(grant.defaultPlanId).toBe('plan-payments-key');

        const switchedToClassic = await updatePortalAccessGrant(grant.id, { provisioning: 'CLASSIC' });
        expect(switchedToClassic?.defaultPlanId).toBeUndefined();

        const downgradedToView = await updatePortalAccessGrant(grant.id, { access: 'VIEW' });
        expect(downgradedToView?.provisioning).toBeUndefined();
    });

    it('should store, replace, and clear a navigation override', async () => {
        const grant = await createPortalAccessGrant({
            groupId: GROUP_ID,
            tenantId: TENANT_ID,
            scopeType: 'API',
            scopeId: 'api-payments',
            access: 'CONSUME',
        });

        const withOverride = await setNavigationOverride(grant.id, {
            navigationItemId: 'nav-quickstart',
            portalId: 'portal-payments',
            access: 'NONE',
        });
        expect(withOverride?.overrides).toEqual([
            { navigationItemId: 'nav-quickstart', portalId: 'portal-payments', access: 'NONE' },
        ]);

        const replaced = await setNavigationOverride(grant.id, {
            navigationItemId: 'nav-quickstart',
            portalId: 'portal-payments',
            access: 'VIEW',
        });
        expect(replaced?.overrides).toEqual([
            { navigationItemId: 'nav-quickstart', portalId: 'portal-payments', access: 'VIEW' },
        ]);

        const cleared = await setNavigationOverride(grant.id, {
            navigationItemId: 'nav-quickstart',
            portalId: 'portal-payments',
            access: 'INHERIT',
        });
        expect(cleared?.overrides).toEqual([]);
    });

    it('should delete a grant', async () => {
        const grant = await createPortalAccessGrant({
            groupId: GROUP_ID,
            tenantId: TENANT_ID,
            scopeType: 'PORTAL',
            scopeId: 'portal-payments',
            access: 'VIEW',
        });

        await deletePortalAccessGrant(grant.id);

        expect(await getGrantsByGroupId(GROUP_ID)).toEqual([]);
    });
});
