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
import {
    belongsToUserMenu,
    compareNavItemsByOrder,
    getNextSiblingOrder,
    isFooterNavItem,
    isHeaderRootNavItem,
    isUserMenuRootItem,
} from './nav-items';

describe('nav-items', () => {
    const navItems: PortalNavigationItem[] = [
        { id: 'page-1', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
        { id: 'footer-1', portalId: 'p1', title: 'Docs', type: 'LINK', parentId: null, order: 0, slug: 'docs', url: '#', area: 'FOOTER' },
        { id: 'menu-folder', portalId: 'p1', title: 'Account', type: 'FOLDER', parentId: null, order: 0, slug: 'account', area: 'USER_MENU' },
        { id: 'menu-page', portalId: 'p1', title: 'Settings', type: 'PAGE', parentId: 'menu-folder', order: 0, slug: 'settings' },
    ];

    it('should identify user menu root items', () => {
        expect(isUserMenuRootItem(navItems[2])).toBe(true);
        expect(isUserMenuRootItem(navItems[0])).toBe(false);
    });

    it('should identify items belonging to user menu tree', () => {
        expect(belongsToUserMenu(navItems[2], navItems)).toBe(true);
        expect(belongsToUserMenu(navItems[3], navItems)).toBe(true);
        expect(belongsToUserMenu(navItems[0], navItems)).toBe(false);
    });

    it('should exclude footer and user menu items from header roots', () => {
        expect(isHeaderRootNavItem(navItems[0])).toBe(true);
        expect(isHeaderRootNavItem(navItems[1])).toBe(false);
        expect(isHeaderRootNavItem(navItems[2])).toBe(false);
    });

    it('should identify footer items', () => {
        expect(isFooterNavItem(navItems[1])).toBe(true);
        expect(isFooterNavItem(navItems[0])).toBe(false);
    });

    it('should compute next sibling order from the highest existing order', () => {
        expect(getNextSiblingOrder([])).toBe(0);
        expect(getNextSiblingOrder([navItems[0], navItems[2]])).toBe(1);
        expect(getNextSiblingOrder([
            { ...navItems[0], order: 0 },
            { ...navItems[2], order: 5 },
        ])).toBe(6);
    });

    it('should sort siblings deterministically when order values collide', () => {
        const linkA = {
            id: 'menu-link-a',
            portalId: 'p1',
            title: 'A',
            type: 'LINK' as const,
            parentId: null,
            order: 2,
            slug: 'a',
            url: '#',
            area: 'USER_MENU' as const,
        };
        const linkB = {
            ...linkA,
            id: 'menu-link-b',
            title: 'B',
            slug: 'b',
            order: 2,
        };
        const page = {
            id: 'menu-page-root',
            portalId: 'p1',
            title: 'Settings',
            type: 'PAGE' as const,
            parentId: null,
            order: 1,
            slug: 'settings-root',
            area: 'USER_MENU' as const,
        };

        expect([linkB, page, linkA].sort(compareNavItemsByOrder).map(item => item.id)).toEqual([
            'menu-page-root',
            'menu-link-a',
            'menu-link-b',
        ]);
    });
});
