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
import { PORTAL_TENANTS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { PortalTenant } from '../types/portal-tenant.types';

export const STORE_NAME = PORTAL_TENANTS_STORE_NAME;

export async function getTenantsByPortalId(portalId: string): Promise<PortalTenant[]> {
    return runTransaction<PortalTenant[]>(PORTAL_TENANTS_STORE_NAME, 'readonly', store => {
        const index = store.index('portalId');
        return index.getAll(portalId);
    });
}

export async function getAllPortalTenants(): Promise<PortalTenant[]> {
    return runTransaction<PortalTenant[]>(PORTAL_TENANTS_STORE_NAME, 'readonly', store => store.getAll());
}

export async function getPortalTenant(id: string): Promise<PortalTenant | undefined> {
    return runTransaction<PortalTenant | undefined>(PORTAL_TENANTS_STORE_NAME, 'readonly', store => store.get(id));
}

export async function savePortalTenant(tenant: PortalTenant): Promise<void> {
    await runTransaction(PORTAL_TENANTS_STORE_NAME, 'readwrite', store => store.put(tenant));
}

export async function deletePortalTenant(id: string): Promise<void> {
    await runTransaction(PORTAL_TENANTS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteTenantsForPortal(portalId: string): Promise<void> {
    const tenants = await getTenantsByPortalId(portalId);
    await Promise.all(tenants.map(tenant => deletePortalTenant(tenant.id)));
}
