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
import type { TableSortingState } from './tableSort';
import type { ApplicationStatus } from '../types/application';

/**
 * Maps human-readable DataTable column ids (also shown in the column-visibility toggle)
 * to the backend `order` fields. Management API `GET /applications/_paged?order=` only
 * supports `name` and `updated_at`.
 */
const SORT_FIELD_BY_COLUMN_ID: Record<string, string> = {
    Name: 'name',
    'Archived at': 'updated_at',
};

export const ACTIVE_APPLICATIONS_DEFAULT_SORT: TableSortingState = [{ id: 'Name', desc: false }];
export const ARCHIVED_APPLICATIONS_DEFAULT_SORT: TableSortingState = [{ id: 'Archived at', desc: true }];

/** Column ids that the backend can sort server-side. */
export const APPLICATION_LIST_SERVER_SORT_IDS = new Set(Object.keys(SORT_FIELD_BY_COLUMN_ID));

/** Convert the active sorting column into the backend `order` value (`field` asc, `-field` desc). */
export function applicationListSortToOrder(sorting: TableSortingState): string | undefined {
    const active = sorting[0];
    const field = active?.id ? SORT_FIELD_BY_COLUMN_ID[active.id] : undefined;
    if (!field) return undefined;
    return active.desc ? `-${field}` : field;
}

export function defaultApplicationListSort(status: ApplicationStatus): TableSortingState {
    return status === 'ARCHIVED' ? ARCHIVED_APPLICATIONS_DEFAULT_SORT : ACTIVE_APPLICATIONS_DEFAULT_SORT;
}

export function defaultApplicationListOrder(status: ApplicationStatus): string {
    return status === 'ARCHIVED' ? '-updated_at' : 'name';
}
