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
    listGroupApiProductRoles,
    listGroupApiRoles,
    listGroupApplicationRoles,
    listGroupClusterRoles,
    listGroupIntegrationRoles,
} from '../services/groups';
import type { GroupRole } from '../types/group';
import { groupKeys } from '../utils/queryKeys';

function useGroupRolesQuery(queryKey: readonly unknown[], queryFn: () => Promise<GroupRole[]>, enabled: boolean) {
    return useQuery({
        queryKey,
        queryFn,
        staleTime: 5 * 60_000,
        enabled,
    });
}

export function useGroupApiRoles({ enabled = true }: { enabled?: boolean } = {}) {
    return useGroupRolesQuery(groupKeys.apiRoles(), listGroupApiRoles, enabled);
}

export function useGroupApplicationRoles({ enabled = true }: { enabled?: boolean } = {}) {
    return useGroupRolesQuery(groupKeys.applicationRoles(), listGroupApplicationRoles, enabled);
}

export function useGroupApiProductRoles({ enabled = true }: { enabled?: boolean } = {}) {
    return useGroupRolesQuery(groupKeys.apiProductRoles(), listGroupApiProductRoles, enabled);
}

export function useGroupIntegrationRoles() {
    return useGroupRolesQuery(groupKeys.integrationRoles(), listGroupIntegrationRoles, true);
}

export function useGroupClusterRoles() {
    return useGroupRolesQuery(groupKeys.clusterRoles(), listGroupClusterRoles, true);
}
