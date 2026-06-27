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
import type { Subscription, SubscriptionStatus } from '../../features/editor/entities/subscription';

export interface SubscriptionTableRow {
    id: string;
    api: string;
    plan: string;
    application: string;
    createdAt: string;
    status: SubscriptionStatus;
}

export interface SubscriptionFilters {
    apiIds: string[];
    applicationIds: string[];
    statuses: SubscriptionStatus[];
    page: number;
    pageSize: number;
}

export const SUBSCRIPTION_STATUSES: SubscriptionStatus[] = [
    'PENDING',
    'ACCEPTED',
    'CLOSED',
    'REJECTED',
    'PAUSED',
    'REVOKED',
];

export function toTableRow(subscription: Subscription): SubscriptionTableRow {
    return {
        id: subscription.id,
        api: subscription.apiName ?? subscription.api,
        plan: subscription.planName ?? subscription.plan,
        application: subscription.applicationName ?? subscription.application,
        createdAt: subscription.created_at ?? '',
        status: subscription.status,
    };
}

export function formatStatus(status: SubscriptionStatus): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
}

export function formatDate(isoDate: string): string {
    if (!isoDate) return '—';
    return new Date(isoDate).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    });
}

export function isActiveApiKey(key: { revoked_at?: string; expire_at?: string }): boolean {
    if (key.revoked_at) return false;
    if (!key.expire_at) return true;
    return new Date(key.expire_at).getTime() > Date.now();
}
