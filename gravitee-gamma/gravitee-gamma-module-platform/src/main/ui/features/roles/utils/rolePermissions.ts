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
import { ROLE_SCOPES, type PermissionEligibleScope, type Role, type RoleRight, type RoleScope } from '../types/role';

/** Guards a raw route param (e.g. `:roleScope`) before treating it as a RoleScope. */
export function isRoleScope(value: string | undefined): value is RoleScope {
    return (ROLE_SCOPES as readonly string[]).includes(value ?? '');
}

export const PERMISSION_ELIGIBLE_SCOPES: readonly PermissionEligibleScope[] = [
    'API',
    'APPLICATION',
    'ENVIRONMENT',
    'ORGANIZATION',
    'INTEGRATION',
    'CLUSTER',
    'API_PRODUCT',
];

export function isPermissionEligibleScope(scope: RoleScope): scope is PermissionEligibleScope {
    return (PERMISSION_ELIGIBLE_SCOPES as readonly string[]).includes(scope);
}

const PERMISSIONS_MOVED_TO_ORGANIZATION_SCOPE = ['TAG', 'TENANT', 'ENTRYPOINT'];

/** Environment-scope permissions that now live under the Organization scope role editor instead. */
export function isPermissionMovedToOrganizationScope(scope: RoleScope, permission: string): boolean {
    return scope === 'ENVIRONMENT' && PERMISSIONS_MOVED_TO_ORGANIZATION_SCOPE.includes(permission);
}

export type RoleRightsForm = Record<RoleRight, boolean>;
export type RolePermissionsForm = Record<string, RoleRightsForm>;

const ROLE_RIGHTS: readonly RoleRight[] = ['C', 'R', 'U', 'D'];

/** Server shape `{ PERMISSION: ['C', 'R'] }` → form shape `{ PERMISSION: { C: true, R: true, U: false, D: false } }`. */
export function toFormPermissions(role: Pick<Role, 'permissions'> | undefined, permissionNames: string[]): RolePermissionsForm {
    return permissionNames.reduce<RolePermissionsForm>((form, permission) => {
        const grantedRights = role?.permissions?.[permission] ?? [];
        form[permission] = ROLE_RIGHTS.reduce<RoleRightsForm>((rights, right) => {
            rights[right] = grantedRights.includes(right);
            return rights;
        }, {} as RoleRightsForm);
        return form;
    }, {});
}

/** Form shape → server shape, keeping only the rights that are checked. */
export function fromFormPermissionsToPermissions(form: RolePermissionsForm): Role['permissions'] {
    return Object.entries(form).reduce<Record<string, RoleRight[]>>((permissions, [permission, rights]) => {
        permissions[permission] = ROLE_RIGHTS.filter(right => rights[right]);
        return permissions;
    }, {});
}

/** Mirrors OrgSettingsRoleComponent.isReadOnly: with system-role edition enabled, only ORGANIZATION/ADMIN stays locked. */
export function isRoleReadOnly(role: Pick<Role, 'scope' | 'name' | 'system'>, systemRoleEditionEnabled: boolean): boolean {
    if (systemRoleEditionEnabled) {
        return role.scope === 'ORGANIZATION' && role.name === 'ADMIN';
    }
    return Boolean(role.system);
}

export function canRoleBeDeleted(role: Pick<Role, 'default' | 'system'>): boolean {
    return !role.default && !role.system;
}
