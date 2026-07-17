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
import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { canAddApiNavItem } from './can-add-api-nav-item';

describe('canAddApiNavItem', () => {
    const navItems: PortalNavigationItem[] = [
        {
            id: 'folder-1',
            portalId: 'portal-1',
            title: 'APIs',
            type: 'FOLDER',
            parentId: null,
            order: 0,
            slug: 'apis',
        },
        {
            id: 'folder-2',
            portalId: 'portal-1',
            title: 'Guides',
            type: 'FOLDER',
            parentId: 'folder-1',
            order: 1,
            slug: 'guides',
        },
        {
            id: 'api-1',
            portalId: 'portal-1',
            title: 'Payments API',
            type: 'API',
            apiId: 'api-payments',
            parentId: 'folder-1',
            order: 0,
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

    it('should allow API when parent is a root folder', () => {
        expect(canAddApiNavItem(navItems, 'folder-1')).toBe(true);
    });

    it('should allow API when parent is a folder under another folder', () => {
        expect(canAddApiNavItem(navItems, 'folder-2')).toBe(true);
    });

    it('should disallow API when parent is an API item', () => {
        expect(canAddApiNavItem(navItems, 'api-1')).toBe(false);
    });

    it('should disallow API when parent is a folder nested under an API', () => {
        expect(canAddApiNavItem(navItems, 'nested-folder')).toBe(false);
    });

    it('should allow API at root level', () => {
        expect(canAddApiNavItem(navItems, null)).toBe(true);
    });
});
