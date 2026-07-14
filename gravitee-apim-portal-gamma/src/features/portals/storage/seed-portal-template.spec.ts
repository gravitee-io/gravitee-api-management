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
import { getNavItems } from './navigation-items.storage';
import { getPageContent } from './page-contents.storage';
import { clearPortalsDatabase } from './portals.storage.test-utils';
import { seedPortalFromTemplate } from './seed-portal-template';

describe('seed-portal-template', () => {
    const portalId = 'portal-test';

    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should not seed navigation for the blank template', async () => {
        await seedPortalFromTemplate(portalId, 'blank');

        expect(await getNavItems(portalId)).toEqual([]);
    });

    it('should seed starter navigation with placeholder pages', async () => {
        await seedPortalFromTemplate(portalId, 'starter');

        const navItems = await getNavItems(portalId);
        expect(navItems).toHaveLength(10);
        expect(navItems.some(item => item.title === 'Getting Started')).toBe(true);

        const gettingStartedContent = await getPageContent(`${portalId}-nav-getting-started`);
        expect(gettingStartedContent).toMatchObject({ contentType: 'BLOCK' });
        expect(
            gettingStartedContent &&
                'document' in gettingStartedContent &&
                gettingStartedContent.document[0],
        ).toMatchObject({ type: 'heading' });
    });

    it('should seed payments navigation with rich demo content', async () => {
        await seedPortalFromTemplate(portalId, 'payments');

        const gettingStartedContent = await getPageContent(`${portalId}-nav-getting-started`);
        expect(
            gettingStartedContent &&
                'document' in gettingStartedContent &&
                gettingStartedContent.document[0],
        ).toMatchObject({ type: 'graviteeBanner' });

        const navItems = await getNavItems(portalId);
        expect(navItems.some(item => item.type === 'API' && item.title === 'Commerce Platform API')).toBe(true);
    });

    it('should seed active fitness navigation with marketplace content', async () => {
        await seedPortalFromTemplate(portalId, 'active-fitness');

        const navItems = await getNavItems(portalId);
        expect(navItems.some(item => item.type === 'FOLDER' && item.title === 'Marketplace')).toBe(true);
        expect(navItems.some(item => item.title === 'Home')).toBe(true);
        expect(navItems.some(item => item.type === 'API' && item.title === 'Commerce Platform API')).toBe(false);

        const homeContent = await getPageContent(`${portalId}-nav-home`);
        expect(
            homeContent && 'document' in homeContent && homeContent.document[0],
        ).toMatchObject({ type: 'graviteeBanner' });
    });

    it('should keep seedDefaultNavigationForPortal as a payments alias', async () => {
        const { seedDefaultNavigationForPortal } = await import('./seed-default-navigation');

        await seedDefaultNavigationForPortal(portalId);

        const gettingStartedContent = await getPageContent(`${portalId}-nav-getting-started`);
        expect(
            gettingStartedContent &&
                'document' in gettingStartedContent &&
                gettingStartedContent.document[0],
        ).toMatchObject({ type: 'graviteeBanner' });
    });
});
