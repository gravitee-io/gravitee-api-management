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
import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiRole,
    GroupMembersMap,
    GroupsResponse,
    Member,
    MembersResponse,
    SearchableUser,
    TransferOwnershipPayload,
} from '../types/members.types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function getApiMembers(environmentId: string, apiId: string): Promise<MembersResponse> {
    return apimFetchJsonV2<MembersResponse>(environmentId, `/apis/${apiId}/members`);
}

export async function addApiMember(
    environmentId: string,
    apiId: string,
    payload: { userId?: string; externalReference?: string; roleName: string },
): Promise<Member> {
    return apimFetchJsonV2<Member>(environmentId, `/apis/${apiId}/members`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(payload),
    });
}

export async function updateApiMember(environmentId: string, apiId: string, memberId: string, roleName: string): Promise<Member> {
    return apimFetchJsonV2<Member>(environmentId, `/apis/${apiId}/members/${memberId}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ memberId, roleName }),
    });
}

export async function deleteApiMember(environmentId: string, apiId: string, memberId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/apis/${apiId}/members/${memberId}`, { method: 'DELETE' });
}

export async function transferApiOwnership(environmentId: string, apiId: string, payload: TransferOwnershipPayload): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/apis/${apiId}/_transfer-ownership`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(payload),
    });
}

export async function updateApiGroups(environmentId: string, apiId: string, groupIds: string[]): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/apis/${apiId}/groups`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(groupIds),
    });
}

export async function updateApiNotifications(environmentId: string, apiId: string, disable: boolean): Promise<void> {
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, disableMembershipNotifications: disable }),
    });
}

export async function getApiGroupMembers(environmentId: string, apiId: string): Promise<GroupMembersMap> {
    return apimFetchJsonV1Env<GroupMembersMap>(environmentId, `/apis/${apiId}/groups`);
}

export async function getApiRoles(): Promise<ApiRole[]> {
    return apimFetchJsonOrg<ApiRole[]>(`/configuration/rolescopes/API/roles`);
}

export async function getGroups(environmentId: string): Promise<GroupsResponse> {
    return apimFetchJsonV2<GroupsResponse>(environmentId, `/groups?page=1&perPage=9999`);
}

export async function searchUsers(query: string): Promise<SearchableUser[]> {
    return apimFetchJsonOrg<SearchableUser[]>(`/search/users?q=${encodeURIComponent(query)}`);
}
