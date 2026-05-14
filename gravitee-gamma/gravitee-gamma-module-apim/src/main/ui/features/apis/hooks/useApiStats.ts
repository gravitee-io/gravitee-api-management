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
import { useQuery } from '@tanstack/react-query';

import { searchApis } from '../services/apiList';
import { apiListKeys } from '../utils/queryKeys';

const STATS_PAGE = 1;
const STATS_PER_PAGE = 1;

export interface ApiStats {
    total: number | null;
    private: number | null;
    published: number | null;
    isLoading: boolean;
}

export function useApiStats(query?: string): ApiStats {
    const env = useEnvironment();
    const enabled = Boolean(env);

    const totalQuery = useQuery({
        queryKey: apiListKeys.count(env?.id ?? '', { query }),
        queryFn: () => searchApis(env!.id, { query }, STATS_PAGE, STATS_PER_PAGE),
        enabled,
        staleTime: 60_000,
    });

    const privateQuery = useQuery({
        queryKey: apiListKeys.count(env?.id ?? '', { query, visibilities: ['PRIVATE'] }),
        queryFn: () => searchApis(env!.id, { query, visibilities: ['PRIVATE'] }, STATS_PAGE, STATS_PER_PAGE),
        enabled,
        staleTime: 60_000,
    });

    const publishedQuery = useQuery({
        queryKey: apiListKeys.count(env?.id ?? '', { query, published: ['PUBLISHED'] }),
        queryFn: () => searchApis(env!.id, { query, published: ['PUBLISHED'] }, STATS_PAGE, STATS_PER_PAGE),
        enabled,
        staleTime: 60_000,
    });

    return {
        total: totalQuery.data?.pagination?.totalCount ?? null,
        private: privateQuery.data?.pagination?.totalCount ?? null,
        published: publishedQuery.data?.pagination?.totalCount ?? null,
        isLoading: totalQuery.isLoading || privateQuery.isLoading || publishedQuery.isLoading,
    };
}
