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
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NewRolePayload, PermissionsByScopes, Role, RoleMembershipListItem, RoleMembershipPayload, RoleScope } from '../types/role';

// Mirrors Classic, which upper-cases the scope before building the URL: callers can hand us a scope derived
// from a route param (e.g. RoleFormPage's `:roleScope`), which isn't guaranteed to already be upper-case.
function normalizeScope(scope: RoleScope): RoleScope {
    return scope.toUpperCase() as RoleScope;
}

// Mirrors Classic's RoleService.list/get, which both map `scope: role.scope.toUpperCase()`: isRoleReadOnly
// compares the server's own `role.scope` against 'ORGANIZATION', so a lowercase value from the response
// would break that check even once the outgoing request itself is normalized above.
function normalizeRoleScope(role: Role): Role {
    return { ...role, scope: role.scope.toUpperCase() as RoleScope };
}

export async function listRolesByScope(scope: RoleScope): Promise<Role[]> {
    const roles = await apimFetchJsonOrg<Role[]>(`/configuration/rolescopes/${normalizeScope(scope)}/roles`);
    return roles.map(normalizeRoleScope);
}

export async function getPermissionsByScopes(): Promise<PermissionsByScopes> {
    return apimFetchJsonOrg<PermissionsByScopes>('/configuration/rolescopes');
}

export async function getRole(scope: RoleScope, roleName: string): Promise<Role> {
    const role = await apimFetchJsonOrg<Role>(`/configuration/rolescopes/${normalizeScope(scope)}/roles/${encodeURIComponent(roleName)}`);
    return normalizeRoleScope(role);
}

export async function createRole(role: NewRolePayload): Promise<Role> {
    return apimFetchJsonOrg<Role>(`/configuration/rolescopes/${normalizeScope(role.scope)}/roles`, {
        method: 'POST',
        body: JSON.stringify(role),
    });
}

export async function updateRole(role: Role): Promise<Role> {
    return apimFetchJsonOrg<Role>(`/configuration/rolescopes/${normalizeScope(role.scope)}/roles/${encodeURIComponent(role.name)}`, {
        method: 'PUT',
        body: JSON.stringify(role),
    });
}

export async function deleteRole(scope: RoleScope, roleName: string): Promise<void> {
    await apimFetchJsonOrg<void>(`/configuration/rolescopes/${normalizeScope(scope)}/roles/${encodeURIComponent(roleName)}`, {
        method: 'DELETE',
    });
}

export async function listRoleMemberships(scope: RoleScope, roleName: string): Promise<RoleMembershipListItem[]> {
    return apimFetchJsonOrg<RoleMembershipListItem[]>(
        `/configuration/rolescopes/${normalizeScope(scope)}/roles/${encodeURIComponent(roleName)}/users`,
    );
}

export async function createRoleMembership(scope: RoleScope, roleName: string, membership: RoleMembershipPayload): Promise<void> {
    await apimFetchJsonOrg<void>(`/configuration/rolescopes/${normalizeScope(scope)}/roles/${encodeURIComponent(roleName)}/users`, {
        method: 'POST',
        body: JSON.stringify(membership),
    });
}

export async function deleteRoleMembership(scope: RoleScope, roleName: string, userId: string): Promise<void> {
    await apimFetchJsonOrg<void>(
        `/configuration/rolescopes/${normalizeScope(scope)}/roles/${encodeURIComponent(roleName)}/users/${encodeURIComponent(userId)}`,
        { method: 'DELETE' },
    );
}
