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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ApiListResponse } from '../../apis/types/api';
import type {
    ApiProductListItem,
    ApiProductListResponse,
    ApiProductSearchQuery,
    CreateApiProductRequest,
    UpdateApiProductRequest,
    VerifyApiProductNameRequest,
    VerifyApiProductNameResponse,
} from '../types/apiProduct';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function searchApiProducts(
    environmentId: string,
    query: ApiProductSearchQuery,
    page: number,
    perPage: number,
    sortBy?: string,
): Promise<ApiProductListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    if (sortBy) params.set('sortBy', sortBy);
    return apimFetchJsonV2<ApiProductListResponse>(environmentId, `/api-products/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(query),
    });
}

export async function createApiProduct(environmentId: string, request: CreateApiProductRequest): Promise<ApiProductListItem> {
    return apimFetchJsonV2<ApiProductListItem>(environmentId, `/api-products`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(request),
    });
}

export async function verifyApiProductName(
    environmentId: string,
    name: string,
    apiProductId?: string,
): Promise<VerifyApiProductNameResponse> {
    const body: VerifyApiProductNameRequest = apiProductId ? { name, apiProductId } : { name };
    return apimFetchJsonV2<VerifyApiProductNameResponse>(environmentId, `/api-products/_verify`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}

export async function getApiProductById(environmentId: string, productId: string): Promise<ApiProductListItem> {
    return apimFetchJsonV2<ApiProductListItem>(environmentId, `/api-products/${productId}`);
}

export async function updateApiProduct(
    environmentId: string,
    productId: string,
    request: UpdateApiProductRequest,
): Promise<ApiProductListItem> {
    return apimFetchJsonV2<ApiProductListItem>(environmentId, `/api-products/${productId}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(request),
    });
}

export async function deleteApiProduct(environmentId: string, productId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/api-products/${productId}`, { method: 'DELETE' });
}

export async function getApiProductApis(
    environmentId: string,
    productId: string,
    page: number,
    perPage: number,
    query?: string,
): Promise<ApiListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    if (query) params.set('query', query);
    return apimFetchJsonV2<ApiListResponse>(environmentId, `/api-products/${productId}/apis?${params}`);
}

export async function searchApisAllowedInProducts(
    environmentId: string,
    query: string,
    page: number,
    perPage: number,
): Promise<ApiListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    return apimFetchJsonV2<ApiListResponse>(environmentId, `/apis/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ query: query || undefined, allowedInApiProducts: true }),
    });
}
