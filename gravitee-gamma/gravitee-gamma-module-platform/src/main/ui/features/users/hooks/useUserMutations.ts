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
import type { QueryClient } from '@tanstack/react-query';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { resolveOrganizationId } from '../../../shared/api/apimClient';
import {
    addUserToGroup,
    createOrganizationUser,
    processUserRegistration,
    removeUserFromGroup,
    updateOrganizationUserRoles,
    updateUserGroupMembership,
} from '../services/organizationUsers';
import type { AddUserGroupMembershipPayload, NewPreRegisterUserPayload, UpdateUserRolesPayload } from '../types/user';
import { organizationUserKeys } from '../utils/queryKeys';

function invalidateOrganizationUserGroupMembershipQueries(queryClient: QueryClient, userId: string, environmentId: string): void {
    void queryClient.invalidateQueries({ queryKey: organizationUserKeys.groups(userId, environmentId) });
    void queryClient.invalidateQueries({ queryKey: organizationUserKeys.apis(userId, environmentId) });
    void queryClient.invalidateQueries({ queryKey: organizationUserKeys.apiProducts(userId, environmentId) });
    void queryClient.invalidateQueries({ queryKey: organizationUserKeys.applications(userId, environmentId) });
}

export function useCreateOrganizationUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: NewPreRegisterUserPayload) => createOrganizationUser(payload),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: organizationUserKeys.all });
        },
    });
}

export function useProcessUserRegistration(userId: string | undefined) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (accepted: boolean) => processUserRegistration(userId!, accepted),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: organizationUserKeys.all });
        },
    });
}

export function useUpdateUserGroupMembership(userId: string | undefined, environmentId: string | undefined) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: AddUserGroupMembershipPayload) =>
            updateUserGroupMembership(environmentId!, payload.groupId, userId!, payload),
        onSuccess: () => {
            if (userId && environmentId) {
                invalidateOrganizationUserGroupMembershipQueries(queryClient, userId, environmentId);
            }
        },
    });
}

export function useAddUserToGroup(userId: string | undefined, environmentId: string | undefined) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: AddUserGroupMembershipPayload) => addUserToGroup(environmentId!, payload.groupId, userId!, payload),
        onSuccess: () => {
            if (userId && environmentId) {
                invalidateOrganizationUserGroupMembershipQueries(queryClient, userId, environmentId);
            }
        },
    });
}

export function useRemoveUserFromGroup(userId: string | undefined, environmentId: string | undefined) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (groupId: string) => removeUserFromGroup(environmentId!, groupId, userId!),
        onSuccess: () => {
            if (userId && environmentId) {
                invalidateOrganizationUserGroupMembershipQueries(queryClient, userId, environmentId);
            }
        },
    });
}

export function useUpdateOrganizationUserRoles(userId: string | undefined) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: Omit<UpdateUserRolesPayload, 'referenceId'> & { referenceId?: string }) => {
            const referenceId =
                payload.referenceId ?? (payload.referenceType === 'ORGANIZATION' ? await resolveOrganizationId() : undefined);
            if (!referenceId) {
                throw new Error('Reference id is required to update user roles.');
            }
            await updateOrganizationUserRoles(userId!, {
                referenceType: payload.referenceType,
                referenceId,
                roles: payload.roles,
            });
        },
        onSettled: () => {
            if (userId) {
                void queryClient.invalidateQueries({ queryKey: organizationUserKeys.detail(userId) });
            }
        },
    });
}
