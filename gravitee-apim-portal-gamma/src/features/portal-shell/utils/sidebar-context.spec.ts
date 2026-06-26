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
import { findRootNavItem, getSidebarRootFolder, isNavContainer } from './sidebar-context';

const navItems: PortalNavigationItem[] = [
    { id: 'home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 1, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
    { id: 'advanced', portalId: 'p1', title: 'Advanced', type: 'FOLDER', parentId: 'guides', order: 1, slug: 'advanced' },
    { id: 'auth', portalId: 'p1', title: 'Authentication', type: 'PAGE', parentId: 'advanced', order: 0, slug: 'auth' },
    { id: 'api-item', portalId: 'p1', title: 'Payments API', type: 'API', parentId: 'guides', order: 2, slug: 'payments-api', apiId: 'api-1' },
];

describe('sidebar-context', () => {
    it('should find root nav item for nested selection', () => {
        expect(findRootNavItem(navItems, 'auth')?.id).toBe('guides');
        expect(findRootNavItem(navItems, 'home')?.id).toBe('home');
    });

    it('should return sidebar root folder for items under a root folder', () => {
        expect(getSidebarRootFolder(navItems, 'quick-start')?.id).toBe('guides');
        expect(getSidebarRootFolder(navItems, 'advanced')?.id).toBe('guides');
        expect(getSidebarRootFolder(navItems, 'auth')?.id).toBe('guides');
        expect(getSidebarRootFolder(navItems, 'guides')?.id).toBe('guides');
    });

    it('should not return sidebar root for root pages', () => {
        expect(getSidebarRootFolder(navItems, 'home')).toBeNull();
    });

    it('should treat API items as containers', () => {
        expect(isNavContainer('API')).toBe(true);
        expect(isNavContainer('FOLDER')).toBe(true);
        expect(isNavContainer('PAGE')).toBe(false);
    });
});
