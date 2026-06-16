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

import { searchApiProducts } from '../services/apiProduct';
import type { ApiProductListResponse } from '../types/apiProduct';
import { apiProductKeys } from '../utils/queryKeys';

export function useApiProductList({ query, page, perPage, sortBy }: { query: string; page: number; perPage: number; sortBy?: string }) {
    const env = useEnvironment();
    // Default ordering (no explicit user sort): by name when browsing, relevance when searching.
    const effectiveSortBy = sortBy ?? (query ? undefined : 'name');
    return useQuery<ApiProductListResponse>({
        queryKey: [...apiProductKeys.list(env?.id ?? '', query, page, perPage), effectiveSortBy ?? null],
        queryFn: () => searchApiProducts(env!.id, { query: query || undefined }, page, perPage, effectiveSortBy),
        enabled: Boolean(env),
        placeholderData: keepPreviousData,
    });
}
