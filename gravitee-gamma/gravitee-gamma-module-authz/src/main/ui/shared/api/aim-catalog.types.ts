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
export type CatalogItemKind = 'mcp-server' | 'model' | 'agent';

export interface ModelDefinition {
    readonly name: string;
    readonly queryName: string;
    readonly provider?: string;
    readonly family?: string;
    readonly contextWindow?: number;
    readonly description?: string;
}

export interface McpServerDefinition {
    readonly serverInfo?: {
        readonly name: string;
        readonly title?: string;
    };
}

export interface McpServerExtensions {
    /** Catalog-side short slug ("filesystem", "github"). When set, the canonical
     * Authorization entityId becomes `mcp.<entityId>` — see ImportFromCatalogDialog. */
    readonly entityId?: string;
    readonly description?: string;
}

export interface AgentDefinition {
    readonly name: string;
    readonly description?: string;
    readonly url?: string;
}

interface CatalogItemBase {
    readonly id: string;
    /**
     * Catalog-side stable slug, server-derived (aim ≥ 1.0.0-alpha.71). The
     * leaf segment only — does NOT include the kind prefix. Older catalog
     * rows may have `null` until the importer touches them again.
     */
    readonly entityId: string | null;
    readonly sourceId: string | null;
    readonly sourceKind: string | null;
    readonly parentId: string | null;
    readonly environmentId: string;
    readonly organizationId: string;
    readonly creationDate: string;
    readonly updateDate: string;
    readonly metadata?: Record<string, string>;
}

export interface ModelCatalogItem extends CatalogItemBase {
    readonly kind: 'model';
    readonly definition: ModelDefinition;
}

export interface McpServerCatalogItem extends CatalogItemBase {
    readonly kind: 'mcp-server';
    readonly definition: McpServerDefinition;
    readonly extensions?: McpServerExtensions;
}

export interface AgentCatalogItem extends CatalogItemBase {
    readonly kind: 'agent';
    readonly definition: AgentDefinition;
}

export type CatalogItem = ModelCatalogItem | McpServerCatalogItem | AgentCatalogItem;

export interface AimPagination {
    readonly page: number;
    readonly perPage: number;
    readonly totalCount: number;
}

export interface AimPagedResponse<T> {
    readonly data: readonly T[];
    readonly pagination: AimPagination;
}

export interface CatalogItemPage<T extends CatalogItem = CatalogItem> {
    readonly data: readonly T[];
    readonly page: number;
    readonly perPage: number;
    readonly total: number;
}
