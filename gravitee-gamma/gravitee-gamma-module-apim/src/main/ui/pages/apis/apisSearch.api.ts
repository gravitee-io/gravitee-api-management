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
import { apimFetchJson } from '../create-proxy/apiCreation/services/apimFetch';
import { getEnvironmentV2BaseUrl, type ApimRuntimeConfig } from '../create-proxy/apiCreation/context/apimRuntimeContext';
import type { ApisSearchResponse, ApisSearchResult, ApiSummary } from './apisSearch.types';

function parseOptionalDate(value: string | undefined): Date | undefined {
    if (!value) return undefined;
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return undefined;
    return d;
}

function parseApiSummary(dto: { readonly id: string; readonly name: string; readonly description?: string; readonly apiVersion?: string; readonly state?: string; readonly deployedAt?: string; readonly updatedAt?: string }): ApiSummary {
    return {
        id: dto.id,
        name: dto.name,
        description: dto.description,
        version: dto.apiVersion,
        state: dto.state,
        deployedAt: parseOptionalDate(dto.deployedAt),
        updatedAt: parseOptionalDate(dto.updatedAt),
    };
}

export async function searchApisV2(
    runtime: ApimRuntimeConfig,
    options: Readonly<{ query: string; page: number; perPage: number; sortBy: string; signal?: AbortSignal }>,
): Promise<ApisSearchResult> {
    const base = getEnvironmentV2BaseUrl(runtime);
    const url = `${base}/apis/_search?page=${options.page}&perPage=${options.perPage}&sortBy=${encodeURIComponent(options.sortBy)}`;

    const res = await apimFetchJson<ApisSearchResponse>(url, {
        method: 'POST',
        body: JSON.stringify({ query: options.query }),
        signal: options.signal,
    });

    return {
        apis: res.data.map(parseApiSummary),
        pagination: res.pagination,
    };
}

