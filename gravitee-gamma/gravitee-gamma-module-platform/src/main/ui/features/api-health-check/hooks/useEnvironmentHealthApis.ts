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

import { searchEnvironmentHealthApis, toEnvironmentHealthApi } from '../services/environmentHealthApis';
import type { EnvironmentHealthApi } from '../types';
import { environmentHealthKeys } from '../utils/queryKeys';

export function useEnvironmentHealthApis({
    query,
    page,
    perPage,
    sortBy,
    reloadToken,
}: {
    query: string;
    page: number;
    perPage: number;
    sortBy?: string;
    reloadToken: number;
}) {
    const env = useEnvironment();
    const result = useQuery({
        queryKey: environmentHealthKeys.search(env?.id ?? '', query, page, perPage, sortBy, reloadToken),
        queryFn: ({ signal }) =>
            searchEnvironmentHealthApis(env!.id, { query: query || undefined, page, perPage, sortBy: sortBy || undefined }, signal),
        enabled: Boolean(env),
        placeholderData: keepPreviousData,
    });

    const apis: EnvironmentHealthApi[] = (result.data?.data ?? []).map(toEnvironmentHealthApi);

    return {
        apis,
        totalCount: result.data?.pagination.totalCount ?? 0,
        isLoading: result.isLoading,
        isFetching: result.isFetching,
        isError: result.isError,
        refetch: result.refetch,
    };
}
