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
import type { PortalNavigationItem } from '../../portals/types';

export function isFooterNavItem(item: PortalNavigationItem): boolean {
    return item.area === 'FOOTER';
}

export function isUserMenuRootItem(item: PortalNavigationItem): boolean {
    return item.parentId === null && item.area === 'USER_MENU';
}

export function belongsToUserMenu(
    item: PortalNavigationItem,
    allItems: readonly PortalNavigationItem[],
): boolean {
    if (item.area === 'USER_MENU') {
        return true;
    }

    if (!item.parentId) {
        return false;
    }

    const parent = allItems.find(navItem => navItem.id === item.parentId);
    return parent ? belongsToUserMenu(parent, allItems) : false;
}

export function isHeaderRootNavItem(item: PortalNavigationItem): boolean {
    return item.parentId === null && !isFooterNavItem(item) && !isUserMenuRootItem(item);
}

export function collectDescendantIds(
    navItems: readonly PortalNavigationItem[],
    parentId: string,
): string[] {
    const children = navItems.filter(item => item.parentId === parentId);
    return children.flatMap(child => [child.id, ...collectDescendantIds(navItems, child.id)]);
}

export function hasNavItemChildren(
    navItems: readonly PortalNavigationItem[],
    itemId: string,
): boolean {
    return navItems.some(item => item.parentId === itemId);
}

export function collectIdsToDelete(
    navItems: readonly PortalNavigationItem[],
    itemId: string,
): string[] {
    return [itemId, ...collectDescendantIds(navItems, itemId)];
}

export function getNextSiblingOrder(siblings: readonly PortalNavigationItem[]): number {
    if (siblings.length === 0) {
        return 0;
    }

    return Math.max(...siblings.map(sibling => sibling.order)) + 1;
}

export function compareNavItemsByOrder(
    left: PortalNavigationItem,
    right: PortalNavigationItem,
): number {
    const orderDiff = left.order - right.order;
    return orderDiff !== 0 ? orderDiff : left.id.localeCompare(right.id);
}

export function sortNavItemsByOrder<T extends PortalNavigationItem>(items: readonly T[]): T[] {
    return [...items].sort((left, right) => compareNavItemsByOrder(left, right));
}
