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
import { getApplicationById } from './applications.mock';
import { getPlanById } from './plans.mock';
import type {
    CreateSubscriptionRequest,
    Subscription,
    SubscriptionsResponse,
} from '../entities/subscription';

let subscriptionCounter = 100;
const subscriptions: Subscription[] = [
    {
        id: 'sub-001',
        api: 'api-payments',
        apiName: 'Payments API',
        application: 'app-mobile',
        applicationName: 'Mobile Banking App',
        plan: 'plan-payments-key',
        planName: 'Standard API Key',
        status: 'ACCEPTED',
        security: 'API_KEY',
        created_at: '2025-06-01T08:00:00Z',
        keys: [{ key: 'gk-****-payments-001', id: 'key-001', created_at: '2025-06-01T08:00:00Z' }],
    },
    {
        id: 'sub-002',
        api: 'api-accounts',
        apiName: 'Accounts API',
        application: 'app-web-portal',
        applicationName: 'Customer Web Portal',
        plan: 'plan-accounts-key',
        planName: 'Developer API Key',
        status: 'PENDING',
        security: 'API_KEY',
        created_at: '2025-06-10T12:00:00Z',
    },
    {
        id: 'sub-003',
        api: 'api-payments',
        apiName: 'Payments API',
        application: 'app-partner',
        applicationName: 'Partner Integration',
        plan: 'plan-payments-oauth',
        planName: 'OAuth2 Partner',
        status: 'REVOKED',
        security: 'OAUTH2',
        created_at: '2025-05-20T09:30:00Z',
    },
    {
        id: 'sub-004',
        api: 'api-accounts',
        apiName: 'Accounts API',
        application: 'app-internal',
        applicationName: 'Internal Tools',
        plan: 'plan-accounts-push',
        planName: 'Account Events Webhook',
        status: 'ACCEPTED',
        security: 'API_KEY',
        created_at: '2025-06-15T16:00:00Z',
        consumerConfiguration: {
            entrypointId: 'webhook',
            channel: 'http',
            entrypointConfiguration: {
                callbackUrl: 'https://internal.example.com/webhooks/accounts',
                headers: [],
                auth: { type: 'none' },
                ssl: { trustAll: false },
                retry: { retryOption: 'Retry On Fail' },
            },
        },
    },
    {
        id: 'sub-005',
        api: 'api-notifications',
        apiName: 'Notifications API',
        application: 'app-analytics',
        applicationName: 'Analytics Dashboard',
        plan: 'plan-notifications-key',
        planName: 'Messaging API Key',
        status: 'CLOSED',
        security: 'API_KEY',
        created_at: '2025-04-01T10:00:00Z',
    },
];

export interface ListSubscriptionsParams {
    apiIds?: string[];
    applicationIds?: string[];
    statuses?: Subscription['status'][];
    page?: number;
    size?: number;
}

export async function listSubscriptions({
    apiIds,
    applicationIds,
    statuses,
    page = 1,
    size = 10,
}: ListSubscriptionsParams = {}): Promise<SubscriptionsResponse> {
    let filtered = [...subscriptions];

    if (apiIds?.length) {
        filtered = filtered.filter(sub => apiIds.includes(sub.api));
    }
    if (applicationIds?.length) {
        filtered = filtered.filter(sub => applicationIds.includes(sub.application));
    }
    if (statuses?.length) {
        filtered = filtered.filter(sub => statuses.includes(sub.status));
    }

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

export async function createSubscription(
    apiId: string,
    apiName: string,
    request: CreateSubscriptionRequest,
): Promise<Subscription> {
    const plan = await getPlanById(request.plan);
    const application = await getApplicationById(request.application);

    if (!plan || !application) {
        throw new Error('Invalid plan or application');
    }

    const subscription: Subscription = {
        id: `sub-${++subscriptionCounter}`,
        api: apiId,
        apiName,
        application: request.application,
        applicationName: application.name,
        plan: request.plan,
        planName: plan.name,
        status: plan.validation === 'AUTO' ? 'ACCEPTED' : 'PENDING',
        security: plan.security,
        created_at: new Date().toISOString(),
        consumerConfiguration: request.configuration,
    };

    if (plan.security === 'API_KEY') {
        subscription.keys = [
            {
                key: `gk-${Math.random().toString(36).slice(2, 10)}`,
                id: `key-${subscriptionCounter}`,
                created_at: subscription.created_at,
            },
        ];
    }

    subscriptions.push(subscription);
    return subscription;
}

export function getActiveSubscriptionsForApi(apiId: string): Subscription[] {
    return subscriptions.filter(
        sub => sub.api === apiId && (sub.status === 'ACCEPTED' || sub.status === 'PENDING'),
    );
}

export function resetMockSubscriptions(): void {
    subscriptions.length = 5;
    subscriptionCounter = 100;
}
