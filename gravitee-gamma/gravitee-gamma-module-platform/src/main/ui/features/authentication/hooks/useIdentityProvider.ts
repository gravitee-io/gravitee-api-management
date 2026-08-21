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

import { listOrgGroups } from '../../entrypoints/services/groups';
import { orgGroupKeys } from '../../entrypoints/utils/queryKeys';
import { useEnvironmentRoleCatalog, useOrganizationEnvironments, useOrganizationRoleCatalog } from '../../users/hooks/useOrganizationUser';
import { getIdentityProvider } from '../services/identityProviders';
import { authenticationKeys } from '../utils/queryKeys';

export function useIdentityProvider(id: string | undefined) {
    return useQuery({
        queryKey: authenticationKeys.detail(id ?? ''),
        queryFn: () => getIdentityProvider(id!),
        enabled: Boolean(id),
    });
}

export function useIdentityProviderMappingCatalog() {
    const groupsQuery = useQuery({
        queryKey: orgGroupKeys.list(),
        queryFn: listOrgGroups,
    });
    const environmentsQuery = useOrganizationEnvironments();
    const organizationRolesQuery = useOrganizationRoleCatalog();
    const environmentRolesQuery = useEnvironmentRoleCatalog();

    function refetchCatalogs() {
        void Promise.all([
            groupsQuery.refetch(),
            environmentsQuery.refetch(),
            organizationRolesQuery.refetch(),
            environmentRolesQuery.refetch(),
        ]);
    }

    return { groupsQuery, environmentsQuery, organizationRolesQuery, environmentRolesQuery, refetchCatalogs };
}
