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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ManagedPlan, ManagedPlanPage, PlanContext, PlanStatus, PlanTransitionAction } from '../types/plan';

const entityBase = (ctx: PlanContext) =>
    ctx.type === 'api' ? `/apis/${encodeURIComponent(ctx.entityId)}` : `/api-products/${encodeURIComponent(ctx.entityId)}`;

const planBase = (ctx: PlanContext) => `${entityBase(ctx)}/plans`;

const planUrl = (ctx: PlanContext, planId: string) => `${planBase(ctx)}/${encodeURIComponent(planId)}`;

function buildQuery(params: Record<string, string | string[] | number | boolean | undefined>): string {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || (Array.isArray(v) && v.length === 0)) continue;
        p.set(k, Array.isArray(v) ? v.join(',') : String(v));
    }
    const s = p.toString();
    return s ? `?${s}` : '';
}

export async function listPlans(envId: string, ctx: PlanContext, statuses: PlanStatus[], page = 1, perPage = 10): Promise<ManagedPlanPage> {
    return apimFetchJsonV2<ManagedPlanPage>(envId, `${planBase(ctx)}${buildQuery({ statuses, page, perPage })}`);
}

export async function getPlan(envId: string, ctx: PlanContext, planId: string): Promise<ManagedPlan> {
    return apimFetchJsonV2<ManagedPlan>(envId, planUrl(ctx, planId));
}

export async function createPlan(envId: string, ctx: PlanContext, payload: Omit<ManagedPlan, 'id' | 'order'>): Promise<ManagedPlan> {
    return apimFetchJsonV2<ManagedPlan>(envId, planBase(ctx), { method: 'POST', body: JSON.stringify(payload) });
}

export async function updatePlan(envId: string, ctx: PlanContext, planId: string, payload: Partial<ManagedPlan>): Promise<ManagedPlan> {
    return apimFetchJsonV2<ManagedPlan>(envId, planUrl(ctx, planId), { method: 'PUT', body: JSON.stringify(payload) });
}

export async function transitionPlan(envId: string, ctx: PlanContext, planId: string, action: PlanTransitionAction): Promise<ManagedPlan> {
    return apimFetchJsonV2<ManagedPlan>(envId, `${planUrl(ctx, planId)}/_${action}`, { method: 'POST', body: JSON.stringify({}) });
}

export async function deletePlan(envId: string, ctx: PlanContext, planId: string): Promise<void> {
    return apimFetchJsonV2<void>(envId, planUrl(ctx, planId), { method: 'DELETE' });
}
