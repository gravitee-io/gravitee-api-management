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
import type { TracingApiOption, TracingApiPaginatedLoaderConfig } from '@gravitee/gamma-lib-observability';

import type { ApiListItem, ApiListResponse, ApiSearchQuery } from '../../features/apis/types';
import { apimFetchJsonV2 } from '../../shared/api/apimClient';

export const TRACING_COMPATIBLE_API_TYPES = ['V4_HTTP_PROXY'] as const;

const DEFAULT_PER_PAGE = 20;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

function toTracingApiOption(api: ApiListItem): TracingApiOption {
    return {
        id: api.id,
        label: api.name,
        description: api.apiVersion ? `v${api.apiVersion}` : undefined,
    };
}

function hasNextPage(pagination: ApiListResponse['pagination']): boolean {
    return pagination.page < pagination.pageCount;
}

function buildSearchPath(page: number, perPage = DEFAULT_PER_PAGE): string {
    const params = new URLSearchParams({
        page: String(page),
        perPage: String(perPage),
    });
    return `/apis/_search?${params.toString()}`;
}

function buildSearchBody(search: string, ids?: string[]): ApiSearchQuery {
    return {
        ...(search ? { query: search } : {}),
        ...(ids ? { ids } : {}),
        apiTypes: [...TRACING_COMPATIBLE_API_TYPES],
        statuses: ['STARTED'],
    };
}

/** Paginated loader feeding the global tracing screen API selector. */
export function createTracingApiPaginatedLoader(environmentId: string): TracingApiPaginatedLoaderConfig {
    return {
        load: async ({ search, page, signal }) => {
            const response = await apimFetchJsonV2<ApiListResponse>(environmentId, buildSearchPath(page), {
                method: 'POST',
                headers: JSON_HEADERS,
                body: JSON.stringify(buildSearchBody(search)),
                signal,
            });
            return {
                data: response.data.map(toTracingApiOption),
                hasNextPage: hasNextPage(response.pagination),
            };
        },
        resolveApiLabel: async (id, signal) => {
            const response = await apimFetchJsonV2<ApiListResponse>(environmentId, buildSearchPath(1, 1), {
                method: 'POST',
                headers: JSON_HEADERS,
                body: JSON.stringify(buildSearchBody('', [id])),
                signal,
            });
            const match = response.data[0];
            return match ? toTracingApiOption(match) : null;
        },
    };
}
