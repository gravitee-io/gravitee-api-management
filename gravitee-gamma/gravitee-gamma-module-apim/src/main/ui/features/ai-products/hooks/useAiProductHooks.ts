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
import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiProductKeys } from '../../api-products/utils/queryKeys';
import {
    approveSubscription,
    closeSubscription,
    listSubscriptions,
    rejectSubscription,
    transferSubscription,
    updateSubscriptionMetadata,
} from '../../apis/services/subscriptions';
import type { ApiListResponse } from '../../apis/types';
import type { Subscription } from '../../apis/types/subscription';
import { DEVELOPER_RATE_LIMIT_METADATA_KEY, DEVELOPER_TOKEN_LIMIT_METADATA_KEY } from '../../apis/utils/planTransformers';
import { apiSubscriptionKeys } from '../../apis/utils/queryKeys';
import {
    type BudgetWindow,
    deployApiProduct,
    ensurePlanForWindow,
    getComponentModels,
    getProductModels,
    type ProviderModel,
    searchLlmComponents,
} from '../services/aiProduct';
import type { LlmModel } from '../types/aiProduct';
import { aiProductKeys } from '../utils/queryKeys';

/** Search LLM proxies eligible as components (debounced query handled by the caller). */
export function useLlmComponentSearch(query: string, page: number, perPage: number) {
    const env = useEnvironment();
    return useQuery<ApiListResponse>({
        queryKey: aiProductKeys.llmComponentSearch(env?.id ?? '', query, page, perPage),
        queryFn: () => searchLlmComponents(env!.id, query, page, perPage),
        enabled: Boolean(env) && query.trim().length > 0,
        staleTime: 0,
        placeholderData: keepPreviousData,
    });
}

/** Models served by an attached LLM proxy component. */
export function useComponentModels(apiId: string | undefined) {
    const env = useEnvironment();
    return useQuery<LlmModel[]>({
        queryKey: aiProductKeys.componentModels(env?.id ?? '', apiId ?? ''),
        queryFn: () => getComponentModels(env!.id, apiId!),
        enabled: Boolean(env) && Boolean(apiId),
    });
}

/** All models the product exposes across its LLM proxies, tagged by upstream provider. */
export function useProductModels(apiIds: string[]) {
    const env = useEnvironment();
    const key = [...apiIds].sort().join(',');
    return useQuery<ProviderModel[]>({
        queryKey: [...aiProductKeys.all, 'product-models', env?.id ?? '', key],
        queryFn: () => getProductModels(env!.id, apiIds),
        enabled: Boolean(env) && apiIds.length > 0,
    });
}

/** Deploy the product to the gateway and refresh its deployment state. */
export function useDeployAiProduct() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (productId: string) => deployApiProduct(env!.id, productId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiProductKeys.all });
        },
    });
}

export interface UpdateDeveloperLimitsInput {
    productId: string;
    subscriptionId: string;
    tokenLimit: number;
    rateLimit: number;
    /** Existing subscription metadata to preserve (e.g. developerUserId) while limits change. */
    existingMetadata?: Record<string, string>;
}

/**
 * Change a user's personal token budget / rate limit after onboarding. The limits live on the
 * subscription metadata, so this re-personalises just that one user — no plan or policy edit,
 * and nobody else is affected. The gateway picks up the new `dynamicLimit` on the next sync.
 */
export function useUpdateDeveloperLimits() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<Subscription, Error, UpdateDeveloperLimitsInput>({
        mutationFn: ({ productId, subscriptionId, tokenLimit, rateLimit, existingMetadata }) =>
            updateSubscriptionMetadata(env!.id, { type: 'api-product', entityId: productId }, subscriptionId, {
                ...existingMetadata,
                [DEVELOPER_TOKEN_LIMIT_METADATA_KEY]: String(tokenLimit),
                [DEVELOPER_RATE_LIMIT_METADATA_KEY]: String(rateLimit),
            }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiSubscriptionKeys.all });
            queryClient.invalidateQueries({ queryKey: aiProductKeys.all });
        },
    });
}

/**
 * Ensure the product has a published API-key access plan (with the per-user token + rate budget policies),
 * so it becomes subscribable from the Developer Portal. Idempotent: reuses the plan if it already exists.
 * This is the console "make available on the portal" action in the pull flow.
 */
export function useEnsureAccessPlan() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<string, Error, { productId: string; window?: BudgetWindow }>({
        mutationFn: ({ productId, window }) => ensurePlanForWindow(env!.id, productId, window ?? 'MONTH', 'API_KEY'),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: aiProductKeys.all });
        },
    });
}

export interface ApproveDeveloperInput {
    productId: string;
    subscriptionId: string;
    /** Per-user token budget the admin grants at approval time. */
    tokenLimit: number;
    /** Per-user request rate limit (requests/minute) the admin grants at approval time. */
    rateLimit: number;
    /**
     * Reset window the admin picks at approval. The token-ratelimit period is plan-level (it can't be
     * read per-user from metadata), so the chosen window maps to the matching per-window plan and the
     * subscription is transferred onto it.
     */
    window?: BudgetWindow;
    /** The subscription's current plan id, so we only transfer when the chosen window actually differs. */
    currentPlanId?: string;
    /** Existing subscription metadata to preserve (e.g. the requested models / developerUserId). */
    existingMetadata?: Record<string, string>;
}

/**
 * Approve a portal access request and grant the user their personal limits in one step.
 *
 * This is the console half of the pull flow: a developer self-subscribes from the portal (PENDING),
 * the admin reviews it here, sets that user's token budget + rate limit, and accepts — which issues
 * the key. Limits are written to the subscription before accept so the active subscription reaches
 * the gateway already personalised.
 */
export function useApproveDeveloper() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<void, Error, ApproveDeveloperInput>({
        mutationFn: async ({ productId, subscriptionId, tokenLimit, rateLimit, window, currentPlanId, existingMetadata }) => {
            const ctx = { type: 'api-product' as const, entityId: productId };
            // The reset window is a plan-level property, so the admin's choice maps to the matching
            // per-window plan (created + published on demand). The budget + rate numbers stay per-user.
            const windowPlanId = await ensurePlanForWindow(env!.id, productId, window ?? 'MONTH', 'API_KEY');
            await updateSubscriptionMetadata(env!.id, ctx, subscriptionId, {
                ...existingMetadata,
                [DEVELOPER_TOKEN_LIMIT_METADATA_KEY]: String(tokenLimit),
                [DEVELOPER_RATE_LIMIT_METADATA_KEY]: String(rateLimit),
            });
            // Accept first (issues the key), then move the now-active subscription onto the chosen
            // window plan when it isn't already there (the period lives on that plan's flow).
            await approveSubscription(env!.id, ctx, subscriptionId, {});
            if (currentPlanId !== windowPlanId) {
                await transferSubscription(env!.id, ctx, subscriptionId, windowPlanId);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiSubscriptionKeys.all });
            queryClient.invalidateQueries({ queryKey: aiProductKeys.all });
        },
    });
}

/**
 * Revoke an active subscriber's access. Closing the subscription invalidates their API key at the
 * gateway (subsequent calls get 401) and removes them from the active subscriber list. This is the
 * admin "cut off this user" control; the user would have to request access again from the portal.
 */
export function useCloseDeveloper() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<void, Error, { productId: string; subscriptionId: string }>({
        mutationFn: async ({ productId, subscriptionId }) => {
            await closeSubscription(env!.id, { type: 'api-product', entityId: productId }, subscriptionId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiSubscriptionKeys.all });
            queryClient.invalidateQueries({ queryKey: aiProductKeys.all });
        },
    });
}

/** Reject a portal access request. */
export function useRejectDeveloper() {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    return useMutation<void, Error, { productId: string; subscriptionId: string; reason?: string }>({
        mutationFn: async ({ productId, subscriptionId, reason }) => {
            await rejectSubscription(env!.id, { type: 'api-product', entityId: productId }, subscriptionId, reason ?? 'Not approved');
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiSubscriptionKeys.all });
            queryClient.invalidateQueries({ queryKey: aiProductKeys.all });
        },
    });
}

/** Count of ACCEPTED subscriptions for one product (used by snapshot/stat cards). */
export function useAiProductSubscribersCount(productId: string | undefined) {
    const env = useEnvironment();
    return useQuery<number>({
        queryKey: aiProductKeys.subscribersCount(env?.id ?? '', productId ?? ''),
        queryFn: async () => {
            const result = await listSubscriptions(
                env!.id,
                { type: 'api-product', entityId: productId! },
                { statuses: ['ACCEPTED'], page: 1, perPage: 1 },
            );
            return result.pagination?.totalCount ?? 0;
        },
        enabled: Boolean(env) && Boolean(productId),
        staleTime: 30_000,
    });
}

/** Sum of ACCEPTED subscriptions across the listed products (list stat card). */
export function useAiProductsSubscribersTotal(productIds: string[]) {
    const env = useEnvironment();
    const results = useQueries({
        queries: productIds.map(productId => ({
            queryKey: aiProductKeys.subscribersCount(env?.id ?? '', productId),
            queryFn: async () => {
                const result = await listSubscriptions(
                    env!.id,
                    { type: 'api-product', entityId: productId },
                    { statuses: ['ACCEPTED'], page: 1, perPage: 1 },
                );
                return result.pagination?.totalCount ?? 0;
            },
            enabled: Boolean(env),
            staleTime: 30_000,
        })),
    });
    const isLoading = results.some(r => r.isLoading);
    const total = results.reduce((sum, r) => sum + (r.data ?? 0), 0);
    return { total, isLoading };
}
