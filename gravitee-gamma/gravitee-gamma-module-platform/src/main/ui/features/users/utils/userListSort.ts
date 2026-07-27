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
import { formatRoleSummary } from './userDisplay';
import type { TableSortingState } from '../../applications/utils/tableSort';
import { toSortableTimestamp } from '../../applications/utils/tableSort';
import type { OrganizationUser } from '../types/user';

const SORTABLE_COLUMN_IDS = new Set(['user', 'status', 'source', 'roles', 'lastActivity']);

function userDisplayName(user: OrganizationUser): string {
    return (user.displayName ?? user.email ?? user.id).trim();
}

function compareStrings(a: string, b: string): number {
    return a.localeCompare(b, undefined, { sensitivity: 'base' });
}

function sortValue(user: OrganizationUser, columnId: string): string | number {
    switch (columnId) {
        case 'user':
            return userDisplayName(user);
        case 'status':
            return (user.status ?? '').toUpperCase();
        case 'source':
            return (user.source ?? '').toLowerCase();
        case 'roles':
            return formatRoleSummary(user.roles);
        case 'lastActivity':
            return toSortableTimestamp(user.lastConnectionAt);
        default:
            return '';
    }
}

/** Sorts the current page of users client-side; the org users API has no server-side order parameter. */
export function sortOrganizationUsers(users: OrganizationUser[], sorting: TableSortingState): OrganizationUser[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_COLUMN_IDS.has(active.id)) {
        return users;
    }

    const direction = active.desc ? -1 : 1;
    return [...users].sort((left, right) => {
        const leftValue = sortValue(left, active.id);
        const rightValue = sortValue(right, active.id);

        if (typeof leftValue === 'number' && typeof rightValue === 'number') {
            return (leftValue - rightValue) * direction;
        }

        return compareStrings(String(leftValue), String(rightValue)) * direction;
    });
}
