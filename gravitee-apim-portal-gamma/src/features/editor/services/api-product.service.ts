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
import type { ApiProduct, ApiProductsResponse } from '../entities/api-product';

const MOCK_API_PRODUCTS: ApiProduct[] = [
    {
        id: 'product-commerce',
        name: 'Commerce Platform',
        version: '1.0.0',
        description: 'Payments, orders, inventory, and shipping APIs for e-commerce integrations.',
        apiIds: ['api-payments', 'api-orders', 'api-inventory', 'api-shipping'],
    },
    {
        id: 'product-finance',
        name: 'Finance Suite',
        version: '2.1.0',
        description: 'Accounts, billing, and payments APIs for financial services.',
        apiIds: ['api-accounts', 'api-billing', 'api-payments'],
    },
    {
        id: 'product-customer',
        name: 'Customer Experience',
        version: '1.3.0',
        description: 'Identity, notifications, and support APIs for customer-facing applications.',
        apiIds: ['api-identity', 'api-notifications', 'api-support'],
    },
];

export interface ApiProductSearchParams {
    page?: number;
    size?: number;
    q?: string;
}

function filterApiProducts({ q = '' }: ApiProductSearchParams): ApiProduct[] {
    const query = q.trim().toLowerCase();

    return MOCK_API_PRODUCTS.filter(product => {
        if (!query) {
            return true;
        }

        return (
            product.name.toLowerCase().includes(query) ||
            product.description.toLowerCase().includes(query)
        );
    });
}

export async function getApiProductById(id: string): Promise<ApiProduct | undefined> {
    return MOCK_API_PRODUCTS.find(product => product.id === id);
}

export async function searchApiProducts({
    page = 1,
    size = 9,
    q = '',
}: ApiProductSearchParams = {}): Promise<ApiProductsResponse> {
    const filtered = filterApiProducts({ q });
    const start = (page - 1) * size;
    const data = filtered.slice(start, start + size);

    return {
        data,
        metadata: {
            pagination: {
                current_page: page,
                size,
                total: filtered.length,
                total_pages: Math.max(1, Math.ceil(filtered.length / size)),
            },
        },
    };
}
