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
import { findApiAncestor } from './find-api-ancestor';

describe('findApiAncestor', () => {
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
            id: 'page-1',
            portalId: 'portal-1',
            title: 'Overview',
            type: 'PAGE',
            parentId: 'api-1',
            order: 0,
            slug: 'overview-abc123',
        },
        {
            id: 'page-2',
            portalId: 'portal-1',
            title: 'Getting Started',
            type: 'PAGE',
            parentId: null,
            order: 1,
            slug: 'getting-started',
        },
    ];

    it('should find API ancestor for a nested page', () => {
        const result = findApiAncestor(navItems, 'page-1');
        expect(result?.id).toBe('api-1');
        expect(result?.apiId).toBe('api-payments');
    });

    it('should return null when page has no API ancestor', () => {
        expect(findApiAncestor(navItems, 'page-2')).toBeNull();
    });

    it('should return null for null item id', () => {
        expect(findApiAncestor(navItems, null)).toBeNull();
    });
});
