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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { useFederationEnabled } from '../../license/useFederationEnabled';
import { searchApis } from '../services/apiList';
import type { ApiListResponse } from '../types';
import { apiListKeys } from '../utils/queryKeys';

// `T extends unknown` distributes over the result union, so each member keeps the narrowing that ties
// its `isSuccess`/`isPending` to a defined `data`; a plain Omit would collapse the union and lose it.
type WidenIsLoading<T> = T extends unknown ? Omit<T, 'isLoading'> & { isLoading: boolean } : never;

export type ApiListQueryResult = WidenIsLoading<ReturnType<typeof useQuery<ApiListResponse>>>;

export function useApiList({
    query,
    page,
    perPage,
    sortBy,
}: {
    query: string;
    page: number;
    perPage: number;
    sortBy?: string;
}): ApiListQueryResult {
    const env = useEnvironment();
    const { enabled: includeFederated, isResolved: isFederationResolved } = useFederationEnabled();
    // Default ordering (no explicit user sort): by name when browsing, relevance when searching.
    const effectiveSortBy = sortBy ?? (query ? undefined : 'name');
    const listQuery = useQuery<ApiListResponse>({
        queryKey: [...apiListKeys.search(env?.id ?? '', query, page, perPage, includeFederated), effectiveSortBy ?? null],
        queryFn: () => searchApis(env!.id, { query: query || undefined }, page, perPage, effectiveSortBy, includeFederated),
        // Waiting for the gate keeps the list from showing federation-free rows it would replace milliseconds later.
        enabled: Boolean(env) && isFederationResolved,
        placeholderData: keepPreviousData,
        // The spread below reads every field of react-query's tracked-properties proxy, which would
        // subscribe callers to all of them — including the ones every background refetch touches.
        notifyOnChangeProps: ['data', 'isLoading', 'isPlaceholderData', 'error'],
    });
    // A disabled query reports isLoading false with no data, which callers read as "loaded and empty".
    // While the gate is still resolving the list has not loaded at all, so report that wait as loading.
    return { ...listQuery, isLoading: listQuery.isLoading || !isFederationResolved };
}
