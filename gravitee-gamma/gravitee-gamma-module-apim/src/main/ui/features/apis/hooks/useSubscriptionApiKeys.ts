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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useEnv } from './useEnv';
import { expireApiKey, listApiKeys, renewApiKey, revokeApiKey } from '../services/subscriptions';
import type { SubscriptionContext } from '../types/subscription';
import { apiSubscriptionKeys } from '../utils/queryKeys';

export function useApiKeyList(ctx: SubscriptionContext, subscriptionId: string, page: number, perPage = 5) {
    const envId = useEnv();

    return useQuery({
        queryKey: apiSubscriptionKeys.apiKeys(envId, ctx, subscriptionId, page),
        queryFn: () => listApiKeys(envId, ctx, subscriptionId, page, perPage),
        enabled: Boolean(envId && ctx.entityId && subscriptionId),
        staleTime: 30_000,
    });
}

export function useRenewApiKey(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: () => renewApiKey(envId, ctx, subscriptionId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'api-keys', envId, subscriptionId] });
        },
    });
}

export function useRevokeApiKey(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: (apiKeyId: string) => revokeApiKey(envId, ctx, subscriptionId, apiKeyId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'api-keys', envId, subscriptionId] });
        },
    });
}

export function useExpireApiKey(ctx: SubscriptionContext, subscriptionId: string) {
    const envId = useEnv();
    const qc = useQueryClient();

    return useMutation({
        mutationFn: ({ apiKeyId, expireAt }: { apiKeyId: string; expireAt: string }) =>
            expireApiKey(envId, ctx, subscriptionId, apiKeyId, expireAt),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'api-keys', envId, subscriptionId] });
        },
    });
}
