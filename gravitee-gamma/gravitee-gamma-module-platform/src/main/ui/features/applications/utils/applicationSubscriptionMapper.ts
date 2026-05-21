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
    ApplicationSubscriptionTableRow,
    ApplicationSubscriptionsPagedResponse,
    SubscriptionPageItem,
    SubscriptionStatus,
} from '../types/applicationSubscription';

const SECURITY_TYPE_LABELS: Record<string, string> = {
    API_KEY: 'API Key',
    OAUTH2: 'OAuth2',
    JWT: 'JWT',
    KEY_LESS: 'Keyless',
    MTLS: 'mTLS',
    PUSH: 'PUSH',
};

export function formatSubscriptionSecurityType(securityType: string): string {
    return SECURITY_TYPE_LABELS[securityType] ?? securityType;
}

export function canCloseSubscription(status: SubscriptionStatus | undefined): boolean {
    return status === 'ACCEPTED' || status === 'PENDING' || status === 'PAUSED';
}

function getMetadataRecord(metadata: ApplicationSubscriptionsPagedResponse['metadata'], key: string | undefined): Record<string, unknown> {
    if (!key || !metadata) return {};
    return metadata[key] ?? {};
}

export function mapSubscriptionToTableRow(
    subscription: SubscriptionPageItem,
    metadata: ApplicationSubscriptionsPagedResponse['metadata'],
    apiKeyMode: ApiKeyMode | undefined,
): ApplicationSubscriptionTableRow | null {
    if (!subscription.id || !subscription.status) return null;

    const planMetadata = getMetadataRecord(metadata, subscription.plan);
    const apiMetadataKey =
        subscription.referenceType === 'API_PRODUCT' ? subscription.referenceId : (subscription.api ?? subscription.referenceId);
    const apiMetadata = getMetadataRecord(metadata, apiMetadataKey);

    const planMode = planMetadata.planMode as string | undefined;
    const planSecurityType = planMetadata.securityType as string | undefined;
    const securityType = planMode === 'PUSH' ? 'PUSH' : formatSubscriptionSecurityType(planSecurityType ?? 'UNKNOWN');
    const isApiProduct = subscription.referenceType === 'API_PRODUCT';

    return {
        id: subscription.id,
        apiName: (apiMetadata.name as string | undefined) ?? subscription.api ?? subscription.referenceId ?? '',
        apiVersion: apiMetadata.apiVersion as string | undefined,
        referenceTypeLabel: isApiProduct ? 'API Product' : 'API',
        createdAt: subscription.created_at,
        endAt: subscription.ending_at,
        planName: (planMetadata.name as string | undefined) ?? subscription.plan ?? '',
        securityType,
        isSharedApiKey: apiKeyMode === 'SHARED' && planSecurityType === 'API_KEY',
        processedAt: subscription.processed_at,
        startingAt: subscription.starting_at,
        status: subscription.status,
        origin: subscription.origin ?? 'MANAGEMENT',
    };
}

export function mapSubscriptionsPageToRows(
    response: ApplicationSubscriptionsPagedResponse,
    apiKeyMode: ApiKeyMode | undefined,
): ApplicationSubscriptionTableRow[] {
    return (response.data ?? [])
        .map(item => mapSubscriptionToTableRow(item, response.metadata, apiKeyMode))
        .filter((row): row is ApplicationSubscriptionTableRow => row !== null);
}
