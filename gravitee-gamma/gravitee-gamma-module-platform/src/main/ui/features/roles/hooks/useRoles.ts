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
import { useQueries, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { getPermissionsByScopes, getRole, listRolesByScope } from '../services/roles';
import { ROLE_SCOPES, type Role, type RoleScope } from '../types/role';
import { roleKeys } from '../utils/queryKeys';
import { ROLE_SCOPE_LABELS } from '../utils/roleScopeLabels';

export interface RolesByScopeGroup {
    readonly scope: RoleScope;
    readonly label: string;
    readonly roles: Role[];
    readonly isLoading: boolean;
    readonly isError: boolean;
}

/** Mirrors OrgSettingsRolesComponent.ngOnInit's combineLatest over all nine role scopes. */
export function useRolesByScope(): { groups: RolesByScopeGroup[]; isLoading: boolean } {
    const results = useQueries({
        queries: ROLE_SCOPES.map(scope => ({
            queryKey: roleKeys.listByScope(scope),
            queryFn: () => listRolesByScope(scope),
        })),
    });

    const groups = useMemo<RolesByScopeGroup[]>(
        () =>
            ROLE_SCOPES.map((scope, index) => ({
                scope,
                label: ROLE_SCOPE_LABELS[scope],
                // Only defaults to [] while the query hasn't settled yet; an error is surfaced via isError
                // below instead of being folded into "no roles exist" the way `?? []` alone would.
                roles: results[index]?.data ?? [],
                isLoading: results[index]?.isLoading ?? false,
                isError: results[index]?.isError ?? false,
            })),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [results],
    );

    return { groups, isLoading: results.some(result => result.isLoading) };
}

export function useRole(scope: RoleScope | undefined, roleName: string | undefined) {
    return useQuery({
        queryKey: roleKeys.detail(scope ?? 'ORGANIZATION', roleName ?? ''),
        queryFn: () => getRole(scope!, roleName!),
        enabled: Boolean(scope) && Boolean(roleName),
    });
}

export function usePermissionsByScopes() {
    return useQuery({
        queryKey: roleKeys.permissionsByScopes(),
        queryFn: getPermissionsByScopes,
    });
}
