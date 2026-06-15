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
import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiKey,
    ApiKeyPage,
    Application,
    ApplicationPage,
    ApproveSubscriptionPayload,
    CreateSubscriptionPayload,
    PlanPage,
    Subscription,
    SubscriptionContext,
    SubscriptionPage,
    SubscriptionStatus,
} from '../types/subscription';

function buildQuery(params: Record<string, string | string[] | number | boolean | undefined>): string {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || (Array.isArray(v) && v.length === 0)) continue;
        p.set(k, Array.isArray(v) ? v.join(',') : String(v));
    }
    const s = p.toString();
    return s ? `?${s}` : '';
}

const entityBase = (ctx: SubscriptionContext) =>
    ctx.type === 'api' ? `/apis/${encodeURIComponent(ctx.entityId)}` : `/api-products/${encodeURIComponent(ctx.entityId)}`;

const sub = (ctx: SubscriptionContext, subId: string) => `${entityBase(ctx)}/subscriptions/${encodeURIComponent(subId)}`;

export async function listSubscriptions(
    envId: string,
    ctx: SubscriptionContext,
    filters: {
        statuses?: SubscriptionStatus[];
        planIds?: string[];
        applicationIds?: string[];
        apiKey?: string;
        page?: number;
        perPage?: number;
    },
): Promise<SubscriptionPage> {
    const q = buildQuery({
        statuses: filters.statuses,
        planIds: filters.planIds,
        applicationIds: filters.applicationIds,
        apiKey: filters.apiKey,
        page: filters.page ?? 1,
        perPage: filters.perPage ?? 10,
        expands: 'plan,application,subscribedBy',
    });
    return apimFetchJsonV2<SubscriptionPage>(envId, `${entityBase(ctx)}/subscriptions${q}`);
}

export async function getSubscription(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}?expands=plan,application,subscribedBy`);
}

export async function createSubscription(
    envId: string,
    ctx: SubscriptionContext,
    payload: CreateSubscriptionPayload,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${entityBase(ctx)}/subscriptions`, { method: 'POST', body: JSON.stringify(payload) });
}

export async function transferSubscription(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    planId: string,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_transfer`, {
        method: 'POST',
        body: JSON.stringify({ planId }),
    });
}

export async function pauseSubscription(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_pause`, { method: 'POST', body: JSON.stringify({}) });
}

export async function resumeSubscription(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_resume`, { method: 'POST', body: JSON.stringify({}) });
}

export async function closeSubscription(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_close`, { method: 'POST', body: JSON.stringify({}) });
}

export async function updateSubscriptionEndDate(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    endingAt: string | null,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, sub(ctx, subscriptionId), { method: 'PUT', body: JSON.stringify({ endingAt }) });
}

/**
 * Update a subscription's metadata in place. AI Products use this to change a user's personal
 * token budget / rate limit after onboarding (the limits live on the subscription, not the plan,
 * so editing them here re-personalises that one user without touching anyone else).
 */
export async function updateSubscriptionMetadata(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    metadata: Record<string, string>,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, sub(ctx, subscriptionId), { method: 'PUT', body: JSON.stringify({ metadata }) });
}

export async function listApiKeys(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    page = 1,
    perPage = 10,
): Promise<ApiKeyPage> {
    return apimFetchJsonV2<ApiKeyPage>(envId, `${sub(ctx, subscriptionId)}/api-keys${buildQuery({ page, perPage })}`);
}

export async function renewApiKey(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<ApiKey> {
    return apimFetchJsonV2<ApiKey>(envId, `${sub(ctx, subscriptionId)}/api-keys/_renew`, { method: 'POST', body: JSON.stringify({}) });
}

export async function revokeApiKey(envId: string, ctx: SubscriptionContext, subscriptionId: string, apiKeyId: string): Promise<void> {
    return apimFetchJsonV2<void>(envId, `${sub(ctx, subscriptionId)}/api-keys/${encodeURIComponent(apiKeyId)}/_revoke`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function expireApiKey(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    apiKeyId: string,
    expireAt: string,
): Promise<ApiKey> {
    return apimFetchJsonV2<ApiKey>(envId, `${sub(ctx, subscriptionId)}/api-keys/${encodeURIComponent(apiKeyId)}`, {
        method: 'PUT',
        body: JSON.stringify({ expireAt }),
    });
}

export async function approveSubscription(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    payload: ApproveSubscriptionPayload,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_accept`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function rejectSubscription(
    envId: string,
    ctx: SubscriptionContext,
    subscriptionId: string,
    reason: string,
): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_reject`, {
        method: 'POST',
        body: JSON.stringify({ reason }),
    });
}

export async function resumeFailedSubscription(envId: string, ctx: SubscriptionContext, subscriptionId: string): Promise<Subscription> {
    return apimFetchJsonV2<Subscription>(envId, `${sub(ctx, subscriptionId)}/_resumeFailure`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function listApiPlans(envId: string, ctx: SubscriptionContext): Promise<PlanPage> {
    return apimFetchJsonV2<PlanPage>(envId, `${entityBase(ctx)}/plans${buildQuery({ statuses: 'PUBLISHED', perPage: 100 })}`);
}

interface V1ApplicationEntry {
    id: string;
    name: string;
    description?: string;
    type?: string;
    apiKeyMode?: Application['apiKeyMode'];
    owner?: { displayName: string; id?: string; email?: string };
    primaryOwner?: { displayName: string; id?: string; email?: string };
}

export async function searchApplications(envId: string, query: string): Promise<ApplicationPage> {
    const res = await apimFetchJsonV1Env<{ data: V1ApplicationEntry[]; metadata?: { pagination?: { total?: number } } }>(
        envId,
        `/applications/_paged${buildQuery({ query: query.trim(), status: 'ACTIVE', page: 1, size: 20, order: 'name' })}`,
    );
    const data = (res.data ?? []).map(
        (app): Application => ({
            id: app.id,
            name: app.name,
            description: app.description,
            type: app.type,
            apiKeyMode: app.apiKeyMode,
            primaryOwner: app.primaryOwner ?? (app.owner ? { displayName: app.owner.displayName } : undefined),
        }),
    );
    return { data, pagination: { totalCount: res.metadata?.pagination?.total ?? 0 } };
}
