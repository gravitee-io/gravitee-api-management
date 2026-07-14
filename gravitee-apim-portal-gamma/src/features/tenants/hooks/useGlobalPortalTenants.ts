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

import { getPublishedApiNavItems } from '../../../blocks/ApiCatalogBlock/catalog-utils';
import { getAllPortals } from '../../portals/storage/portals.storage';
import { getNavItems } from '../../portals/storage/navigation-items.storage';
import { getAllPortalTenants } from '../storage/portal-tenants.storage';
import { countTenantApps, getTenantMemberCount, seedPortalTenantsIfEmpty } from '../storage/seed-portal-tenants';
import type { PortalTenant } from '../types/portal-tenant.types';

export interface GlobalPortalTenantSummary extends PortalTenant {
    portalName: string;
    userCount: number;
    apiCount: number;
    appCount: number;
}

export function useGlobalPortalTenants() {
    const [tenants, setTenants] = useState<GlobalPortalTenantSummary[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            await seedPortalTenantsIfEmpty();
            const [portals, allTenants] = await Promise.all([getAllPortals(), getAllPortalTenants()]);
            const portalNameById = new Map(portals.map(portal => [portal.id, portal.name]));
            const apiCountByPortalId = new Map<string, number>();

            await Promise.all(
                portals.map(async portal => {
                    const navItems = await getNavItems(portal.id);
                    apiCountByPortalId.set(portal.id, getPublishedApiNavItems(navItems).length);
                }),
            );

            const summaries = await Promise.all(
                allTenants.map(async tenant => {
                    const userCount = await getTenantMemberCount(tenant.id);
                    const appCount = await countTenantApps(tenant.id);
                    const totalApiCount = apiCountByPortalId.get(tenant.portalId) ?? 0;
                    const apiCount =
                        tenant.apiAccessMode === 'all' ? totalApiCount : tenant.allowedApiIds.length;

                    return {
                        ...tenant,
                        portalName: portalNameById.get(tenant.portalId) ?? tenant.portalId,
                        userCount,
                        apiCount,
                        appCount,
                    };
                }),
            );

            setTenants(summaries);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    return { tenants, loading, refresh };
}
