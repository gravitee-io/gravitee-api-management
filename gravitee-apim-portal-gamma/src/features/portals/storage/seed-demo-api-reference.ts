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
import { buildTagPageDefinitions } from '../../../blocks/ApiSpecBlock/api-ref-page-generator';
import type { PortalNavigationApi, PortalNavigationItem, PortalNavigationPage } from '../types';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';

const DEMO_API_ID = 'api-payments';
const DEMO_API_TITLE = 'Commerce Platform API';

function navId(portalId: string, key: string): string {
    return `${portalId}-${key}`;
}

/**
 * Seeds a demo API nav item with Overview + one block page per OpenAPI tag so the
 * composable API reference blocks can be tested without manually adding an API.
 */
export async function seedDemoApiReferenceNav(portalId: string): Promise<void> {
    const { overviewDocument, tagPages } = await buildTagPageDefinitions(DEMO_API_ID, DEMO_API_TITLE);

    const apiItem: PortalNavigationApi = {
        id: navId(portalId, 'nav-commerce-platform-api'),
        portalId,
        title: DEMO_API_TITLE,
        type: 'API',
        parentId: null,
        order: 9,
        slug: 'commerce-platform-api-nav015',
        apiId: DEMO_API_ID,
    };

    const overviewPage: PortalNavigationPage = {
        id: navId(portalId, 'nav-commerce-overview'),
        portalId,
        title: 'Overview',
        type: 'PAGE',
        parentId: apiItem.id,
        order: 0,
        slug: 'commerce-overview-nav016',
    };

    const tagNavPages: PortalNavigationPage[] = tagPages.map((tagPage, index) => ({
        id: navId(portalId, `nav-commerce-tag-${tagPage.tag.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`),
        portalId,
        title: tagPage.title,
        type: 'PAGE' as const,
        parentId: apiItem.id,
        order: index + 1,
        slug: `commerce-${tagPage.tag.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-nav0${17 + index}`,
    }));

    const navItems: PortalNavigationItem[] = [apiItem, overviewPage, ...tagNavPages];

    await Promise.all(navItems.map(item => saveNavItem(item)));

    await savePageContent({
        id: `page-content-${overviewPage.id}`,
        portalId,
        navigationItemId: overviewPage.id,
        contentType: 'BLOCK',
        document: overviewDocument,
    });

    await Promise.all(
        tagNavPages.map((page, index) =>
            savePageContent({
                id: `page-content-${page.id}`,
                portalId,
                navigationItemId: page.id,
                contentType: 'BLOCK',
                document: tagPages[index].document,
            }),
        ),
    );
}
