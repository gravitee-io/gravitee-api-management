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
    GroupInvitationPayload,
    GroupMember,
    GroupMembershipItem,
    GroupMembershipPayload,
    GroupMembershipType,
    GroupRole,
    GroupsPagedResponse,
    NewGroupPayload,
    SearchableUser,
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

/** Unpaged, like classic Console's own group.component.ts — search and pagination for members happens
 *  client-side (the `_paged` endpoint has no server-side search param to filter by). */
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

async function listGroupRolesByScope(scope: 'API' | 'APPLICATION' | 'API_PRODUCT' | 'INTEGRATION' | 'CLUSTER'): Promise<GroupRole[]> {
    return apimFetchJsonOrg<GroupRole[]>(`/configuration/rolescopes/${scope}/roles`);
}

export async function listGroupApiRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('API');
}

export async function listGroupApplicationRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('APPLICATION');
}

export async function listGroupApiProductRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('API_PRODUCT');
}

export async function listGroupIntegrationRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('INTEGRATION');
}

export async function listGroupClusterRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('CLUSTER');
}

/** Org-scoped platform user search — same endpoint Applications' AddMembersSheet uses, no pagination. */
export async function searchUsers(query: string): Promise<SearchableUser[]> {
    return apimFetchJsonOrg<SearchableUser[]>(`/search/users?q=${encodeURIComponent(query)}`);
}

/** Adds or updates memberships (existing platform users) in one call — each item can carry roles across
 *  multiple scopes (API, APPLICATION, API_PRODUCT, INTEGRATION, CLUSTER, GROUP) at once. */
export async function addGroupMembers(environmentId: string, groupId: string, memberships: GroupMembershipPayload[]): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/members`, {
        method: 'POST',
        body: JSON.stringify(memberships),
    });
}

/** Invites a not-yet-registered user by email. Only api_role/application_role are supported here.
 *  The backend returns 200 with the created Invitation when the email is new/unambiguous, or 202 with an
 *  array of matching platform users when more than one existing user shares that email — no invitation is
 *  sent in that case (mirrors classic's `response.status === 202` branch in group.component.ts). Since the
 *  shared fetch client doesn't expose the raw status code, the two cases are told apart by response shape:
 *  the 202 body is an array, the 200 body is a single object. */
export async function inviteGroupMember(
    environmentId: string,
    groupId: string,
    data: GroupInvitationPayload,
): Promise<{ ambiguous: boolean }> {
    const result = await apimFetchJsonV1Env<unknown>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/invitations`, {
        method: 'POST',
        body: JSON.stringify(data),
    });
    return { ambiguous: Array.isArray(result) };
}
