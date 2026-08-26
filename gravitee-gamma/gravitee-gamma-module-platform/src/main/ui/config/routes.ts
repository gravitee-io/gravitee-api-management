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
import type { ModuleRouteConfig } from '@gravitee/gamma-modules-sdk/routing';

export const ROUTE_KEYS: readonly string[] = [
    'applications',
    'users',
    'groups',
    'roles',
    'access-management',
    'authentication',
    'metadata',
    'dictionaries',
    'shared-policy-groups',
    'security-plan-types',
    'gateways',
    'tenants',
    'entrypoints-and-sharding-tags',
    'policy-studio',
    'alerts',
    'notification-settings',
    'organization-audit',
    'environment-audit',
    'management-and-schedulers',
    'cors',
    'smtp',
    'templates',
    'no-access',
];
export type RouteKey = (typeof ROUTE_KEYS)[number];

export const DEFAULT_ROUTE_KEY: RouteKey = 'applications';

export const ROUTES: Record<RouteKey, { readonly path: string; readonly label: string }> = {
    applications: { path: 'applications', label: 'Applications' },
    users: { path: 'users', label: 'Users' },
    groups: { path: 'groups', label: 'Groups' },
    roles: { path: 'roles', label: 'Roles' },
    'access-management': { path: 'access-management', label: 'Access Management' },
    authentication: { path: 'authentication', label: 'Authentication' },
    'security-plan-types': { path: 'security-plan-types', label: 'Security Plan Types' },
    metadata: { path: 'metadata', label: 'Metadata' },
    dictionaries: { path: 'dictionaries', label: 'Dictionaries' },
    'shared-policy-groups': { path: 'shared-policy-groups', label: 'Shared Policy Groups' },
    gateways: { path: 'gateways', label: 'Gateways' },
    tenants: { path: 'tenants', label: 'Tenants' },
    'entrypoints-and-sharding-tags': { path: 'entrypoints-and-sharding-tags', label: 'Entrypoints & Sharding Tags' },
    'policy-studio': { path: 'policy-studio', label: 'Policy Studio' },
    alerts: { path: 'alerts', label: 'Alerts' },
    'notification-settings': { path: 'notification-settings', label: 'Notification settings' },
    'organization-audit': { path: 'organization-audit', label: 'Audit' },
    'environment-audit': { path: 'environment-audit', label: 'Audit' },
    'management-and-schedulers': { path: 'management-and-schedulers', label: 'Management & Schedulers' },
    cors: { path: 'cors', label: 'CORS' },
    smtp: { path: 'smtp', label: 'SMTP' },
    templates: { path: 'templates', label: 'Templates' },
    'no-access': { path: 'no-access', label: 'No access' },
};

export const PLATFORM_ROUTE_CONFIG: ModuleRouteConfig<RouteKey> = {
    routeKeys: ROUTE_KEYS,
    routes: ROUTES,
    defaultRouteKey: DEFAULT_ROUTE_KEY,
} as const;
