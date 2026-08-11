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

import {
    getOrganizationUser,
    getOrganizationUserApiProducts,
    getOrganizationUserApis,
    getOrganizationUserApplications,
    getOrganizationUserGroups,
    listEnvironmentGroups,
    listEnvironmentRoles,
    listGroupMembershipRoleCatalog,
    listOrganizationEnvironments,
    listOrganizationRoles,
    listOrganizationUserTokens,
} from '../services/organizationUsers';
import type { GroupMembershipRoleCatalogScope } from '../types/user';
import { USER_DETAIL_FULL_FETCH_SIZE } from '../utils/paginationConstants';
import { organizationUserKeys } from '../utils/queryKeys';

export function useOrganizationUser(userId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.detail(userId ?? ''),
        queryFn: () => getOrganizationUser(userId!),
        enabled: Boolean(userId),
    });
}

export function useOrganizationEnvironments() {
    return useQuery({
        queryKey: organizationUserKeys.environments(),
        queryFn: listOrganizationEnvironments,
    });
}

export function useOrganizationUserGroups(userId: string | undefined, environmentId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.groups(userId ?? '', environmentId),
        queryFn: () => getOrganizationUserGroups(userId!, { environmentId, perPage: USER_DETAIL_FULL_FETCH_SIZE }),
        enabled: Boolean(userId) && Boolean(environmentId),
    });
}

export function useOrganizationUserApis(userId: string | undefined, environmentId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.apis(userId ?? '', environmentId ?? ''),
        queryFn: () =>
            getOrganizationUserApis(userId!, {
                environmentId: environmentId!,
                perPage: USER_DETAIL_FULL_FETCH_SIZE,
            }),
        enabled: Boolean(userId) && Boolean(environmentId),
    });
}

export function useOrganizationUserApiProducts(userId: string | undefined, environmentId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.apiProducts(userId ?? '', environmentId ?? ''),
        queryFn: () =>
            getOrganizationUserApiProducts(userId!, {
                environmentId: environmentId!,
                perPage: USER_DETAIL_FULL_FETCH_SIZE,
            }),
        enabled: Boolean(userId) && Boolean(environmentId),
    });
}

export function useOrganizationUserApplications(userId: string | undefined, environmentId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.applications(userId ?? '', environmentId ?? ''),
        queryFn: () =>
            getOrganizationUserApplications(userId!, {
                environmentId: environmentId!,
                perPage: USER_DETAIL_FULL_FETCH_SIZE,
            }),
        enabled: Boolean(userId) && Boolean(environmentId),
    });
}

export function useEnvironmentGroups(environmentId: string | undefined, enabled = true) {
    return useQuery({
        queryKey: organizationUserKeys.environmentGroups(environmentId ?? ''),
        queryFn: () => listEnvironmentGroups(environmentId!, { perPage: USER_DETAIL_FULL_FETCH_SIZE }),
        enabled: Boolean(environmentId) && enabled,
    });
}

export function useGroupMembershipRoleCatalog(scope: GroupMembershipRoleCatalogScope) {
    return useQuery({
        queryKey: organizationUserKeys.groupMembershipRoles(scope),
        queryFn: () => listGroupMembershipRoleCatalog(scope),
    });
}

export function useOrganizationRoleCatalog() {
    return useQuery({
        queryKey: organizationUserKeys.organizationRoles(),
        queryFn: listOrganizationRoles,
    });
}

export function useEnvironmentRoleCatalog() {
    return useQuery({
        queryKey: organizationUserKeys.environmentRoles(),
        queryFn: listEnvironmentRoles,
    });
}

export function useOrganizationUserTokens(userId: string | undefined) {
    return useQuery({
        queryKey: organizationUserKeys.tokens(userId ?? ''),
        queryFn: () => listOrganizationUserTokens(userId!),
        enabled: Boolean(userId),
    });
}
