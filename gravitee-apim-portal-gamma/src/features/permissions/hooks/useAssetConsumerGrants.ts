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

import { getAllPortalTenants } from '../../tenants/storage/portal-tenants.storage';
import { getGrantsByScopeId } from '../storage/portal-access-grants.storage';
import { getGroupMembers } from '../storage/portal-group-members.storage';
import { getAllPortalGroups } from '../storage/portal-groups.storage';
import { seedPermissionsIfEmpty } from '../storage/seed-permissions';
import type { ConsumeProvisioning, PortalAccessLevel } from '../types/permissions.types';

export interface AssetConsumerGrantRow {
    grantId: string;
    groupId: string;
    groupName: string;
    tenantName: string;
    memberCount: number;
    access: PortalAccessLevel;
    provisioning?: ConsumeProvisioning;
}

/** Which portal groups already hold consumer access on one asset. Read-only by design. */
export function useAssetConsumerGrants(scopeId: string | undefined) {
    const [rows, setRows] = useState<AssetConsumerGrantRow[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!scopeId) {
            setRows([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            await seedPermissionsIfEmpty();
            const [grants, groups, tenants] = await Promise.all([
                getGrantsByScopeId(scopeId),
                getAllPortalGroups(),
                getAllPortalTenants(),
            ]);

            const groupById = new Map(groups.map(group => [group.id, group]));
            const tenantById = new Map(tenants.map(tenant => [tenant.id, tenant]));

            const resolved = await Promise.all(
                grants.map(async grant => {
                    const group = groupById.get(grant.groupId);
                    const members = await getGroupMembers(grant.groupId);

                    return {
                        grantId: grant.id,
                        groupId: grant.groupId,
                        groupName: group?.name ?? grant.groupId,
                        tenantName: tenantById.get(grant.tenantId)?.name ?? grant.tenantId,
                        memberCount: members.length,
                        access: grant.access,
                        provisioning: grant.provisioning,
                    };
                }),
            );

            setRows(resolved.sort((a, b) => a.groupName.localeCompare(b.groupName)));
        } finally {
            setLoading(false);
        }
    }, [scopeId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    return { rows, loading, refresh };
}
