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
import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2, apimFetchJsonV2Org } from '../../../shared/api/apimClient';
import type {
    AddUserGroupMembershipPayload,
    EnvironmentGroupsListResponse,
    GroupMemberRolePayload,
    GroupMembershipRoleCatalogScope,
    IdentityProviderListItem,
    NewPreRegisterUserPayload,
    OrganizationEnvironment,
    OrganizationRole,
    OrganizationUser,
    OrganizationUserListResponse,
    UpdateUserRolesPayload,
    UserGroupsListResponse,
    UserInheritedApi,
    UserInheritedApiProduct,
    UserInheritedApplication,
    UserInheritedResourcesListResponse,
} from '../types/user';
import { GROUP_MEMBERSHIP_FETCH_SIZE } from '../utils/userInheritedResources';

function buildGroupMemberRoles(payload: AddUserGroupMembershipPayload): GroupMemberRolePayload[] {
    const roles: GroupMemberRolePayload[] = [];

    if (payload.isGroupAdmin) {
        roles.push({ scope: 'GROUP', name: 'ADMIN' });
    }
    if (payload.apiRole) {
        roles.push({ scope: 'API', name: payload.apiRole });
    }
    if (payload.apiProductRole) {
        roles.push({ scope: 'API_PRODUCT', name: payload.apiProductRole });
    }
    if (payload.applicationRole) {
        roles.push({ scope: 'APPLICATION', name: payload.applicationRole });
    }
    if (payload.integrationRole) {
        roles.push({ scope: 'INTEGRATION', name: payload.integrationRole });
    }

    return roles;
}

function buildGroupMemberRolesForUpsert(payload: AddUserGroupMembershipPayload): GroupMemberRolePayload[] {
    return [
        { scope: 'GROUP', name: payload.isGroupAdmin ? 'ADMIN' : '' },
        { scope: 'API', name: payload.apiRole ?? '' },
        { scope: 'API_PRODUCT', name: payload.apiProductRole ?? '' },
        { scope: 'APPLICATION', name: payload.applicationRole ?? '' },
        { scope: 'INTEGRATION', name: payload.integrationRole ?? '' },
    ];
}

export async function listOrganizationUsers(params: { query: string; page: number; size: number }): Promise<OrganizationUserListResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('size', String(params.size));
    // Sent verbatim: the backend truncates at the first '@' so full email addresses still match.
    const q = params.query.trim();
    if (q) {
        searchParams.set('q', q);
    }
    return apimFetchJsonOrg<OrganizationUserListResponse>(`/users?${searchParams.toString()}`);
}

export async function createOrganizationUser(payload: NewPreRegisterUserPayload): Promise<OrganizationUser> {
    return apimFetchJsonOrg<OrganizationUser>('/users', { method: 'POST', body: JSON.stringify(payload) });
}

export async function listIdentityProviders(): Promise<IdentityProviderListItem[]> {
    return apimFetchJsonOrg<IdentityProviderListItem[]>('/configuration/identities');
}

export async function getOrganizationUser(userId: string): Promise<OrganizationUser> {
    return apimFetchJsonOrg<OrganizationUser>(`/users/${encodeURIComponent(userId)}`);
}

export async function listOrganizationEnvironments(): Promise<OrganizationEnvironment[]> {
    return apimFetchJsonOrg<OrganizationEnvironment[]>('/environments');
}

export async function getOrganizationUserGroups(
    userId: string,
    params: { environmentId?: string; page?: number; perPage?: number } = {},
): Promise<UserGroupsListResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page ?? 1));
    searchParams.set('perPage', String(params.perPage ?? GROUP_MEMBERSHIP_FETCH_SIZE));
    if (params.environmentId) {
        searchParams.set('environmentId', params.environmentId);
    }
    return apimFetchJsonV2Org<UserGroupsListResponse>(`/users/${encodeURIComponent(userId)}/groups?${searchParams.toString()}`);
}

export async function getOrganizationUserApis(
    userId: string,
    params: { environmentId: string; page?: number; perPage?: number },
): Promise<UserInheritedResourcesListResponse<UserInheritedApi>> {
    const searchParams = new URLSearchParams();
    searchParams.set('environmentId', params.environmentId);
    searchParams.set('page', String(params.page ?? 1));
    searchParams.set('perPage', String(params.perPage ?? GROUP_MEMBERSHIP_FETCH_SIZE));
    return apimFetchJsonV2Org<UserInheritedResourcesListResponse<UserInheritedApi>>(
        `/users/${encodeURIComponent(userId)}/apis?${searchParams.toString()}`,
    );
}

export async function getOrganizationUserApiProducts(
    userId: string,
    params: { environmentId: string; page?: number; perPage?: number },
): Promise<UserInheritedResourcesListResponse<UserInheritedApiProduct>> {
    const searchParams = new URLSearchParams();
    searchParams.set('environmentId', params.environmentId);
    searchParams.set('page', String(params.page ?? 1));
    searchParams.set('perPage', String(params.perPage ?? GROUP_MEMBERSHIP_FETCH_SIZE));
    return apimFetchJsonV2Org<UserInheritedResourcesListResponse<UserInheritedApiProduct>>(
        `/users/${encodeURIComponent(userId)}/api-products?${searchParams.toString()}`,
    );
}

export async function getOrganizationUserApplications(
    userId: string,
    params: { environmentId: string; page?: number; perPage?: number },
): Promise<UserInheritedResourcesListResponse<UserInheritedApplication>> {
    const searchParams = new URLSearchParams();
    searchParams.set('environmentId', params.environmentId);
    searchParams.set('page', String(params.page ?? 1));
    searchParams.set('perPage', String(params.perPage ?? GROUP_MEMBERSHIP_FETCH_SIZE));
    return apimFetchJsonV2Org<UserInheritedResourcesListResponse<UserInheritedApplication>>(
        `/users/${encodeURIComponent(userId)}/applications?${searchParams.toString()}`,
    );
}

export async function listEnvironmentGroups(
    environmentId: string,
    params: { page?: number; perPage?: number } = {},
): Promise<EnvironmentGroupsListResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page ?? 1));
    searchParams.set('perPage', String(params.perPage ?? GROUP_MEMBERSHIP_FETCH_SIZE));
    return apimFetchJsonV2<EnvironmentGroupsListResponse>(environmentId, `/groups?${searchParams.toString()}`);
}

export async function listGroupMembershipRoleCatalog(scope: GroupMembershipRoleCatalogScope): Promise<OrganizationRole[]> {
    return apimFetchJsonOrg<OrganizationRole[]>(`/configuration/rolescopes/${scope}/roles`);
}

export async function updateUserGroupMembership(
    environmentId: string,
    groupId: string,
    userId: string,
    payload: Omit<AddUserGroupMembershipPayload, 'groupId'>,
): Promise<void> {
    const roles = buildGroupMemberRolesForUpsert({ ...payload, groupId });
    await apimFetchJsonV1Env(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/members`, {
        method: 'POST',
        body: JSON.stringify([{ id: userId, roles }]),
    });
}

export async function addUserToGroup(
    environmentId: string,
    groupId: string,
    userId: string,
    payload: Omit<AddUserGroupMembershipPayload, 'groupId'>,
): Promise<void> {
    const roles = buildGroupMemberRoles({ ...payload, groupId });
    if (roles.length === 0) {
        throw new Error('At least one group membership role is required.');
    }
    await apimFetchJsonV1Env(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}/members`, {
        method: 'POST',
        body: JSON.stringify([{ id: userId, roles }]),
    });
}

export async function removeUserFromGroup(environmentId: string, groupId: string, userId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(
        environmentId,
        `/configuration/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(userId)}`,
        { method: 'DELETE' },
    );
}

export async function processUserRegistration(userId: string, accepted: boolean): Promise<void> {
    await apimFetchJsonOrg<void>(`/users/${encodeURIComponent(userId)}/_process`, {
        method: 'POST',
        body: JSON.stringify(accepted),
    });
}

export async function updateOrganizationUserServiceAccount(userId: string, serviceAccount: boolean): Promise<void> {
    await apimFetchJsonOrg<void>(`/users/${encodeURIComponent(userId)}/serviceAccount`, {
        method: 'PATCH',
        body: JSON.stringify({ serviceAccount }),
    });
}

export async function deleteOrganizationUser(userId: string): Promise<void> {
    await apimFetchJsonOrg<void>(`/users/${encodeURIComponent(userId)}`, {
        method: 'DELETE',
    });
}

export async function listOrganizationRoles(): Promise<OrganizationRole[]> {
    return apimFetchJsonOrg<OrganizationRole[]>('/configuration/rolescopes/ORGANIZATION/roles');
}

export async function listEnvironmentRoles(): Promise<OrganizationRole[]> {
    return apimFetchJsonOrg<OrganizationRole[]>('/configuration/rolescopes/ENVIRONMENT/roles');
}

export async function updateOrganizationUserRoles(userId: string, payload: UpdateUserRolesPayload): Promise<void> {
    await apimFetchJsonOrg<void>(`/users/${encodeURIComponent(userId)}/roles`, {
        method: 'PUT',
        body: JSON.stringify({
            user: userId,
            referenceType: payload.referenceType,
            referenceId: payload.referenceId,
            roles: payload.roles,
        }),
    });
}
