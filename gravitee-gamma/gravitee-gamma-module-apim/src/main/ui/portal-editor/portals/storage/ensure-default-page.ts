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
import type { PageContent, PortalNavigationPage } from '../types';
import { generateSlug } from '../utils/slug';
import { createPlaceholderDocument } from './dummy-navigation';
import { getNavItems, saveNavItem, getNavItem } from './navigation-items.storage';
import { getPageContent, savePageContent } from './page-contents.storage';

function defaultHomeNavItemId(portalId: string): string {
    return `${portalId}-default-home`;
}

function defaultHomePageContentId(portalId: string): string {
    return `page-content-${defaultHomeNavItemId(portalId)}`;
}

const ensureDefaultPageInFlight = new Map<string, Promise<PageContent>>();

async function ensureDefaultPageForPortalInternal(portalId: string): Promise<PageContent> {
    const navItemId = defaultHomeNavItemId(portalId);
    const existingNavItem = await getNavItem(navItemId);
    let firstPage =
        existingNavItem?.type === 'PAGE'
            ? existingNavItem
            : (await getNavItems(portalId)).find((item): item is PortalNavigationPage => item.type === 'PAGE');

    if (!firstPage) {
        firstPage = {
            id: navItemId,
            portalId,
            title: 'Home',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: generateSlug('Home', navItemId),
        };
        await saveNavItem(firstPage);
    }

    const existing = await getPageContent(firstPage.id);
    if (existing) {
        return existing;
    }

    const content: PageContent = {
        id: defaultHomePageContentId(portalId),
        portalId,
        navigationItemId: firstPage.id,
        contentType: 'BLOCK',
        document: createPlaceholderDocument(firstPage.title),
    };
    await savePageContent(content);

    return (await getPageContent(firstPage.id)) ?? content;
}

export async function ensureDefaultPageForPortal(portalId: string): Promise<PageContent> {
    const inFlight = ensureDefaultPageInFlight.get(portalId);
    if (inFlight) {
        return inFlight;
    }

    const promise = ensureDefaultPageForPortalInternal(portalId);
    ensureDefaultPageInFlight.set(portalId, promise);

    try {
        return await promise;
    } finally {
        ensureDefaultPageInFlight.delete(portalId);
    }
}
