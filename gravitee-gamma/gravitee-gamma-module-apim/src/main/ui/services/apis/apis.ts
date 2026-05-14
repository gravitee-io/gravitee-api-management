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
import type {
    ApiDetailDto,
    ApiEventsPage,
    DuplicateApiOptions,
    Cors,
    DynamicPropertyConfig,
    Property,
} from '../../features/apis/types/api';
import { apimFetchJsonV2 } from '../../shared/api/apimClient';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function getApiV4(environmentId: string, apiId: string): Promise<ApiDetailDto> {
    return apimFetchJsonV2<ApiDetailDto>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
}

export async function updateApiShardingTags(environmentId: string, apiId: string, tagIds: string[]): Promise<void> {
    // v2 has no PATCH or dedicated tags endpoint; GET then PUT is the only available mechanism.
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, tags: tagIds }),
    });
}

export async function getApiEvents(
    environmentId: string,
    apiId: string,
    options: { page: number; perPage: number },
): Promise<ApiEventsPage> {
    return apimFetchJsonV2<ApiEventsPage>(
        environmentId,
        `/apis/${encodeURIComponent(apiId)}/events?page=${options.page}&perPage=${options.perPage}&types=PUBLISH_API`,
    );
}

export async function rollbackApi(environmentId: string, apiId: string, eventId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/_rollback`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ eventId }),
    });
}

export async function updateApiGeneral(
    environmentId: string,
    apiId: string,
    current: ApiDetailDto,
    patch: Partial<ApiDetailDto>,
): Promise<ApiDetailDto> {
    return apimFetchJsonV2<ApiDetailDto>(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, ...patch }),
    });
}

export async function startApi(environmentId: string, apiId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/_start`, {
        method: 'POST',
        headers: JSON_HEADERS,
    });
}

export async function stopApi(environmentId: string, apiId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/_stop`, {
        method: 'POST',
        headers: JSON_HEADERS,
    });
}

export async function deleteApi(environmentId: string, apiId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'DELETE',
    });
}

export async function duplicateApi(environmentId: string, apiId: string, options: DuplicateApiOptions): Promise<ApiDetailDto> {
    return apimFetchJsonV2<ApiDetailDto>(environmentId, `/apis/${encodeURIComponent(apiId)}/_duplicate`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(options),
    });
}

export async function exportApiDefinition(environmentId: string, apiId: string): Promise<Blob> {
    const definition = await apimFetchJsonV2<unknown>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    return new Blob([JSON.stringify(definition, null, 2)], { type: 'application/json' });
}

export async function updateApiFromDefinition(environmentId: string, apiId: string, definition: unknown): Promise<ApiDetailDto> {
    return apimFetchJsonV2<ApiDetailDto>(environmentId, `/apis/${encodeURIComponent(apiId)}/definition`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(definition),
    });
}

export async function updateApiPicture(environmentId: string, apiId: string, base64: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/picture`, {
        method: 'PUT',
        headers: { 'Content-Type': 'text/plain' },
        body: base64,
    });
}

export async function deleteApiPicture(environmentId: string, apiId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/picture`, {
        method: 'DELETE',
    });
}

export async function updateApiBackground(environmentId: string, apiId: string, base64: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/background`, {
        method: 'PUT',
        headers: { 'Content-Type': 'text/plain' },
        body: base64,
    });
}

export async function deleteApiBackground(environmentId: string, apiId: string): Promise<void> {
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}/background`, {
        method: 'DELETE',
    });
}

export async function updateApiProperties(environmentId: string, apiId: string, properties: Property[]): Promise<void> {
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, properties }),
    });
}

export async function updateApiCors(environmentId: string, apiId: string, cors: Cors): Promise<void> {
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    const listeners = (current.listeners as Record<string, unknown>[]) ?? [];
    const updatedListeners = listeners.map(l => (l['type'] === 'HTTP' ? { ...l, cors } : l));
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, listeners: updatedListeners.length > 0 ? updatedListeners : current.listeners }),
    });
}

export async function updateDynamicProperties(environmentId: string, apiId: string, config: DynamicPropertyConfig): Promise<void> {
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    const services = (current.services as Record<string, unknown>) ?? {};
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, services: { ...services, dynamicProperty: config } }),
    });
}
