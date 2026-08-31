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
import { DEFAULT_CATALOG_PAGE_SIZE } from '../../api/catalog';

export type CatalogView = 'grid' | 'list';

export interface CatalogParams {
    query: string;
    category: string;
    protocol: string;
    label: string;
    view: CatalogView;
    page: number;
    pageSize: number;
}

export function parseCatalogSearchParams(searchParams: URLSearchParams): CatalogParams {
    const page = Number.parseInt(searchParams.get('page') ?? '1', 10);
    const pageSize = Number.parseInt(searchParams.get('size') ?? String(DEFAULT_CATALOG_PAGE_SIZE), 10);
    return {
        query: searchParams.get('query') ?? '',
        category: searchParams.get('category') ?? '',
        protocol: searchParams.get('protocol') ?? '',
        label: searchParams.get('label') ?? '',
        view: searchParams.get('view') === 'list' ? 'list' : 'grid',
        page: Number.isFinite(page) && page > 0 ? page : 1,
        pageSize: Number.isFinite(pageSize) && pageSize > 0 ? pageSize : DEFAULT_CATALOG_PAGE_SIZE,
    };
}

export function serializeCatalogSearchParams(params: CatalogParams): URLSearchParams {
    const next = new URLSearchParams();
    if (params.query) {
        next.set('query', params.query);
    }
    if (params.category) {
        next.set('category', params.category);
    }
    if (params.protocol) {
        next.set('protocol', params.protocol);
    }
    if (params.label) {
        next.set('label', params.label);
    }
    if (params.view === 'list') {
        next.set('view', 'list');
    }
    if (params.page > 1) {
        next.set('page', String(params.page));
    }
    if (params.pageSize !== DEFAULT_CATALOG_PAGE_SIZE) {
        next.set('size', String(params.pageSize));
    }
    return next;
}

export function hasCatalogFilters(params: Pick<CatalogParams, 'query' | 'category' | 'protocol' | 'label'>): boolean {
    return Boolean(params.query || params.category || params.protocol || params.label);
}
