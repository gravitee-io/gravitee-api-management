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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { EnvironmentGroup, GroupMember, GroupsPagedResponse } from '../types/groupMembers';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Resolves environment groups by id (console GroupV2Service.listById). */
export async function searchEnvironmentGroupsByIds(environmentId: string, ids: string[]): Promise<EnvironmentGroup[]> {
    if (ids.length === 0) {
        return [];
    }
    const response = await apimFetchJsonV2<GroupsPagedResponse>(environmentId, `/groups/_search?page=1&perPage=${ids.length}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ids }),
    });
    return response.data ?? [];
}

export async function getGroupMembers(environmentId: string, groupId: string): Promise<GroupMember[]> {
    const response = await apimFetchJsonV2<{
        data?: Array<{ id?: string; displayName?: string; roles?: Array<{ name?: string; scope?: string }> }>;
    }>(environmentId, `/groups/${encodeURIComponent(groupId)}/members?page=1&perPage=100`);
    return (response.data ?? []).map(m => ({
        id: m.id ?? '',
        displayName: m.displayName ?? '',
        roles: Object.fromEntries((m.roles ?? []).map(r => [r.scope ?? '', r.name ?? ''])),
    }));
}
