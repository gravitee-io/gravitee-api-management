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
import { getApplicationById } from './applications.service';
import { getPlanById } from './plans.mock';
import type {
    CreateSubscriptionRequest,
    Subscription,
    SubscriptionApiKey,
    SubscriptionsResponse,
} from '../entities/subscription';
import {
    getAllSubscriptions,
    getSubscription,
    saveSubscription,
    saveSubscriptions,
} from '../../portals/storage/subscriptions.storage';
import { createDummySubscriptions } from '../../portals/storage/dummy-subscriptions';

export interface ListSubscriptionsParams {
    apiIds?: string[];
    applicationIds?: string[];
    statuses?: Subscription['status'][];
    page?: number;
    size?: number;
}

let subscriptionCounter = 100;

export async function listSubscriptions({
    apiIds,
    applicationIds,
    statuses,
    page = 1,
    size = 10,
}: ListSubscriptionsParams = {}): Promise<SubscriptionsResponse> {
    let filtered = await getAllSubscriptions();

    if (apiIds?.length) {
        filtered = filtered.filter(sub => apiIds.includes(sub.api));
    }
    if (applicationIds?.length) {
        filtered = filtered.filter(sub => applicationIds.includes(sub.application));
    }
    if (statuses?.length) {
        filtered = filtered.filter(sub => statuses.includes(sub.status));
    }

    filtered = filtered.sort(
        (a, b) => new Date(b.created_at ?? 0).getTime() - new Date(a.created_at ?? 0).getTime(),
    );

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

export async function getSubscriptionById(id: string): Promise<Subscription | undefined> {
    return getSubscription(id);
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

    if (plan.security === 'OAUTH2' || plan.security === 'JWT') {
        subscription.clientId = application.settings.oauth?.client_id;
        subscription.clientSecret = application.settings.oauth?.client_secret;
    }

    await saveSubscription(subscription);
    return subscription;
}

export async function getActiveSubscriptionsForApi(apiId: string): Promise<Subscription[]> {
    const all = await getAllSubscriptions();
    return all.filter(
        sub => sub.api === apiId && (sub.status === 'ACCEPTED' || sub.status === 'PENDING'),
    );
}

export async function revokeApiKey(subscriptionId: string, keyId: string): Promise<Subscription> {
    const subscription = await getSubscription(subscriptionId);
    if (!subscription?.keys) {
        throw new Error('Subscription not found');
    }

    const updated: Subscription = {
        ...subscription,
        keys: subscription.keys.map(key =>
            key.id === keyId ? { ...key, revoked_at: new Date().toISOString() } : key,
        ),
    };
    await saveSubscription(updated);
    return updated;
}

export async function renewApiKey(subscriptionId: string): Promise<Subscription> {
    const subscription = await getSubscription(subscriptionId);
    if (!subscription) {
        throw new Error('Subscription not found');
    }

    const newKey: SubscriptionApiKey = {
        key: `gk-${Math.random().toString(36).slice(2, 10)}`,
        id: `key-${Date.now()}`,
        created_at: new Date().toISOString(),
    };

    const updated: Subscription = {
        ...subscription,
        keys: [...(subscription.keys ?? []), newKey],
    };
    await saveSubscription(updated);
    return updated;
}

/** Test helper — re-seed subscriptions from dummy data. */
export async function resetSubscriptionsForTests(): Promise<void> {
    subscriptionCounter = 100;
    await saveSubscriptions(createDummySubscriptions());
}
