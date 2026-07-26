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

import { getMembersByTenantId, savePortalTenantMember } from '../../tenants/storage/portal-tenant-members.storage';
import type { PortalTenantMember, PortalUser } from '../../tenants/types/portal-tenant.types';
import { createTenantMemberId } from '../../tenants/utils/tenant-hrid';
import {
    addPortalGroupMember,
    deletePortalGroupMember,
    getGroupMemberViews,
    setPortalGroupMemberRole,
} from '../storage/portal-group-members.storage';
import type { PortalGroupMemberRole, PortalGroupMemberView } from '../types/permissions.types';

export function useGroupMembers(groupId: string | undefined, tenantId: string | undefined) {
    const [members, setMembers] = useState<PortalGroupMemberView[]>([]);
    const [tenantMembers, setTenantMembers] = useState<PortalTenantMember[]>([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        if (!groupId || !tenantId) {
            setMembers([]);
            setTenantMembers([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const [views, directory] = await Promise.all([
                getGroupMemberViews(groupId, tenantId),
                getMembersByTenantId(tenantId),
            ]);
            setMembers(views);
            setTenantMembers(directory);
        } finally {
            setLoading(false);
        }
    }, [groupId, tenantId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    /** Adds tenant directory members to the group, enrolling new users in the tenant when needed. */
    const addMembers = useCallback(
        async (users: readonly PortalUser[]) => {
            if (!groupId || !tenantId) {
                return;
            }

            const existingByUserId = new Map(tenantMembers.map(member => [member.userId, member]));

            for (const user of users) {
                let tenantMember = existingByUserId.get(user.id);
                if (!tenantMember) {
                    tenantMember = {
                        id: createTenantMemberId(),
                        tenantId,
                        userId: user.id,
                        displayName: user.displayName,
                        email: user.email,
                        role: 'member',
                    };
                    await savePortalTenantMember(tenantMember);
                }

                await addPortalGroupMember({ groupId, tenantId, memberId: tenantMember.id });
            }

            await refresh();
        },
        [groupId, refresh, tenantId, tenantMembers],
    );

    const setRole = useCallback(
        async (memberId: string, role: PortalGroupMemberRole) => {
            await setPortalGroupMemberRole(memberId, role);
            await refresh();
        },
        [refresh],
    );

    const removeMember = useCallback(
        async (memberId: string) => {
            await deletePortalGroupMember(memberId);
            await refresh();
        },
        [refresh],
    );

    return { members, tenantMembers, loading, refresh, addMembers, setRole, removeMember };
}
