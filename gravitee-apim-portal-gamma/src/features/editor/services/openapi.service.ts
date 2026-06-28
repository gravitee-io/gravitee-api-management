/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { DETAILED_DUMMY_OPENAPI_SPEC } from '../../portals/storage/dummy-openapi-spec';
import type { OpenApiSpec } from '../entities/openapi';
import { parseOpenApiContent } from '../utils/parse-openapi-spec';

/** Default inline OpenAPI document for demos, seeds, and editor fixtures. */
export const PETSTORE_OPENAPI_SPEC = DETAILED_DUMMY_OPENAPI_SPEC;

/** Default spec seeded into newly created OpenAPI pages. */
export const DEFAULT_OPENAPI_PAGE_SPEC = PETSTORE_OPENAPI_SPEC;

const API_SPECS: Record<string, string> = {
    'api-payments': PETSTORE_OPENAPI_SPEC,
    'api-accounts': PETSTORE_OPENAPI_SPEC,
    'api-notifications': PETSTORE_OPENAPI_SPEC,
    'api-identity': PETSTORE_OPENAPI_SPEC,
    'api-analytics': PETSTORE_OPENAPI_SPEC,
};

const URL_SPECS: Record<string, string> = {
    'https://petstore.swagger.io/v2/swagger.json': PETSTORE_OPENAPI_SPEC,
};

function toOpenApiSpec(content: string): OpenApiSpec {
    const validation = parseOpenApiContent(content);
    return {
        content,
        metadata: validation.metadata ?? {
            title: 'Unknown API',
            version: 'unknown',
            pathsCount: 0,
        },
    };
}

export async function getOpenApiSpec(apiId: string): Promise<OpenApiSpec> {
    await delay(200);
    const content = API_SPECS[apiId] ?? PETSTORE_OPENAPI_SPEC;
    return toOpenApiSpec(content);
}

export async function fetchOpenApiSpecFromUrl(url: string): Promise<OpenApiSpec> {
    await delay(500);
    const content = URL_SPECS[url] ?? PETSTORE_OPENAPI_SPEC;
    return toOpenApiSpec(content);
}

function delay(ms: number): Promise<void> {
    return new Promise(resolve => {
        setTimeout(resolve, ms);
    });
}
