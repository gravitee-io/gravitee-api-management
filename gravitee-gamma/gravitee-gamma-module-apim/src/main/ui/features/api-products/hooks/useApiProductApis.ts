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

import type { ApiListResponse } from '../../apis/types';
import { getApiProductApis, searchApisAllowedInProducts } from '../services/apiProduct';
import { apiProductKeys } from '../utils/queryKeys';

export function useApiProductApis(productId: string | undefined, page: number, perPage: number, query?: string) {
    const env = useEnvironment();
    return useQuery<ApiListResponse>({
        queryKey: apiProductKeys.apis(env?.id ?? '', productId ?? '', page, perPage, query),
        queryFn: () => getApiProductApis(env!.id, productId!, page, perPage, query),
        enabled: Boolean(env) && Boolean(productId),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}

export function useApisAvailableForProduct(query: string, page: number, perPage: number) {
    const env = useEnvironment();
    return useQuery<ApiListResponse>({
        queryKey: apiProductKeys.availableApis(env?.id ?? '', query, page, perPage),
        queryFn: () => searchApisAllowedInProducts(env!.id, query, page, perPage),
        enabled: Boolean(env) && query.trim().length > 0,
        staleTime: 15_000,
        placeholderData: keepPreviousData,
    });
}
