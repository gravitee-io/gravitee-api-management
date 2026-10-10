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

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { listGroups, listGroupsPaged } from '../services/groups';
import { groupKeys } from '../utils/queryKeys';

export function useGroupsPaged({ query, page, size }: { query: string; page: number; size: number }) {
    const env = useEnvironment();

    return useQuery({
        queryKey: groupKeys.list(env?.id ?? '', query, page, size),
        queryFn: () => listGroupsPaged(env!.id, { query, page, size }),
        enabled: Boolean(env),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}

/** All group names in the environment — for create/edit uniqueness (not limited to the current page). */
export function useAllGroupNames(enabled = true) {
    const env = useEnvironment();
    const query = useQuery({
        queryKey: groupKeys.allNames(env?.id ?? ''),
        queryFn: () => listGroups(env!.id),
        enabled: enabled && Boolean(env),
        staleTime: 30_000,
    });
    const names = useMemo(() => (query.data ?? []).map(group => group.name), [query.data]);
    return { names, ...query };
}
