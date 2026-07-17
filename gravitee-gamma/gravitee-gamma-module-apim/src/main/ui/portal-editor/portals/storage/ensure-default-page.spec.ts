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
import { savePortal } from './portals.storage';
import { getNavItems } from './navigation-items.storage';
import { getPageContent } from './page-contents.storage';
import { ensureDefaultPageForPortal } from './ensure-default-page';
import { createDefaultPortalScreenshot } from './dummy-portals';
import { DEFAULT_PORTAL_LABEL } from '../types';

describe('ensureDefaultPageForPortal', () => {
    it('should create a default page and content when portal has none', async () => {
        const portalId = 'portal-empty';
        await savePortal({
            id: portalId,
            name: 'Empty Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('Empty Portal'),
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer',
            showFooter: true,
            pageWidth: 'narrow',
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        });

        const content = await ensureDefaultPageForPortal(portalId);

        expect(content.portalId).toBe(portalId);
        expect(content.document.length).toBeGreaterThan(0);

        const navItems = await getNavItems(portalId);
        expect(navItems.some(item => item.type === 'PAGE')).toBe(true);
        expect(await getPageContent(content.navigationItemId)).toEqual(content);
    });

    it('should return existing page content when already present', async () => {
        const portalId = 'portal-with-page';
        await savePortal({
            id: portalId,
            name: 'Portal With Page',
            screenshotDataUrl: createDefaultPortalScreenshot('Portal With Page'),
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer',
            showFooter: true,
            pageWidth: 'narrow',
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        });

        const created = await ensureDefaultPageForPortal(portalId);
        const loaded = await ensureDefaultPageForPortal(portalId);

        expect(loaded).toEqual(created);
    });

    it('should create only one default page when called concurrently', async () => {
        const portalId = 'portal-concurrent';
        await savePortal({
            id: portalId,
            name: 'Concurrent Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('Concurrent Portal'),
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer',
            showFooter: true,
            pageWidth: 'narrow',
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        });

        const [first, second] = await Promise.all([
            ensureDefaultPageForPortal(portalId),
            ensureDefaultPageForPortal(portalId),
        ]);

        expect(first.navigationItemId).toBe(second.navigationItemId);

        const navItems = await getNavItems(portalId);
        expect(navItems.filter(item => item.type === 'PAGE')).toHaveLength(1);
    });
});
