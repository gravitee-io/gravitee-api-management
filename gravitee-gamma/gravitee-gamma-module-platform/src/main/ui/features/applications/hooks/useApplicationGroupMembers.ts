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

import { getGroupMembers } from '../services/applicationMembers';
import type { EnvironmentGroup, GroupMember } from '../types/applicationMembers.types';
import { applicationMemberKeys } from '../utils/queryKeys';

export interface ApplicationGroupMembersView {
    group: EnvironmentGroup;
    members: GroupMember[];
    isLoading: boolean;
    isError: boolean;
}

export function useApplicationGroupMembers(applicationId: string | undefined, groups: EnvironmentGroup[]) {
    const env = useEnvironment();

    const results = useQueries({
        queries: groups.map(g => ({
            queryKey: applicationMemberKeys.groupMembers(env?.id ?? '', applicationId ?? '', g.id),
            queryFn: () => getGroupMembers(env!.id, g.id),
            enabled: Boolean(env && applicationId && g.id),
            staleTime: 30_000,
        })),
    });

    const isLoading = groups.length > 0 && results.some(r => r.isLoading);
    const groupIds = groups.map(g => g.id).join(',');

    const views = useMemo<ApplicationGroupMembersView[]>(
        () =>
            groups.map((group, index) => ({
                group,
                members: results[index]?.data ?? [],
                isLoading: results[index]?.isLoading ?? false,
                isError: results[index]?.isError ?? false,
            })),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [results, groupIds],
    );

    return { views, isLoading };
}
