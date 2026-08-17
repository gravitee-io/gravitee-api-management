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

import type { TableSortingState } from '../../applications/utils/tableSort';
import { sortToOrder } from '../../applications/utils/tableSort';
import type { SharedPolicyGroupHistoriesSortByParam } from '../types/sharedPolicyGroup';

const HISTORIES_SORT_BY = new Set<SharedPolicyGroupHistoriesSortByParam>([
    'version',
    '-version',
    'updatedAt',
    '-updatedAt',
    'deployedAt',
    '-deployedAt',
]);

export function toSharedPolicyGroupHistoriesSortByParam(sorting: TableSortingState): SharedPolicyGroupHistoriesSortByParam | undefined {
    const order = sortToOrder(sorting);
    if (!order || !HISTORIES_SORT_BY.has(order as SharedPolicyGroupHistoriesSortByParam)) {
        return undefined;
    }
    return order as SharedPolicyGroupHistoriesSortByParam;
}

export function toSharedPolicyGroupHistoryRowId(entry: { version?: number; updatedAt?: string }): string {
    return `${entry.version ?? 0}-${entry.updatedAt ?? ''}`;
}
