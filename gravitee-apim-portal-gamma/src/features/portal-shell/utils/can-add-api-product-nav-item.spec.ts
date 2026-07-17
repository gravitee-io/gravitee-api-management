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
import { canAddApiProductNavItem } from './can-add-api-product-nav-item';

describe('canAddApiProductNavItem', () => {
    const navItems: PortalNavigationItem[] = [
        {
            id: 'folder-1',
            portalId: 'portal-1',
            title: 'Catalog',
            type: 'FOLDER',
            parentId: null,
            order: 0,
            slug: 'catalog',
        },
        {
            id: 'product-1',
            portalId: 'portal-1',
            title: 'Commerce Platform',
            type: 'API_PRODUCT',
            apiProductId: 'product-commerce',
            parentId: 'folder-1',
            order: 0,
            slug: 'commerce-platform',
        } as PortalNavigationApiProduct,
        {
            id: 'api-1',
            portalId: 'portal-1',
            title: 'Payments API',
            type: 'API',
            apiId: 'api-payments',
            parentId: 'folder-1',
            order: 1,
            slug: 'payments-api',
        } as PortalNavigationApi,
        {
            id: 'nested-folder',
            portalId: 'portal-1',
            title: 'Nested',
            type: 'FOLDER',
            parentId: 'api-1',
            order: 0,
            slug: 'nested',
        },
    ];

    it('should allow API Product when parent is a root folder', () => {
        expect(canAddApiProductNavItem(navItems, 'folder-1')).toBe(true);
    });

    it('should disallow API Product when parent is an API Product item', () => {
        expect(canAddApiProductNavItem(navItems, 'product-1')).toBe(false);
    });

    it('should disallow API Product when parent is an API item', () => {
        expect(canAddApiProductNavItem(navItems, 'api-1')).toBe(false);
    });

    it('should disallow API Product when parent is a folder nested under an API', () => {
        expect(canAddApiProductNavItem(navItems, 'nested-folder')).toBe(false);
    });

    it('should allow API Product at root level', () => {
        expect(canAddApiProductNavItem(navItems, null)).toBe(true);
    });
});
