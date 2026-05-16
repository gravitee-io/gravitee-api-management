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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useEnv } from './useEnv';
import {
    closeSubscription,
    createSubscription,
    pauseSubscription,
    resumeSubscription,
    transferSubscription,
    updateSubscriptionEndDate,
} from '../services/subscriptions';
import type { CreateSubscriptionPayload, SubscriptionContext } from '../types/subscription';
import { apiSubscriptionKeys } from '../utils/queryKeys';

export function useCreateSubscription(ctx: SubscriptionContext) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (payload: CreateSubscriptionPayload) => createSubscription(envId, ctx, payload),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
        },
    });
}

export function useTransferSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (planId: string) => transferSubscription(envId, ctx, subscriptionId, planId),
        onSuccess: sub => {
            qc.setQueryData(apiSubscriptionKeys.detail(envId, ctx, subscriptionId), sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
        },
    });
}

export function usePauseSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => pauseSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            qc.setQueryData(apiSubscriptionKeys.detail(envId, ctx, subscriptionId), sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
        },
    });
}

export function useResumeSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => resumeSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            qc.setQueryData(apiSubscriptionKeys.detail(envId, ctx, subscriptionId), sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
        },
    });
}

export function useCloseSubscription(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => closeSubscription(envId, ctx, subscriptionId),
        onSuccess: sub => {
            qc.setQueryData(apiSubscriptionKeys.detail(envId, ctx, subscriptionId), sub);
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list'] });
        },
    });
}

export function useUpdateSubscriptionEndDate(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (endingAt: string | null) => updateSubscriptionEndDate(envId, ctx, subscriptionId, endingAt),
        onSuccess: sub => {
            qc.setQueryData(apiSubscriptionKeys.detail(envId, ctx, subscriptionId), sub);
        },
    });
}
