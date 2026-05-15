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
import { useQueries } from '@tanstack/react-query';
import { useMemo } from 'react';

import { getGroupMembers } from '../../apis/services/members';
import type { Group, GroupMembersMap } from '../../apis/types/members.types';
import { apiProductKeys } from '../utils/queryKeys';

export function useApiProductGroupMembers(productId: string | undefined, groups: Group[]) {
    const env = useEnvironment();

    const results = useQueries({
        queries: groups.map(g => ({
            queryKey: apiProductKeys.groupMembers(env?.id ?? '', productId ?? '', g.id),
            queryFn: () => getGroupMembers(env!.id, g.id),
            enabled: Boolean(env && productId),
            staleTime: 30_000,
            retry: false,
        })),
    });

    const isLoading = results.some(r => r.isLoading);

    const groupIds = groups.map(g => g.id).join(',');
    const data = useMemo<GroupMembersMap>(
        () => Object.fromEntries(results.flatMap((r, i) => (r.data?.length ? [[groups[i].name, r.data]] : []))),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [results, groupIds],
    );

    return { data, isLoading };
}
