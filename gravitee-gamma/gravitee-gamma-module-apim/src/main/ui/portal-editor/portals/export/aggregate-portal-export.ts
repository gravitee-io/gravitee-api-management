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
import { getTheme } from '../../theming/storage/theme.storage';
import { getNavItems } from '../storage/navigation-items.storage';
import { getPageContent } from '../storage/page-contents.storage';
import { getPortal } from '../storage/portals.storage';
import type { PageContent } from '../types';
import { PORTAL_EXPORT_FORMAT_VERSION, type PortalExportBundle } from './portal-export.types';

export class PortalExportError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'PortalExportError';
    }
}

export async function aggregatePortalExport(portalId: string): Promise<PortalExportBundle> {
    const portal = await getPortal(portalId);
    if (!portal) {
        throw new PortalExportError(`Portal not found: ${portalId}`);
    }

    const [navigation, theme] = await Promise.all([getNavItems(portalId), getTheme(portalId)]);

    const pageNavItems = navigation.filter(item => item.type === 'PAGE');
    const pageContents = (
        await Promise.all(pageNavItems.map(item => getPageContent(item.id)))
    ).filter((content): content is PageContent => content !== undefined);

    const { screenshotDataUrl: _screenshot, ...portalWithoutScreenshot } = portal;

    return {
        formatVersion: PORTAL_EXPORT_FORMAT_VERSION,
        exportedAt: new Date().toISOString(),
        portal: portalWithoutScreenshot,
        navigation,
        pageContents,
        theme,
    };
}
