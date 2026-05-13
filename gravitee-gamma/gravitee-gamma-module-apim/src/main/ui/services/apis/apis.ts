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
import type { ApiDetailDto, ApiEventsPage } from '../../features/apis/types/api';
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
