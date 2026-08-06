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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { organizationGroupKeys } from '../../../shared/utils/queryKeys';
import {
    addGroupMembers,
    associateGroupToExisting,
    createGroup,
    deleteGroup,
    deleteGroupInvitation,
    inviteGroupMember,
    removeGroupMember,
    updateGroup,
} from '../services/groups';
import type {
    Group,
    GroupInvitationPayload,
    GroupMembershipPayload,
    GroupMembershipType,
    InviteGroupMemberResult,
    NewGroupPayload,
    UpdateGroupPayload,
} from '../types/group';
import type { RemovalOwnershipTransfer } from '../utils/primaryOwnership';
import { groupKeys } from '../utils/queryKeys';

type InvalidationKeys<TData, TResult> = (environmentId: string, data: TData, result: TResult) => readonly (readonly unknown[])[];

function useGroupMutation<TData, TResult>(
    mutationFn: (envId: string, data: TData) => Promise<TResult>,
    invalidationKeys: InvalidationKeys<TData, TResult> = () => [groupKeys.all],
) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => {
            if (!env?.id) {
                return Promise.reject(new Error('No active environment'));
            }
            return mutationFn(env.id, data);
        },
        onSuccess: (result, data) => {
            if (env?.id) {
                invalidationKeys(env.id, data, result).forEach(queryKey => {
                    void queryClient.invalidateQueries({ queryKey });
                });
            }
        },
    });
}

export function useCreateGroup() {
    return useGroupMutation<NewGroupPayload, Group>(createGroup, () => [groupKeys.all, organizationGroupKeys.all]);
}

export function useUpdateGroup() {
    return useGroupMutation<{ groupId: string; data: UpdateGroupPayload }, Group>(
        (envId, { groupId, data }) => updateGroup(envId, groupId, data),
        () => [groupKeys.all, organizationGroupKeys.all],
    );
}

export function useDeleteGroup() {
    return useGroupMutation<string, void>(deleteGroup, () => [groupKeys.all, organizationGroupKeys.all]);
}

export function useAddGroupMembers() {
    return useGroupMutation<{ groupId: string; memberships: GroupMembershipPayload[] }, void>(
        (envId, { groupId, memberships }) => addGroupMembers(envId, groupId, memberships),
        (envId, { groupId }) => [groupKeys.members(envId, groupId)],
    );
}

export function useInviteGroupMember() {
    return useGroupMutation<{ groupId: string; data: GroupInvitationPayload }, InviteGroupMemberResult>(
        (envId, { groupId, data }) => inviteGroupMember(envId, groupId, data),
        (envId, { groupId }, result) => {
            switch (result.outcome) {
                case 'ambiguous':
                    return [];
                case 'member-added':
                    return [groupKeys.members(envId, groupId)];
                case 'invitation-created':
                    return [groupKeys.invitations(envId, groupId)];
            }
        },
    );
}

export class GroupMemberRemovalError extends Error {
    constructor(
        readonly phase: 'transfer' | 'remove' | 'rollback',
        readonly operationError: unknown,
        readonly rollbackSucceeded = false,
        readonly rollbackError?: unknown,
    ) {
        super(`Group member removal failed during ${phase}`);
        this.name = 'GroupMemberRemovalError';
    }
}

export class GroupMemberUpdateError extends Error {
    constructor(
        readonly phase: 'update' | 'rollback',
        readonly operationError: unknown,
        readonly rollbackSucceeded = false,
        readonly rollbackError?: unknown,
    ) {
        super(`Group member update failed during ${phase}`);
        this.name = 'GroupMemberUpdateError';
    }
}

export function useUpdateGroupMembersWithRollback() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            groupId,
            apply,
            rollback,
        }: {
            groupId: string;
            apply: GroupMembershipPayload[];
            rollback: GroupMembershipPayload[];
        }) => {
            if (!env?.id) {
                throw new Error('No active environment');
            }

            try {
                await addGroupMembers(env.id, groupId, apply);
            } catch (operationError) {
                try {
                    await addGroupMembers(env.id, groupId, rollback);
                } catch (rollbackError) {
                    throw new GroupMemberUpdateError('rollback', operationError, false, rollbackError);
                }
                throw new GroupMemberUpdateError('update', operationError, true);
            }
        },
        onSettled: (_result, _error, data) => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: groupKeys.members(env.id, data.groupId) });
            }
        },
    });
}

export function useRemoveGroupMemberWithOwnershipTransfer() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            groupId,
            memberId,
            ownershipTransfer,
        }: {
            groupId: string;
            memberId: string;
            ownershipTransfer?: RemovalOwnershipTransfer;
        }) => {
            if (!env?.id) {
                throw new Error('No active environment');
            }

            if (ownershipTransfer) {
                try {
                    await addGroupMembers(env.id, groupId, ownershipTransfer.apply);
                } catch (operationError) {
                    try {
                        await addGroupMembers(env.id, groupId, ownershipTransfer.rollback);
                    } catch (rollbackError) {
                        throw new GroupMemberRemovalError('rollback', operationError, false, rollbackError);
                    }
                    throw new GroupMemberRemovalError('transfer', operationError, true);
                }
            }

            try {
                await removeGroupMember(env.id, groupId, memberId);
            } catch (error) {
                if (!ownershipTransfer) {
                    throw new GroupMemberRemovalError('remove', error);
                }

                try {
                    await addGroupMembers(env.id, groupId, ownershipTransfer.rollback);
                } catch (rollbackError) {
                    throw new GroupMemberRemovalError('rollback', error, false, rollbackError);
                }
                throw new GroupMemberRemovalError('remove', error, true);
            }
        },
        onSettled: (_result, _error, data) => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: groupKeys.members(env.id, data.groupId) });
            }
        },
    });
}

export function useDeleteGroupInvitation() {
    return useGroupMutation<{ groupId: string; invitationId: string }, void>(
        (envId, { groupId, invitationId }) => deleteGroupInvitation(envId, groupId, invitationId),
        (envId, { groupId }) => [groupKeys.invitations(envId, groupId)],
    );
}

export function useAssociateGroupToExisting() {
    return useGroupMutation<{ groupId: string; type: GroupMembershipType }, Group>(
        (envId, { groupId, type }) => associateGroupToExisting(envId, groupId, type),
        (envId, { groupId, type }) => [groupKeys.memberships(envId, groupId, type)],
    );
}
