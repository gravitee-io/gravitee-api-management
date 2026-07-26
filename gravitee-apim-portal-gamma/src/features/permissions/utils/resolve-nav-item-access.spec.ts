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
import { flattenGrantScopeTree, resolveNavItemAccess } from './resolve-nav-item-access';
import { buildNavItem } from '../../portals/storage/navigation-items.storage.test-utils';
import type { PortalNavigationItem } from '../../portals/types/navigation-item.types';
import type { PortalAccessGrant } from '../types/permissions.types';

const PORTAL_ID = 'portal-test';

function buildGrant(overrides: Partial<PortalAccessGrant> = {}): PortalAccessGrant {
    return {
        id: 'grant-1',
        groupId: 'group-1',
        tenantId: 'tenant-1',
        scopeType: 'API',
        scopeId: 'api-payments',
        access: 'VIEW',
        overrides: [],
        createdAt: '2026-01-01T00:00:00.000Z',
        updatedAt: '2026-01-01T00:00:00.000Z',
        ...overrides,
    };
}

function itemsById(items: readonly PortalNavigationItem[]): Map<string, PortalNavigationItem> {
    return new Map(items.map(item => [item.id, item]));
}

describe('resolveNavItemAccess', () => {
    const apiNode = buildNavItem({ id: 'nav-api', type: 'API', apiId: 'api-payments', title: 'Payments API' });
    const apiFolder = buildNavItem({ id: 'nav-guides', type: 'FOLDER', parentId: 'nav-api', title: 'Guides' });
    const apiPage = buildNavItem({ id: 'nav-quickstart', parentId: 'nav-guides', title: 'Quickstart' });
    const looseFolder = buildNavItem({ id: 'nav-about', type: 'FOLDER', title: 'About' });
    const loosePage = buildNavItem({ id: 'nav-contact', parentId: 'nav-about', title: 'Contact' });

    const allItems = [apiNode, apiFolder, apiPage, looseFolder, loosePage];

    it('should inherit the enclosing asset grant', () => {
        const grant = buildGrant({ access: 'CONSUME', provisioning: 'CLASSIC' });

        expect(resolveNavItemAccess(apiPage, itemsById(allItems), [grant])).toEqual({
            access: 'CONSUME',
            inherited: true,
            grant,
        });
    });

    it('should apply an override on the item instead of the inherited access', () => {
        const grant = buildGrant({
            access: 'CONSUME',
            overrides: [{ navigationItemId: 'nav-quickstart', portalId: PORTAL_ID, access: 'NONE' }],
        });

        expect(resolveNavItemAccess(apiPage, itemsById(allItems), [grant])).toEqual({
            access: 'NONE',
            inherited: false,
            grant,
        });
    });

    it('should fall back to the portal grant for items outside any asset', () => {
        const portalGrant = buildGrant({ id: 'grant-portal', scopeType: 'PORTAL', scopeId: PORTAL_ID });

        expect(resolveNavItemAccess(loosePage, itemsById(allItems), [portalGrant])).toEqual({
            access: 'VIEW',
            inherited: true,
            grant: portalGrant,
        });
    });

    it('should not grant asset pages through the portal grant', () => {
        const portalGrant = buildGrant({ id: 'grant-portal', scopeType: 'PORTAL', scopeId: PORTAL_ID });

        expect(resolveNavItemAccess(apiPage, itemsById(allItems), [portalGrant])).toEqual({
            access: 'NONE',
            inherited: true,
            grant: null,
        });
    });

    it('should report no access when nothing grants the item', () => {
        expect(resolveNavItemAccess(loosePage, itemsById(allItems), [])).toEqual({
            access: 'NONE',
            inherited: true,
            grant: null,
        });
    });
});

describe('flattenGrantScopeTree', () => {
    const apiNode = buildNavItem({ id: 'nav-api', type: 'API', apiId: 'api-payments', title: 'Payments API' });
    const apiFolder = buildNavItem({ id: 'nav-guides', type: 'FOLDER', parentId: 'nav-api', order: 0 });
    const apiPage = buildNavItem({ id: 'nav-quickstart', parentId: 'nav-guides', order: 0 });
    const nestedApi = buildNavItem({
        id: 'nav-nested-api',
        type: 'API',
        apiId: 'api-accounts',
        parentId: 'nav-api',
        order: 1,
    });
    const nestedApiPage = buildNavItem({ id: 'nav-nested-page', parentId: 'nav-nested-api' });
    const looseFolder = buildNavItem({ id: 'nav-about', type: 'FOLDER', order: 1 });

    const allItems = [apiNode, apiFolder, apiPage, nestedApi, nestedApiPage, looseFolder];

    it('should list the asset subtree without the asset node itself', () => {
        const grant = buildGrant();

        expect(flattenGrantScopeTree(grant, allItems, [grant]).map(row => [row.item.id, row.depth])).toEqual([
            ['nav-guides', 0],
            ['nav-quickstart', 1],
        ]);
    });

    it('should stop descending at nested assets that own their own grant', () => {
        const grant = buildGrant();
        const rows = flattenGrantScopeTree(grant, allItems, [grant]);

        expect(rows.map(row => row.item.id)).not.toContain('nav-nested-api');
        expect(rows.map(row => row.item.id)).not.toContain('nav-nested-page');
    });

    it('should list portal-level items excluding asset nodes for a portal grant', () => {
        const grant = buildGrant({ scopeType: 'PORTAL', scopeId: PORTAL_ID });

        expect(flattenGrantScopeTree(grant, allItems, [grant]).map(row => row.item.id)).toEqual(['nav-about']);
    });
});
