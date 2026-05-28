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
    ApiProductSearchResponse,
    ApiSearchResponse,
    ApplicationSubscriptionApiKeyEntity,
    ApplicationSubscriptionEntity,
    ApplicationSubscriptionsFilters,
    ApplicationSubscriptionsPagedResponse,
    CreatedApplicationSubscription,
    NewApplicationSubscriptionPayload,
    SubscribablePlansResponse,
    SubscribedApi,
    SubscriptionReferenceType,
} from '../types/applicationSubscription';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Path parent for Management API v2 subscription API key update (same as console `ApiSubscriptionV2Service`). */
export type SubscriptionApiKeyV2Parent = { kind: 'API'; apiId: string } | { kind: 'API_PRODUCT'; apiProductId: string };

/**
 * Resolves the Management API v2 parent resource needed to mutate API keys for
 * subscriptions backed by either an API or an API Product.
 */
export function resolveSubscriptionApiKeyV2Parent(entity: ApplicationSubscriptionEntity): SubscriptionApiKeyV2Parent | null {
    const ref: SubscriptionReferenceType | undefined = entity.referenceType;
    if (ref === 'API_PRODUCT' || entity.apiProduct?.id) {
        const apiProductId = entity.apiProduct?.id ?? entity.referenceId;
        return apiProductId ? { kind: 'API_PRODUCT', apiProductId } : null;
    }
    const apiId = entity.api?.id ?? (ref === 'API' ? entity.referenceId : undefined);
    return apiId ? { kind: 'API', apiId } : null;
}

/**
 * Builds the subscriptions list query string the same way as console `ApplicationService.getSubscriptionsPage`
 * (comma-separated list values, commas not percent-encoded).
 */
export function buildApplicationSubscriptionsQuery(page: number, size: number, filters?: ApplicationSubscriptionsFilters): string {
    const parts = [`page=${page}`, `size=${size}`];
    if (filters?.status && filters.status.length > 0) {
        parts.push(`status=${filters.status.join(',')}`);
    }
    if (filters?.apis && filters.apis.length > 0) {
        parts.push(`api=${filters.apis.join(',')}`);
    }
    if (filters?.apiKey) {
        parts.push(`api_key=${encodeURIComponent(filters.apiKey)}`);
    }
    if (filters?.securityTypes && filters.securityTypes.length > 0) {
        parts.push(`security_types=${filters.securityTypes.join(',')}`);
    }
    return parts.join('&');
}

export async function listApplicationSubscriptions(
    environmentId: string,
    applicationId: string,
    filters: ApplicationSubscriptionsFilters | undefined,
    page: number,
    size: number,
): Promise<ApplicationSubscriptionsPagedResponse> {
    return apimFetchJsonV1Env<ApplicationSubscriptionsPagedResponse>(
        environmentId,
        `/applications/${applicationId}/subscriptions?${buildApplicationSubscriptionsQuery(page, size, filters)}`,
    );
}

export async function listSubscribedApis(environmentId: string, applicationId: string): Promise<SubscribedApi[]> {
    return apimFetchJsonV1Env<SubscribedApi[]>(environmentId, `/applications/${applicationId}/subscribed`);
}

export async function searchApisForSubscription(environmentId: string, query: string, page = 1, perPage = 20): Promise<ApiSearchResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    return apimFetchJsonV2<ApiSearchResponse>(environmentId, `/apis/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ query }),
    });
}

export async function searchApiProductsForSubscription(
    environmentId: string,
    query: string,
    page = 1,
    perPage = 20,
): Promise<ApiProductSearchResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    return apimFetchJsonV2<ApiProductSearchResponse>(environmentId, `/api-products/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ query }),
    });
}

export async function listSubscribableApiPlans(
    environmentId: string,
    apiId: string,
    applicationId: string,
): Promise<SubscribablePlansResponse> {
    const params = new URLSearchParams({ page: '1', perPage: '9999', subscribableBy: applicationId });
    return apimFetchJsonV2<SubscribablePlansResponse>(environmentId, `/apis/${apiId}/plans?${params}`);
}

export async function listSubscribableApiProductPlans(
    environmentId: string,
    apiProductId: string,
    applicationId: string,
): Promise<SubscribablePlansResponse> {
    const params = new URLSearchParams({ page: '1', perPage: '9999', subscribableBy: applicationId });
    return apimFetchJsonV2<SubscribablePlansResponse>(environmentId, `/api-products/${apiProductId}/plans?${params}`);
}

export async function createApplicationSubscription(
    environmentId: string,
    applicationId: string,
    planId: string,
    payload: NewApplicationSubscriptionPayload,
): Promise<CreatedApplicationSubscription> {
    const params = new URLSearchParams({ plan: planId });
    return apimFetchJsonV1Env<CreatedApplicationSubscription>(environmentId, `/applications/${applicationId}/subscriptions?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(payload),
    });
}

export async function getApplicationSubscription(
    environmentId: string,
    applicationId: string,
    subscriptionId: string,
): Promise<ApplicationSubscriptionEntity> {
    return apimFetchJsonV1Env<ApplicationSubscriptionEntity>(
        environmentId,
        `/applications/${applicationId}/subscriptions/${subscriptionId}`,
    );
}

export async function updateApplicationSubscription(
    environmentId: string,
    applicationId: string,
    subscriptionId: string,
    subscription: ApplicationSubscriptionEntity,
): Promise<ApplicationSubscriptionEntity> {
    return apimFetchJsonV1Env<ApplicationSubscriptionEntity>(
        environmentId,
        `/applications/${applicationId}/subscriptions/${subscriptionId}`,
        {
            method: 'PUT',
            headers: JSON_HEADERS,
            body: JSON.stringify(subscription),
        },
    );
}

export async function closeApplicationSubscription(environmentId: string, applicationId: string, subscriptionId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `/applications/${applicationId}/subscriptions/${subscriptionId}`, { method: 'DELETE' });
}

export async function listSubscriptionApiKeys(
    environmentId: string,
    applicationId: string,
    subscriptionId: string,
): Promise<ApplicationSubscriptionApiKeyEntity[]> {
    return apimFetchJsonV1Env<ApplicationSubscriptionApiKeyEntity[]>(
        environmentId,
        `/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys`,
    );
}

export async function renewSubscriptionApiKey(environmentId: string, applicationId: string, subscriptionId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys/_renew`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: '{}',
    });
}

export async function revokeSubscriptionApiKey(
    environmentId: string,
    applicationId: string,
    subscriptionId: string,
    apiKeyId: string,
): Promise<void> {
    await apimFetchJsonV1Env<void>(
        environmentId,
        `/applications/${applicationId}/subscriptions/${subscriptionId}/apikeys/${encodeURIComponent(apiKeyId)}`,
        { method: 'DELETE' },
    );
}

export async function expireSubscriptionApiKey(
    environmentId: string,
    parent: SubscriptionApiKeyV2Parent | null,
    subscriptionId: string,
    apiKeyId: string,
    expireAt: Date,
): Promise<void> {
    if (!parent) {
        throw new Error('Missing API/product context for expiring this key');
    }
    const encodedSub = encodeURIComponent(subscriptionId);
    const encodedKey = encodeURIComponent(apiKeyId);
    const path =
        parent.kind === 'API'
            ? `/apis/${encodeURIComponent(parent.apiId)}/subscriptions/${encodedSub}/api-keys/${encodedKey}`
            : `/api-products/${encodeURIComponent(parent.apiProductId)}/subscriptions/${encodedSub}/api-keys/${encodedKey}`;
    await apimFetchJsonV2<unknown>(environmentId, path, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ expireAt: expireAt.toISOString() }),
    });
}
