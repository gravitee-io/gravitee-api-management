/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';

import { searchUsers } from '../../../shared/services/userSearch';
import type { SearchableUser } from '../../../shared/types/userSearch';
import type { GroupMember, GroupMembershipPayload } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { buildMembershipRoles, getMemberRoleLockFlags, type RoleField } from '../utils/memberRoles';
import { GROUP_SEARCH_DEBOUNCE_MS } from '../utils/paginationConstants';
import { isPrimaryOwnerUnavailable } from '../utils/primaryOwnership';
import { groupKeys } from '../utils/queryKeys';
import { nextSearchableUserSelection } from '../utils/searchableUsers';

const DEFAULT_USER_ROLES: Record<RoleField, string> = {
    apiRole: 'USER',
    apiProductRole: 'USER',
    applicationRole: 'USER',
    integrationRole: 'USER',
    clusterRole: 'USER',
    explorerRole: 'USER',
};

export function useGroupAddMembersForm({
    open,
    groupRoles,
    members,
    lockApiRole,
    lockApiProductRole,
    lockApplicationRole,
    canOverrideLocks,
    maxInvitation,
    apiPrimaryOwnerMode,
    apiProductPrimaryOwnerMode,
    initialSearch,
    onSubmit,
}: {
    open: boolean;
    groupRoles: Record<string, string> | undefined;
    members: GroupMember[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    maxInvitation: number | null;
    apiPrimaryOwnerMode: string | undefined;
    apiProductPrimaryOwnerMode: string | undefined;
    initialSearch?: string;
    onSubmit: (memberships: GroupMembershipPayload[]) => void;
}) {
    const [search, setSearch] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const [selected, setSelected] = useState<SearchableUser[]>([]);
    const [roleValues, setRoleValues] = useState(DEFAULT_USER_ROLES);

    useEffect(() => {
        if (!open) return;
        setSearch(initialSearch?.trim() ?? '');
        setDebouncedQuery('');
        setSelected([]);
        setRoleValues({
            apiRole: groupRoles?.API ?? 'USER',
            apiProductRole: groupRoles?.API_PRODUCT ?? 'USER',
            applicationRole: groupRoles?.APPLICATION ?? 'USER',
            integrationRole: 'USER',
            clusterRole: 'USER',
            explorerRole: 'USER',
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps -- intentionally keyed on the open transition only
    }, [open]);

    useEffect(() => {
        if (!open) return;
        const timer = setTimeout(() => setDebouncedQuery(search), GROUP_SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [open, search]);

    const roleLocks = getMemberRoleLockFlags({ lockApiRole, lockApiProductRole, lockApplicationRole }, canOverrideLocks);
    const groupMemberCapReached = typeof maxInvitation === 'number' && maxInvitation <= members.length;
    const invitationLimitReached = typeof maxInvitation === 'number' && maxInvitation <= members.length + selected.length;

    const { data: results, isFetching } = useQuery({
        queryKey: groupKeys.userSearch(debouncedQuery),
        queryFn: () => searchUsers(debouncedQuery),
        enabled: open && debouncedQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    const existingMemberIds = useMemo(() => new Set(members.map(m => m.id)), [members]);
    const candidates = useMemo(
        () => (results ?? []).filter(u => !existingMemberIds.has(u.id ?? u.reference)),
        [results, existingMemberIds],
    );

    const apiPrimaryOwnerDisabled =
        isPrimaryOwnerUnavailable(apiPrimaryOwnerMode) || members.some(m => m.roles?.API === PRIMARY_OWNER_ROLE);
    const apiProductPrimaryOwnerDisabled =
        isPrimaryOwnerUnavailable(apiProductPrimaryOwnerMode) || members.some(m => m.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE);

    const primaryOwnerSelected = roleValues.apiRole === PRIMARY_OWNER_ROLE || roleValues.apiProductRole === PRIMARY_OWNER_ROLE;

    return {
        search,
        setSearch,
        selected,
        roleValues,
        roleLocks,
        groupMemberCapReached,
        invitationLimitReached,
        primaryOwnerSelected,
        debouncedQuery,
        isFetching,
        candidates,
        disabledOptionNames: {
            api: apiPrimaryOwnerDisabled ? new Set([PRIMARY_OWNER_ROLE]) : undefined,
            apiProduct: apiProductPrimaryOwnerDisabled ? new Set([PRIMARY_OWNER_ROLE]) : undefined,
        },
        handleRoleChange: (field: RoleField, value: string) => {
            setRoleValues(prev => ({ ...prev, [field]: value }));
            if (field === 'apiRole' || field === 'apiProductRole') {
                setSelected([]);
            }
        },
        handleToggle: (user: SearchableUser) => setSelected(prev => nextSearchableUserSelection(prev, user, primaryOwnerSelected)),
        handleSubmit: () => {
            const roles = buildMembershipRoles(roleValues);
            onSubmit(
                selected.map(user => ({
                    ...(user.id ? { id: user.id } : {}),
                    reference: user.reference,
                    roles,
                })),
            );
        },
        canSubmit: selected.length > 0,
        submitLabel: selected.length > 1 ? `Add ${selected.length} members` : 'Add member',
    };
}
