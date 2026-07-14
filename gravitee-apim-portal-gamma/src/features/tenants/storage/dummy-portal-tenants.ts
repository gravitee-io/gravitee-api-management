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
import type { PortalTenant, PortalTenantMember } from '../types/portal-tenant.types';
import { DEFAULT_PORTAL_TENANT_FEATURES } from '../types/portal-tenant.types';

const PAYMENTS_PORTAL_ID = 'portal-payments';

export function createDummyPortalTenants(portalId: string = PAYMENTS_PORTAL_ID): PortalTenant[] {
    const createdAt = '2026-01-10T09:00:00.000Z';
    const updatedAt = '2026-06-15T14:30:00.000Z';

    return [
        {
            id: 'tenant-acme',
            portalId,
            name: 'Acme Corp',
            hrid: 'acme-corp',
            description: 'Payment integration partner',
            allowedApiIds: ['api-payments', 'api-billing', 'api-orders', 'api-shipping', 'api-support'],
            apiAccessMode: 'selected',
            features: {
                ...DEFAULT_PORTAL_TENANT_FEATURES,
                analytics: false,
                dashboard: false,
            },
            createdAt,
            updatedAt,
        },
        {
            id: 'tenant-beta',
            portalId,
            name: 'Beta Industries',
            hrid: 'beta-industries',
            description: 'Early adopter for refunds workflow',
            allowedApiIds: ['api-payments', 'api-billing'],
            apiAccessMode: 'selected',
            features: {
                ...DEFAULT_PORTAL_TENANT_FEATURES,
                analytics: false,
            },
            createdAt: '2026-02-01T11:00:00.000Z',
            updatedAt: '2026-05-20T10:00:00.000Z',
        },
        {
            id: 'tenant-gamma',
            portalId,
            name: 'Gamma LLC',
            hrid: 'gamma-llc',
            description: 'Sandbox tenant for onboarding demos',
            allowedApiIds: [],
            apiAccessMode: 'all',
            features: DEFAULT_PORTAL_TENANT_FEATURES,
            createdAt: '2026-03-05T08:00:00.000Z',
            updatedAt: '2026-03-05T08:00:00.000Z',
        },
    ];
}

export function createDummyPortalTenantMembers(): PortalTenantMember[] {
    return [
        {
            id: 'member-alice-acme',
            tenantId: 'tenant-acme',
            userId: 'user-alice',
            displayName: 'Alice Smith',
            email: 'alice@acme.com',
            role: 'admin',
        },
        {
            id: 'member-bob-acme',
            tenantId: 'tenant-acme',
            userId: 'user-bob',
            displayName: 'Bob Jones',
            email: 'bob@acme.com',
            role: 'member',
        },
        {
            id: 'member-carol-beta',
            tenantId: 'tenant-beta',
            userId: 'user-carol',
            displayName: 'Carol White',
            email: 'carol@example.com',
            role: 'admin',
        },
        {
            id: 'member-dan-beta',
            tenantId: 'tenant-beta',
            userId: 'user-dan',
            displayName: 'Dan Green',
            email: 'dan@example.com',
            role: 'member',
        },
        {
            id: 'member-eve-beta',
            tenantId: 'tenant-beta',
            userId: 'user-eve',
            displayName: 'Eve Black',
            email: 'eve@example.com',
            role: 'member',
        },
    ];
}
