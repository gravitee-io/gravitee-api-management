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

import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { Fetcher } from '../types/apiDocumentation';
import { fetcherLabel, stripLegacyAutoFetchFromSchema } from '../utils/fetcherSchema';

interface FetcherListItem {
    id: string;
    name?: string;
    schema?: string | null;
}

const NO_SETTINGS: JsonSchema = { type: 'object', properties: {} };

/**
 * The installed fetcher plugins, each with its settings form. The list comes from the v1 Management API, the only one
 * exposing it. An import needs a fetcher that can list a folder, which `import=true` keeps.
 */
export async function listFetchers(environmentId: string, options: { forImport?: boolean } = {}): Promise<Fetcher[]> {
    const query = options.forImport ? '?expand=schema&import=true' : '?expand=schema';
    const fetchers = await apimFetchJsonV1Env<FetcherListItem[]>(environmentId, `/fetchers${query}`);
    return fetchers.map(fetcher => ({
        id: fetcher.id,
        name: fetcher.name ?? fetcherLabel(fetcher.id),
        schema: stripLegacyAutoFetchFromSchema(parseSchema(fetcher.schema)),
    }));
}

/**
 * A plugin may ship no schema, or one that is not a JSON object: rather than failing the whole list, it gets no settings
 * form.
 */
function parseSchema(schema: string | null | undefined): JsonSchema {
    if (!schema) return NO_SETTINGS;
    try {
        const parsed: unknown = JSON.parse(schema);
        return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as JsonSchema) : NO_SETTINGS;
    } catch {
        return NO_SETTINGS;
    }
}
