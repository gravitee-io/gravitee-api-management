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
import type { ApplicationApiKeySubscriptionRef } from '../../../services/subscriptions';
import type { ApiKeyMode, SubscriptionContext } from '../../../types/subscription';

export function subscriptionMatchesContext(
    subscription: ApplicationApiKeySubscriptionRef,
    ctx: SubscriptionContext,
): boolean {
    if (ctx.type === 'api') {
        return subscription.api === ctx.entityId;
    }
    return subscription.referenceType === 'API_PRODUCT' && subscription.referenceId === ctx.entityId;
}

/**
 * Same rule as classic `api-portal-subscription-creation-dialog`: prompt for exclusive vs
 * shared only when the app mode is still unspecified and it already has another API-key
 * subscription (not for this API / API product).
 */
export function shouldShowApiKeyModeChoice(params: {
    applicationApiKeyMode: ApiKeyMode | undefined;
    isApiKeyPlan: boolean;
    canUseSharedApiKeys: boolean;
    isFederated: boolean;
    ctx: SubscriptionContext;
    apiKeySubscriptions: ApplicationApiKeySubscriptionRef[];
}): boolean {
    const { applicationApiKeyMode, isApiKeyPlan, canUseSharedApiKeys, isFederated, ctx, apiKeySubscriptions } = params;
    if (!isApiKeyPlan || isFederated || !canUseSharedApiKeys || applicationApiKeyMode !== 'UNSPECIFIED') {
        return false;
    }
    return apiKeySubscriptions.some(subscription => !subscriptionMatchesContext(subscription, ctx));
}
