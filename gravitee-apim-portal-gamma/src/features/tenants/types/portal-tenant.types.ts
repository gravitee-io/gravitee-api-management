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
export interface PortalTenantFeatures {
    catalog: boolean;
    createApplication: boolean;
    subscriptions: boolean;
    dashboard: boolean;
    analytics: boolean;
    documentation: boolean;
}

export type PortalTenantApiAccessMode = 'all' | 'selected';

export interface PortalTenant {
    id: string;
    portalId: string;
    name: string;
    hrid: string;
    description?: string;
    allowedApiIds: string[];
    apiAccessMode: PortalTenantApiAccessMode;
    features: PortalTenantFeatures;
    createdAt: string;
    updatedAt: string;
}

export type PortalTenantMemberRole = 'admin' | 'member';

export interface PortalTenantMember {
    id: string;
    tenantId: string;
    userId: string;
    displayName: string;
    email: string;
    role: PortalTenantMemberRole;
}

export interface PortalUser {
    id: string;
    displayName: string;
    email: string;
}

export const DEFAULT_PORTAL_TENANT_FEATURES: PortalTenantFeatures = {
    catalog: true,
    createApplication: true,
    subscriptions: true,
    dashboard: true,
    analytics: true,
    documentation: true,
};

export const PORTAL_TENANT_FEATURE_LABELS: Record<keyof PortalTenantFeatures, { title: string; description: string }> = {
    catalog: {
        title: 'API catalog',
        description: 'Browse and search published APIs',
    },
    createApplication: {
        title: 'Create applications',
        description: 'Register new OAuth / API key applications',
    },
    subscriptions: {
        title: 'Subscriptions',
        description: 'Subscribe applications to API plans',
    },
    dashboard: {
        title: 'Application dashboard',
        description: 'Manage apps, keys, and members',
    },
    analytics: {
        title: 'Analytics',
        description: 'View API usage and traffic',
    },
    documentation: {
        title: 'Documentation pages',
        description: 'Access guides and getting-started content',
    },
};
