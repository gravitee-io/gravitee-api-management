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

import { retryUnlessClientError } from './retryUnlessClientError';
import { getApiSwaggerPagePaths, listApiSwaggerPages } from '../services/apis';
import { apiSwaggerPagesKeys } from '../utils/queryKeys';

export function useApiSwaggerPages(environmentId: string | undefined, apiId: string | undefined, enabled: boolean) {
    return useQuery({
        queryKey: apiSwaggerPagesKeys.list(environmentId ?? '', apiId ?? ''),
        queryFn: () => listApiSwaggerPages(environmentId!, apiId!),
        enabled: enabled && Boolean(environmentId && apiId),
        retry: retryUnlessClientError,
        staleTime: 60_000,
    });
}

export function useApiSwaggerPagePaths(
    environmentId: string | undefined,
    apiId: string | undefined,
    pageId: string | undefined,
    enabled: boolean,
) {
    return useQuery({
        queryKey: apiSwaggerPagesKeys.paths(environmentId ?? '', apiId ?? '', pageId ?? ''),
        queryFn: () => getApiSwaggerPagePaths(environmentId!, apiId!, pageId!),
        enabled: enabled && Boolean(environmentId && apiId && pageId),
        retry: retryUnlessClientError,
        staleTime: 60_000,
    });
}
