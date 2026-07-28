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
import { formatRoleDisplayName } from './userDisplay';
import type { OrganizationUser, UserRole } from '../types/user';

export function formatUserDisplayName(user: Pick<OrganizationUser, 'displayName' | 'firstname' | 'lastname' | 'email' | 'id'>): string {
    if (user.displayName?.trim()) {
        return user.displayName.trim();
    }
    const parts = [user.firstname, user.lastname].filter((part): part is string => Boolean(part?.trim()));
    if (parts.length > 0) {
        return parts.join(' ');
    }
    return user.email ?? user.id;
}

export function getOrganizationRoles(roles: UserRole[] | undefined): UserRole[] {
    return roles?.filter(role => role.scope === 'ORGANIZATION') ?? [];
}

export function resolveAssignedRoleIds(assignedRoles: UserRole[] | undefined, catalog: { id: string; name?: string }[]): string[] {
    if (!assignedRoles?.length) {
        return [];
    }

    return assignedRoles
        .map(role => {
            if (role.id) {
                return role.id;
            }
            const match = catalog.find(item => item.name === role.name);
            return match?.id;
        })
        .filter((id): id is string => Boolean(id));
}

export function roleLabelsForIds(roleIds: string[], catalog: { id: string; name?: string }[]): string[] {
    return roleIds.map(id => catalog.find(role => role.id === id)?.name ?? id).map(name => formatRoleDisplayName(name));
}

export function formatRoleNames(roles: UserRole[] | undefined): string[] {
    if (!roles?.length) {
        return [];
    }
    return roles
        .map(role => role.name ?? role.id)
        .filter((name): name is string => Boolean(name))
        .map(name => formatRoleDisplayName(name));
}

export function formatUserTimestamp(timestamp: number | undefined): string {
    if (!timestamp) {
        return 'Never';
    }
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(timestamp));
}

export function formatCustomFieldValue(value: unknown): string {
    if (value === null) {
        return '—';
    }
    if (typeof value === 'string') {
        return value;
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }
    return JSON.stringify(value);
}
