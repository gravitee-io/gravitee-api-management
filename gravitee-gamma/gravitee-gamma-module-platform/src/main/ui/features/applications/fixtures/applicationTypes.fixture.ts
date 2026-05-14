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
import type { ApplicationTypeConfig } from '../types/applicationCreate';

/**
 * Full catalog shape from gravitee-apim-rest-api-service `applications/types.json`
 * (filtered subset returned by GET .../configuration/applications/types per environment).
 */
export const APPLICATION_TYPES_FIXTURE: ApplicationTypeConfig[] = [
    {
        id: 'simple',
        name: 'Simple',
        description: 'A standalone client where you manage your own client_id. No DCR involved.',
        requires_redirect_uris: false,
        allowed_grant_types: [],
        default_grant_types: [],
        mandatory_grant_types: [],
    },
    {
        id: 'browser',
        name: 'SPA',
        description: 'Front-end JS apps (React, Angular, Vue) using DCR for authentication.',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'implicit', name: 'Implicit', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [{ type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] }],
        mandatory_grant_types: [],
    },
    {
        id: 'web',
        name: 'Web',
        description: 'Server-side web apps (e.g., .NET, Java) that authenticate users through DCR.',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'refresh_token', name: 'Refresh Token', response_types: [] },
            { type: 'implicit', name: 'Implicit (Hybrid)', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [{ type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] }],
        mandatory_grant_types: [{ type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] }],
    },
    {
        id: 'native',
        name: 'Native',
        description: 'Mobile and desktop apps (iOS, Android) that authenticate through DCR.',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'refresh_token', name: 'Refresh Token', response_types: [] },
            { type: 'password', name: 'Resource Owner Password', response_types: [] },
            { type: 'implicit', name: 'Implicit (Hybrid)', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [{ type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] }],
        mandatory_grant_types: [{ type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] }],
    },
    {
        id: 'backend_to_backend',
        name: 'Backend to backend',
        description: 'Machine-to-machine apps (scripts, daemons, CLIs) using DCR for API access.',
        requires_redirect_uris: false,
        allowed_grant_types: [{ type: 'client_credentials', name: 'Client Credentials', response_types: [] }],
        default_grant_types: [{ type: 'client_credentials', name: 'Client Credentials', response_types: [] }],
        mandatory_grant_types: [{ type: 'client_credentials', name: 'Client Credentials', response_types: [] }],
    },
];
