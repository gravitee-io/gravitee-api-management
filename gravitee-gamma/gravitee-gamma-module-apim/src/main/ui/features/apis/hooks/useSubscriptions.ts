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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';

import { getSubscription, listApiPlans, listSubscriptions, searchApplications } from '../services/subscriptions';
import type { SubscriptionContext, SubscriptionFilters, SubscriptionStatus } from '../types/subscription';
import { apiSubscriptionKeys } from '../utils/queryKeys';

export function isSubscriptionFiltersDirty(filters: SubscriptionFilters): boolean {
    return (
        filters.statuses.length > 0 || filters.planIds.length > 0 || filters.applicationIds.length > 0 || filters.apiKey.trim().length > 0
    );
}

export const DEFAULT_STATUSES = ['ACCEPTED', 'PAUSED', 'PENDING'] as const;

const SAFE_CTX: SubscriptionContext = { type: 'api', entityId: '' };

export function useSubscriptionList(ctx: SubscriptionContext | null, filters: Partial<SubscriptionFilters>, page: number, perPage = 10) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const safeCtx = ctx ?? SAFE_CTX;
    const filtersKey = { ...filters, page, perPage };

    return useQuery({
        queryKey: apiSubscriptionKeys.list(envId, safeCtx, filtersKey),
        queryFn: () =>
            listSubscriptions(envId, ctx!, {
                statuses: filters.statuses?.length ? filters.statuses : [...DEFAULT_STATUSES],
                planIds: filters.planIds,
                applicationIds: filters.applicationIds,
                apiKey: filters.apiKey,
                page,
                perPage,
            }),
        enabled: Boolean(env && ctx?.entityId),
    });
}

export function useSubscriptionDetail(ctx: SubscriptionContext | null, subscriptionId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const safeCtx = ctx ?? SAFE_CTX;

    return useQuery({
        queryKey: apiSubscriptionKeys.detail(envId, safeCtx, subscriptionId ?? ''),
        queryFn: () => getSubscription(envId, ctx!, subscriptionId!),
        enabled: Boolean(env && ctx?.entityId && subscriptionId),
    });
}

export function useApiPlans(ctx: SubscriptionContext | null) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const safeCtx = ctx ?? SAFE_CTX;

    return useQuery({
        queryKey: apiSubscriptionKeys.plans(envId, safeCtx),
        queryFn: () => listApiPlans(envId, ctx!),
        enabled: Boolean(env && ctx?.entityId),
        staleTime: 5 * 60_000,
        select: data => data.data.filter(p => p.security?.type !== 'KEY_LESS'),
    });
}

export function useSubscriptionCount(ctx: SubscriptionContext | null, statuses: SubscriptionStatus[]) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const safeCtx = ctx ?? SAFE_CTX;
    return useQuery({
        queryKey: apiSubscriptionKeys.list(envId, safeCtx, { statuses, page: 1, perPage: 1 }),
        queryFn: () => listSubscriptions(envId, ctx!, { statuses, page: 1, perPage: 1 }),
        enabled: Boolean(env && ctx?.entityId),
        select: data => data.pagination.totalCount,
    });
}

export function useApplicationSearch(query: string) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const trimmed = query.trim();

    return useQuery({
        queryKey: apiSubscriptionKeys.applications(envId, trimmed),
        queryFn: () => searchApplications(envId, trimmed),
        enabled: Boolean(env && trimmed.length > 0),
        select: data => data.data,
    });
}
