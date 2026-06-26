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
import type { PortalNavigationItem } from '../../portals/types';
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

    it('should add a footer link', async () => {
        const { result } = renderHook(() => useNavigation(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        await act(async () => {
            await result.current.addFooterLink();
        });

        await waitFor(() => {
            expect(result.current.getFooterItems()).toHaveLength(1);
        });

        expect(result.current.getFooterItems()[0].area).toBe('FOOTER');
        expect(result.current.getFooterItems()[0].type).toBe('LINK');
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

    it('should redirect to first page when slug is invalid', async () => {
        const onNavigate = jest.fn();
        renderHook(() =>
            useNavigation(PORTAL_ID, {
                slug: 'missing-slug',
                getPagePath: slug => `/portals/${PORTAL_ID}/edit/${slug}`,
                onNavigate,
            }),
        );

        await waitFor(() => {
            expect(onNavigate).toHaveBeenCalledWith(`/portals/${PORTAL_ID}/edit/home-abc123`, { replace: true });
        });
    });
});
