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
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { authzApiService } from '../api/authz-api.service';
import { authzQueryKeys } from '../api/query-keys';

const CATALOG_SOURCE = 'gravitee-catalog';
const PER_PAGE = 1000;
const MAX_PAGES = 50;

export interface UseImportedCatalogIdsResult {
    readonly catalogIds: ReadonlySet<string>;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    /** True when the tenant has more imported catalog rows than we could fetch (hit MAX_PAGES * PER_PAGE cap). */
    readonly truncated: boolean;
    /** Optimistic push so a freshly-imported item is flagged before the refetch lands. */
    readonly markImported: (catalogIds: readonly string[]) => void;
}

async function fetchAllImportedIds(environmentId: string): Promise<{ ids: string[]; truncated: boolean }> {
    const ids: string[] = [];
    let page = 1;
    let total = 0;
    while (page <= MAX_PAGES) {
        const slice = await authzApiService.listEntities(environmentId, {
            page,
            perPage: PER_PAGE,
            kind: 'RESOURCE',
            source: CATALOG_SOURCE,
        });
        const data = slice.data ?? [];
        for (const entity of data) {
            const value = entity.attributes?._catalogId;
            if (typeof value === 'string' && value.length > 0) ids.push(value);
        }
        total = slice.total;
        if (data.length === 0 || ids.length >= total) break;
        page++;
    }
    return { ids, truncated: total > ids.length };
}

export function useImportedCatalogIds(environmentId: string, options: { enabled?: boolean } = {}): UseImportedCatalogIdsResult {
    const { enabled = true } = options;
    const queryClient = useQueryClient();
    const queryKey = authzQueryKeys.importedCatalogIds(environmentId);

    const query = useQuery({
        queryKey,
        queryFn: () => fetchAllImportedIds(environmentId),
        enabled: enabled && Boolean(environmentId),
        staleTime: 30_000,
    });

    const catalogIds = useMemo<ReadonlySet<string>>(() => new Set(query.data?.ids ?? []), [query.data]);

    const markImported = useCallback(
        (newIds: readonly string[]) => {
            if (newIds.length === 0) return;
            queryClient.setQueryData(queryKey, (current: { ids: string[]; truncated: boolean } | undefined) => {
                if (!current) return current;
                const next = new Set(current.ids);
                newIds.forEach(id => next.add(id));
                return { ...current, ids: Array.from(next) };
            });
        },
        [queryClient, queryKey],
    );

    return {
        catalogIds,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
        truncated: query.data?.truncated ?? false,
        markImported,
    };
}
