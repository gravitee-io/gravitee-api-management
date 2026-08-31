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
import { portalApi } from './portal-client';
import type { ApiType, ApisResponse } from './types';

export const DEFAULT_CATALOG_PAGE_SIZE = 12;
export const CATALOG_PAGE_SIZE_OPTIONS = [12, 25, 50] as const;

export interface CatalogSearchParams {
    query?: string;
    category?: string;
    page: number;
    size: number;
}

export function buildCatalogSearchPath(params: CatalogSearchParams): string {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('size', String(params.size));
    if (params.query) {
        searchParams.set('q', params.query);
    }
    if (params.category) {
        searchParams.set('category', params.category);
    }
    return `/apis/_search?${searchParams.toString()}`;
}

export function searchApis(params: CatalogSearchParams): Promise<ApisResponse> {
    return portalApi.post<ApisResponse>(buildCatalogSearchPath(params));
}

export function matchesProtocol(type: ApiType | undefined, protocol: string): boolean {
    return protocol === '' || type === protocol;
}

export function matchesLabel(labels: readonly string[] | undefined, label: string): boolean {
    return label === '' || (labels?.includes(label) ?? false);
}
