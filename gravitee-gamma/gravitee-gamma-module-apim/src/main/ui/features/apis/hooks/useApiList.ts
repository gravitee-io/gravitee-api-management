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

import { searchApis } from '../services/apiList';
import type { ApiListResponse } from '../types';
import { apiListKeys } from '../utils/queryKeys';

export function useApiList({
    query,
    page,
    perPage,
}: {
    query: string;
    page: number;
    perPage: number;
}): ReturnType<typeof useQuery<ApiListResponse>> {
    const env = useEnvironment();
    return useQuery<ApiListResponse>({
        queryKey: apiListKeys.search(env?.id ?? '', query, page, perPage),
        queryFn: () => searchApis(env!.id, { query: query || undefined }, page, perPage, query ? undefined : 'name'),
        enabled: Boolean(env),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}
