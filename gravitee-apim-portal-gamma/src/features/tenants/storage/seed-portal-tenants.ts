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
import { createDefaultPortalTenant } from './create-default-portal-tenant';
import { createDummyPortalTenantMembers, createDummyPortalTenants } from './dummy-portal-tenants';
import { getTenantsByPortalId, savePortalTenant } from './portal-tenants.storage';
import { getMembersByTenantId, savePortalTenantMember } from './portal-tenant-members.storage';

const PAYMENTS_PORTAL_ID = 'portal-payments';

export async function seedPortalTenantsForPortal(portalId: string): Promise<void> {
    const existing = await getTenantsByPortalId(portalId);
    if (existing.length > 0) {
        return;
    }

    if (portalId === PAYMENTS_PORTAL_ID) {
        const tenants = createDummyPortalTenants(portalId);
        await Promise.all(tenants.map(tenant => savePortalTenant(tenant)));

        const members = createDummyPortalTenantMembers();
        await Promise.all(members.map(member => savePortalTenantMember(member)));
        return;
    }

    await createDefaultPortalTenant(portalId);
}

export async function seedPortalTenantsIfEmpty(): Promise<void> {
    const { getAllPortals } = await import('../../portals/storage/portals.storage');
    const portals = await getAllPortals();
    await Promise.all(portals.map(portal => seedPortalTenantsForPortal(portal.id)));
}

export async function countTenantApps(tenantId: string): Promise<number> {
    const { getAllApplications } = await import('../../portals/storage/applications.storage');
    const apps = await getAllApplications();
    return apps.filter(app => app.portalTenantId === tenantId).length;
}

export async function getTenantMemberCount(tenantId: string): Promise<number> {
    const members = await getMembersByTenantId(tenantId);
    return members.length;
}
