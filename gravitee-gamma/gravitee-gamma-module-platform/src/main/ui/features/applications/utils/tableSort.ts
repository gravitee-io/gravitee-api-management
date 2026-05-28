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
/** TanStack Table sorting state used by Graphene `DataTable`. */
export type TableSortingState = { id: string; desc: boolean }[];

export function sortToOrder(sort: TableSortingState): string | undefined {
    const first = sort[0];
    if (!first?.id) return undefined;
    return first.desc ? `-${first.id}` : first.id;
}

export function orderToSort(order: string | undefined, fallback: TableSortingState): TableSortingState {
    if (!order?.trim()) return fallback;
    return [{ id: order.startsWith('-') ? order.slice(1) : order, desc: order.startsWith('-') }];
}

/** Epoch ms for sorting date-like API fields. */
export function toSortableTimestamp(value: number | string | undefined): number {
    if (value === undefined || value === '') return 0;
    const parsed = typeof value === 'number' ? value : Date.parse(String(value));
    return Number.isFinite(parsed) ? parsed : 0;
}
