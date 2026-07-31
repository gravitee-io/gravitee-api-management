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
import { http, HttpResponse } from 'msw';

import { TEST_MANAGEMENT_BASE } from '../factories';

export const TEST_TASKS_RESPONSE = {
    data: [
        {
            type: 'SUBSCRIPTION_APPROVAL',
            created_at: 1_700_000_600_000,
            data: { id: 'sub-1', application: 'app-1', plan: 'plan-1', referenceId: 'api-1', referenceType: 'API' },
        },
        {
            type: 'IN_REVIEW',
            created_at: 1_700_000_400_000,
            data: { referenceId: 'api-2', referenceType: 'API' },
        },
        {
            type: 'PROMOTION_APPROVAL',
            created_at: 1_700_000_200_000,
            data: {
                promotionId: 'promo-1',
                apiName: 'Loyalty API',
                sourceEnvironmentName: 'Staging',
                targetEnvironmentName: 'Production',
                targetApiId: 'api-9',
            },
        },
    ],
    metadata: {
        'app-1': { name: 'Passenger Mobile App' },
        'plan-1': { name: 'Gold', api: 'api-1' },
        'api-1': { name: 'Booking MCP Server', apiType: 'mcp-proxy', environmentId: 'env-1-id' },
        'api-2': { name: 'Baggage Tracking API', apiType: 'proxy', environmentId: 'env-1-id' },
    },
    page: { total_elements: 3 },
};

export const tasksHandlers = [
    http.get(`${TEST_MANAGEMENT_BASE}/user/tasks`, () => HttpResponse.json(TEST_TASKS_RESPONSE)),
    http.get(`${TEST_MANAGEMENT_BASE}/settings`, () => HttpResponse.json({ scheduler: { tasks: 10 } })),
];
