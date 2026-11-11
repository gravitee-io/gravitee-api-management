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
export const CREDENTIAL_STRATEGIES = ['BEARER', 'API_KEY', 'OAUTH2', 'BASIC', 'SERVICE_ACCOUNT'] as const;
export type CredentialStrategy = (typeof CREDENTIAL_STRATEGIES)[number];

export type CredentialSettings = Record<string, string | string[]>;

export interface CredentialSecretReference {
    reference: string;
    /** Gravitee stores the value behind the reference. */
    managed: boolean;
}

/** v2 `Credential`. No response carries a secret value. */
export interface Credential {
    id: string;
    name: string;
    strategy: CredentialStrategy;
    serviceId?: string;
    ownerId?: string;
    expiresAt?: string;
    configuration?: CredentialSettings;
    secretReferences?: Record<string, CredentialSecretReference>;
    managed?: boolean;
    createdAt?: string;
    updatedAt?: string;
    rotatedAt?: string;
}

export interface CredentialsPagination {
    page: number;
    perPage: number;
    pageCount: number;
    pageItemsCount: number;
    totalCount: number;
}

export interface CredentialsPagedResponse {
    data: Credential[];
    pagination: CredentialsPagination;
}

export interface CreateCredentialPayload {
    name: string;
    strategy: CredentialStrategy;
    serviceId?: string;
    ownerId?: string;
    expiresAt?: string;
    configuration?: CredentialSettings;
    references?: Record<string, string>;
    values?: Record<string, string>;
}

export interface UpdateCredentialPayload {
    name: string;
    serviceId?: string;
    ownerId?: string;
    expiresAt?: string;
    configuration?: CredentialSettings;
    references?: Record<string, string>;
}

export interface RotateCredentialPayload {
    values: Record<string, string>;
}

export interface CredentialConsumer {
    module: string;
    type: string;
    id: string;
    name: string;
}
