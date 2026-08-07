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

import {
    addGroupMembers,
    associateGroupToExisting,
    createGroup,
    deleteGroup,
    inviteGroupMember,
    removeGroupMember,
    updateGroup,
} from '../services/groups';
import type {
    Group,
    GroupInvitationPayload,
    GroupMembershipPayload,
    GroupMembershipType,
    NewGroupPayload,
    UpdateGroupPayload,
} from '../types/group';
import { groupKeys } from '../utils/queryKeys';

function useGroupMutation<TData, TResult>(mutationFn: (envId: string, data: TData) => Promise<TResult>) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: TData) => {
            if (!env?.id) {
                return Promise.reject(new Error('No active environment'));
            }
            return mutationFn(env.id, data);
        },
        onSuccess: () => {
            if (env?.id) {
                void queryClient.invalidateQueries({ queryKey: groupKeys.all });
            }
        },
    });
}

export function useCreateGroup() {
    return useGroupMutation<NewGroupPayload, Group>(createGroup);
}

export function useUpdateGroup() {
    return useGroupMutation<{ groupId: string; data: UpdateGroupPayload }, Group>((envId, { groupId, data }) =>
        updateGroup(envId, groupId, data),
    );
}

export function useDeleteGroup() {
    return useGroupMutation<string, void>(deleteGroup);
}

export function useAddGroupMembers() {
    return useGroupMutation<{ groupId: string; memberships: GroupMembershipPayload[] }, void>((envId, { groupId, memberships }) =>
        addGroupMembers(envId, groupId, memberships),
    );
}

export function useInviteGroupMember() {
    return useGroupMutation<{ groupId: string; data: GroupInvitationPayload }, { ambiguous: boolean }>((envId, { groupId, data }) =>
        inviteGroupMember(envId, groupId, data),
    );
}

export function useRemoveGroupMember() {
    return useGroupMutation<{ groupId: string; memberId: string }, void>((envId, { groupId, memberId }) =>
        removeGroupMember(envId, groupId, memberId),
    );
}

export function useAssociateGroupToExisting() {
    return useGroupMutation<{ groupId: string; type: GroupMembershipType }, Group>((envId, { groupId, type }) =>
        associateGroupToExisting(envId, groupId, type),
    );
}
