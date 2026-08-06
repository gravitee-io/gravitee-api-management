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
import type { GroupMembershipRoleScope, OrganizationUserGroup } from '../types/user';

export const GROUP_MEMBERSHIP_TABLE_COLUMNS: ReadonlyArray<{
    scope: GroupMembershipRoleScope;
    label: string;
}> = [
    { scope: 'GROUP', label: 'Group Admin' },
    { scope: 'API', label: 'API Role' },
    { scope: 'API_PRODUCT', label: 'API Product Role' },
    { scope: 'APPLICATION', label: 'Application Role' },
    { scope: 'INTEGRATION', label: 'Integration Role' },
];

export function formatGroupScopeRole(roles: OrganizationUserGroup['roles'], scope: GroupMembershipRoleScope): string | undefined {
    return roles?.[scope] || undefined;
}

export function isGroupAdmin(roles: OrganizationUserGroup['roles']): boolean {
    return roles?.GROUP === 'ADMIN';
}

export function hasAnyGroupScopeRole(roles: OrganizationUserGroup['roles']): boolean {
    if (!roles) {
        return false;
    }
    return GROUP_MEMBERSHIP_TABLE_COLUMNS.some(column => Boolean(roles[column.scope]));
}

export function groupMembershipStatusLabel(group: OrganizationUserGroup): string | undefined {
    if (isGroupAdmin(group.roles)) {
        return 'Group Admin';
    }
    if (hasAnyGroupScopeRole(group.roles)) {
        return 'Member';
    }
    return undefined;
}

export function formatResourceVisibility(visibility: string | undefined): string {
    if (!visibility) {
        return '—';
    }
    return visibility.charAt(0) + visibility.slice(1).toLowerCase();
}
