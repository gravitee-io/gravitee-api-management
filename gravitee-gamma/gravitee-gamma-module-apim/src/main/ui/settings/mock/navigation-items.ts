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
export type SettingsScope = 'org' | 'env';

export interface SettingsNavItem {
    readonly key: string;
    readonly label: string;
    readonly scope: SettingsScope;
    readonly permissionKey: string;
}

export interface SettingsNavGroup {
    readonly label: string;
    readonly items: readonly SettingsNavItem[];
}

export const SETTINGS_NAV_GROUPS: readonly SettingsNavGroup[] = [
    {
        label: 'Overview',
        items: [
            { key: 'dashboard', label: 'Dashboard', scope: 'org', permissionKey: 'dashboard' },
            { key: 'applications', label: 'Applications', scope: 'env', permissionKey: 'application' },
        ],
    },
    {
        label: 'Identity & Access',
        items: [
            { key: 'users', label: 'Users', scope: 'org', permissionKey: 'user' },
            { key: 'roles', label: 'Roles', scope: 'org', permissionKey: 'role' },
            { key: 'user-groups', label: 'User Groups', scope: 'env', permissionKey: 'group' },
            { key: 'identity-providers', label: 'Identity Providers', scope: 'org', permissionKey: 'identity_provider' },
            { key: 'authentication', label: 'Authentication', scope: 'org', permissionKey: 'authentication' },
            { key: 'user-fields', label: 'User Fields', scope: 'env', permissionKey: 'user_field' },
            { key: 'primary-owner-mode', label: 'Primary Owner Mode', scope: 'env', permissionKey: 'primary_owner_mode' },
        ],
    },
    {
        label: 'APIs & Assets',
        items: [
            { key: 'api-behavior', label: 'API Behavior', scope: 'env', permissionKey: 'api_behavior' },
            { key: 'api-logging', label: 'API Logging', scope: 'env', permissionKey: 'api_logging' },
            { key: 'dictionaries-metadata', label: 'Dictionaries & Metadata', scope: 'env', permissionKey: 'dictionary' },
            { key: 'policy-groups', label: 'Policy Groups', scope: 'env', permissionKey: 'policy_group' },
            { key: 'resources', label: 'Resources', scope: 'env', permissionKey: 'resource' },
            { key: 'api-scores', label: 'API Scores (Labs)', scope: 'env', permissionKey: 'api_score' },
        ],
    },
    {
        label: 'Gateways & Infrastructure',
        items: [
            { key: 'entrypoints-sharding-tags', label: 'Entrypoints & Sharding Tags', scope: 'org', permissionKey: 'entrypoint' },
            { key: 'tenants', label: 'Tenants', scope: 'org', permissionKey: 'tenant' },
            { key: 'gateway-policies', label: 'Gateway Policies', scope: 'env', permissionKey: 'gateway_policy' },
        ],
    },
    {
        label: 'System & Security',
        items: [
            { key: 'security-plan-types', label: 'Security Plan Types', scope: 'env', permissionKey: 'security_plan' },
            { key: 'management', label: 'Management', scope: 'org', permissionKey: 'management' },
            { key: 'client-registration', label: 'Client Registration', scope: 'env', permissionKey: 'client_registration' },
            { key: 'cors-settings', label: 'CORS Settings', scope: 'env', permissionKey: 'cors' },
            { key: 'smtp-schedulers', label: 'SMTP & Schedulers', scope: 'env', permissionKey: 'smtp' },
            { key: 'notifications', label: 'Notifications', scope: 'env', permissionKey: 'notification' },
            { key: 'audit-logs', label: 'Audit Logs', scope: 'org', permissionKey: 'audit' },
            { key: 'environment-audit', label: 'Environment Audit (Labs)', scope: 'env', permissionKey: 'environment_audit' },
            { key: 'alerts', label: 'Alerts (Labs)', scope: 'env', permissionKey: 'alert' },
        ],
    },
] as const;

export const ALL_SETTINGS_ITEMS: readonly SettingsNavItem[] = SETTINGS_NAV_GROUPS.flatMap(g => g.items);

export const ALL_ENV_PERMISSION_KEYS: readonly string[] = ALL_SETTINGS_ITEMS.filter(i => i.scope === 'env').map(i => i.permissionKey);

export const ALL_ORG_PERMISSION_KEYS: readonly string[] = ALL_SETTINGS_ITEMS.filter(i => i.scope === 'org').map(i => i.permissionKey);
