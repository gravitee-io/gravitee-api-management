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
import { PORTAL_TENANT_MEMBERS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { PortalTenantMember } from '../types/portal-tenant.types';

export const STORE_NAME = PORTAL_TENANT_MEMBERS_STORE_NAME;

export async function getMembersByTenantId(tenantId: string): Promise<PortalTenantMember[]> {
    return runTransaction<PortalTenantMember[]>(PORTAL_TENANT_MEMBERS_STORE_NAME, 'readonly', store => {
        const index = store.index('tenantId');
        return index.getAll(tenantId);
    });
}

export async function getMembersByPortalId(portalId: string, tenantIds: readonly string[]): Promise<PortalTenantMember[]> {
    if (tenantIds.length === 0) {
        return [];
    }

    const membersByTenant = await Promise.all(tenantIds.map(tenantId => getMembersByTenantId(tenantId)));
    return membersByTenant.flat();
}

export async function getMemberByUserId(userId: string): Promise<PortalTenantMember | undefined> {
    const members = await runTransaction<PortalTenantMember[]>(PORTAL_TENANT_MEMBERS_STORE_NAME, 'readonly', store => {
        const index = store.index('userId');
        return index.getAll(userId);
    });

    return members[0];
}

export async function savePortalTenantMember(member: PortalTenantMember): Promise<void> {
    await runTransaction(PORTAL_TENANT_MEMBERS_STORE_NAME, 'readwrite', store => store.put(member));
}

export async function deletePortalTenantMember(id: string): Promise<void> {
    await runTransaction(PORTAL_TENANT_MEMBERS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteMembersForTenant(tenantId: string): Promise<void> {
    const members = await getMembersByTenantId(tenantId);
    await Promise.all(members.map(member => deletePortalTenantMember(member.id)));
}
