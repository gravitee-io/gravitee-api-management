/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { PortalTenantMemberRole } from '../../tenants/types/portal-tenant.types';

export type ConsumerAuthProvider = 'local' | 'google' | 'github' | 'microsoft';

export interface PortalConsumer {
    id: string;
    portalId: string;
    tenantId: string;
    email: string;
    username?: string;
    firstName: string;
    lastName: string;
    company?: string;
    jobTitle?: string;
    useCase?: string;
    /** POC: stored in plaintext for local preview only — not for production. */
    password: string;
    authProvider: ConsumerAuthProvider;
    createdAt: string;
}

export interface PortalInvitation {
    id: string;
    portalId: string;
    tenantId: string;
    email: string;
    token: string;
    role: PortalTenantMemberRole;
    createdAt: string;
    acceptedAt?: string;
}

export interface PortalConsumerSession {
    consumerId: string;
    portalId: string;
    tenantId: string;
}

export type ConsumerUseCase =
    | 'mobile-app'
    | 'backend-service'
    | 'partner-integration'
    | 'internal-tool'
    | 'other';

export const CONSUMER_USE_CASE_OPTIONS: ReadonlyArray<{ value: ConsumerUseCase; label: string }> = [
    { value: 'mobile-app', label: 'Mobile app' },
    { value: 'backend-service', label: 'Backend service' },
    { value: 'partner-integration', label: 'Partner integration' },
    { value: 'internal-tool', label: 'Internal tool' },
    { value: 'other', label: 'Other' },
];

export interface SignupInput {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    company: string;
    jobTitle?: string;
    useCase?: ConsumerUseCase;
    marketingOptIn?: boolean;
    tenantId?: string;
}

export interface AcceptInviteInput {
    token: string;
    firstName: string;
    lastName: string;
    password: string;
    company?: string;
}
