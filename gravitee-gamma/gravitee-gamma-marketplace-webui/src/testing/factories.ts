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
import type { Api, Category, IdentityProvider, Page, User } from '../api/types';
import type { BootstrapConfig } from '../shared/config/bootstrap.store';

export const TEST_CONFIG: BootstrapConfig = {
    portalBaseURL: 'http://portal.test/portal',
    environmentId: 'DEFAULT',
    organizationId: 'DEFAULT',
    identityProviders: [],
    localLoginEnabled: true,
    forceLoginEnabled: false,
};

export const TEST_PORTAL_API = `${TEST_CONFIG.portalBaseURL}/environments/${TEST_CONFIG.environmentId}`;

export function buildUser(overrides: Partial<User> = {}): User {
    return {
        id: 'user-1',
        display_name: 'Jane Doe',
        first_name: 'Jane',
        last_name: 'Doe',
        email: 'jane@example.com',
        ...overrides,
    };
}

export function buildBootstrapConfig(overrides: Partial<BootstrapConfig> = {}): BootstrapConfig {
    return { ...TEST_CONFIG, ...overrides };
}

export function buildIdentityProvider(overrides: Partial<IdentityProvider> = {}): IdentityProvider {
    return {
        id: 'google-idp',
        name: 'Google',
        client_id: 'google-client-id',
        type: 'GOOGLE',
        authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
        scopes: ['openid', 'profile', 'email'],
        color: '#4285F4',
        ...overrides,
    };
}

export function buildCategory(overrides: Partial<Category> = {}): Category {
    return {
        id: 'it',
        name: 'IT',
        description: 'IT agents',
        total_apis: 12,
        ...overrides,
    };
}

export function buildApi(overrides: Partial<Api> = {}): Api {
    return {
        id: 'api-helpdesk',
        name: 'IT Helpdesk Agent',
        version: '1.2',
        description: 'Triage and route IT tickets to the right queue.',
        type: 'A2A_PROXY',
        labels: ['ticketing', 'triage'],
        categories: ['it'],
        owner: buildUser({ id: 'owner-1', display_name: 'Acme Platform' }),
        entrypoints: ['https://gw.example/a2a/it-helpdesk'],
        ...overrides,
    };
}

export function buildApisResponse(apis: Api[], total = apis.length, page = 1, size = 12) {
    return {
        data: apis,
        metadata: {
            pagination: {
                current_page: page,
                size,
                total,
                total_pages: Math.max(1, Math.ceil(total / size)),
            },
        },
    };
}

export function buildPage(overrides: Partial<Page> = {}): Page {
    return {
        id: 'page-overview',
        name: 'Overview',
        type: 'MARKDOWN',
        order: 0,
        content: '# Getting started\n\nSubscribe to a plan, then copy the A2A endpoint.',
        ...overrides,
    };
}

export function buildPagesResponse(pages: Page[]) {
    return { data: pages };
}
