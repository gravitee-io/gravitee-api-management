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

import { apimFetchJsonOrg, apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type {
    Group,
    GroupInvitation,
    GroupInvitationPayload,
    GroupMember,
    GroupMembershipItem,
    GroupMembershipPayload,
    GroupMembershipType,
    GroupRole,
    GroupsPagedResponse,
    InviteGroupMemberResult,
    NewGroupPayload,
    UpdateGroupPayload,
} from '../types/group';

export async function listGroupsPaged(
    environmentId: string,
    params: { query: string; page: number; size: number },
): Promise<GroupsPagedResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('size', String(params.size));
    const query = params.query.trim();
    if (query) {
        searchParams.set('query', query);
    }
    return apimFetchJsonV1Env<GroupsPagedResponse>(environmentId, `/configuration/groups/_paged?${searchParams.toString()}`);
}

export async function getGroup(environmentId: string, groupId: string): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}`);
}

export async function listGroupMembers(environmentId: string, groupId: string): Promise<GroupMember[]> {
    return apimFetchJsonV1Env<GroupMember[]>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/members`);
}

export async function listGroupMemberships(
    environmentId: string,
    groupId: string,
    type: GroupMembershipType,
): Promise<GroupMembershipItem[]> {
    const items = await apimFetchJsonV1Env<GroupMembershipItem[] | undefined>(
        environmentId,
        `/configuration/groups/${encodeURIComponent(groupId)}/memberships?type=${type}`,
    );
    return items ?? [];
}

export async function associateGroupToExisting(environmentId: string, groupId: string, type: GroupMembershipType): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/memberships?type=${type}`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function removeGroupMember(environmentId: string, groupId: string, memberId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(
        environmentId,
        `/configuration/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(memberId)}`,
        { method: 'DELETE' },
    );
}

export async function createGroup(environmentId: string, data: NewGroupPayload): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, '/configuration/groups', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateGroup(environmentId: string, groupId: string, data: UpdateGroupPayload): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteGroup(environmentId: string, groupId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}`, {
        method: 'DELETE',
    });
}

export type GroupRoleScope = 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER' | 'EXPLORER';

export async function listGroupRolesByScope(scope: GroupRoleScope): Promise<GroupRole[]> {
    return apimFetchJsonOrg<GroupRole[]>(`/configuration/rolescopes/${scope}/roles`);
}

export interface EnvironmentPrimaryOwnerSettings {
    api?: { primaryOwnerMode?: string };
    apiProduct?: { primaryOwnerMode?: string };
}

export async function getEnvironmentSettings(environmentId: string): Promise<EnvironmentPrimaryOwnerSettings> {
    return apimFetchJsonV1Env<EnvironmentPrimaryOwnerSettings>(environmentId, '/portal');
}

export async function addGroupMembers(environmentId: string, groupId: string, memberships: GroupMembershipPayload[]): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/members`, {
        method: 'POST',
        body: JSON.stringify(memberships),
    });
}

export async function inviteGroupMember(
    environmentId: string,
    groupId: string,
    data: GroupInvitationPayload,
): Promise<InviteGroupMemberResult> {
    const result = await apimFetchJsonV1Env<unknown>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/invitations`, {
        method: 'POST',
        body: JSON.stringify(data),
    });
    if (Array.isArray(result)) {
        return { outcome: 'ambiguous' };
    }
    if (result === null || result === undefined) {
        return { outcome: 'member-added' };
    }
    if (typeof result === 'object' && 'id' in result && typeof result.id === 'string') {
        return { outcome: 'invitation-created' };
    }
    throw new Error('Unexpected group invitation response');
}

export async function listGroupInvitations(environmentId: string, groupId: string): Promise<GroupInvitation[]> {
    return apimFetchJsonV1Env<GroupInvitation[]>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/invitations`);
}

export async function deleteGroupInvitation(environmentId: string, groupId: string, invitationId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(
        environmentId,
        `/configuration/groups/${encodeURIComponent(groupId)}/invitations/${encodeURIComponent(invitationId)}`,
        { method: 'DELETE' },
    );
}
