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
import { createDummyPortals } from '../storage/dummy-portals';
import { buildNavItem } from '../storage/navigation-items.storage.test-utils';
import { buildPageContent } from '../storage/page-contents.storage.test-utils';
import { exportPortalToJson } from './portal-export-json';
import type { PortalExportBundle } from './portal-export.types';

function createBundle(): PortalExportBundle {
    const portal = createDummyPortals()[0];
    const { screenshotDataUrl: _screenshot, ...portalWithoutScreenshot } = portal;

    return {
        formatVersion: '1',
        exportedAt: '2026-06-29T12:00:00.000Z',
        portal: portalWithoutScreenshot,
        navigation: [
            buildNavItem({ id: 'nav-home', portalId: portal.id, slug: 'home-navhome' }),
        ],
        pageContents: [
            buildPageContent({
                portalId: portal.id,
                navigationItemId: 'nav-home',
            }),
        ],
        theme: createDefaultTheme(portal.id),
    };
}

describe('portal-export-json', () => {
    it('should serialize bundle as pretty-printed JSON without screenshot', () => {
        const bundle = createBundle();
        const json = exportPortalToJson(bundle);
        const parsed = JSON.parse(json) as PortalExportBundle;

        expect(parsed.formatVersion).toBe('1');
        expect(parsed.portal.id).toBe('portal-payments');
        expect(parsed.portal).not.toHaveProperty('screenshotDataUrl');
        expect(parsed.navigation).toHaveLength(1);
        expect(parsed.pageContents).toHaveLength(1);
        expect(parsed.theme.portalId).toBe('portal-payments');
        expect(json.endsWith('\n')).toBe(true);
    });
});
