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
import { createDefaultPortalScreenshot } from './dummy-portals';
import { getNavItems } from './navigation-items.storage';
import { getPageContent } from './page-contents.storage';
import { DEFAULT_PORTAL_LABEL } from '../types';
import {
    deletePortal,
    getAllPortals,
    getPortal,
    savePortal,
    seedPortalsIfEmpty,
} from './portals.storage';
import { clearPortalsDatabase } from './portals.storage.test-utils';

function buildPortal(overrides: Partial<Parameters<typeof savePortal>[0]> = {}) {
    return {
        id: 'portal-1',
        name: 'Test Portal',
        screenshotDataUrl: createDefaultPortalScreenshot('Test Portal'),
        updatedAt: new Date().toISOString(),
        layout: 'header-content-footer' as const,
        pageWidth: 'narrow' as const,
        portalIconUrl: '',
        portalLabel: DEFAULT_PORTAL_LABEL,
        footerLinks: [],
        userMenuItems: [],
        ...overrides,
    };
}

describe('portals.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should save and load a portal', async () => {
        const portal = buildPortal();

        await savePortal(portal);

        expect(await getPortal('portal-1')).toEqual(portal);
        expect(await getAllPortals()).toEqual([portal]);
    });

    it('should delete a portal', async () => {
        const portal = buildPortal();

        await savePortal(portal);
        await deletePortal('portal-1');

        expect(await getPortal('portal-1')).toBeUndefined();
        expect(await getAllPortals()).toEqual([]);
    });

    it('should seed dummy portals when store is empty', async () => {
        const seeded = await seedPortalsIfEmpty();

        expect(seeded).toHaveLength(3);
        expect(await getAllPortals()).toHaveLength(3);
    });

    it('should seed demo navigation items and page content for the payments portal', async () => {
        await seedPortalsIfEmpty();

        const navItems = await getNavItems('portal-payments');
        expect(navItems).toHaveLength(26);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(20);
        expect(navItems.filter(item => item.area === 'FOOTER')).toHaveLength(2);
        expect(navItems.filter(item => item.area === 'USER_MENU')).toHaveLength(2);

        const gettingStartedContent = await getPageContent('portal-payments-nav-getting-started');
        expect(gettingStartedContent?.portalId).toBe('portal-payments');
        expect(gettingStartedContent && 'document' in gettingStartedContent && gettingStartedContent.document[0]).toMatchObject({
            type: 'graviteeBanner',
        });

        const openApiContent = await getPageContent('portal-payments-nav-petstore-openapi');
        expect(openApiContent).toMatchObject({
            contentType: 'OPENAPI',
            renderer: 'swagger',
        });

        const paymentApiContent = await getPageContent('portal-payments-nav-payment-api');
        expect(paymentApiContent).toMatchObject({
            contentType: 'OPENAPI',
            renderer: 'swagger',
        });

        const commerceOverviewContent = await getPageContent('portal-payments-nav-commerce-overview');
        expect(commerceOverviewContent).toMatchObject({ contentType: 'BLOCK' });
        expect(
            commerceOverviewContent &&
                'document' in commerceOverviewContent &&
                commerceOverviewContent.document.some(block => block.type === 'graviteeApiMetadata'),
        ).toBe(true);

        const productsTagContent = await getPageContent('portal-payments-nav-commerce-tag-products');
        expect(productsTagContent).toMatchObject({ contentType: 'BLOCK' });
        expect(
            productsTagContent &&
                'document' in productsTagContent &&
                productsTagContent.document.some(block => block.type === 'graviteeApiOperations'),
        ).toBe(true);
    });

    it('should seed demo navigation items and page content for the ABC Fitness portal', async () => {
        await seedPortalsIfEmpty();

        const navItems = await getNavItems('portal-abc-fitness');
        expect(navItems).toHaveLength(26);
        expect(navItems.filter(item => item.type === 'FOLDER')).toHaveLength(1);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(20);
        expect(navItems.filter(item => item.area === 'FOOTER')).toHaveLength(2);
        expect(navItems.filter(item => item.area === 'USER_MENU')).toHaveLength(2);

        expect(navItems.some(item => item.type === 'API' && item.title === 'Commerce Platform API')).toBe(true);
        expect(navItems.some(item => item.slug === 'commerce-products-nav021')).toBe(true);
    });

    it('should not re-seed when portals already exist', async () => {
        const firstSeed = await seedPortalsIfEmpty();
        const secondSeed = await seedPortalsIfEmpty();

        expect(secondSeed).toEqual(firstSeed);
        expect(await getAllPortals()).toHaveLength(3);
    });

    it('should default missing portal fields when loading legacy portals', async () => {
        const legacyPortal = {
            id: 'portal-legacy',
            name: 'Legacy Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('Legacy Portal'),
            updatedAt: new Date().toISOString(),
        };

        await savePortal(legacyPortal as Parameters<typeof savePortal>[0]);

        expect(await getPortal('portal-legacy')).toEqual({
            ...legacyPortal,
            layout: 'header-content-footer',
            pageWidth: 'narrow',
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        });
    });
});
