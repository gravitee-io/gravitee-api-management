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
import { BellIcon, LayoutDashboardIcon, PlugIcon, ShieldCheckIcon, SlidersHorizontalIcon } from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';

import { APPLICATION_IMPLEMENTED_DETAIL_PATHS } from './applicationDetailPages';

export interface ApplicationDetailNavItem {
    path: string;
    label: string;
    icon: ComponentType<{ className?: string }>;
    /** When set, the tab is shown only if the user has any of these permissions (application scope). */
    permissions?: string[];
}

export interface ApplicationDetailNavGroup {
    label: string;
    items: ApplicationDetailNavItem[];
}

/** Single source of truth for application detail sidebar labels, paths, and nested routes. */
export const APPLICATION_NAV_GROUPS: ApplicationDetailNavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon, permissions: ['application-definition-r'] },
            { path: 'general', label: 'General', icon: SlidersHorizontalIcon, permissions: ['application-definition-r'] },
        ],
    },
    {
        label: 'Security',
        items: [{ path: 'user-permissions', label: 'User Permissions', icon: ShieldCheckIcon, permissions: ['application-member-r'] }],
    },
    {
        label: 'Subscriptions',
        items: [{ path: 'subscriptions', label: 'Subscriptions', icon: PlugIcon, permissions: ['application-subscription-r'] }],
    },
    {
        label: 'Settings',
        items: [
            {
                path: 'notifications',
                label: 'Notification settings',
                icon: BellIcon,
                permissions: ['application-notification-r', 'application-alert-r'],
            },
        ],
    },
];

export function flattenApplicationDetailNavItems(groups: ApplicationDetailNavGroup[]): ApplicationDetailNavItem[] {
    return groups.flatMap(group => group.items);
}

/** Console-aligned default landing tab (Global settings). */
export const APPLICATION_CONSOLE_DEFAULT_DETAIL_PATH = 'general';

export function filterApplicationDetailNavGroups(
    groups: ApplicationDetailNavGroup[],
    hasAnyPermission: (permissions: string[]) => boolean,
): ApplicationDetailNavGroup[] {
    return groups
        .map(group => ({
            ...group,
            items: group.items.filter(item => !item.permissions || hasAnyPermission(item.permissions)),
        }))
        .filter(group => group.items.length > 0);
}

export function getFirstAccessibleApplicationDetailPath(
    groups: ApplicationDetailNavGroup[],
    hasAnyPermission: (permissions: string[]) => boolean,
): string | null {
    for (const item of flattenApplicationDetailNavItems(groups)) {
        if (!item.permissions || hasAnyPermission(item.permissions)) {
            return item.path;
        }
    }
    return null;
}

/** First permission-visible tab that has a real page (skips placeholder-only routes). */
export function getFirstAccessibleImplementedApplicationDetailPath(
    groups: ApplicationDetailNavGroup[],
    implementedPaths: Set<string>,
    hasAnyPermission: (permissions: string[]) => boolean,
): string | null {
    for (const item of flattenApplicationDetailNavItems(groups)) {
        if (!implementedPaths.has(item.path)) {
            continue;
        }
        if (!item.permissions || hasAnyPermission(item.permissions)) {
            return item.path;
        }
    }
    return null;
}

export function getApplicationDetailTabPermissions(tabPath: string): string[] | undefined {
    return flattenApplicationDetailNavItems(APPLICATION_NAV_GROUPS).find(item => item.path === tabPath)?.permissions;
}

/** First tab the user may open (implemented pages preferred); null when no nav item is permitted. */
export function resolveApplicationDetailLandingPath(hasAnyPermission: (permissions: string[]) => boolean): string | null {
    return (
        getFirstAccessibleImplementedApplicationDetailPath(
            APPLICATION_NAV_GROUPS,
            APPLICATION_IMPLEMENTED_DETAIL_PATHS,
            hasAnyPermission,
        ) ?? getFirstAccessibleApplicationDetailPath(APPLICATION_NAV_GROUPS, hasAnyPermission)
    );
}

export { APPLICATION_IMPLEMENTED_DETAIL_PATHS } from './applicationDetailPages';
