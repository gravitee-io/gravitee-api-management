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

import { filterNavSections, firstNavItemKey, NAV_SECTIONS, type PlatformNavSection } from './navigation';
import { DEFAULT_ROUTE_KEY, ROUTE_KEYS } from './routes';
import { ENVIRONMENT_ALERT_READ_PERMISSION } from '../features/alerts/utils/alertPermissions';
import { ENVIRONMENT_AUDIT_READ_PERMISSION, ORGANIZATION_AUDIT_READ_PERMISSION } from '../features/audit-logs/utils/auditPermissions';
import { ENVIRONMENT_GROUP_READ_PERMISSION } from '../features/groups/utils/groupPermissions';
import { ORGANIZATION_NOTIFICATION_TEMPLATES_READ } from '../features/notification-templates/utils/permissions';
import { ORGANIZATION_POLICIES_ACCESS_PERMISSIONS } from '../features/platform-policies/utils/platformPolicyPermissions';
import { ENVIRONMENT_SHARED_POLICY_GROUP_READ_PERMISSION } from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { ORGANIZATION_USER_ACCESS_PERMISSIONS } from '../features/users/utils/userPermissions';

export const ORGANIZATION_SETTINGS_READ_PERMISSION = 'organization-settings-r' as const;
export const ORGANIZATION_SETTINGS_GATE_PERMISSIONS = [ORGANIZATION_SETTINGS_READ_PERMISSION, 'organization-settings-u'] as const;

export const ENVIRONMENT_APPLICATION_READ_PERMISSION = 'environment-application-r' as const;
export const ENVIRONMENT_AM_CONFIGURATION_READ_PERMISSION = 'environment-am_configuration-r' as const;
export const ENVIRONMENT_SETTINGS_READ_PERMISSION = 'environment-settings-r' as const;
export const ORGANIZATION_TAG_READ_PERMISSION = 'organization-tag-r' as const;
export const ORGANIZATION_TENANT_READ_PERMISSION = 'organization-tenant-r' as const;
export const ORGANIZATION_IDENTITY_PROVIDER_READ_PERMISSION = 'organization-identity_provider-r' as const;

export const NO_ACCESS_ROUTE_KEY = 'no-access' as const;

const ORGANIZATION_SETTINGS_GATED_ITEMS: ReadonlySet<string> = new Set([
    'tenants',
    'entrypoints-and-sharding-tags',
    'policy-studio',
    'authentication',
    'management-and-schedulers',
    'cors',
    'smtp',
    'templates',
    'organization-audit',
    'users',
]);

/**
 * Item-level anyOf permissions. Classic org nav is the reference for Classic-equivalent
 * items: Tenants and Entrypoints use `organization-tenant-r` / `organization-tag-r`
 * after the org-settings outer gate (organization-navigation.service.ts). Gamma does
 * not also accept environment-tenant-r or environment-entrypoint-r for those items.
 *
 * Two deliberate divergences: Classic filters its org Audit item on license only, while
 * Gamma also requires `organization-audit-r`; and Access Management is environment-scoped
 * here, outside the org-settings gate, because it is Gamma-only.
 */
export const NAV_ITEM_PERMISSIONS: Readonly<Record<string, readonly string[]>> = {
    tenants: [ORGANIZATION_TENANT_READ_PERMISSION],
    'entrypoints-and-sharding-tags': [ORGANIZATION_TAG_READ_PERMISSION],
    'policy-studio': ORGANIZATION_POLICIES_ACCESS_PERMISSIONS,
    'access-management': [ENVIRONMENT_AM_CONFIGURATION_READ_PERMISSION],
    authentication: [ORGANIZATION_IDENTITY_PROVIDER_READ_PERMISSION],
    'management-and-schedulers': [ORGANIZATION_SETTINGS_READ_PERMISSION],
    cors: [ORGANIZATION_SETTINGS_READ_PERMISSION],
    smtp: [ORGANIZATION_SETTINGS_READ_PERMISSION],
    templates: [ORGANIZATION_NOTIFICATION_TEMPLATES_READ],
    'organization-audit': [ORGANIZATION_AUDIT_READ_PERMISSION],
    applications: [ENVIRONMENT_APPLICATION_READ_PERMISSION],
    metadata: ['environment-metadata-r'],
    dictionaries: ['environment-dictionary-r'],
    'shared-policy-groups': [ENVIRONMENT_SHARED_POLICY_GROUP_READ_PERMISSION],
    gateways: ['environment-instance-r'],
    alerts: [ENVIRONMENT_ALERT_READ_PERMISSION],
    'security-plan-types': [ENVIRONMENT_SETTINGS_READ_PERMISSION],
    'environment-audit': [ENVIRONMENT_AUDIT_READ_PERMISSION],
    users: ORGANIZATION_USER_ACCESS_PERMISSIONS,
    groups: [ENVIRONMENT_GROUP_READ_PERMISSION],
};

const MODULE_LEAF_KEYS: ReadonlySet<string> = new Set(ROUTE_KEYS);

export interface NavVisibilityInput {
    readonly permissionsReady: boolean;
    readonly has: (permission: string) => boolean;
    readonly metadataForbidden?: boolean;
    readonly dictionariesForbidden?: boolean;
    /** Items shown but not enterable (missing license). Visible in the sidebar, never a landing target. */
    readonly lockedItemKeys?: readonly string[];
}

export function requiresOrganizationSettingsGate(itemKey: string): boolean {
    return ORGANIZATION_SETTINGS_GATED_ITEMS.has(itemKey);
}

export function pageGuardForNavItem(itemKey: string): {
    readonly anyOf: readonly string[];
    readonly alsoAnyOf?: readonly string[];
} {
    const itemPermissions = NAV_ITEM_PERMISSIONS[itemKey];
    if (itemPermissions === undefined) {
        return { anyOf: [] };
    }
    if (!requiresOrganizationSettingsGate(itemKey)) {
        return { anyOf: itemPermissions };
    }
    if (itemPermissions.length === 0) {
        return { anyOf: ORGANIZATION_SETTINGS_GATE_PERMISSIONS };
    }
    return { anyOf: itemPermissions, alsoAnyOf: ORGANIZATION_SETTINGS_GATE_PERMISSIONS };
}

function hasAny(has: (permission: string) => boolean, permissions: readonly string[]): boolean {
    return permissions.some(permission => has(permission));
}

export function isNavItemVisible(itemKey: string, visibility: NavVisibilityInput): boolean {
    if (!visibility.permissionsReady) {
        return false;
    }
    const itemPermissions = NAV_ITEM_PERMISSIONS[itemKey];
    if (!itemPermissions) {
        return false;
    }
    if (requiresOrganizationSettingsGate(itemKey) && !hasAny(visibility.has, ORGANIZATION_SETTINGS_GATE_PERMISSIONS)) {
        return false;
    }
    if (itemPermissions.length > 0 && !hasAny(visibility.has, itemPermissions)) {
        return false;
    }
    if (itemKey === 'metadata' && visibility.metadataForbidden) {
        return false;
    }
    if (itemKey === 'dictionaries' && visibility.dictionariesForbidden) {
        return false;
    }
    return true;
}

export function firstVisibleNavItemKey(
    sections: readonly PlatformNavSection[],
    isVisible: (itemKey: string) => boolean,
): string | undefined {
    const firstSection = filterNavSections(sections, isVisible)[0];
    return firstSection ? firstNavItemKey(firstSection) : undefined;
}

export function visibleNavItemKeys(visibility: NavVisibilityInput): string[] {
    return NAV_SECTIONS.flatMap(section => section.groups.flatMap(group => group.items.map(item => item.key))).filter(itemKey =>
        isNavItemVisible(itemKey, visibility),
    );
}

/**
 * Prefer Applications when visible; otherwise the first remaining nav item.
 *
 * Locked items are excluded: their route redirects away, and every redirect out of a page lands back
 * here, so choosing one as the landing target would bounce between the two forever.
 */
export function landingNavItemKey(visibility: NavVisibilityInput): string | undefined {
    const locked = new Set(visibility.lockedItemKeys ?? []);
    const keys = visibleNavItemKeys(visibility).filter(itemKey => !locked.has(itemKey));
    if (keys.includes(DEFAULT_ROUTE_KEY)) {
        return DEFAULT_ROUTE_KEY;
    }
    return keys[0];
}

/** Replace the module leaf (a platform route key) with `itemKey`, keeping host prefixes. */
export function modulePathFor(pathname: string, itemKey: string): string {
    const segments = pathname.split('/').filter(Boolean);
    let leafIndex = -1;
    for (let index = segments.length - 1; index >= 0; index--) {
        if (MODULE_LEAF_KEYS.has(segments[index]!)) {
            leafIndex = index;
            break;
        }
    }
    const prefix = leafIndex >= 0 ? segments.slice(0, leafIndex) : segments;
    return `/${[...prefix, itemKey].join('/')}`;
}
