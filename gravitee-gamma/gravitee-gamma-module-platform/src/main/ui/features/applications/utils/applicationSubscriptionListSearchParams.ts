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
import { DEFAULT_SUBSCRIPTION_FILTER_STATUSES } from './applicationSubscriptionConstants';
import { DEFAULT_SUBSCRIPTION_PAGE_SIZE } from './paginationConstants';
import type { SubscriptionStatus } from '../types/applicationSubscription';

export interface ApplicationSubscriptionListUrlState {
    page: number;
    pageSize: number;
    apiFilters: string[];
    statusFilters: SubscriptionStatus[];
    apiKeyInput: string;
}

function parsePositiveInt(value: string | null, fallback: number): number {
    if (!value) return fallback;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseStatusFilters(raw: string | null): SubscriptionStatus[] {
    if (!raw) return [...DEFAULT_SUBSCRIPTION_FILTER_STATUSES];
    const values = raw.split(',').filter(Boolean) as SubscriptionStatus[];
    return values.length > 0 ? values : [...DEFAULT_SUBSCRIPTION_FILTER_STATUSES];
}

function isDefaultStatusFilters(statusFilters: SubscriptionStatus[]): boolean {
    if (statusFilters.length !== DEFAULT_SUBSCRIPTION_FILTER_STATUSES.length) {
        return false;
    }
    const defaults = new Set(DEFAULT_SUBSCRIPTION_FILTER_STATUSES);
    return statusFilters.every(status => defaults.has(status));
}

/** Reads subscription list filters from the URL (console: page, size, status, apis, apiKey). */
export function parseApplicationSubscriptionListSearchParams(params: URLSearchParams): ApplicationSubscriptionListUrlState {
    return {
        page: parsePositiveInt(params.get('page'), 1),
        pageSize: parsePositiveInt(params.get('size'), DEFAULT_SUBSCRIPTION_PAGE_SIZE),
        apiFilters: params.get('apis')?.split(',').filter(Boolean) ?? [],
        statusFilters: parseStatusFilters(params.get('status')),
        apiKeyInput: params.get('apiKey') ?? '',
    };
}

/** Writes subscription list filters to the URL; omits default values to keep URLs short. */
export function buildApplicationSubscriptionListSearchParams(state: ApplicationSubscriptionListUrlState): URLSearchParams {
    const next = new URLSearchParams();

    if (state.page > 1) next.set('page', String(state.page));
    if (state.pageSize !== DEFAULT_SUBSCRIPTION_PAGE_SIZE) next.set('size', String(state.pageSize));
    if (state.apiFilters.length > 0) next.set('apis', state.apiFilters.join(','));
    const sortedStatusFilters = [...state.statusFilters].sort();
    if (!isDefaultStatusFilters(sortedStatusFilters)) {
        next.set('status', sortedStatusFilters.join(','));
    }
    if (state.apiKeyInput.trim()) next.set('apiKey', state.apiKeyInput.trim());

    return next;
}
