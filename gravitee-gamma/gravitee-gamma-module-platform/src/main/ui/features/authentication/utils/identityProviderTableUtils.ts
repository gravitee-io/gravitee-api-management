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

import { identityProviderTypeLabel } from './identityProviderDisplay';
import { toSortableTimestamp } from '../../applications/utils/tableSort';
import type { TableSortingState } from '../../applications/utils/tableSort';
import type { IdentityProviderActivation, IdentityProviderListItem, IdentityProviderRow } from '../types/identityProvider';

const SORTABLE_IDS = new Set(['name', 'id', 'activated', 'type', 'description', 'sync', 'enabled', 'updated_at']);

export function toIdentityProviderRows(
    providers: IdentityProviderListItem[],
    /** Loaded activations; omit (`undefined`) when status is unknown. `[]` means none are activated. */
    activations: IdentityProviderActivation[] | undefined,
): IdentityProviderRow[] {
    if (activations === undefined) {
        return sortIdentityProviderRows(
            providers.map(provider => ({
                ...provider,
                description: provider.description ?? '',
            })),
        );
    }
    const activatedIds = new Set(activations.map(activation => activation.identityProvider));
    return sortIdentityProviderRows(
        providers.map(provider => ({
            ...provider,
            description: provider.description ?? '',
            activated: activatedIds.has(provider.id),
        })),
    );
}

export function sortIdentityProviderRows(rows: IdentityProviderRow[]): IdentityProviderRow[] {
    return [...rows].sort((left, right) => {
        if (left.id === 'gravitee-am') return -1;
        if (right.id === 'gravitee-am') return 1;
        return left.id.localeCompare(right.id);
    });
}

function activationSearchToken(activated: boolean | undefined): string {
    if (activated === true) return 'activated';
    if (activated === false) return 'deactivated';
    return '';
}

function activationSortValue(activated: boolean | undefined): number {
    if (activated === true) return 1;
    if (activated === false) return 0;
    return -1;
}

export function filterIdentityProviders(rows: IdentityProviderRow[], query: string): IdentityProviderRow[] {
    const needle = query.trim().toLowerCase();
    if (needle === '') return rows;
    return rows.filter(row => {
        return (
            row.id.toLowerCase().includes(needle) ||
            row.name.toLowerCase().includes(needle) ||
            (row.description ?? '').toLowerCase().includes(needle) ||
            identityProviderTypeLabel(row.type).toLowerCase().includes(needle) ||
            activationSearchToken(row.activated).startsWith(needle)
        );
    });
}

export function sortFilteredIdentityProviders(rows: IdentityProviderRow[], sorting: TableSortingState): IdentityProviderRow[] {
    const active = sorting[0];
    if (!active?.id || !SORTABLE_IDS.has(active.id)) return rows;
    const direction = active.desc ? -1 : 1;
    return [...rows].sort((left, right) => compareIdentityProviderRows(left, right, active.id) * direction);
}

function compareIdentityProviderRows(left: IdentityProviderRow, right: IdentityProviderRow, columnId: string): number {
    switch (columnId) {
        case 'name':
            return left.name.localeCompare(right.name);
        case 'id':
            return left.id.localeCompare(right.id);
        case 'type':
            return identityProviderTypeLabel(left.type).localeCompare(identityProviderTypeLabel(right.type));
        case 'description':
            return (left.description ?? '').localeCompare(right.description ?? '');
        case 'activated':
            return activationSortValue(left.activated) - activationSortValue(right.activated);
        case 'sync':
            return Number(left.sync) - Number(right.sync);
        case 'enabled':
            return Number(left.enabled) - Number(right.enabled);
        case 'updated_at':
            return toSortableTimestamp(left.updated_at) - toSortableTimestamp(right.updated_at);
        default:
            return 0;
    }
}
