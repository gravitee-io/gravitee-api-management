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
import { useCallback } from 'react';
import { authzApiService, MAX_PER_PAGE, type EntityKindFilter } from '../api/authz-api.service';
import type { EntityResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';

/** Safety cap: MAX_PER_PAGE * MAX_PAGES rows before we stop and report truncation. */
const MAX_PAGES = 50;

export interface UseAllEntitiesFilter {
    readonly kind?: EntityKindFilter;
    readonly entityIdPrefix?: string;
    readonly excludeEntityIdPrefix?: string;
}

export interface UseAllEntitiesResult {
    readonly data: readonly EntityResponse[];
    readonly total: number;
    readonly truncated: boolean;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    readonly reload: () => void;
}

/**
 * Fetch the full entity set for a kind by walking every page (capped), so the
 * page can filter/search/paginate client-side over the complete dataset rather
 * than a single server page. Mirrors the listPolicies fetch-all approach.
 */
export function useAllEntities(environmentId: string, filter: UseAllEntitiesFilter = {}): UseAllEntitiesResult {
    const { kind, entityIdPrefix, excludeEntityIdPrefix } = filter;
    const queryClient = useQueryClient();

    const query = useQuery({
        queryKey: authzQueryKeys.entities.list(environmentId, kind, entityIdPrefix, excludeEntityIdPrefix),
        queryFn: async () => {
            const acc: EntityResponse[] = [];
            let total = 0;
            for (let page = 1; page <= MAX_PAGES; page++) {
                const res = await authzApiService.listEntities(environmentId, {
                    page,
                    perPage: MAX_PER_PAGE,
                    kind,
                    entityIdPrefix,
                    excludeEntityIdPrefix,
                });
                total = res.total;
                const slice = res.data ?? [];
                acc.push(...slice);
                if (slice.length === 0 || acc.length >= total) break;
            }
            return { data: acc, total, truncated: acc.length < total };
        },
        enabled: Boolean(environmentId),
        staleTime: 30_000,
    });

    const reload = useCallback(
        () => void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) }),
        [environmentId, queryClient],
    );

    return {
        data: query.data?.data ?? [],
        total: query.data?.total ?? 0,
        truncated: query.data?.truncated ?? false,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
        reload,
    };
}
