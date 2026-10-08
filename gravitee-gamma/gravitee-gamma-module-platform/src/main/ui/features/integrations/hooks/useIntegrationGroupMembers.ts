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
import { useQueries, useQuery, type UseQueryResult } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { retryTransientRequest } from '../../../shared/api/queryRetry';
import { searchEnvironmentGroupsByIds } from '../../shared/services/groupMembers';
import type { EnvironmentGroup, GroupMember } from '../../shared/types/groupMembers';
import { getIntegrationGroupMembership, type IntegrationGroupMembership } from '../services/integrationGroupMembers';
import { integrationKeys } from '../utils/queryKeys';

export type IntegrationGroupMembersView =
    | { group: EnvironmentGroup; status: 'loading' | 'forbidden' | 'error' }
    | { group: EnvironmentGroup; status: 'loaded'; members: GroupMember[] };

function toGroupMembersView(
    group: EnvironmentGroup,
    result: UseQueryResult<IntegrationGroupMembership> | undefined,
): IntegrationGroupMembersView {
    if (!result || result.isPending) return { group, status: 'loading' };
    if (result.isError) return { group, status: 'error' };
    return result.data.canViewMembers ? { group, status: 'loaded', members: result.data.members } : { group, status: 'forbidden' };
}

function useGroupNames(groupIds: string[]) {
    const env = useEnvironment();
    return useQuery({
        queryKey: integrationKeys.groups(env?.id ?? '', groupIds.join(',')),
        queryFn: () => searchEnvironmentGroupsByIds(env!.id, groupIds),
        enabled: Boolean(env) && groupIds.length > 0,
    });
}

export function useIntegrationGroupMembers(integrationId: string, groupIds: string[]) {
    const env = useEnvironment();
    const groupNames = useGroupNames(groupIds);
    const memberships = useQueries({
        queries: groupIds.map(groupId => ({
            queryKey: integrationKeys.groupMembership(env?.id ?? '', integrationId, groupId),
            queryFn: () => getIntegrationGroupMembership(env!.id, groupId),
            enabled: Boolean(env && integrationId),
            retry: retryTransientRequest,
        })),
    });

    const views: IntegrationGroupMembersView[] = groupIds.map((id, index) => {
        const name = groupNames.data?.find(group => group.id === id)?.name ?? id;
        return toGroupMembersView({ id, name }, memberships[index]);
    });
    const isLoading = (groupIds.length > 0 && groupNames.isLoading) || views.some(view => view.status === 'loading');

    return { views, isLoading };
}
