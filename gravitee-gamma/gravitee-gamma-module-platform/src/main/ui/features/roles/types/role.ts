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

export type RoleScope =
    | 'ORGANIZATION'
    | 'ENVIRONMENT'
    | 'API'
    | 'APPLICATION'
    | 'INTEGRATION'
    | 'CLUSTER'
    | 'EXPLORER'
    | 'API_PRODUCT'
    | 'AI_WORKSPACE';

export const ROLE_SCOPES: readonly RoleScope[] = [
    'ORGANIZATION',
    'ENVIRONMENT',
    'API',
    'APPLICATION',
    'INTEGRATION',
    'CLUSTER',
    'EXPLORER',
    'API_PRODUCT',
    'AI_WORKSPACE',
];

/** Scopes the `/configuration/rolescopes` permissions-by-scope endpoint covers. */
export type PermissionEligibleScope = 'API' | 'APPLICATION' | 'ENVIRONMENT' | 'ORGANIZATION' | 'INTEGRATION' | 'CLUSTER' | 'API_PRODUCT';

export type RoleRight = 'C' | 'R' | 'U' | 'D';

export interface Role {
    id?: string;
    name: string;
    description?: string;
    scope: RoleScope;
    system?: boolean;
    default?: boolean;
    permissions?: Record<string, RoleRight[]>;
}

export type NewRolePayload = Omit<Role, 'id'>;
export type UpdateRolePayload = Role;

export type PermissionsByScopes = Partial<Record<PermissionEligibleScope, string[]>>;

export interface RoleMembershipListItem {
    id: string;
    displayName: string;
    role?: string;
}

export interface RoleMembershipPayload {
    id?: string;
    reference: string;
}
