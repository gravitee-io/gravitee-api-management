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
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { createPlan, deletePlan, getPlan, listPlans, transitionPlan, updatePlan } from '../services/plans';
import type { ManagedPlan, PlanContext, PlanFormValue, PlanStatus, PlanTransitionAction } from '../types/plan';
import { planFormToPayload } from '../utils/planTransformers';
import { apiDetailKeys, apiPlanKeys } from '../utils/queryKeys';

/** Paginated plan list filtered by statuses. */
export function usePlanList(ctx: PlanContext, statuses: PlanStatus[], page: number, perPage = 10) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    return useQuery({
        queryKey: apiPlanKeys.list(envId, ctx, statuses, page, perPage),
        queryFn: () => listPlans(envId, ctx, statuses, page, perPage),
        enabled: Boolean(envId && ctx.entityId),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}

/** Total count of plans for a single status — used by the 4 status cards. */
export function usePlanStatusCount(ctx: PlanContext, status: PlanStatus) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    return useQuery({
        queryKey: apiPlanKeys.count(envId, ctx, status),
        queryFn: () => listPlans(envId, ctx, [status], 1, 1),
        enabled: Boolean(envId && ctx.entityId),
        staleTime: 30_000,
        select: data => data.pagination.totalCount,
    });
}

export interface PlanStatusCounts {
    staging: number;
    published: number;
    deprecated: number;
    closed: number;
    total: number;
    isLoading: boolean;
}

/** Convenient hook returning counts for all four statuses at once. */
export function usePlanStatusCounts(ctx: PlanContext): PlanStatusCounts {
    const staging = usePlanStatusCount(ctx, 'STAGING');
    const published = usePlanStatusCount(ctx, 'PUBLISHED');
    const deprecated = usePlanStatusCount(ctx, 'DEPRECATED');
    const closed = usePlanStatusCount(ctx, 'CLOSED');
    const total = (staging.data ?? 0) + (published.data ?? 0) + (deprecated.data ?? 0) + (closed.data ?? 0);
    return {
        staging: staging.data ?? 0,
        published: published.data ?? 0,
        deprecated: deprecated.data ?? 0,
        closed: closed.data ?? 0,
        total,
        isLoading: staging.isLoading || published.isLoading || deprecated.isLoading || closed.isLoading,
    };
}

/** Single plan detail (for the edit/view wizard). */
export function usePlan(ctx: PlanContext, planId: string | undefined) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    return useQuery({
        queryKey: apiPlanKeys.detail(envId, ctx, planId ?? ''),
        queryFn: () => getPlan(envId, ctx, planId!),
        enabled: Boolean(envId && ctx.entityId && planId),
        staleTime: 30_000,
    });
}

function invalidatePlans(qc: ReturnType<typeof useQueryClient>, ctx: PlanContext, envId: string) {
    qc.invalidateQueries({ queryKey: [apiPlanKeys.all[0], ctx.type, ctx.entityId] });
    if (ctx.type === 'api') {
        qc.invalidateQueries({ queryKey: apiDetailKeys.detail(envId, ctx.entityId) });
    }
}

/** Create a new plan — mutate receives a PlanFormValue and transforms it internally. */
export function useCreatePlan(ctx: PlanContext) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (form: PlanFormValue) => createPlan(envId, ctx, planFormToPayload(form, ctx)),
        onSuccess: () => invalidatePlans(qc, ctx, envId),
    });
}

/** Update an existing plan — mutate receives { planId, form }. */
export function useUpdatePlan(ctx: PlanContext) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ planId, form }: { planId: string; form: PlanFormValue }) =>
            updatePlan(envId, ctx, planId, planFormToPayload(form, ctx)),
        onSuccess: updated => {
            qc.setQueryData(apiPlanKeys.detail(envId, ctx, updated.id), updated);
            invalidatePlans(qc, ctx, envId);
        },
    });
}

/** Publish / Deprecate / Close — mutate receives { planId, action }. */
export function usePlanTransition(ctx: PlanContext) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ planId, action }: { planId: string; action: PlanTransitionAction }) => transitionPlan(envId, ctx, planId, action),
        onSuccess: updated => {
            qc.setQueryData(apiPlanKeys.detail(envId, ctx, updated.id), updated);
            invalidatePlans(qc, ctx, envId);
        },
    });
}

/** Reorder — mutate receives { planId, newOrder } where newOrder is the adjacent plan's current order. */
export function useReorderPlan(ctx: PlanContext) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ planId, fullPlan, newOrder }: { planId: string; fullPlan: ManagedPlan; newOrder: number }) => {
            // Exclude id (already in the URL) and status (managed via transition endpoints — PUT rejects it).
            const { id: _id, status: _status, ...planFields } = fullPlan;
            return updatePlan(envId, ctx, planId, { ...planFields, order: newOrder });
        },
        onSuccess: () => invalidatePlans(qc, ctx, envId),
    });
}

/** Delete a STAGING plan — mutate receives planId. Backend returns 400 if plan has subscribers. */
export function useDeletePlan(ctx: PlanContext) {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (planId: string) => deletePlan(envId, ctx, planId),
        onSuccess: () => invalidatePlans(qc, ctx, envId),
    });
}
