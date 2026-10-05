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
import type { ApiListResponse, ApiSearchQuery } from '../types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/**
 * This page lists V4 HTTP and TCP proxies, plus `FEDERATED` when the federation gate is on.
 * A selected type narrows that set. A type outside it is dropped, so the client cannot widen
 * the search. A selection that matches none of the set keeps the full set: an empty
 * intersection must not be sent as "no type filter", which the backend would read as every API.
 */
const V4_PROXY_API_TYPES = ['V4_HTTP_PROXY', 'V4_TCP_PROXY'];
const FEDERATED_API_TYPE = 'FEDERATED';

function moduleApiTypes(includeFederated: boolean): string[] {
    return includeFederated ? [...V4_PROXY_API_TYPES, FEDERATED_API_TYPE] : [...V4_PROXY_API_TYPES];
}

/**
 * Resolves `apiTypes` for this page's `_search` body.
 * With no type filter, the module default set is used. With a filter, only allowed types are kept; if every
 * requested type is outside the module set (stale URL), an empty array is returned so results are not widened.
 * UI/URL state should be sanitized earlier via {@link sanitizeApiListFilters}.
 */
export function resolveSearchApiTypes(requested: readonly string[] | undefined, includeFederated: boolean): string[] {
    const allowed = moduleApiTypes(includeFederated);
    if (!requested?.length) {
        return allowed;
    }
    return requested.filter(type => allowed.includes(type));
}

export function definedSearchList(values: readonly string[] | undefined): string[] | undefined {
    return values && values.length > 0 ? [...values] : undefined;
}

export async function searchApis(
    environmentId: string,
    query: ApiSearchQuery,
    page: number,
    perPage: number,
    sortBy?: string,
    includeFederated = false,
): Promise<ApiListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage), expands: 'deploymentState' });
    if (sortBy) params.set('sortBy', sortBy);
    const body: ApiSearchQuery = {
        query: query.query || undefined,
        ids: definedSearchList(query.ids),
        statuses: definedSearchList(query.statuses),
        tags: definedSearchList(query.tags),
        categories: definedSearchList(query.categories),
        published: definedSearchList(query.published),
        visibilities: definedSearchList(query.visibilities),
        apiTypes: resolveSearchApiTypes(query.apiTypes, includeFederated),
    };
    return apimFetchJsonV2<ApiListResponse>(environmentId, `/apis/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}
