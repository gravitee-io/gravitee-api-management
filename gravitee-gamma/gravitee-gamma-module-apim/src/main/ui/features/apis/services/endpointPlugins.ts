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
import type { JsonSchema } from '@gravitee/graphene-core';

import { apimFetchJsonV2Org } from '../../../shared/api/apimClient';

function normalizeSchema(raw: unknown, pluginId: string): JsonSchema {
    if (typeof raw === 'string') {
        try {
            return JSON.parse(raw) as JsonSchema;
        } catch (cause) {
            throw new Error(`Malformed JSON schema returned for endpoint plugin "${pluginId}"`, { cause });
        }
    }
    return (raw ?? {}) as JsonSchema;
}

/** Endpoint-level configuration JSON Schema (target URL, tcp target, …). */
export async function getEndpointConfigurationSchema(endpointType: string): Promise<JsonSchema> {
    const raw = await apimFetchJsonV2Org<unknown>(`/plugins/endpoints/${encodeURIComponent(endpointType)}/schema`);
    return normalizeSchema(raw, endpointType);
}

/** Group shared-configuration JSON Schema for an endpoint plugin (http-proxy, tcp-proxy, …). */
export async function getEndpointSharedConfigurationSchema(endpointType: string): Promise<JsonSchema> {
    const raw = await apimFetchJsonV2Org<unknown>(`/plugins/endpoints/${encodeURIComponent(endpointType)}/shared-configuration-schema`);
    return normalizeSchema(raw, endpointType);
}
