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
import type { ApiDetailDto, ExposedEntrypoint, HttpListener } from '../types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function getExposedEntrypoints(environmentId: string, apiId: string): Promise<ExposedEntrypoint[]> {
    return apimFetchJsonV2<ExposedEntrypoint[]>(environmentId, `/apis/${encodeURIComponent(apiId)}/exposedEntrypoints`);
}

export async function updateApiListeners(
    environmentId: string,
    apiId: string,
    current: ApiDetailDto,
    listeners: HttpListener[],
): Promise<ApiDetailDto> {
    return apimFetchJsonV2<ApiDetailDto>(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, listeners }),
    });
}
