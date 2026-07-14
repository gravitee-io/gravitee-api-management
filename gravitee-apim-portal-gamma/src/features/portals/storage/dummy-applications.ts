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
import type { Application } from '../../editor/entities/application';

export function createDummyApplications(): Application[] {
    return [
        {
            id: 'app-mobile',
            name: 'Mobile Banking App',
            description: 'iOS and Android consumer banking application.',
            domain: 'mobile.example.com',
            applicationType: 'NATIVE',
            api_key_mode: 'EXCLUSIVE',
            portalTenantId: 'tenant-acme',
            created_at: '2025-01-15T10:00:00Z',
            updated_at: '2025-06-01T08:00:00Z',
            owner: {
                id: 'user-1',
                display_name: 'Admin User',
                email: 'admin@example.com',
            },
            settings: {
                oauth: {
                    client_id: 'mobile-banking-client',
                    client_secret: 'mobile-secret-xyz789',
                    redirect_uris: ['com.example.banking://callback'],
                    grant_types: ['authorization_code', 'refresh_token'],
                    application_type: 'native',
                },
            },
        },
        {
            id: 'app-web-portal',
            name: 'Customer Web Portal',
            description: 'Single-page application for end-customer self-service.',
            domain: 'portal.example.com',
            applicationType: 'SPA',
            api_key_mode: 'EXCLUSIVE',
            portalTenantId: 'tenant-acme',
            created_at: '2025-02-20T14:30:00Z',
            updated_at: '2025-06-02T10:00:00Z',
            owner: {
                id: 'user-2',
                display_name: 'Portal Owner',
                email: 'portal@example.com',
            },
            settings: {
                oauth: {
                    client_id: 'web-portal-client',
                    client_secret: 'portal-secret-def456',
                    redirect_uris: ['https://portal.example.com/callback'],
                    grant_types: ['authorization_code', 'refresh_token'],
                    application_type: 'browser',
                },
            },
        },
        {
            id: 'app-partner',
            name: 'Partner Integration',
            description: 'Backend-to-backend integration for partner systems.',
            applicationType: 'BACKEND_TO_BACKEND',
            api_key_mode: 'SHARED',
            portalTenantId: 'tenant-beta',
            created_at: '2025-03-10T09:00:00Z',
            updated_at: '2025-05-20T12:00:00Z',
            owner: {
                id: 'user-3',
                display_name: 'Integration Lead',
                email: 'integration@example.com',
            },
            settings: {
                oauth: {
                    client_id: 'partner-b2b-client',
                    client_secret: 'partner-secret-abc123',
                    grant_types: ['client_credentials'],
                    application_type: 'web',
                },
            },
        },
        {
            id: 'app-internal',
            name: 'Internal Tools',
            description: 'Simple application for internal tooling and scripts.',
            applicationType: 'SIMPLE',
            api_key_mode: 'EXCLUSIVE',
            created_at: '2025-04-05T16:45:00Z',
            updated_at: '2025-04-05T16:45:00Z',
            owner: {
                id: 'user-4',
                display_name: 'Internal Tools Owner',
                email: 'tools@example.com',
            },
            settings: {
                app: {
                    type: 'internal',
                    client_id: 'internal-tools-client',
                },
            },
        },
        {
            id: 'app-analytics',
            name: 'Analytics Dashboard',
            description: 'Web application for business intelligence dashboards.',
            domain: 'analytics.example.com',
            applicationType: 'WEB',
            api_key_mode: 'EXCLUSIVE',
            created_at: '2025-05-12T11:20:00Z',
            updated_at: '2025-06-10T14:00:00Z',
            owner: {
                id: 'user-5',
                display_name: 'Analytics Admin',
                email: 'analytics@example.com',
            },
            settings: {
                oauth: {
                    client_id: 'analytics-web',
                    client_secret: 'analytics-secret-ghi012',
                    redirect_uris: ['https://analytics.example.com/oauth'],
                    grant_types: ['authorization_code'],
                    application_type: 'web',
                },
            },
        },
    ];
}
