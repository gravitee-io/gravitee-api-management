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
import { useQuery } from '@tanstack/react-query';
import { aimCatalogService } from '../api/aim-catalog.service';
import type { CatalogItem, CatalogItemKind, CatalogItemPage } from '../api/aim-catalog.types';
import { aimQueryKeys } from '../api/query-keys';

const PER_PAGE = 500;
const MAX_PAGES = 20;

export interface UseAimCatalogItemsResult<T extends CatalogItem = CatalogItem> {
    readonly data: CatalogItemPage<T> | null;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    /** True when the catalog has more items than we fetched (hit MAX_PAGES * PER_PAGE cap). */
    readonly truncated: boolean;
}

async function fetchAllPages<T extends CatalogItem>(environmentId: string, kind: CatalogItemKind): Promise<CatalogItemPage<T>> {
    const accumulated: T[] = [];
    let page = 1;
    let total = 0;
    while (page <= MAX_PAGES) {
        const slice = await aimCatalogService.listItems<T>(environmentId, kind, page, PER_PAGE);
        const data = slice.data ?? [];
        accumulated.push(...data);
        total = slice.total;
        if (accumulated.length >= total || data.length === 0) break;
        page++;
    }
    return { data: accumulated, page: 1, perPage: accumulated.length, total };
}

export function useAimCatalogItems<T extends CatalogItem = CatalogItem>(
    environmentId: string,
    kind: CatalogItemKind,
    options: { enabled?: boolean } = {},
): UseAimCatalogItemsResult<T> {
    const { enabled = true } = options;
    const query = useQuery({
        queryKey: aimQueryKeys.catalog.items(environmentId, kind),
        queryFn: () => fetchAllPages<T>(environmentId, kind),
        enabled: enabled && Boolean(environmentId),
        staleTime: 30_000,
    });

    const page = query.data;
    const truncated = page ? page.total > page.data.length : false;

    return {
        data: page ?? null,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
        truncated,
    };
}
