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

export const ACTIVE_APPLICATIONS_DEFAULT_SORT: TableSortingState = [{ id: 'name', desc: false }];
export const ARCHIVED_APPLICATIONS_DEFAULT_SORT: TableSortingState = [{ id: 'updated_at', desc: true }];

/** Fields supported by Management API `GET /applications/_paged?order=`. */
export const APPLICATION_LIST_SERVER_SORT_IDS = new Set(['name', 'updated_at']);

export function defaultApplicationListSort(status: ApplicationStatus): TableSortingState {
    return status === 'ARCHIVED' ? ARCHIVED_APPLICATIONS_DEFAULT_SORT : ACTIVE_APPLICATIONS_DEFAULT_SORT;
}

export function defaultApplicationListOrder(status: ApplicationStatus): string {
    return status === 'ARCHIVED' ? '-updated_at' : 'name';
}
