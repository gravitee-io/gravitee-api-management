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
import { apimFetchJsonV2, apimFetchJsonV2Org } from '../../../shared/api/apimClient';
import type { ApiResource, ResourcePlugin } from '../types/resource';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Org-scoped resource plugin catalog (shared with the classic console). */
export async function listResourcePlugins(): Promise<ResourcePlugin[]> {
    return apimFetchJsonV2Org<ResourcePlugin[]>('/plugins/resources');
}

/** JSON Schema describing a resource plugin's configuration. */
export async function getResourceSchema(resourceId: string): Promise<Record<string, unknown>> {
    const raw = await apimFetchJsonV2Org<unknown>(`/plugins/resources/${encodeURIComponent(resourceId)}/schema`);
    // The backend may serialize the schema as a JSON string (double-encoded); normalize to an object.
    if (typeof raw === 'string') {
        try {
            return JSON.parse(raw) as Record<string, unknown>;
        } catch (cause) {
            throw new Error(`Malformed JSON schema returned for resource plugin "${resourceId}"`, { cause });
        }
    }
    return (raw ?? {}) as Record<string, unknown>;
}

/**
 * Resources live inside the API definition (`resources[]`); v2 exposes no dedicated
 * sub-resource endpoint, so the only mechanism is GET-then-PUT on the full API.
 */
export async function updateApiResources(environmentId: string, apiId: string, resources: ApiResource[]): Promise<void> {
    const current = await apimFetchJsonV2<Record<string, unknown>>(environmentId, `/apis/${encodeURIComponent(apiId)}`);
    await apimFetchJsonV2(environmentId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...current, resources }),
    });
}
