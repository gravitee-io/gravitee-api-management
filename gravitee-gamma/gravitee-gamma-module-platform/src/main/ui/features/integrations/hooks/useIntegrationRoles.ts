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

import { PRIMARY_OWNER_ROLE } from '../../shared/utils/memberRoles';
import { listIntegrationRoles } from '../services/integrationMembers';
import type { IntegrationRole } from '../types/integrationMembers';
import { integrationKeys } from '../utils/queryKeys';

const integrationRolesQuery = {
    queryKey: integrationKeys.roles(),
    queryFn: listIntegrationRoles,
    staleTime: 60_000,
};

function assignableRoleNames(roles: IntegrationRole[]): string[] {
    return roles.map(role => role.name).filter(name => name !== PRIMARY_OWNER_ROLE);
}

function defaultAssignableRoleName(roles: IntegrationRole[]): string | undefined {
    return roles.find(role => role.default && role.name !== PRIMARY_OWNER_ROLE)?.name;
}

export function useIntegrationRoles({ enabled = true }: { enabled?: boolean } = {}) {
    return useQuery({
        ...integrationRolesQuery,
        enabled,
        select: assignableRoleNames,
    });
}

export function useIntegrationDefaultRole() {
    return useQuery({
        ...integrationRolesQuery,
        select: defaultAssignableRoleName,
    });
}
