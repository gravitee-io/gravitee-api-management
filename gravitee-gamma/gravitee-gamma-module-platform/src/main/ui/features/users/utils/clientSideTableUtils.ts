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

import { paginate, totalPagesFor } from '../../../shared/utils/clientPagination';

export const CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE = 10;

export function clampPage(page: number, totalCount: number, pageSize: number): number {
    return Math.min(Math.max(page, 1), totalPagesFor(totalCount, pageSize));
}

export function filterClientSideTableItems<T extends object>(items: readonly T[], search: string, ignoreKeys: readonly string[] = []): T[] {
    const query = search.trim().toLowerCase();
    if (!query) {
        return [...items];
    }

    const ignored = new Set(ignoreKeys);
    return items.filter(item =>
        Object.entries(item)
            .filter(([key]) => !ignored.has(key))
            .some(([, value]) =>
                String(value ?? '')
                    .toLowerCase()
                    .includes(query),
            ),
    );
}

export const paginateClientSideTableItems = paginate;
