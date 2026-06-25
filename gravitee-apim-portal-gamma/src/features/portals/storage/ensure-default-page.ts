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
import { createPlaceholderDocument } from './dummy-navigation';
import { getNavItems, saveNavItem } from './navigation-items.storage';
import { getPageContent, savePageContent } from './page-contents.storage';

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

export async function ensureDefaultPageForPortal(portalId: string): Promise<PageContent> {
    const navItems = await getNavItems(portalId);
    let firstPage = navItems.find((item): item is PortalNavigationPage => item.type === 'PAGE');

    if (!firstPage) {
        const navItemId = createUniqueId();
        firstPage = {
            id: navItemId,
            portalId,
            title: 'Home',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: `home-${navItemId.slice(0, 6)}`,
        };
        await saveNavItem(firstPage);
    }

    const existing = await getPageContent(firstPage.id);
    if (existing) {
        return existing;
    }

    const content: PageContent = {
        id: createUniqueId(),
        portalId,
        navigationItemId: firstPage.id,
        document: createPlaceholderDocument(firstPage.title),
    };
    await savePageContent(content);
    return content;
}
