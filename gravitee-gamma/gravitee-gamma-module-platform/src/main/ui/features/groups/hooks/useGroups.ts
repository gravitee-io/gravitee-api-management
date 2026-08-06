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

import { listGroupsPaged, listOrganizationGroups } from '../services/groups';
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

/** Org-wide — not scoped to (or dependent on) the currently selected environment. */
export function useOrganizationGroups() {
    return useQuery({
        queryKey: groupKeys.organizationGroups(),
        queryFn: listOrganizationGroups,
        staleTime: 30_000,
    });
}
