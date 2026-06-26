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
import type { PlanSecurity } from './plan';

export type SubscriptionStatus = 'PENDING' | 'ACCEPTED' | 'CLOSED' | 'REJECTED' | 'PAUSED' | 'REVOKED';

export interface SubscriptionConsumerConfiguration {
    entrypointId: string;
    channel: string;
    entrypointConfiguration: {
        callbackUrl: string;
        headers: { name: string; value: string }[];
        auth: { type: 'none' | 'basic' | 'token' };
        ssl: { trustAll?: boolean };
        retry: { retryOption: 'No Retry' | 'Retry On Fail' };
    };
}

export interface SubscriptionApiKey {
    key?: string;
    id?: string;
    created_at?: string;
    revoked_at?: string;
    expire_at?: string;
}

export interface Subscription {
    id: string;
    api: string;
    apiName?: string;
    application: string;
    applicationName?: string;
    plan: string;
    planName?: string;
    status: SubscriptionStatus;
    security?: PlanSecurity;
    created_at?: string;
    updated_at?: string;
    keys?: SubscriptionApiKey[];
    consumerConfiguration?: SubscriptionConsumerConfiguration;
}

export interface CreateSubscriptionRequest {
    application: string;
    plan: string;
    api_key_mode?: 'SHARED' | 'EXCLUSIVE' | 'UNSPECIFIED';
    configuration?: SubscriptionConsumerConfiguration;
}

export interface SubscriptionsResponse {
    data: Subscription[];
    metadata?: {
        pagination?: {
            current_page: number;
            size: number;
            total: number;
            total_pages: number;
        };
    };
}
