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
        expect(navItems).toHaveLength(18);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(13);
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
    });

    it('should seed demo navigation items and page content for the ABC Fitness portal', async () => {
        await seedPortalsIfEmpty();

        const navItems = await getNavItems('portal-abc-fitness');
        expect(navItems).toHaveLength(45);
        expect(navItems.filter(item => item.type === 'FOLDER')).toHaveLength(13);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(26);
        expect(navItems.filter(item => item.area === 'FOOTER')).toHaveLength(3);
        expect(navItems.filter(item => item.area === 'USER_MENU')).toHaveLength(3);

        const igniteApisFolder = navItems.find(item => item.slug === 'ignite-apis-abc104');
        expect(igniteApisFolder?.type).toBe('FOLDER');
        expect(navItems.filter(item => item.parentId === igniteApisFolder?.id)).toHaveLength(2);

        const homeContent = await getPageContent('portal-abc-fitness-nav-home');
        expect(homeContent?.portalId).toBe('portal-abc-fitness');
        expect(homeContent && 'document' in homeContent && homeContent.document[0]).toMatchObject({
            type: 'graviteeBanner',
            props: { title: 'Grow Your Fitness Business' },
        });

        const memberApiContent = await getPageContent('portal-abc-fitness-nav-ignite-members-api');
        expect(memberApiContent).toMatchObject({
            contentType: 'OPENAPI',
            renderer: 'swagger',
        });

        expect(navItems.some(item => item.slug === 'getting-started-nav001')).toBe(false);
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
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        });
    });
});
