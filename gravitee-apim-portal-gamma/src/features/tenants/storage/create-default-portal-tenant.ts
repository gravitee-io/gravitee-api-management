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
import { DEFAULT_PORTAL_TENANT_FEATURES, type PortalTenant } from '../types/portal-tenant.types';
import { createTenantId, deriveTenantHrid } from '../utils/tenant-hrid';
import { getTenantsByPortalId, savePortalTenant } from './portal-tenants.storage';

const DEFAULT_TENANT_NAME = 'Acme';

export async function createDefaultPortalTenant(portalId: string): Promise<PortalTenant | undefined> {
    const existing = await getTenantsByPortalId(portalId);
    if (existing.length > 0) {
        return undefined;
    }

    const now = new Date().toISOString();
    const tenant: PortalTenant = {
        id: createTenantId(),
        portalId,
        name: DEFAULT_TENANT_NAME,
        hrid: deriveTenantHrid(DEFAULT_TENANT_NAME),
        allowedApiIds: [],
        apiAccessMode: 'all',
        features: DEFAULT_PORTAL_TENANT_FEATURES,
        createdAt: now,
        updatedAt: now,
    };

    await savePortalTenant(tenant);
    return tenant;
}
