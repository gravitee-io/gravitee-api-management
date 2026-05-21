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
import type { ApiKeyMode } from '../types/application';
import type {
    NewApplicationSubscriptionPayload,
    SubscribablePlan,
    SubscriptionPageItem,
    SubscriptionReferenceSelection,
} from '../types/applicationSubscription';

export function isPlanDisabled(plan: SubscribablePlan): boolean {
    return Boolean(plan.generalConditions) || plan.mode === 'PUSH';
}

export function planDisabledReason(plan: SubscribablePlan): string | undefined {
    if (plan.generalConditions) {
        return 'Plans with general conditions can only be subscribed through the portal.';
    }
    if (plan.mode === 'PUSH') {
        return 'Push plan subscriptions require additional configuration.';
    }
    return undefined;
}

export function subscriptionMatchesPlan(
    subscription: Pick<SubscriptionPageItem, 'api' | 'referenceType' | 'referenceId'>,
    reference: SubscriptionReferenceSelection,
): boolean {
    if (reference.type === 'API') {
        return subscription.api === reference.id;
    }
    return subscription.referenceType === 'API_PRODUCT' && subscription.referenceId === reference.id;
}

/**
 * Prompts for exclusive vs shared API keys only when the first API-key subscription
 * would make the application mode ambiguous.
 */
export function shouldShowApiKeyModeChoice(params: {
    applicationApiKeyMode: ApiKeyMode | undefined;
    planSecurityType: string | undefined;
    canUseSharedApiKeys: boolean;
    isFederatedApi: boolean;
    selectedReference: SubscriptionReferenceSelection | null;
    apiKeySubscriptions: SubscriptionPageItem[];
}): boolean {
    const { applicationApiKeyMode, planSecurityType, canUseSharedApiKeys, isFederatedApi, selectedReference, apiKeySubscriptions } = params;

    if (!selectedReference || planSecurityType !== 'API_KEY' || isFederatedApi || !canUseSharedApiKeys) {
        return false;
    }
    if (applicationApiKeyMode !== 'UNSPECIFIED') {
        return false;
    }

    return apiKeySubscriptions.some(subscription => !subscriptionMatchesPlan(subscription, selectedReference));
}

export function buildSubscriptionCreatePayload(
    request: string,
    showApiKeyModeChoice: boolean,
    apiKeyMode: ApiKeyMode | null,
): NewApplicationSubscriptionPayload {
    const payload: NewApplicationSubscriptionPayload = { request: request.trim() };
    if (showApiKeyModeChoice && apiKeyMode) {
        payload.apiKeyMode = apiKeyMode;
    }
    return payload;
}

export function getSubscriptionCreateValidationError(
    plan: SubscribablePlan,
    request: string,
    showApiKeyModeChoice: boolean,
    apiKeyMode: ApiKeyMode | null,
): string | null {
    if (isPlanDisabled(plan)) {
        return planDisabledReason(plan) ?? 'This plan cannot be subscribed.';
    }
    if (plan.commentRequired && !request.trim()) {
        return 'The message to the API owner is required';
    }
    if (showApiKeyModeChoice && !apiKeyMode) {
        return 'Choose whether to use a dedicated API key or a shared API key for this application.';
    }
    return null;
}
