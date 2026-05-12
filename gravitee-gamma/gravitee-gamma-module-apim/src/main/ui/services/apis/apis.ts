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
import { getEnvironmentV2BaseUrl, type ApimRuntimeConfig } from '../../core/context/apimRuntimeContext';
import { apimFetchJson } from '../../core/http/apimFetch';
import type { ApiDetailDto, ApiEventsPage, ApiV4Dto, CreateApiV4Payload, VerifyPathResponse } from '../../features/apis/types/api.types';

export async function createApiV4(runtime: ApimRuntimeConfig, body: CreateApiV4Payload): Promise<ApiV4Dto> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<ApiV4Dto>(`${base}/apis`, {
        method: 'POST',
        body: JSON.stringify(body),
    });
}

export async function startApi(runtime: ApimRuntimeConfig, apiId: string): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    await apimFetchJson<void>(`${base}/apis/${encodeURIComponent(apiId)}/_start`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function getApiV4(runtime: ApimRuntimeConfig, apiId: string): Promise<ApiDetailDto> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<ApiDetailDto>(`${base}/apis/${encodeURIComponent(apiId)}`);
}

export async function verifyPaths(
    runtime: ApimRuntimeConfig,
    apiId: string | undefined,
    paths: Array<{ path: string; host?: string }>,
): Promise<VerifyPathResponse> {
    const base = getEnvironmentV2BaseUrl(runtime);
    return apimFetchJson<VerifyPathResponse>(`${base}/apis/_verify/paths`, {
        method: 'POST',
        body: JSON.stringify({
            ...(apiId ? { apiId } : {}),
            paths,
        }),
    });
}

export async function updateApiShardingTags(runtime: ApimRuntimeConfig, apiId: string, tagIds: string[]): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    const current = await apimFetchJson<Record<string, unknown>>(`${base}/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJson(`${base}/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        body: JSON.stringify({ ...current, tags: tagIds }),
    });
}

export async function getApiEvents(
    runtime: ApimRuntimeConfig,
    apiId: string,
    options: { page: number; perPage: number },
): Promise<ApiEventsPage> {
    const base = getEnvironmentV2BaseUrl(runtime);
    const url = `${base}/apis/${encodeURIComponent(apiId)}/events?page=${options.page}&perPage=${options.perPage}&types=PUBLISH_API`;
    return apimFetchJson<ApiEventsPage>(url);
}

export async function rollbackApi(runtime: ApimRuntimeConfig, apiId: string, eventId: string): Promise<void> {
    const base = getEnvironmentV2BaseUrl(runtime);
    await apimFetchJson(`${base}/apis/${encodeURIComponent(apiId)}/_rollback`, {
        method: 'POST',
        body: JSON.stringify({ eventId }),
    });
}
