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
export const authzQueryKeys = {
    all: ['authz'] as const,
    entities: {
        all: (environmentId: string) => ['authz', 'entities', environmentId] as const,
        list: (environmentId: string, kind?: string, entityIdPrefix?: string, excludeEntityIdPrefix?: string) =>
            ['authz', 'entities', environmentId, 'list', kind, entityIdPrefix, excludeEntityIdPrefix] as const,
        page: (
            environmentId: string,
            page: number,
            perPage: number,
            kind?: string,
            source?: string,
            entityIdPrefix?: string,
            excludeEntityIdPrefix?: string,
        ) => ['authz', 'entities', environmentId, page, perPage, kind, source, entityIdPrefix, excludeEntityIdPrefix] as const,
    },
    policies: {
        all: (environmentId: string) => ['authz', 'policies', environmentId] as const,
        page: (environmentId: string, page: number, perPage: number, type?: string, status?: string) =>
            ['authz', 'policies', environmentId, page, perPage, type, status] as const,
    },
    schema: (environmentId: string) => ['authz', 'schema', environmentId] as const,
    entityOptions: (environmentId: string) => ['authz', 'entity-options', environmentId] as const,
    importedCatalogIds: (environmentId: string) => ['authz', 'imported-catalog-ids', environmentId] as const,
} as const;

export const aimQueryKeys = {
    catalog: {
        items: (environmentId: string, kind: string) => ['aim', 'catalog', 'items', environmentId, kind] as const,
    },
} as const;
