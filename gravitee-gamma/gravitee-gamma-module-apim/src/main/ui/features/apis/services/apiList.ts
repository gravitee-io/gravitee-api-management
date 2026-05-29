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
 * The gamma APIM module only manages V4 HTTP proxy APIs. This filter is enforced server-side
 * on every search — overriding any caller-supplied `apiTypes` — so neither the list nor any
 * count derived from it can be widened to other API types from the client.
 */
const V4_HTTP_PROXY_API_TYPES = ['V4_HTTP_PROXY'];

export async function searchApis(
    environmentId: string,
    query: ApiSearchQuery,
    page: number,
    perPage: number,
    sortBy?: string,
): Promise<ApiListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage), expands: 'deploymentState' });
    if (sortBy) params.set('sortBy', sortBy);
    const body: ApiSearchQuery = { ...query, apiTypes: [...V4_HTTP_PROXY_API_TYPES] };
    return apimFetchJsonV2<ApiListResponse>(environmentId, `/apis/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}
