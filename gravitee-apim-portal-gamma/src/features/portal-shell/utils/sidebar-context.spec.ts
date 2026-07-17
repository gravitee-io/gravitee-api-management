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
import type { PortalNavigationApi, PortalNavigationApiProduct, PortalNavigationItem } from '../../portals/types';
import { getSidebarRootFolder } from './sidebar-context';

describe('getSidebarRootFolder', () => {
    const navItems: PortalNavigationItem[] = [
        { id: 'folder-1', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
        { id: 'page-1', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'folder-1', order: 0, slug: 'quick-start' },
        {
            id: 'api-root',
            portalId: 'p1',
            title: 'Payments API',
            type: 'API',
            apiId: 'api-payments',
            parentId: null,
            order: 1,
            slug: 'payments-api',
        } as PortalNavigationApi,
        {
            id: 'api-overview',
            portalId: 'p1',
            title: 'Overview',
            type: 'PAGE',
            parentId: 'api-root',
            order: 0,
            slug: 'overview',
        },
        {
            id: 'menu-folder',
            portalId: 'p1',
            title: 'Account',
            type: 'FOLDER',
            parentId: null,
            order: 0,
            slug: 'account',
            area: 'USER_MENU',
        },
        { id: 'menu-page', portalId: 'p1', title: 'Settings', type: 'PAGE', parentId: 'menu-folder', order: 0, slug: 'settings' },
    ];

    it('should return header folder when a header page is selected', () => {
        const folder = getSidebarRootFolder(navItems, 'page-1');
        expect(folder?.id).toBe('folder-1');
    });

    it('should return user menu folder when a user menu page is selected', () => {
        const folder = getSidebarRootFolder(navItems, 'menu-page');
        expect(folder?.id).toBe('menu-folder');
    });

    it('should return user menu folder when the folder itself is selected', () => {
        const folder = getSidebarRootFolder(navItems, 'menu-folder');
        expect(folder?.id).toBe('menu-folder');
    });

    it('should return root API when a page under a root-level API is selected', () => {
        const api = getSidebarRootFolder(navItems, 'api-overview');
        expect(api?.id).toBe('api-root');
        expect(api?.type).toBe('API');
    });

    it('should return root API Product when a page under a root-level API Product is selected', () => {
        const productNavItems: PortalNavigationItem[] = [
            {
                id: 'product-root',
                portalId: 'p1',
                title: 'Commerce Platform',
                type: 'API_PRODUCT',
                apiProductId: 'product-commerce',
                parentId: null,
                order: 0,
                slug: 'commerce-platform',
            } as PortalNavigationApiProduct,
            {
                id: 'api-child',
                portalId: 'p1',
                title: 'Payments API',
                type: 'API',
                apiId: 'api-payments',
                parentId: 'product-root',
                order: 0,
                slug: 'payments-api',
            } as PortalNavigationApi,
            {
                id: 'api-page',
                portalId: 'p1',
                title: 'Overview',
                type: 'PAGE',
                parentId: 'api-child',
                order: 0,
                slug: 'overview',
            },
        ];

        const product = getSidebarRootFolder(productNavItems, 'api-page');
        expect(product?.id).toBe('product-root');
        expect(product?.type).toBe('API_PRODUCT');
    });
});
