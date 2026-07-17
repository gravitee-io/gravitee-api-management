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
import type { PortalNavigationItem, PortalNavigationPage } from '../types';
import {
    belongsToUserMenu,
    isFooterNavItem,
    isNavItemVisible,
    isUserMenuRootItem,
    sortNavItemsByOrder,
} from '../../portal-shell/utils/nav-items';

export function slugifyTitle(title: string): string {
    const normalized = title
        .normalize('NFKD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');

    return normalized || 'untitled';
}

export function shortIdFromItemId(itemId: string): string {
    const segments = itemId.split('-').filter(Boolean);

    // Nav items are created as `id-<timestamp36>-<random36>` — use the random tail, not the
    // shared timestamp prefix (items created in the same millisecond share the same middle segment).
    if (segments.length >= 3 && segments[0] === 'id') {
        const randomPart = segments[segments.length - 1];
        const cleaned = randomPart.replace(/[^a-z0-9]/gi, '').toLowerCase();
        if (cleaned.length > 0) {
            return cleaned.slice(0, 6).padEnd(6, '0');
        }
    }

    const cleaned = itemId.replace(/[^a-z0-9]/gi, '').toLowerCase();
    return cleaned.slice(-6).padStart(6, '0') || '000000';
}

export function generateSlug(title: string, itemId: string): string {
    return `${slugifyTitle(title)}-${shortIdFromItemId(itemId)}`;
}

export function ensureUniqueSlug(slug: string, existingSlugs: ReadonlySet<string>): string {
    if (!existingSlugs.has(slug)) {
        return slug;
    }

    let counter = 2;
    let candidate = `${slug}-${counter}`;
    while (existingSlugs.has(candidate)) {
        counter += 1;
        candidate = `${slug}-${counter}`;
    }

    return candidate;
}

export function findNavItemBySlug(
    items: readonly PortalNavigationItem[],
    slug: string,
): PortalNavigationItem | undefined {
    return items.find(item => item.slug === slug);
}

export function findFirstPageNavItem(items: readonly PortalNavigationItem[]): PortalNavigationPage | undefined {
    const isMainNavPage = (item: PortalNavigationItem): item is PortalNavigationPage =>
        item.type === 'PAGE'
        && !isFooterNavItem(item)
        && !isUserMenuRootItem(item)
        && !belongsToUserMenu(item, items);

    return (
        items.find((item): item is PortalNavigationPage => isMainNavPage(item) && item.parentId === null)
        ?? items.find((item): item is PortalNavigationPage => isMainNavPage(item))
    );
}

export function findFirstVisiblePageNavItem(items: readonly PortalNavigationItem[]): PortalNavigationPage | undefined {
    const isMainNavPage = (item: PortalNavigationItem): item is PortalNavigationPage =>
        item.type === 'PAGE'
        && !isFooterNavItem(item)
        && !isUserMenuRootItem(item)
        && !belongsToUserMenu(item, items)
        && isNavItemVisible(item, items);

    return (
        items.find((item): item is PortalNavigationPage => isMainNavPage(item) && item.parentId === null)
        ?? items.find((item): item is PortalNavigationPage => isMainNavPage(item))
    );
}

export function findFirstDescendantPageNavItem(
    items: readonly PortalNavigationItem[],
    parentId: string,
): PortalNavigationPage | undefined {
    for (const child of sortNavItemsByOrder(items.filter(item => item.parentId === parentId))) {
        if (child.type === 'PAGE') {
            return child;
        }

        if (child.type === 'FOLDER' || child.type === 'API') {
            const nested = findFirstDescendantPageNavItem(items, child.id);
            if (nested) {
                return nested;
            }
        }
    }

    return undefined;
}

export function findFirstVisibleDescendantPageNavItem(
    items: readonly PortalNavigationItem[],
    parentId: string,
): PortalNavigationPage | undefined {
    for (const child of sortNavItemsByOrder(items.filter(item => item.parentId === parentId))) {
        if (child.type === 'PAGE' && isNavItemVisible(child, items)) {
            return child;
        }

        if ((child.type === 'FOLDER' || child.type === 'API') && isNavItemVisible(child, items)) {
            const nested = findFirstVisibleDescendantPageNavItem(items, child.id);
            if (nested) {
                return nested;
            }
        }
    }

    return undefined;
}
