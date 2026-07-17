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
import { serializeDocumentToGmd } from '../../editor/gmd/gmd-content';
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import { savePageContent } from '../../portals/storage/page-contents.storage';
import type { PortalNavigationApi, PortalNavigationItem, PortalNavigationPage } from '../../portals/types';
import {
    ensureUniqueSlug,
    generateSlug,
} from '../../portals/utils/slug';

export interface CreateApiNavItemWithPagesResult {
    readonly apiItem: PortalNavigationApi;
    readonly firstPage: PortalNavigationPage | null;
    readonly nextItems: PortalNavigationItem[];
}

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function createItemSlug(title: string, id: string, existingItems: readonly PortalNavigationItem[]): string {
    const existingSlugs = new Set(existingItems.map(item => item.slug));
    return ensureUniqueSlug(generateSlug(title, id), existingSlugs);
}

export async function createApiNavItemWithPages(
    portalId: string,
    apiId: string,
    apiName: string,
    parentId: string | null,
    order: number,
    existingItems: readonly PortalNavigationItem[],
): Promise<CreateApiNavItemWithPagesResult> {
    const apiItemId = createUniqueId();
    const apiTitle = apiName;

    const apiItem: PortalNavigationApi = {
        id: apiItemId,
        portalId,
        title: apiTitle,
        type: 'API',
        parentId,
        order,
        slug: createItemSlug(apiTitle, apiItemId, existingItems),
        apiId,
        published: true,
    };
    await saveNavItem(apiItem);

    const { overviewDocument, tagPages } = await buildTagPageDefinitions(apiId, apiTitle);

    const pageId = createUniqueId();
    const pageTitle = 'Overview';
    const pageItem: PortalNavigationPage = {
        id: pageId,
        portalId,
        title: pageTitle,
        type: 'PAGE',
        parentId: apiItemId,
        order: 0,
        slug: createItemSlug(pageTitle, pageId, [...existingItems, apiItem]),
        published: true,
    };
    await saveNavItem(pageItem);

    await savePageContent({
        id: createUniqueId(),
        portalId,
        navigationItemId: pageId,
        document: overviewDocument,
        gmd: serializeDocumentToGmd(overviewDocument),
    });

    let nextItems: PortalNavigationItem[] = [...existingItems, apiItem, pageItem];
    for (const [index, tagPage] of tagPages.entries()) {
        const tagPageId = createUniqueId();
        const tagPageItem: PortalNavigationPage = {
            id: tagPageId,
            portalId,
            title: tagPage.title,
            type: 'PAGE',
            parentId: apiItemId,
            order: index + 1,
            slug: createItemSlug(tagPage.title, tagPageId, nextItems),
            published: true,
        };
        await saveNavItem(tagPageItem);
        nextItems = [...nextItems, tagPageItem];

        await savePageContent({
            id: createUniqueId(),
            portalId,
            navigationItemId: tagPageId,
            document: tagPage.document,
            gmd: serializeDocumentToGmd(tagPage.document),
        });
    }

    return {
        apiItem,
        firstPage: pageItem,
        nextItems,
    };
}
