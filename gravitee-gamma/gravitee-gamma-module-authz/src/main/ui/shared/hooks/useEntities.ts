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
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { authzApiService, DEFAULT_PER_PAGE, type EntityKindFilter } from '../api/authz-api.service';
import type { EntityResponse, PagedResponse } from '../api/authz-api.types';
import { authzQueryKeys } from '../api/query-keys';

export interface UseEntitiesResult {
    readonly data: PagedResponse<EntityResponse> | null;
    readonly isLoading: boolean;
    readonly error: string | undefined;
    readonly page: number;
    readonly perPage: number;
    readonly setPage: (page: number) => void;
    readonly setPerPage: (perPage: number) => void;
    readonly reload: () => void;
}

export interface UseEntitiesFilter {
    readonly kind?: EntityKindFilter;
    readonly source?: string;
    readonly entityIdPrefix?: string;
    readonly excludeEntityIdPrefix?: string;
    readonly enabled?: boolean;
}

export function useEntities(
    environmentId: string,
    initialPerPage: number = DEFAULT_PER_PAGE,
    filter: UseEntitiesFilter = {},
): UseEntitiesResult {
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(initialPerPage);
    const [lastInitialPerPage, setLastInitialPerPage] = useState(initialPerPage);

    // Adjust perPage during render (React recommended pattern) rather than a
    // secondary useEffect, avoiding the extra render cycle that the effect causes.
    // Reset page too — same empty-slice protection as setPerPageAndResetPage below,
    // otherwise a prop change from perPage=10 to 50 while paginated to page=3
    // would request page=3&perPage=50 and serve an empty slice.
    if (initialPerPage !== lastInitialPerPage) {
        setLastInitialPerPage(initialPerPage);
        setPerPage(initialPerPage);
        setPage(1);
    }

    const { kind, source, entityIdPrefix, excludeEntityIdPrefix, enabled } = filter;
    const queryClient = useQueryClient();

    const query = useQuery({
        queryKey: authzQueryKeys.entities.page(environmentId, page, perPage, kind, source, entityIdPrefix, excludeEntityIdPrefix),
        queryFn: () => authzApiService.listEntities(environmentId, { page, perPage, kind, source, entityIdPrefix, excludeEntityIdPrefix }),
        enabled: Boolean(environmentId) && enabled !== false,
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });

    const reload = useCallback(
        () => void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) }),
        [environmentId, queryClient],
    );

    // Wrap setPerPage so page resets to 1 — otherwise a switch from perPage=10
    // while on page=2 leaves page=2&perPage=50 and the backend serves an empty
    // slice when the new perPage exceeds total rows.
    const setPerPageAndResetPage = useCallback((next: number) => {
        setPerPage(next);
        setPage(1);
    }, []);

    return {
        data: query.data ?? null,
        isLoading: query.isLoading,
        error: query.error instanceof Error ? query.error.message : query.error ? String(query.error) : undefined,
        page,
        perPage,
        setPage,
        setPerPage: setPerPageAndResetPage,
        reload,
    };
}
