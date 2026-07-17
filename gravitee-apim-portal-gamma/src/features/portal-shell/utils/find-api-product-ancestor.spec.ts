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
import type { PortalNavigationApiProduct, PortalNavigationItem } from '../../portals/types';
import { findApiProductAncestor } from './find-api-product-ancestor';

describe('findApiProductAncestor', () => {
    const navItems: PortalNavigationItem[] = [
        {
            id: 'folder-1',
            portalId: 'portal-1',
            title: 'Products',
            type: 'FOLDER',
            parentId: null,
            order: 0,
            slug: 'products',
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
            parentId: 'product-1',
            order: 0,
            slug: 'payments-api',
        },
        {
            id: 'page-1',
            portalId: 'portal-1',
            title: 'Overview',
            type: 'PAGE',
            parentId: 'api-1',
            order: 0,
            slug: 'overview',
        },
    ];

    it('should find API Product ancestor for a nested API page', () => {
        const result = findApiProductAncestor(navItems, 'page-1');
        expect(result?.id).toBe('product-1');
        expect(result?.apiProductId).toBe('product-commerce');
    });

    it('should return null when item has no API Product ancestor', () => {
        expect(findApiProductAncestor(navItems, 'folder-1')).toBeNull();
    });

    it('should return null for null item id', () => {
        expect(findApiProductAncestor(navItems, null)).toBeNull();
    });
});
