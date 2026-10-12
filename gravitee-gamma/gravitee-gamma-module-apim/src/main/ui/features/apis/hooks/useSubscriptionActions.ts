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
import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';

import { useEnv } from './useEnv';
import {
    approveSubscription,
    closeSubscription,
    createSubscription,
    pauseSubscription,
    rejectSubscription,
    resumeFailedSubscription,
    resumeSubscription,
    transferSubscription,
    updateSubscriptionEndDate,
} from '../services/subscriptions';
import type { ApproveSubscriptionPayload, CreateSubscriptionPayload, Subscription, SubscriptionContext } from '../types/subscription';
import { preserveExpandedSubscription } from '../utils/preserveExpandedSubscription';
import { apiDetailKeys, apiSubscriptionKeys } from '../utils/queryKeys';

function cacheSubscriptionDetail(qc: QueryClient, envId: string, ctx: SubscriptionContext, subscriptionId: string, next: Subscription) {
    const key = apiSubscriptionKeys.detail(envId, ctx, subscriptionId);
    qc.setQueryData(key, preserveExpandedSubscription(qc.getQueryData<Subscription>(key), next));
}

function invalidateApiDetail(qc: ReturnType<typeof useQueryClient>, ctx: SubscriptionContext, envId: string) {
    if (ctx.type === 'api') {
        qc.invalidateQueries({ queryKey: apiDetailKeys.detail(envId, ctx.entityId) });
    }
}

export function useCreateSubscription(ctx: SubscriptionContext) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (payload: CreateSubscriptionPayload) => createSubscription(envId, ctx, payload),
        onSuccess: (_subscription, payload) => {
            qc.invalidateQueries({
                queryKey: apiSubscriptionKeys.applicationApiKeySubscriptions(envId, payload.applicationId),
            });
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useTransferSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (planId: string) => transferSubscription(envId, ctx, subscriptionId, planId),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function usePauseSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => pauseSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useResumeSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => resumeSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useCloseSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => closeSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useApproveSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (payload: ApproveSubscriptionPayload) => approveSubscription(envId, ctx, subscriptionId, payload),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useRejectSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (reason: string) => rejectSubscription(envId, ctx, subscriptionId, reason),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useResumeFailedSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => resumeFailedSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
            invalidateApiDetail(qc, ctx, envId);
        },
    });
}

export function useUpdateSubscriptionEndDate(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: ({
            endingAt,
            subscription,
        }: {
            endingAt: string | null;
            subscription: Pick<Subscription, 'startingAt' | 'consumerConfiguration' | 'metadata'>;
        }) => updateSubscriptionEndDate(envId, ctx, subscriptionId, subscription, endingAt),
        onSuccess: sub => {
            cacheSubscriptionDetail(qc, envId, ctx, subscriptionId, sub);
        },
    });
}
