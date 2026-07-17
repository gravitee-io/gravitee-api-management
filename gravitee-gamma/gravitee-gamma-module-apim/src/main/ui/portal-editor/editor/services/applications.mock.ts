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
import type { ApplicationTypeConfig } from '../entities/application';
import type { ApplicationMember } from '../entities/member';
import { PRIMARY_OWNER_ROLE } from '../entities/member';

const APPLICATION_TYPES: ApplicationTypeConfig[] = [
    {
        id: 'simple',
        name: 'Simple',
        description: 'A hands-free application. Using this type, you will be able to define the client_id by your own.',
        applicationType: 'SIMPLE',
        requires_redirect_uris: false,
        allowed_grant_types: [],
        default_grant_types: [],
        mandatory_grant_types: [],
    },
    {
        id: 'browser',
        name: 'SPA',
        description: 'Angular, React, Ember, ...',
        applicationType: 'SPA',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'implicit', name: 'Implicit', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
        ],
        mandatory_grant_types: [],
    },
    {
        id: 'web',
        name: 'Web',
        description: 'Java, .Net, ...',
        applicationType: 'WEB',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'refresh_token', name: 'Refresh Token', response_types: [] },
            { type: 'implicit', name: 'Implicit (Hybrid)', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
        ],
        mandatory_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
        ],
    },
    {
        id: 'native',
        name: 'Native',
        description: 'iOS, Android, ...',
        applicationType: 'NATIVE',
        requires_redirect_uris: true,
        allowed_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
            { type: 'refresh_token', name: 'Refresh Token', response_types: [] },
            { type: 'password', name: 'Resource Owner Password', response_types: [] },
            { type: 'implicit', name: 'Implicit (Hybrid)', response_types: ['token', 'id_token'] },
        ],
        default_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
        ],
        mandatory_grant_types: [
            { type: 'authorization_code', name: 'Authorization Code', response_types: ['code'] },
        ],
    },
    {
        id: 'backend_to_backend',
        name: 'Backend to backend',
        description: 'Machine to machine',
        applicationType: 'BACKEND_TO_BACKEND',
        requires_redirect_uris: false,
        allowed_grant_types: [
            { type: 'client_credentials', name: 'Client Credentials', response_types: [] },
        ],
        default_grant_types: [
            { type: 'client_credentials', name: 'Client Credentials', response_types: [] },
        ],
        mandatory_grant_types: [
            { type: 'client_credentials', name: 'Client Credentials', response_types: [] },
        ],
    },
];

const MOCK_MEMBERS: Record<string, ApplicationMember[]> = {
    'app-mobile': [
        {
            id: 'member-1',
            displayName: 'Admin User',
            email: 'admin@example.com',
            role: PRIMARY_OWNER_ROLE,
            created_at: '2025-01-15T10:00:00Z',
        },
        {
            id: 'member-2',
            displayName: 'Mobile Developer',
            email: 'mobile-dev@example.com',
            role: 'USER',
            created_at: '2025-01-20T09:00:00Z',
        },
    ],
    'app-web-portal': [
        {
            id: 'member-3',
            displayName: 'Portal Owner',
            email: 'portal@example.com',
            role: PRIMARY_OWNER_ROLE,
            created_at: '2025-02-20T14:30:00Z',
        },
    ],
    'app-partner': [
        {
            id: 'member-4',
            displayName: 'Integration Lead',
            email: 'integration@example.com',
            role: PRIMARY_OWNER_ROLE,
            created_at: '2025-03-10T09:00:00Z',
        },
        {
            id: 'member-5',
            displayName: 'Partner Admin',
            email: 'partner@example.com',
            role: 'ADMIN',
            created_at: '2025-03-12T11:00:00Z',
        },
    ],
    'app-internal': [
        {
            id: 'member-6',
            displayName: 'Internal Tools Owner',
            email: 'tools@example.com',
            role: PRIMARY_OWNER_ROLE,
            created_at: '2025-04-05T16:45:00Z',
        },
    ],
    'app-analytics': [
        {
            id: 'member-7',
            displayName: 'Analytics Admin',
            email: 'analytics@example.com',
            role: PRIMARY_OWNER_ROLE,
            created_at: '2025-05-12T11:20:00Z',
        },
        {
            id: 'member-8',
            displayName: 'Data Analyst',
            email: 'analyst@example.com',
            role: 'USER',
            created_at: '2025-05-15T08:00:00Z',
        },
    ],
};

export function getApplicationTypes(): ApplicationTypeConfig[] {
    return APPLICATION_TYPES;
}

export function getApplicationTypeById(id: string): ApplicationTypeConfig | undefined {
    return APPLICATION_TYPES.find(type => type.id === id);
}

export function getApplicationTypeByEnum(type: ApplicationTypeConfig['applicationType']): ApplicationTypeConfig | undefined {
    return APPLICATION_TYPES.find(config => config.applicationType === type);
}

export function getMembersForApplication(appId: string): ApplicationMember[] {
    return MOCK_MEMBERS[appId] ?? [];
}

export function getDefaultGrantTypes(typeConfig: ApplicationTypeConfig): string[] {
    const mandatory = typeConfig.mandatory_grant_types.map(grant => grant.type);
    const defaults = typeConfig.default_grant_types.map(grant => grant.type);
    return [...new Set([...mandatory, ...defaults])];
}
