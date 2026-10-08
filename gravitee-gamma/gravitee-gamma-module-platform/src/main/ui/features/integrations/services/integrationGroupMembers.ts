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
import { normalizeCrudMapRecord } from '@gravitee/gamma-modules-sdk';

import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';
import { getGroupMembers } from '../../shared/services/groupMembers';
import type { GroupMember } from '../../shared/types/groupMembers';

const GROUP_MEMBER_READ_PERMISSION = 'group-member-r';

export type IntegrationGroupMembership = { canViewMembers: false } | { canViewMembers: true; members: GroupMember[] };

async function canReadGroupMembers(environmentId: string, groupId: string): Promise<boolean> {
    const raw = await apimFetchJsonV2<Record<string, string | string[]>>(
        environmentId,
        `/groups/${encodeURIComponent(groupId)}/permissions`,
    );
    return normalizeCrudMapRecord('group', raw).includes(GROUP_MEMBER_READ_PERMISSION);
}

export async function getIntegrationGroupMembership(environmentId: string, groupId: string): Promise<IntegrationGroupMembership> {
    if (!(await canReadGroupMembers(environmentId, groupId))) {
        return { canViewMembers: false };
    }
    try {
        return { canViewMembers: true, members: await getGroupMembers(environmentId, groupId) };
    } catch (error) {
        if (isForbiddenApiError(true, error)) {
            return { canViewMembers: false };
        }
        throw error;
    }
}
