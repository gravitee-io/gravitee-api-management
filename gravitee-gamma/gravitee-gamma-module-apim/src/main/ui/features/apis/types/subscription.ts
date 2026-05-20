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
import type { EntityContext } from '../../../shared/types/entityContext';

// TODO: Subscription types — prep for subscriptions feature (no services/hooks/pages yet)
export type SubscriptionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CLOSED' | 'PAUSED' | 'RESUMED';
export type SubscriptionSecurityType = 'API_KEY' | 'OAUTH2' | 'JWT' | 'KEY_LESS' | 'MTLS';
export type ApiKeyMode = 'SHARED' | 'EXCLUSIVE' | 'UNSPECIFIED';
export type SubscriptionConsumerStatus = 'STARTED' | 'STOPPED' | 'FAILURE';

export interface SubscriptionPlan {
    id: string;
    name: string;
    security?: { type: SubscriptionSecurityType; configuration?: unknown };
    mode?: 'STANDARD' | 'PUSH';
}

export interface SubscriptionApplication {
    id: string;
    name: string;
    description?: string;
    domain?: string;
    primaryOwner?: { displayName: string };
    apiKeyMode?: ApiKeyMode;
    type?: string;
}

export interface Subscription {
    id: string;
    status: SubscriptionStatus;
    consumerStatus?: SubscriptionConsumerStatus;
    origin?: 'KUBERNETES' | 'MANAGEMENT';
    plan: SubscriptionPlan;
    application: SubscriptionApplication;
    subscribedBy?: { displayName: string };
    createdAt?: string;
    updatedAt?: string;
    processedAt?: string;
    startingAt?: string;
    endingAt?: string;
    closedAt?: string;
    pausedAt?: string;
    publisherMessage?: string;
    consumerMessage?: string;
    failureCause?: string;
    metadata?: Record<string, string>;
}

export interface SubscriptionPage {
    data: Subscription[];
    pagination: { totalCount: number; pageIndex: number; pageSize: number };
}

export interface ApiKey {
    id: string;
    key: string;
    createdAt?: string;
    expireAt?: string;
    revokedAt?: string;
    revoked?: boolean;
    expired?: boolean;
}

export interface ApiKeyPage {
    data: ApiKey[];
    pagination: { totalCount: number; pageIndex: number; pageSize: number };
}

export interface Plan {
    id: string;
    name: string;
    description?: string;
    security?: { type: SubscriptionSecurityType };
    mode?: 'STANDARD' | 'PUSH';
    status?: string;
}

export interface PlanPage {
    data: Plan[];
    pagination: { totalCount: number };
}

export interface Application {
    id: string;
    name: string;
    description?: string;
    primaryOwner?: { displayName: string };
    type?: string;
    apiKeyMode?: ApiKeyMode;
}

export interface ApplicationPage {
    data: Application[];
    pagination: { totalCount: number };
}

export interface SubscriptionFilters {
    statuses: SubscriptionStatus[];
    planIds: string[];
    applicationIds: string[];
    apiKey: string;
}

export interface CreateSubscriptionPayload {
    planId: string;
    applicationId: string;
    customApiKey?: string;
}

export interface ApproveSubscriptionPayload {
    customApiKey?: string;
    reason?: string;
    startingAt?: string;
    endingAt?: string;
}

export type SubscriptionContext = EntityContext;
