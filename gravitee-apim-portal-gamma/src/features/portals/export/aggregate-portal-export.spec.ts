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
import { createDefaultTheme } from '../../theming/storage/default-theme';
import { saveTheme } from '../../theming/storage/theme.storage';
import { buildNavItem } from '../storage/navigation-items.storage.test-utils';
import { saveNavItem } from '../storage/navigation-items.storage';
import { buildPageContent } from '../storage/page-contents.storage.test-utils';
import { savePageContent } from '../storage/page-contents.storage';
import { clearPortalsDatabase } from '../storage/portals.storage.test-utils';
import { savePortal } from '../storage/portals.storage';
import { createDummyPortals } from '../storage/dummy-portals';
import { aggregatePortalExport, PortalExportError } from './aggregate-portal-export';

describe('aggregate-portal-export', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should aggregate portal, navigation, page contents, and theme', async () => {
        const portal = createDummyPortals()[0];
        await savePortal(portal);

        const folder = buildNavItem({
            id: 'nav-folder',
            portalId: portal.id,
            title: 'Docs',
            type: 'FOLDER',
            slug: 'docs',
            order: 0,
        });
        const page = buildNavItem({
            id: 'nav-home',
            portalId: portal.id,
            title: 'Home',
            type: 'PAGE',
            parentId: 'nav-folder',
            slug: 'home',
            order: 0,
        });
        await saveNavItem(folder);
        await saveNavItem(page);

        const pageContent = buildPageContent({
            id: 'content-home',
            portalId: portal.id,
            navigationItemId: 'nav-home',
            blockStyles: { 'block-1': { color: '#ff0000' } },
        });
        await savePageContent(pageContent);

        const theme = createDefaultTheme(portal.id);
        await saveTheme({ ...theme, activeMode: 'dark' });

        const bundle = await aggregatePortalExport(portal.id);

        expect(bundle.formatVersion).toBe('1');
        expect(bundle.exportedAt).toBeTruthy();
        expect(bundle.portal.id).toBe(portal.id);
        expect(bundle.portal).not.toHaveProperty('screenshotDataUrl');
        expect(bundle.navigation).toHaveLength(2);
        expect(bundle.pageContents).toHaveLength(1);
        expect(bundle.pageContents[0]?.blockStyles).toEqual({ 'block-1': { color: '#ff0000' } });
        expect(bundle.theme.activeMode).toBe('dark');
    });

    it('should throw when portal is missing', async () => {
        await expect(aggregatePortalExport('missing-portal')).rejects.toThrow(PortalExportError);
    });
});
