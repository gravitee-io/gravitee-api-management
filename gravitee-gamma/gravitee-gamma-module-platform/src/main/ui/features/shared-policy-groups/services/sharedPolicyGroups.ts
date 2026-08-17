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
import type {
    CreateSharedPolicyGroupPayload,
    SharedPolicyGroup,
    SharedPolicyGroupHistoriesSortByParam,
    SharedPolicyGroupsPagedResponse,
    UpdateSharedPolicyGroupPayload,
} from '../types/sharedPolicyGroup';

export async function listSharedPolicyGroupsPaged(
    environmentId: string,
    params: { query: string; page: number; perPage: number; sortBy?: string },
): Promise<SharedPolicyGroupsPagedResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('perPage', String(params.perPage));
    const query = params.query.trim();
    if (query) {
        searchParams.set('q', query);
    }
    if (params.sortBy) {
        searchParams.set('sortBy', params.sortBy);
    }
    return apimFetchJsonV2<SharedPolicyGroupsPagedResponse>(environmentId, `/shared-policy-groups?${searchParams.toString()}`);
}

export async function getSharedPolicyGroup(environmentId: string, sharedPolicyGroupId: string): Promise<SharedPolicyGroup> {
    return apimFetchJsonV2<SharedPolicyGroup>(environmentId, `/shared-policy-groups/${encodeURIComponent(sharedPolicyGroupId)}`);
}

export async function createSharedPolicyGroup(environmentId: string, data: CreateSharedPolicyGroupPayload): Promise<SharedPolicyGroup> {
    return apimFetchJsonV2<SharedPolicyGroup>(environmentId, '/shared-policy-groups', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateSharedPolicyGroup(
    environmentId: string,
    sharedPolicyGroupId: string,
    data: UpdateSharedPolicyGroupPayload,
): Promise<SharedPolicyGroup> {
    return apimFetchJsonV2<SharedPolicyGroup>(environmentId, `/shared-policy-groups/${encodeURIComponent(sharedPolicyGroupId)}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteSharedPolicyGroup(environmentId: string, sharedPolicyGroupId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/shared-policy-groups/${encodeURIComponent(sharedPolicyGroupId)}`, {
        method: 'DELETE',
    });
}

/** Mirrors classic Console `SharedPolicyGroupsService.listHistories`. */
export async function listSharedPolicyGroupHistories(
    environmentId: string,
    sharedPolicyGroupId: string,
    params: { page: number; perPage: number; sortBy?: SharedPolicyGroupHistoriesSortByParam },
): Promise<SharedPolicyGroupsPagedResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('perPage', String(params.perPage));
    if (params.sortBy) {
        searchParams.set('sortBy', params.sortBy);
    }
    return apimFetchJsonV2<SharedPolicyGroupsPagedResponse>(
        environmentId,
        `/shared-policy-groups/${encodeURIComponent(sharedPolicyGroupId)}/histories?${searchParams.toString()}`,
    );
}
