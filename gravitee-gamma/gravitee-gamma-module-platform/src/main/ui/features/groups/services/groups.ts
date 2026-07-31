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

import { apimFetchJsonOrg, apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { Group, GroupRole, GroupsPagedResponse, NewGroupPayload, UpdateGroupPayload } from '../types/group';

export async function listGroupsPaged(
    environmentId: string,
    params: { query: string; page: number; size: number },
): Promise<GroupsPagedResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('size', String(params.size));
    const query = params.query.trim();
    if (query) {
        searchParams.set('query', query);
    }
    return apimFetchJsonV1Env<GroupsPagedResponse>(environmentId, `/configuration/groups/_paged?${searchParams.toString()}`);
}

export async function createGroup(environmentId: string, data: NewGroupPayload): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, '/configuration/groups', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateGroup(environmentId: string, groupId: string, data: UpdateGroupPayload): Promise<Group> {
    return apimFetchJsonV1Env<Group>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteGroup(environmentId: string, groupId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/groups/${encodeURIComponent(groupId)}`, {
        method: 'DELETE',
    });
}

async function listGroupRolesByScope(scope: 'API' | 'APPLICATION' | 'API_PRODUCT'): Promise<GroupRole[]> {
    return apimFetchJsonOrg<GroupRole[]>(`/configuration/rolescopes/${scope}/roles`);
}

export async function listGroupApiRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('API');
}

export async function listGroupApplicationRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('APPLICATION');
}

export async function listGroupApiProductRoles(): Promise<GroupRole[]> {
    return listGroupRolesByScope('API_PRODUCT');
}
