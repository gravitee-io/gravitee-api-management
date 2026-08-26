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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createRoleMembership, deleteRoleMembership, listRoleMemberships } from '../services/roles';
import type { RoleMembershipPayload, RoleScope } from '../types/role';
import { roleKeys } from '../utils/queryKeys';

export function useRoleMemberships(scope: RoleScope | undefined, roleName: string | undefined) {
    return useQuery({
        queryKey: roleKeys.memberships(scope ?? 'ORGANIZATION', roleName ?? ''),
        queryFn: () => listRoleMemberships(scope!, roleName!),
        enabled: Boolean(scope) && Boolean(roleName),
    });
}

export interface AddRoleMembersInput {
    scope: RoleScope;
    roleName: string;
    users: RoleMembershipPayload[];
}

export interface AddRoleMembersResult {
    succeededCount: number;
    failedCount: number;
}

/**
 * Mirrors OrgSettingsRoleMembersComponent.onAddMemberClicked: one membership call per selected user, in
 * parallel. Unlike Classic's combineLatest (which shares this flaw), a failure here doesn't hide the members
 * that did succeed — Promise.allSettled runs every call to completion and reports counts, so the caller can
 * show "2 of 3 added" instead of a single generic error that silently drops the two that worked.
 */
export function useAddRoleMembers() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ scope, roleName, users }: AddRoleMembersInput): Promise<AddRoleMembersResult> => {
            const outcomes = await Promise.allSettled(users.map(user => createRoleMembership(scope, roleName, user)));
            const failedCount = outcomes.filter(outcome => outcome.status === 'rejected').length;
            return { succeededCount: outcomes.length - failedCount, failedCount };
        },
        onSuccess: (result, { scope, roleName }) => {
            if (result.succeededCount === 0) return;
            return queryClient.invalidateQueries({ queryKey: roleKeys.memberships(scope, roleName) });
        },
    });
}

export interface DeleteRoleMemberInput {
    scope: RoleScope;
    roleName: string;
    userId: string;
}

export function useDeleteRoleMember() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ scope, roleName, userId }: DeleteRoleMemberInput) => deleteRoleMembership(scope, roleName, userId),
        onSuccess: (_result, { scope, roleName }) => queryClient.invalidateQueries({ queryKey: roleKeys.memberships(scope, roleName) }),
    });
}
