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
import { act, renderHook, waitFor } from '@testing-library/react';

import { installFakeIndexedDB, resetFakeIndexedDB } from '../../../testing/fake-indexeddb';
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { useNavigation } from './useNavigation';

installFakeIndexedDB();

const PORTAL_ID = 'test-portal';

const sampleItems: PortalNavigationItem[] = [
    { id: 'page-1', portalId: PORTAL_ID, title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'folder-1', portalId: PORTAL_ID, title: 'Guides', type: 'FOLDER', parentId: null, order: 1, slug: 'guides-def456' },
    { id: 'page-2', portalId: PORTAL_ID, title: 'Quick Start', type: 'PAGE', parentId: 'folder-1', order: 0, slug: 'quick-start-ghi789' },
];

describe('useNavigation', () => {
    beforeEach(async () => {
        resetFakeIndexedDB();
        for (const item of sampleItems) {
            await saveNavItem(item);
        }
    });

    it('should load navigation items for portal', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.navItems).toHaveLength(3);
    });

    it('should auto-select first root page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.selectedNavItemId).toBe('page-1');
    });

    it('should return root items only', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const rootItems = result.current.getRootItems();
        expect(rootItems).toHaveLength(2);
        expect(rootItems.map(i => i.id)).toEqual(['page-1', 'folder-1']);
    });

    it('should return children of a parent', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const children = result.current.getChildren('folder-1');
        expect(children).toHaveLength(1);
        expect(children[0].id).toBe('page-2');
    });

    it('should add a page nav item and select it', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addNavItem('PAGE', null);
        });

        await waitFor(() => {
            expect(result.current.navItems).toHaveLength(4);
        });

        const newPage = result.current.navItems.find(i => i.title === 'New Page');
        expect(newPage).toBeDefined();
        expect(result.current.selectedNavItemId).toBe(newPage!.id);
    });

    it('should select a nav item', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.selectNavItem('page-2');
        });

        expect(result.current.selectedNavItemId).toBe('page-2');
    });

    it('should delete a nav item and auto-select next available page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.selectNavItem('page-1');
        });

        await act(async () => {
            await result.current.deleteNavItem('page-1');
        });

        await waitFor(() => {
            expect(result.current.navItems).toHaveLength(2);
        });

        expect(result.current.selectedNavItemId).toBe('page-2');
    });

    it('should delete a folder and all nested items', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.deleteNavItem('folder-1');
        });

        await waitFor(() => {
            expect(result.current.navItems).toHaveLength(1);
        });

        expect(result.current.navItems[0].id).toBe('page-1');
    });

    it('should return empty when portalId is undefined', async () => {
        const { result } = renderHook(() => useNavigation(undefined));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.navItems).toHaveLength(0);
    });

    it('should exclude footer items from root items', async () => {
        await saveNavItem({
            id: 'footer-link',
            portalId: PORTAL_ID,
            title: 'Docs',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'docs-footer001',
            url: 'https://docs.example.com',
            area: 'FOOTER',
        });

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.getRootItems()).toHaveLength(2);
        expect(result.current.getFooterItems()).toHaveLength(1);
        expect(result.current.getFooterItems()[0].title).toBe('Docs');
    });

    it('should add a footer link from a page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addLinkFromPage(page, null, 'FOOTER');
        });

        await waitFor(() => {
            expect(result.current.getFooterItems()).toHaveLength(1);
        });

        expect(result.current.getFooterItems()[0].area).toBe('FOOTER');
        expect(result.current.getFooterItems()[0].type).toBe('LINK');
        expect(result.current.getFooterItems()[0].url).toBe(page.slug);
    });

    it('should add an API nav item with a default child page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addApiNavItem('api-payments', 'Payments API', 'folder-1');
        });

        await waitFor(() => {
            expect(result.current.navItems).toHaveLength(5);
        });

        const apiItem = result.current.navItems.find(item => item.type === 'API');
        expect(apiItem).toMatchObject({ title: 'Payments API', apiId: 'api-payments', parentId: 'folder-1' });

        const childPage = result.current.navItems.find(item => item.parentId === apiItem?.id);
        expect(childPage).toMatchObject({ type: 'PAGE', title: 'Overview' });
        expect(result.current.selectedNavItemId).toBe(childPage?.id);
    });

    it('should reject adding an API under another API item', async () => {
        await saveNavItem({
            id: 'api-1',
            portalId: PORTAL_ID,
            title: 'Payments API',
            type: 'API',
            apiId: 'api-payments',
            parentId: 'folder-1',
            order: 0,
            slug: 'payments-api',
        } as PortalNavigationApi);

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await expect(
            act(async () => {
                await result.current.addApiNavItem('api-accounts', 'Accounts API', 'api-1');
            }),
        ).rejects.toThrow('Parent hierarchy cannot include API items.');

        expect(result.current.navItems.filter(item => item.type === 'API')).toHaveLength(1);
    });

    it('should reject adding an API under a folder nested inside an API', async () => {
        await saveNavItem({
            id: 'api-1',
            portalId: PORTAL_ID,
            title: 'Payments API',
            type: 'API',
            apiId: 'api-payments',
            parentId: 'folder-1',
            order: 0,
            slug: 'payments-api',
        } as PortalNavigationApi);
        await saveNavItem({
            id: 'nested-folder',
            portalId: PORTAL_ID,
            title: 'Nested',
            type: 'FOLDER',
            parentId: 'api-1',
            order: 0,
            slug: 'nested',
        });

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await expect(
            act(async () => {
                await result.current.addApiNavItem('api-accounts', 'Accounts API', 'nested-folder');
            }),
        ).rejects.toThrow('Parent hierarchy cannot include API items.');

        expect(result.current.navItems.filter(item => item.type === 'API')).toHaveLength(1);
    });

    it('should select nav item from slug in URL', async () => {
        const { result } = renderHook(() =>
            useNavigation(PORTAL_ID, { slug: 'quick-start-ghi789' }),
        );

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.selectedNavItemId).toBe('page-2');
    });

    it('should navigate when selecting a page with URL sync enabled', async () => {
        const onNavigate = jest.fn();
        const { result } = renderHook(() =>
            useNavigation(PORTAL_ID, {
                getPagePath: slug => `/portals/${PORTAL_ID}/edit/${slug}`,
                onNavigate,
            }),
        );

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        onNavigate.mockClear();

        act(() => {
            result.current.selectNavItem('page-2');
        });

        expect(result.current.selectedNavItemId).toBe('page-2');
        expect(onNavigate).toHaveBeenCalledWith(`/portals/${PORTAL_ID}/edit/quick-start-ghi789`, { replace: false });
    });

    it('should update nav item title and slug', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.updateNavItem('page-1', { title: 'Welcome' });
        });

        await waitFor(() => {
            const updated = result.current.navItems.find(item => item.id === 'page-1');
            expect(updated?.title).toBe('Welcome');
            expect(updated?.slug).toMatch(/^welcome-/);
        });
    });

    it('should update footer link url', async () => {
        await saveNavItem({
            id: 'footer-link',
            portalId: PORTAL_ID,
            title: 'Docs',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'docs-footer001',
            url: 'https://docs.example.com',
            area: 'FOOTER',
        });

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.updateNavItem('footer-link', { url: 'https://example.com/help' });
        });

        await waitFor(() => {
            const link = result.current.getFooterItems()[0];
            expect(link.url).toBe('https://example.com/help');
        });
    });

    it('should report page not found when slug is invalid', async () => {
        const onNavigate = jest.fn();
        const { result } = renderHook(() =>
            useNavigation(PORTAL_ID, {
                slug: 'missing-slug',
                getPagePath: slug => `/portals/${PORTAL_ID}/edit/${slug}`,
                onNavigate,
            }),
        );

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.pageNotFound).toBe(true);
        expect(result.current.selectedNavItemId).toBeNull();
        expect(onNavigate).not.toHaveBeenCalled();
    });

    it('should exclude user menu items from root items', async () => {
        await saveNavItem({
            id: 'menu-profile',
            portalId: PORTAL_ID,
            title: 'Profile',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'profile-menu001',
            url: '/profile',
            area: 'USER_MENU',
        });

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.getRootItems()).toHaveLength(2);
        expect(result.current.getUserMenuRootItems()).toHaveLength(1);
        expect(result.current.hasUserMenuItems()).toBe(true);
    });

    it('should add a user menu page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addUserMenuNavItem('PAGE', null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(1);
        });

        expect(result.current.getUserMenuRootItems()[0]).toMatchObject({
            type: 'PAGE',
            area: 'USER_MENU',
            title: 'New Page',
        });
    });

    it('should append user menu links after pages when legacy links exist', async () => {
        await saveNavItem({
            id: 'menu-profile',
            portalId: PORTAL_ID,
            title: 'Profile',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'profile-menu001',
            url: '/profile',
            area: 'USER_MENU',
        });
        await saveNavItem({
            id: 'menu-logout',
            portalId: PORTAL_ID,
            title: 'Log out',
            type: 'LINK',
            parentId: null,
            order: 1,
            slug: 'log-out-menu002',
            url: '/logout',
            area: 'USER_MENU',
        });

        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addUserMenuNavItem('PAGE', null);
            await result.current.addUserMenuNavItem('FOLDER', null);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addUserMenuLinkFromPage(page, null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(5);
        });

        const items = result.current.getUserMenuRootItems();
        expect(items.map(item => item.type)).toEqual(['LINK', 'LINK', 'PAGE', 'FOLDER', 'LINK']);
    });

    it('should append nested links after pages inside a user menu folder', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        let folderId = '';
        await act(async () => {
            const folder = await result.current.addUserMenuNavItem('FOLDER', null);
            folderId = folder.id;
            await result.current.addUserMenuNavItem('PAGE', folderId);
            const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };
            await result.current.addLinkFromPage(page, folderId, 'USER_MENU');
        });

        await waitFor(() => {
            expect(result.current.getChildren(folderId)).toHaveLength(2);
        });

        const [first, second] = result.current.getChildren(folderId);
        expect(first.type).toBe('PAGE');
        expect(second.type).toBe('LINK');
    });

    it('should append user menu links after deletions without reusing order values', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        let pageId = '';
        await act(async () => {
            const page = await result.current.addUserMenuNavItem('PAGE', null);
            pageId = page.id;
            await result.current.addUserMenuNavItem('FOLDER', null);
        });

        await act(async () => {
            await result.current.deleteNavItem(pageId);
        });

        const homePage = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addUserMenuLinkFromPage(homePage, null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(2);
        });

        const items = result.current.getUserMenuRootItems();
        expect(items.map(item => item.type)).toEqual(['FOLDER', 'LINK']);
        expect(items[1].order).toBeGreaterThan(items[0].order);
    });

    it('should preserve add order when multiple user menu items are added in one batch', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const homePage = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addUserMenuNavItem('PAGE', null);
            await result.current.addUserMenuNavItem('FOLDER', null);
            await result.current.addUserMenuLinkFromPage(homePage, null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(3);
        });

        expect(result.current.getUserMenuRootItems().map(item => item.type)).toEqual(['PAGE', 'FOLDER', 'LINK']);
    });

    it('should append user menu links after existing pages and folders', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addUserMenuNavItem('PAGE', null);
            await result.current.addUserMenuNavItem('FOLDER', null);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addUserMenuLinkFromPage(page, null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(3);
        });

        const [first, second, third] = result.current.getUserMenuRootItems();
        expect(first.type).toBe('PAGE');
        expect(second.type).toBe('FOLDER');
        expect(third.type).toBe('LINK');
    });

    it('should add a user menu link from a portal page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addUserMenuLinkFromPage(page, null);
        });

        await waitFor(() => {
            expect(result.current.getUserMenuRootItems()).toHaveLength(1);
        });

        expect(result.current.getUserMenuRootItems()[0]).toMatchObject({
            type: 'LINK',
            area: 'USER_MENU',
            title: 'Home',
            url: 'home-abc123',
        });
    });

    it('should add a header link from a portal page', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        await act(async () => {
            await result.current.addLinkFromPage(page, null, 'HEADER');
        });

        await waitFor(() => {
            expect(result.current.getRootItems()).toHaveLength(3);
        });

        const link = result.current.getRootItems().find(item => item.type === 'LINK' && item.title === 'Home');
        expect(link).toMatchObject({
            type: 'LINK',
            url: 'home-abc123',
        });
    });

    it('should navigate to the target page when selecting a header link', async () => {
        const onNavigate = jest.fn();
        const getPagePath = (pageSlug: string) => `/portals/${PORTAL_ID}/edit/${pageSlug}`;
        const { result } = renderHook(() => useNavigation(PORTAL_ID, { getPagePath, onNavigate }));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        const page = result.current.navItems.find(item => item.id === 'page-1') as PortalNavigationItem & { type: 'PAGE' };

        let linkId = '';
        await act(async () => {
            const link = await result.current.addLinkFromPage(page, null, 'HEADER');
            linkId = link.id;
        });

        act(() => {
            result.current.selectNavItem(linkId);
        });

        expect(result.current.selectedNavItemId).toBe('page-1');
        expect(onNavigate).toHaveBeenCalledWith('/portals/test-portal/edit/home-abc123', { replace: false });
    });

    it('should add nested user menu items under a folder', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        let folderId = '';
        await act(async () => {
            const folder = await result.current.addUserMenuNavItem('FOLDER', null);
            folderId = folder.id;
        });

        await act(async () => {
            await result.current.addUserMenuNavItem('PAGE', folderId);
        });

        await waitFor(() => {
            expect(result.current.getChildren(folderId)).toHaveLength(1);
        });

        expect(result.current.getChildren(folderId)[0]).toMatchObject({
            type: 'PAGE',
            parentId: folderId,
        });
    });

    it('should preserve user menu folder selection when nav items refresh with a stale URL slug', async () => {
        await saveNavItem({
            id: 'menu-folder',
            portalId: PORTAL_ID,
            title: 'Account',
            type: 'FOLDER',
            parentId: null,
            order: 0,
            slug: 'account-menu001',
            area: 'USER_MENU',
        });

        const { result } = renderHook(() =>
            useNavigation(PORTAL_ID, { slug: 'home-abc123' }),
        );

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.selectNavItem('menu-folder');
        });

        expect(result.current.selectedNavItemId).toBe('menu-folder');

        await act(async () => {
            await result.current.addUserMenuNavItem('FOLDER', 'menu-folder');
        });

        await waitFor(() => {
            expect(result.current.getChildren('menu-folder')).toHaveLength(1);
        });

        expect(result.current.selectedNavItemId).toBe('menu-folder');
    });

    it('should migrate legacy user menu items on load', async () => {
        const onPortalChange = jest.fn();
        const portal = {
            id: PORTAL_ID,
            name: 'Test',
            screenshotDataUrl: '',
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer' as const,
            showFooter: true,
            pageWidth: 'narrow' as const,
            portalIconUrl: '',
            portalLabel: 'Test',
            footerLinks: [],
            userMenuItems: [{ id: 'legacy-profile', label: 'Profile', url: '/profile' }],
        };

        const { result } = renderHook(() =>
            useNavigation(PORTAL_ID, { portal, onPortalChange }),
        );

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        expect(result.current.getUserMenuRootItems()).toHaveLength(1);
        expect(result.current.getUserMenuRootItems()[0]).toMatchObject({
            title: 'Profile',
            url: '/profile',
            area: 'USER_MENU',
        });
        expect(onPortalChange).toHaveBeenCalledWith(expect.objectContaining({ userMenuItems: [] }));
    });
});
