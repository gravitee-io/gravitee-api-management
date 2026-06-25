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
import type { Api, ApisResponse } from '../entities/api';

export interface ApiSearchParams {
    page?: number;
    size?: number;
    q?: string;
    category?: string;
}

const MOCK_APIS: Api[] = [
    {
        id: 'api-payments',
        name: 'Payments API',
        version: '1.2.0',
        type: 'PROXY',
        definitionVersion: 'V4',
        description: 'Process card and bank payments with real-time settlement.',
        _public: true,
        running: true,
        entrypoints: ['/payments/v1'],
        owner: { id: 'user-1', displayName: 'Platform Team' },
        categories: ['Payments', 'Finance'],
    },
    {
        id: 'api-accounts',
        name: 'Accounts API',
        version: '2.0.1',
        type: 'PROXY',
        definitionVersion: 'V4',
        description: 'Manage customer accounts, balances, and profile data.',
        _public: true,
        running: true,
        entrypoints: ['/accounts/v2'],
        owner: { id: 'user-2', displayName: 'Core Services' },
        categories: ['Accounts'],
    },
    {
        id: 'api-notifications',
        name: 'Notifications API',
        version: '1.0.0',
        type: 'MESSAGE',
        definitionVersion: 'V4',
        description: 'Send email, SMS, and push notifications to end users.',
        _public: true,
        running: true,
        entrypoints: ['/notifications/v1'],
        owner: { id: 'user-3', displayName: 'Messaging Team' },
        categories: ['Messaging'],
    },
    {
        id: 'api-identity',
        name: 'Identity API',
        version: '3.1.0',
        type: 'PROXY',
        definitionVersion: 'V4',
        description: 'OAuth2 and OpenID Connect identity provider integration.',
        _public: true,
        running: false,
        entrypoints: ['/identity/v3'],
        owner: { id: 'user-4', displayName: 'Security Team' },
        categories: ['Security', 'Identity'],
    },
    {
        id: 'api-analytics',
        name: 'Analytics API',
        version: '1.5.0',
        type: 'PROXY',
        definitionVersion: 'V2',
        description: 'Query usage metrics, traffic patterns, and API health data.',
        _public: true,
        running: true,
        entrypoints: ['/analytics/v1'],
        owner: { id: 'user-5', displayName: 'Data Team' },
        categories: ['Analytics'],
    },
];

function filterApis({ q = '', category = '' }: ApiSearchParams): Api[] {
    const query = q.trim().toLowerCase();

    return MOCK_APIS.filter(api => {
        const matchesQuery =
            !query ||
            api.name.toLowerCase().includes(query) ||
            api.description.toLowerCase().includes(query);
        const matchesCategory =
            !category || category === 'all' || api.categories?.some(item => item.toLowerCase() === category.toLowerCase());

        return matchesQuery && matchesCategory;
    });
}

export async function searchApis({ page = 1, size = 9, q = '', category = '' }: ApiSearchParams = {}): Promise<ApisResponse> {
    const filtered = filterApis({ q, category });
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
