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
import type { Portal, PortalsResponse, PortalsStats } from '../features/portals/types/portal';

export const TEST_CONFIG = {
    gammaBaseURL: 'http://api.test/gamma',
    managementBaseURL: 'http://api.test/management',
    environmentId: 'DEFAULT',
    organizationId: 'DEFAULT',
} as const;

export const TEST_V2_BASE = `${TEST_CONFIG.managementBaseURL}/v2/environments/${TEST_CONFIG.environmentId}`;

export function createPortal(overrides: Partial<Portal> = {}): Portal {
    return {
        id: '00f8c9e7-78fc-4907-b8c9-e778fc790750',
        name: 'Developer Portal',
        url: null,
        default: true,
        logoUrl: null,
        publishedApisCount: 12,
        ...overrides,
    };
}

export function createPortalsResponse(overrides: Partial<PortalsResponse> = {}): PortalsResponse {
    return {
        data: [createPortal()],
        pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
        ...overrides,
    };
}

export function createPortalsStats(overrides: Partial<PortalsStats> = {}): PortalsStats {
    return {
        quota: { used: 1, limit: 10 },
        publishedApisCount: 12,
        ...overrides,
    };
}
