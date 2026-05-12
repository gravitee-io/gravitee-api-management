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
import { getEnvironmentV2BaseUrl, type ApimRuntimeConfig } from '../../../core/context/apimRuntimeContext';
import { apimFetchJson } from '../../../core/http/apimFetch';
import type {
    Member,
    MembersResponse,
    GroupMembersMap,
    ApiRole,
    GroupsResponse,
    TransferOwnershipPayload,
    SearchableUser,
} from '../types/members.types';

function getEnvV1BaseUrl(runtime: ApimRuntimeConfig): string {
    return `${runtime.managementBaseURL}/organizations/${runtime.organizationId}/environments/${runtime.environmentId}`;
}

function getOrgV1BaseUrl(runtime: ApimRuntimeConfig): string {
    return `${runtime.managementBaseURL}/organizations/${runtime.organizationId}`;
}

export async function getApiMembers(runtime: ApimRuntimeConfig, apiId: string): Promise<MembersResponse> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<MembersResponse>(`${base}/apis/${apiId}/members`);
}

export async function updateApiMember(runtime: ApimRuntimeConfig, apiId: string, memberId: string, roleName: string): Promise<Member> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<Member>(`${base}/apis/${apiId}/members/${memberId}`, {
        method: 'PUT',
        body: JSON.stringify({ memberId, roleName }),
    });
}

export async function deleteApiMember(runtime: ApimRuntimeConfig, apiId: string, memberId: string): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<void>(`${base}/apis/${apiId}/members/${memberId}`, { method: 'DELETE' });
}

export async function getApiGroupMembers(runtime: ApimRuntimeConfig, apiId: string): Promise<GroupMembersMap> {
    const base = getEnvV1BaseUrl(runtime);
    return apimFetchJson<GroupMembersMap>(`${base}/apis/${apiId}/groups`);
}

export async function getApiRoles(runtime: ApimRuntimeConfig): Promise<ApiRole[]> {
    const base = getOrgV1BaseUrl(runtime);
    return apimFetchJson<ApiRole[]>(`${base}/configuration/rolescopes/API/roles`);
}

export async function addApiMember(
    runtime: ApimRuntimeConfig,
    apiId: string,
    payload: { userId?: string; externalReference?: string; roleName: string },
): Promise<Member> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<Member>(`${base}/apis/${apiId}/members`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function getGroups(runtime: ApimRuntimeConfig): Promise<GroupsResponse> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<GroupsResponse>(`${base}/groups?page=1&perPage=9999`);
}

export async function transferApiOwnership(runtime: ApimRuntimeConfig, apiId: string, payload: TransferOwnershipPayload): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<void>(`${base}/apis/${apiId}/_transfer-ownership`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updateApiGroups(runtime: ApimRuntimeConfig, apiId: string, groupIds: string[]): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<void>(`${base}/apis/${apiId}/groups`, {
        method: 'PUT',
        body: JSON.stringify(groupIds),
    });
}

export async function searchUsers(runtime: ApimRuntimeConfig, query: string): Promise<SearchableUser[]> {
    const base = getOrgV1BaseUrl(runtime);
    return apimFetchJson<SearchableUser[]>(`${base}/search/users?q=${encodeURIComponent(query)}`);
}

export async function updateApiNotifications(runtime: ApimRuntimeConfig, apiId: string, disable: boolean): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    const current = await apimFetchJson<Record<string, unknown>>(`${base}/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJson(`${base}/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        body: JSON.stringify({ ...current, disableMembershipNotifications: disable }),
    });
}
