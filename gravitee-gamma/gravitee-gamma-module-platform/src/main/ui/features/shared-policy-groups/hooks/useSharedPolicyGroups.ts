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

import { getSharedPolicyGroup, listSharedPolicyGroupsPaged } from '../services/sharedPolicyGroups';
import { sharedPolicyGroupKeys } from '../utils/queryKeys';

export function useSharedPolicyGroupsPaged({
    query,
    page,
    perPage,
    sortBy,
}: {
    query: string;
    page: number;
    perPage: number;
    sortBy?: string;
}) {
    const env = useEnvironment();

    return useQuery({
        queryKey: sharedPolicyGroupKeys.list(env?.id ?? '', query, page, perPage, sortBy),
        queryFn: () => listSharedPolicyGroupsPaged(env!.id, { query, page, perPage, sortBy }),
        enabled: Boolean(env),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}

export function useSharedPolicyGroupDetail(sharedPolicyGroupId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: sharedPolicyGroupKeys.detail(env?.id ?? '', sharedPolicyGroupId ?? ''),
        queryFn: () => getSharedPolicyGroup(env!.id, sharedPolicyGroupId!),
        enabled: Boolean(env) && Boolean(sharedPolicyGroupId),
    });
}
