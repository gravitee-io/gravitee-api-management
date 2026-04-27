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
import { getEnvironmentV2BaseUrl, type ApimRuntimeConfig } from '../context/apimRuntimeContext';
import { apimFetchJson } from './apimFetch';
import type { ApiV4Dto, CreateApiV4Payload, VerifyPathResponse } from '../dto/types';

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
