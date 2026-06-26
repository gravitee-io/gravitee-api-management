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
import { collectDescendantIds, collectIdsToDelete, hasNavItemChildren } from './nav-items';

const navItems: PortalNavigationItem[] = [
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
    { id: 'advanced', portalId: 'p1', title: 'Advanced', type: 'FOLDER', parentId: 'guides', order: 1, slug: 'advanced' },
    { id: 'auth', portalId: 'p1', title: 'Authentication', type: 'PAGE', parentId: 'advanced', order: 0, slug: 'auth' },
];

describe('nav-items utils', () => {
    it('should detect children', () => {
        expect(hasNavItemChildren(navItems, 'guides')).toBe(true);
        expect(hasNavItemChildren(navItems, 'quick-start')).toBe(false);
    });

    it('should collect descendant ids recursively', () => {
        expect(collectDescendantIds(navItems, 'guides')).toEqual(['quick-start', 'advanced', 'auth']);
        expect(collectIdsToDelete(navItems, 'advanced')).toEqual(['advanced', 'auth']);
    });
});
