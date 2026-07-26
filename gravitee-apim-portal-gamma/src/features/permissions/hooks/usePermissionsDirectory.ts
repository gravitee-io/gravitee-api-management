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
import { useCallback, useEffect, useMemo, useState } from 'react';

import { deleteMembersForTenant, getMembersByTenantId } from '../../tenants/storage/portal-tenant-members.storage';
import {
    deletePortalTenant,
    getAllPortalTenants,
    savePortalTenant,
} from '../../tenants/storage/portal-tenants.storage';
import {
    DEFAULT_PORTAL_TENANT_FEATURES,
    type PortalTenant,
    type PortalTenantFeatures,
    type PortalTenantManagementMode,
} from '../../tenants/types/portal-tenant.types';
import { createTenantId, deriveTenantHrid } from '../../tenants/utils/tenant-hrid';
import { deleteGrantsForGroup, getGrantsByTenantId } from '../storage/portal-access-grants.storage';
import {
    deleteMembersForGroup,
    getGroupMembersByTenantId,
} from '../storage/portal-group-members.storage';
import {
    createPortalGroup,
    deletePortalGroup,
    getAllPortalGroups,
    renamePortalGroup,
    updatePortalGroupFeatures,
} from '../storage/portal-groups.storage';
import { seedPermissionsIfEmpty } from '../storage/seed-permissions';
import type { PortalGroup, PortalGroupManagementMode } from '../types/permissions.types';

export interface PermissionsTenantSummary extends PortalTenant {
    groupCount: number;
    userCount: number;
}

export interface PermissionsGroupSummary extends PortalGroup {
    memberCount: number;
    adminCount: number;
    grantCount: number;
}

export interface CreateTenantValues {
    name: string;
    description?: string;
    managementMode?: PortalTenantManagementMode;
}

export interface CreateGroupValues {
    name: string;
    description?: string;
    managementMode?: PortalGroupManagementMode;
}

export function usePermissionsDirectory() {
    const [tenants, setTenants] = useState<PermissionsTenantSummary[]>([]);
    const [groups, setGroups] = useState<PermissionsGroupSummary[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            await seedPermissionsIfEmpty();

            const [storedTenants, allGroups] = await Promise.all([getAllPortalTenants(), getAllPortalGroups()]);

            // Portals create a bare default tenant on first open; only tenants that opted into a
            // management mode take part in the permissions model.
            const allTenants = storedTenants.filter(tenant => tenant.managementMode !== undefined);

            const perTenant = await Promise.all(
                allTenants.map(async tenant => {
                    const [tenantMembers, groupMembers, grants] = await Promise.all([
                        getMembersByTenantId(tenant.id),
                        getGroupMembersByTenantId(tenant.id),
                        getGrantsByTenantId(tenant.id),
                    ]);
                    return { tenantId: tenant.id, tenantMembers, groupMembers, grants };
                }),
            );

            const statsByTenantId = new Map(perTenant.map(entry => [entry.tenantId, entry]));

            setTenants(
                allTenants
                    .map(tenant => ({
                        ...tenant,
                        groupCount: allGroups.filter(group => group.tenantId === tenant.id).length,
                        userCount: statsByTenantId.get(tenant.id)?.tenantMembers.length ?? 0,
                    }))
                    .sort((a, b) => a.name.localeCompare(b.name)),
            );

            setGroups(
                allGroups
                    .filter(group => statsByTenantId.has(group.tenantId))
                    .map(group => {
                        const stats = statsByTenantId.get(group.tenantId);
                        const members = stats?.groupMembers.filter(member => member.groupId === group.id) ?? [];
                        return {
                            ...group,
                            memberCount: members.length,
                            adminCount: members.filter(member => member.role === 'admin').length,
                            grantCount: stats?.grants.filter(grant => grant.groupId === group.id).length ?? 0,
                        };
                    }),
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const groupsByTenantId = useMemo(() => {
        const map = new Map<string, PermissionsGroupSummary[]>();
        for (const group of groups) {
            const existing = map.get(group.tenantId) ?? [];
            existing.push(group);
            map.set(group.tenantId, existing);
        }
        return map;
    }, [groups]);

    /** Environment-level tenants have no portal, so they are shared by every developer portal. */
    const createTenant = useCallback(
        async (values: CreateTenantValues) => {
            const now = new Date().toISOString();
            const name = values.name.trim();
            const tenant: PortalTenant = {
                id: createTenantId(),
                name,
                hrid: deriveTenantHrid(name),
                description: values.description?.trim() || undefined,
                allowedApiIds: [],
                apiAccessMode: 'all',
                features: DEFAULT_PORTAL_TENANT_FEATURES,
                managementMode: values.managementMode ?? 'DELEGATED',
                createdAt: now,
                updatedAt: now,
            };
            await savePortalTenant(tenant);
            await refresh();
            return tenant;
        },
        [refresh],
    );

    const removeTenant = useCallback(
        async (tenantId: string) => {
            const tenantGroups = groups.filter(group => group.tenantId === tenantId);
            for (const group of tenantGroups) {
                await deleteMembersForGroup(group.id);
                await deleteGrantsForGroup(group.id);
                await deletePortalGroup(group.id);
            }
            await deleteMembersForTenant(tenantId);
            await deletePortalTenant(tenantId);
            await refresh();
        },
        [groups, refresh],
    );

    const createGroup = useCallback(
        async (tenantId: string, values: CreateGroupValues) => {
            const group = await createPortalGroup({
                tenantId,
                name: values.name,
                description: values.description,
                managementMode: values.managementMode,
            });
            await refresh();
            return group;
        },
        [refresh],
    );

    const renameGroup = useCallback(
        async (groupId: string, name: string) => {
            const updated = await renamePortalGroup(groupId, name);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const updateGroupFeatures = useCallback(
        async (groupId: string, features: PortalTenantFeatures) => {
            const updated = await updatePortalGroupFeatures(groupId, features);
            await refresh();
            return updated;
        },
        [refresh],
    );

    const removeGroup = useCallback(
        async (groupId: string) => {
            await deleteMembersForGroup(groupId);
            await deleteGrantsForGroup(groupId);
            await deletePortalGroup(groupId);
            await refresh();
        },
        [refresh],
    );

    return {
        tenants,
        groups,
        groupsByTenantId,
        loading,
        refresh,
        createTenant,
        removeTenant,
        createGroup,
        renameGroup,
        updateGroupFeatures,
        removeGroup,
    };
}
