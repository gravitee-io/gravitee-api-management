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
import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';

import type { GroupRoleScope } from '../../groups/services/groups';
import { groupKeys } from '../../groups/utils/queryKeys';
import { organizationUserKeys } from '../../users/utils/queryKeys';
import { createRole, deleteRole, updateRole } from '../services/roles';
import type { NewRolePayload, Role, RoleScope } from '../types/role';
import { roleKeys } from '../utils/queryKeys';

const GROUP_CATALOG_SCOPES: readonly GroupRoleScope[] = ['API', 'APPLICATION', 'API_PRODUCT', 'INTEGRATION', 'CLUSTER', 'EXPLORER'];

function isGroupCatalogScope(scope: RoleScope): scope is GroupRoleScope {
    return (GROUP_CATALOG_SCOPES as readonly string[]).includes(scope);
}

/**
 * Beyond this feature's own role list, the user detail page's role-assignment dropdowns
 * (organizationUserKeys.organizationRoles()/.environmentRoles()) and the group sheets' role selects
 * (groupKeys.roles(scope)) already read from the same `/configuration/rolescopes/{scope}/roles` endpoint.
 * Invalidating their existing cache keys here — rather than touching those features — is what makes a role
 * change made in this feature show up there without a refresh.
 */
async function invalidateRoleQueries(queryClient: QueryClient, scope: RoleScope, name?: string): Promise<void> {
    await queryClient.invalidateQueries({ queryKey: roleKeys.listByScope(scope) });
    // Without this, RoleFormPage's `initialForm` (captured once via useState from the detail query's data)
    // never refreshes after a successful save, so isDirty stays true and the Save button never re-disables.
    if (name) {
        await queryClient.invalidateQueries({ queryKey: roleKeys.detail(scope, name) });
    }
    if (scope === 'ORGANIZATION') {
        await queryClient.invalidateQueries({ queryKey: organizationUserKeys.organizationRoles() });
        return;
    }
    if (scope === 'ENVIRONMENT') {
        await queryClient.invalidateQueries({ queryKey: organizationUserKeys.environmentRoles() });
        return;
    }
    if (isGroupCatalogScope(scope)) {
        await queryClient.invalidateQueries({ queryKey: groupKeys.roles(scope) });
    }
}

export function useCreateRole() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (role: NewRolePayload) => createRole(role),
        onSuccess: (_createdRole, role) => invalidateRoleQueries(queryClient, role.scope),
    });
}

export function useUpdateRole() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (role: Role) => updateRole(role),
        onSuccess: (_updatedRole, role) => invalidateRoleQueries(queryClient, role.scope, role.name),
    });
}

export interface DeleteRoleInput {
    scope: RoleScope;
    name: string;
}

export function useDeleteRole() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ scope, name }: DeleteRoleInput) => deleteRole(scope, name),
        onSuccess: (_result, { scope, name }) => invalidateRoleQueries(queryClient, scope, name),
    });
}
