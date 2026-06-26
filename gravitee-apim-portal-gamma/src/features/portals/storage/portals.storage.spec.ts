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
        expect(navItems).toHaveLength(7);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(4);
        expect(navItems.filter(item => item.area === 'FOOTER')).toHaveLength(2);

        const gettingStartedContent = await getPageContent('nav-getting-started');
        expect(gettingStartedContent?.portalId).toBe('portal-payments');
        expect(gettingStartedContent?.document[0]).toMatchObject({
            type: 'heading',
        });
    });

    it('should not re-seed when portals already exist', async () => {
        const firstSeed = await seedPortalsIfEmpty();
        const secondSeed = await seedPortalsIfEmpty();

        expect(secondSeed).toEqual(firstSeed);
        expect(await getAllPortals()).toHaveLength(3);
    });
});
