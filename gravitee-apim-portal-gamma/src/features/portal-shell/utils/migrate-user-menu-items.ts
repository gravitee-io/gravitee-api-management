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
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import type { DeveloperPortal, PortalNavigationItem, PortalNavigationLink, UserMenuItem } from '../../portals/types';
import { ensureUniqueSlug, generateSlug } from '../../portals/utils/slug';
import { belongsToUserMenu } from './nav-items';

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function createItemSlug(
    title: string,
    id: string,
    existingItems: readonly PortalNavigationItem[],
): string {
    const existingSlugs = new Set(existingItems.map(item => item.slug));
    return ensureUniqueSlug(generateSlug(title, id), existingSlugs);
}

export function shouldMigrateUserMenuItems(
    userMenuItems: readonly UserMenuItem[],
    navItems: readonly PortalNavigationItem[],
): boolean {
    return userMenuItems.length > 0 && !navItems.some(item => belongsToUserMenu(item, navItems));
}

export async function migrateUserMenuItems(
    portal: DeveloperPortal,
    navItems: readonly PortalNavigationItem[],
): Promise<{ migratedNavItems: PortalNavigationLink[]; clearedPortal: DeveloperPortal } | null> {
    if (!shouldMigrateUserMenuItems(portal.userMenuItems, navItems)) {
        return null;
    }

    const migratedNavItems: PortalNavigationLink[] = [];
    let workingItems = [...navItems];

    for (const [order, menuItem] of portal.userMenuItems.entries()) {
        const id = menuItem.id || createUniqueId();
        const title = menuItem.label;
        const linkItem: PortalNavigationLink = {
            id,
            portalId: portal.id,
            title,
            type: 'LINK',
            parentId: null,
            order,
            slug: createItemSlug(title, id, workingItems),
            url: menuItem.url,
            area: 'USER_MENU',
        };

        await saveNavItem(linkItem);
        migratedNavItems.push(linkItem);
        workingItems = [...workingItems, linkItem];
    }

    return {
        migratedNavItems,
        clearedPortal: { ...portal, userMenuItems: [] },
    };
}
