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
import { authzCoreApiClient } from './authz-api-client';
import type { AimPagedResponse, CatalogItem, CatalogItemKind, CatalogItemPage } from './aim-catalog.types';

/**
 * AIM catalog client. The AIM module is mounted next to authz under the same
 * Gamma SPI tree, so we reuse the authz core client and just swap the module path.
 */
function aimPath(environmentId: string, suffix: string): string {
    return `/environments/${encodeURIComponent(environmentId)}/modules/aim${suffix}`;
}

function itemsQuery(kind: CatalogItemKind, page: number, perPage: number): string {
    const q = new URLSearchParams();
    q.set('kind', kind);
    q.set('page', String(page));
    q.set('perPage', String(perPage));
    return `?${q.toString()}`;
}

export const aimCatalogService = {
    listItems: <T extends CatalogItem>(
        environmentId: string,
        kind: CatalogItemKind,
        page: number,
        perPage: number,
    ): Promise<CatalogItemPage<T>> =>
        // Backend filters by `kind` server-side; we trust it and cast at the type level.
        authzCoreApiClient
            .get<AimPagedResponse<CatalogItem>>(aimPath(environmentId, `/catalog/items${itemsQuery(kind, page, perPage)}`))
            .then(p => ({
                data: p.data as readonly T[],
                page: p.pagination.page,
                perPage: p.pagination.perPage,
                total: p.pagination.totalCount,
            })),
};
