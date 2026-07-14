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
import { useCallback, useEffect, useState } from 'react';

import { getNavItems } from '../../portals/storage/navigation-items.storage';
import { getPublishedApiNavItems } from '../../../blocks/ApiCatalogBlock/catalog-utils';
import { countTenantApps, getTenantMemberCount } from '../storage/seed-portal-tenants';
import { deletePortalTenant, getTenantsByPortalId, savePortalTenant } from '../storage/portal-tenants.storage';
import { deleteMembersForTenant } from '../storage/portal-tenant-members.storage';
import { seedPortalTenantsForPortal } from '../storage/seed-portal-tenants';
import type { PortalTenant } from '../types/portal-tenant.types';

export interface PortalTenantSummary extends PortalTenant {
    userCount: number;
    apiCount: number;
    appCount: number;
}

async function buildTenantSummaries(portalId: string, tenants: PortalTenant[]): Promise<PortalTenantSummary[]> {
    const navItems = await getNavItems(portalId);
    const publishedApis = getPublishedApiNavItems(navItems);
    const totalApiCount = publishedApis.length;

    return Promise.all(
        tenants.map(async tenant => {
            const userCount = await getTenantMemberCount(tenant.id);
            const appCount = await countTenantApps(tenant.id);
            const apiCount =
                tenant.apiAccessMode === 'all' ? totalApiCount : tenant.allowedApiIds.length;

            return {
                ...tenant,
                userCount,
                apiCount,
                appCount,
            };
        }),
    );
}

export function usePortalTenants(portalId: string | undefined) {
    const [tenants, setTenants] = useState<PortalTenantSummary[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!portalId) {
            setTenants([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            await seedPortalTenantsForPortal(portalId);
            const stored = await getTenantsByPortalId(portalId);
            const summaries = await buildTenantSummaries(portalId, stored);
            setTenants(summaries);
        } finally {
            setLoading(false);
        }
    }, [portalId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const deleteTenant = useCallback(
        async (tenantId: string) => {
            await deleteMembersForTenant(tenantId);
            await deletePortalTenant(tenantId);
            await refresh();
        },
        [refresh],
    );

    const createTenant = useCallback(
        async (tenant: PortalTenant) => {
            await savePortalTenant(tenant);
            await refresh();
            return tenant;
        },
        [refresh],
    );

    return {
        tenants,
        loading,
        refresh,
        deleteTenant,
        createTenant,
    };
}
