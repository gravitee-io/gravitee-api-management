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

/**
 * Single source of truth for application detail sidebar labels, paths, and nested routes.
 *
 * An Application is not an API object, so it does not carry the canonical API-detail groups (Design,
 * Consumers, Operations mean nothing here) — but it follows the same vocabulary (FOUND-304): the
 * entity's own settings page is `Settings`, `User Permissions` lives in `General` rather than in a
 * `Security` group of its own, and no group is named after its only item.
 *
 * `Access` rather than `Consumers` for the subscriptions: an application *is* a consumer, so its
 * subscriptions are what it reaches, not who reaches it.
 */
export const APPLICATION_NAV_GROUPS: ApplicationDetailNavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon, permissions: ['application-definition-r'] },
            { path: 'general', label: 'Settings', icon: SlidersHorizontalIcon, permissions: ['application-definition-r'] },
            { path: 'user-permissions', label: 'User Permissions', icon: ShieldCheckIcon, permissions: ['application-member-r'] },
        ],
    },
    {
        label: 'Access',
        items: [{ path: 'subscriptions', label: 'Subscriptions', icon: PlugIcon, permissions: ['application-subscription-r'] }],
    },
    {
        // A notification is what the application reports to you, not a setting you tune — same place
        // as on every API detail page.
        label: 'Monitoring',
        items: [
            {
                path: 'notifications',
                label: 'Notifications',
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
