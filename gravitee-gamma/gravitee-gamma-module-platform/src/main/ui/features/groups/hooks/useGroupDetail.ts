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
import { useQuery } from '@tanstack/react-query';

import { getGroup, listGroupMembers, listGroupMemberships } from '../services/groups';
import type { GroupMembershipType } from '../types/group';
import { groupKeys } from '../utils/queryKeys';

export function useGroupDetail(groupId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: groupKeys.detail(env?.id ?? '', groupId ?? ''),
        queryFn: () => getGroup(env!.id, groupId!),
        enabled: Boolean(env && groupId),
    });
}

export function useGroupMembers(groupId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: groupKeys.members(env?.id ?? '', groupId ?? ''),
        queryFn: () => listGroupMembers(env!.id, groupId!),
        enabled: Boolean(env && groupId),
    });
}

function useGroupMemberships(groupId: string | undefined, type: GroupMembershipType) {
    const env = useEnvironment();

    return useQuery({
        queryKey: groupKeys.memberships(env?.id ?? '', groupId ?? '', type),
        queryFn: () => listGroupMemberships(env!.id, groupId!, type),
        enabled: Boolean(env && groupId),
    });
}

export function useGroupApis(groupId: string | undefined) {
    return useGroupMemberships(groupId, 'api');
}

export function useGroupApplications(groupId: string | undefined) {
    return useGroupMemberships(groupId, 'application');
}

export function useGroupApiProducts(groupId: string | undefined) {
    return useGroupMemberships(groupId, 'api_product');
}
