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
            applicationType: 'NATIVE',
            api_key_mode: 'EXCLUSIVE',
            created_at: '2025-01-15T10:00:00Z',
            settings: { app: { type: 'NATIVE' } },
        },
        {
            id: 'app-web-portal',
            name: 'Customer Web Portal',
            description: 'Single-page application for end-customer self-service.',
            applicationType: 'SPA',
            api_key_mode: 'EXCLUSIVE',
            created_at: '2025-02-20T14:30:00Z',
            settings: {
                oauth: {
                    client_id: 'web-portal-client',
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
            created_at: '2025-03-10T09:00:00Z',
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
            settings: { app: { type: 'SIMPLE' } },
        },
        {
            id: 'app-analytics',
            name: 'Analytics Dashboard',
            description: 'Web application for business intelligence dashboards.',
            applicationType: 'WEB',
            api_key_mode: 'EXCLUSIVE',
            created_at: '2025-05-12T11:20:00Z',
            settings: {
                oauth: {
                    client_id: 'analytics-web',
                    redirect_uris: ['https://analytics.example.com/oauth'],
                    grant_types: ['authorization_code'],
                    application_type: 'web',
                },
            },
        },
    ];
}
