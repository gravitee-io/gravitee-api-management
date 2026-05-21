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
import type { ApiKeyMode } from './application';

export type SubscriptionStatus = 'PENDING' | 'REJECTED' | 'ACCEPTED' | 'CLOSED' | 'PAUSED' | 'RESUMED';

export type SubscriptionOrigin = 'KUBERNETES' | 'MANAGEMENT';

export type SubscriptionReferenceType = 'API' | 'API_PRODUCT';

export interface SubscriptionPageItem {
    id?: string;
    api?: string;
    plan?: string;
    application?: string;
    status?: SubscriptionStatus;
    processed_at?: number | string;
    starting_at?: number | string;
    ending_at?: number | string;
    created_at?: number | string;
    security?: string;
    origin?: SubscriptionOrigin;
    referenceType?: SubscriptionReferenceType;
    referenceId?: string;
}

export interface SubscribedApi {
    id?: string;
    name?: string;
}

export interface ApplicationSubscriptionsFilters {
    status?: SubscriptionStatus[];
    apiKey?: string;
    apis?: string[];
    securityTypes?: string[];
}

export interface ApplicationSubscriptionsPagedResponse {
    data: SubscriptionPageItem[];
    metadata?: Record<string, Record<string, unknown>>;
    page: {
        current: number;
        size: number;
        per_page: number;
        total_pages: number;
        total_elements: number;
    };
}

export interface NewApplicationSubscriptionPayload {
    request: string;
    apiKeyMode?: ApiKeyMode;
}

export interface CreatedApplicationSubscription {
    id?: string;
}

export interface ApiSearchHit {
    id: string;
    name: string;
    apiVersion?: string;
    definitionVersion?: string;
    primaryOwner?: { displayName?: string };
}

export interface ApiProductSearchHit {
    id: string;
    name: string;
    version?: string;
    primaryOwner?: { displayName?: string };
}

export type SubscriptionSearchResultItem = { type: 'API'; value: ApiSearchHit } | { type: 'API_PRODUCT'; value: ApiProductSearchHit };

export interface ApiSearchResponse {
    data: ApiSearchHit[];
}

export interface ApiProductSearchResponse {
    data: ApiProductSearchHit[];
}

export type SubscriptionReferenceSelection =
    | { type: 'API'; id: string; name: string; version?: string; isFederated?: boolean }
    | { type: 'API_PRODUCT'; id: string; name: string; version?: string };

export interface SubscribablePlan {
    id: string;
    name: string;
    security?: { type?: string };
    generalConditions?: string | null;
    commentRequired?: boolean;
    mode?: string;
    apiId?: string;
    apiProductId?: string;
    definitionVersion?: string;
}

export interface SubscribablePlansResponse {
    data: SubscribablePlan[];
}

export interface ApplicationSubscriptionEntity {
    id?: string;
    api?: {
        id: string;
        name: string;
        version?: string;
        definitionVersion?: string;
    };
    apiProduct?: {
        id: string;
        name: string;
        version?: string;
    };
    plan?: {
        id: string;
        name: string;
        security?: string;
    };
    status?: SubscriptionStatus;
    processed_at?: number | string;
    processed_by?: string;
    subscribed_by?: { id?: string; displayName?: string };
    request?: string;
    reason?: string;
    starting_at?: number | string;
    ending_at?: number | string;
    created_at?: number | string;
    updated_at?: number | string;
    paused_at?: number | string;
    closed_at?: number | string;
    security?: string;
    origin?: SubscriptionOrigin;
    referenceType?: SubscriptionReferenceType;
    referenceId?: string;
    metadata?: Record<string, string>;
}

export interface ApplicationSubscriptionCloseTarget {
    id: string;
    referenceTypeLabel: string;
    securityType: string;
    isSharedApiKey: boolean;
}

export interface ApplicationSubscriptionApiKeyEntity {
    id: string;
    key?: string;
    revoked?: boolean;
    revoked_at?: number;
    expired?: boolean;
    expire_at?: number;
    created_at?: number;
}

export interface ApplicationSubscriptionApiKeyRow {
    id: string;
    key: string;
    maskedKey: string;
    isValid: boolean;
    createdAt?: number;
    /** Revoked at when revoked, otherwise expire at (console `endDate`). */
    endDate?: number;
    /** Epoch ms from API when present — used to pre-fill the expire dialog. */
    expireAt?: number;
}

export interface ApplicationSubscriptionDetail {
    id: string;
    apiDisplay: string;
    referenceTypeLabel: string;
    planName: string;
    securityType: string;
    status: SubscriptionStatus;
    subscribedBy: string;
    request?: string;
    reason?: string;
    createdAt?: number | string;
    processedAt?: number | string;
    startingAt?: number | string;
    pausedAt?: number | string;
    endingAt?: number | string;
    closedAt?: number | string;
    origin: SubscriptionOrigin;
    isSharedApiKey: boolean;
    metadata?: Record<string, string>;
}

export interface ApplicationSubscriptionTableRow {
    id: string;
    planName: string;
    securityType: string;
    isSharedApiKey: boolean;
    apiName: string;
    apiVersion?: string;
    referenceTypeLabel: string;
    createdAt?: number | string;
    processedAt?: number | string;
    startingAt?: number | string;
    endAt?: number | string;
    status: SubscriptionStatus;
    origin: SubscriptionOrigin;
}
