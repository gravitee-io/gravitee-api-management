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
import { formatSubscriptionSecurityType } from './applicationSubscriptionMapper';
import type { ApiKeyMode } from '../types/application';
import type {
    ApplicationSubscriptionCloseTarget,
    ApplicationSubscriptionDetail,
    ApplicationSubscriptionEntity,
} from '../types/applicationSubscription';

export function mapSubscriptionEntityToDetail(
    subscription: ApplicationSubscriptionEntity,
    apiKeyMode: ApiKeyMode | undefined,
): ApplicationSubscriptionDetail | null {
    if (!subscription.id || !subscription.status) return null;

    const isApiProduct = subscription.referenceType === 'API_PRODUCT' || Boolean(subscription.apiProduct);
    const apiRef = subscription.api ?? subscription.apiProduct;
    const apiDisplay = apiRef ? (apiRef.version ? `${apiRef.name} — ${apiRef.version}` : apiRef.name) : (subscription.referenceId ?? '—');

    const planSecurity = subscription.plan?.security;
    const securityType = planSecurity
        ? formatSubscriptionSecurityType(planSecurity)
        : subscription.security
          ? formatSubscriptionSecurityType(subscription.security)
          : '—';

    return {
        id: subscription.id,
        apiDisplay,
        referenceTypeLabel: isApiProduct ? 'API Product' : 'API',
        planName: subscription.plan?.name ?? '—',
        securityType,
        status: subscription.status,
        subscribedBy: subscription.subscribed_by?.displayName ?? '—',
        request: subscription.request,
        reason: subscription.reason,
        createdAt: subscription.created_at,
        processedAt: subscription.processed_at,
        startingAt: subscription.starting_at,
        pausedAt: subscription.paused_at,
        endingAt: subscription.ending_at,
        closedAt: subscription.closed_at,
        origin: subscription.origin ?? 'MANAGEMENT',
        isSharedApiKey: apiKeyMode === 'SHARED' && planSecurity === 'API_KEY',
        metadata: subscription.metadata,
    };
}

export function mapDetailToCloseTarget(detail: ApplicationSubscriptionDetail): ApplicationSubscriptionCloseTarget {
    return {
        id: detail.id,
        referenceTypeLabel: detail.referenceTypeLabel,
        securityType: detail.securityType,
        isSharedApiKey: detail.isSharedApiKey,
    };
}
