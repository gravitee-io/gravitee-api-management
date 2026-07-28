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
export type UserStatus = 'ACTIVE' | 'PENDING' | 'REJECTED' | 'ARCHIVED';

export interface UserRole {
    id?: string;
    name?: string;
    scope?: 'API' | 'APPLICATION' | 'GROUP' | 'ENVIRONMENT' | 'ORGANIZATION' | 'PLATFORM';
    permissions?: Record<string, string[]>;
}

export interface OrganizationRole {
    id: string;
    name?: string;
    description?: string;
    scope?: string;
    system?: boolean;
}

export type UserRoleReferenceType = 'ORGANIZATION' | 'ENVIRONMENT';

export interface UpdateUserRolesPayload {
    referenceType: UserRoleReferenceType;
    referenceId: string;
    roles: string[];
}

export interface OrganizationUser {
    id: string;
    firstname?: string;
    lastname?: string;
    displayName?: string;
    email?: string;
    roles?: UserRole[];
    envRoles?: Record<string, UserRole[]>;
    source?: string;
    sourceId?: string;
    lastConnectionAt?: number;
    status?: UserStatus | string;
    primary_owner?: boolean;
    number_of_active_tokens?: number;
    isServiceAccount?: boolean;
    hasPassword?: boolean;
    customFields?: Record<string, unknown>;
    created_at?: number;
    updated_at?: number;
}

export interface OrganizationEnvironment {
    id: string;
    name?: string;
    description?: string;
    organizationId?: string;
}

export interface OrganizationUserGroup {
    id: string;
    name?: string;
}

export interface UserPageMeta {
    current: number;
    size: number;
    per_page: number;
    total_pages: number;
    total_elements: number;
}

export interface OrganizationUserListResponse {
    data: OrganizationUser[];
    metadata?: Record<string, Record<string, unknown>>;
    page: UserPageMeta;
}

export interface NewPreRegisterUserPayload {
    firstname?: string | null;
    lastname?: string;
    email?: string;
    source?: string;
    sourceId?: string;
    service?: boolean;
}

export interface IdentityProviderListItem {
    id: string;
    name: string;
}

export type UserType = 'EXTERNAL_USER' | 'SERVICE_ACCOUNT';

export const GRAVITEE_IDP: IdentityProviderListItem = { id: 'gravitee', name: 'Gravitee' };
