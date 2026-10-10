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
import { apimFetchBlobV2, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
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
import { dateInputToOffsetDateTime } from '../utils/dateInputToOffsetDateTime';

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

interface SubscriptionListFilters {
    statuses?: SubscriptionStatus[];
    planIds?: string[];
    applicationIds?: string[];
    apiKey?: string;
    page?: number;
    perPage?: number;
}

function subscriptionFiltersQuery(filters: SubscriptionListFilters) {
    return {
        statuses: filters.statuses,
        planIds: filters.planIds,
        applicationIds: filters.applicationIds,
        apiKey: filters.apiKey,
        page: filters.page ?? 1,
        perPage: filters.perPage ?? 10,
    };
}

export async function listSubscriptions(
    envId: string,
    ctx: SubscriptionContext,
    filters: SubscriptionListFilters,
): Promise<SubscriptionPage> {
    const q = buildQuery({ ...subscriptionFiltersQuery(filters), expands: 'plan,application' });
    return apimFetchJsonV2<SubscriptionPage>(envId, `${entityBase(ctx)}/subscriptions${q}`);
}

export async function exportSubscriptionsCsv(envId: string, apiId: string, filters: SubscriptionListFilters): Promise<Blob> {
    const q = buildQuery(subscriptionFiltersQuery(filters));
    return apimFetchBlobV2(envId, `/apis/${encodeURIComponent(apiId)}/subscriptions/_export${q}`);
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
    const current = await getSubscription(envId, ctx, subscriptionId);
    return apimFetchJsonV2<Subscription>(envId, sub(ctx, subscriptionId), {
        method: 'PUT',
        body: JSON.stringify({
            startingAt: toOffsetDateTime(current.startingAt),
            endingAt,
            consumerConfiguration: current.consumerConfiguration,
            metadata: current.metadata,
        }),
    });
}

/** Management API v2 dates are OffsetDateTime. A calendar date (`YYYY-MM-DD`) fails deserialization. */
function toOffsetDateTime(value: string | undefined): string | undefined {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
    return dateInputToOffsetDateTime(value);
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

export async function renewApiKey(envId: string, ctx: SubscriptionContext, subscriptionId: string, customApiKey?: string): Promise<ApiKey> {
    return apimFetchJsonV2<ApiKey>(envId, `${sub(ctx, subscriptionId)}/api-keys/_renew`, {
        method: 'POST',
        body: JSON.stringify(customApiKey ? { customApiKey } : {}),
    });
}

export async function reactivateApiKey(envId: string, ctx: SubscriptionContext, subscriptionId: string, apiKeyId: string): Promise<ApiKey> {
    return apimFetchJsonV2<ApiKey>(envId, `${sub(ctx, subscriptionId)}/api-keys/${encodeURIComponent(apiKeyId)}/_reactivate`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
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

interface ApiSubscriberEntry {
    id: string;
    name?: string;
}

interface ApiSubscribersPage {
    data?: ApiSubscriberEntry[];
    pagination?: { totalCount?: number };
}

/** Applications subscribed to an API (Classic `getSubscribers`). */
export async function listApiSubscribers(
    envId: string,
    apiId: string,
    options?: { page?: number; perPage?: number; name?: string; signal?: AbortSignal },
): Promise<ApiSubscribersPage> {
    const q = buildQuery({
        page: options?.page ?? 1,
        perPage: options?.perPage ?? 100,
        name: options?.name,
    });
    return apimFetchJsonV2<ApiSubscribersPage>(envId, `/apis/${encodeURIComponent(apiId)}/subscribers${q}`, { signal: options?.signal });
}

/** Fetches every subscriber page for alert filter pickers. */
export async function listAllApiSubscribers(envId: string, apiId: string): Promise<ApiSubscriberEntry[]> {
    const perPage = 100;
    const all: ApiSubscriberEntry[] = [];
    let page = 1;
    let totalCount: number | undefined;
    while (page <= 100) {
        const response = await listApiSubscribers(envId, apiId, { page, perPage });
        const batch = response.data ?? [];
        all.push(...batch);
        totalCount = response.pagination?.totalCount ?? totalCount;
        if (batch.length === 0) {
            break;
        }
        if (totalCount !== undefined && all.length >= totalCount) {
            break;
        }
        if (batch.length < perPage) {
            break;
        }
        page += 1;
    }
    return all;
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

/** V1 application subscription row used to decide exclusive vs shared API key mode. */
export interface ApplicationApiKeySubscriptionRef {
    api?: string;
    referenceType?: string;
    referenceId?: string;
}

/**
 * Lists the application's active API-key subscriptions (classic
 * `ApplicationService.getSubscriptionsPage` with ACCEPTED/PENDING/PAUSED + API_KEY).
 */
export async function listApplicationApiKeySubscriptions(
    envId: string,
    applicationId: string,
): Promise<ApplicationApiKeySubscriptionRef[]> {
    const res = await apimFetchJsonV1Env<{ data?: ApplicationApiKeySubscriptionRef[] }>(
        envId,
        `/applications/${applicationId}/subscriptions?page=1&size=20&status=ACCEPTED,PENDING,PAUSED&security_types=API_KEY`,
    );
    return res.data ?? [];
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
