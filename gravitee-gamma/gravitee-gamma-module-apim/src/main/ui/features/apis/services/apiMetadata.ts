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

import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiMetadata,
    ApiMetadataListResponse,
    NewApiMetadataPayload,
    SearchApiMetadataParams,
    UpdateApiMetadataPayload,
} from '../types/metadata';

const apiPath = (apiId: string) => `/apis/${encodeURIComponent(apiId)}`;

export async function searchApiMetadata(
    environmentId: string,
    apiId: string,
    params: SearchApiMetadataParams = {},
): Promise<ApiMetadataListResponse> {
    const searchParams = new URLSearchParams({
        page: String(params.page ?? 1),
        perPage: String(params.perPage ?? 10),
    });
    if (params.source) searchParams.set('source', params.source);
    if (params.sortBy) searchParams.set('sortBy', params.sortBy);

    const response = await apimFetchJsonV2<ApiMetadataListResponse>(environmentId, `${apiPath(apiId)}/metadata?${searchParams.toString()}`);
    return { data: response.data ?? [], pagination: response.pagination };
}

export async function createApiMetadata(environmentId: string, apiId: string, payload: NewApiMetadataPayload): Promise<ApiMetadata> {
    return apimFetchJsonV1Env<ApiMetadata>(environmentId, `${apiPath(apiId)}/metadata/`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updateApiMetadata(environmentId: string, apiId: string, payload: UpdateApiMetadataPayload): Promise<ApiMetadata> {
    return apimFetchJsonV1Env<ApiMetadata>(environmentId, `${apiPath(apiId)}/metadata/${encodeURIComponent(payload.key)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

export async function deleteApiMetadata(environmentId: string, apiId: string, metadataKey: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `${apiPath(apiId)}/metadata/${encodeURIComponent(metadataKey)}`, {
        method: 'DELETE',
    });
}
