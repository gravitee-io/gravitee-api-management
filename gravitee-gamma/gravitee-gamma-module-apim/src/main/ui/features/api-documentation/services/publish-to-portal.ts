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
import type {
    PageContent,
    PortalNavigationApi,
    PortalNavigationItem,
    PortalNavigationPage,
} from '@apim/portal-editor/portals/types';
import { getNavItems, saveNavItem, deleteNavItem } from '@apim/portal-editor/portals/storage/navigation-items.storage';
import {
    deletePageContent,
    getPageContentsForPortal,
    savePageContent,
} from '@apim/portal-editor/portals/storage/page-contents.storage';
import { collectDescendantIds } from '@apim/portal-editor/portal-shell/utils/nav-items';
import { ensureUniqueSlug, generateSlug } from '@apim/portal-editor/portals/utils/slug';

export type PublishMode = 'replace' | 'merge';

export interface PublishApiDocumentationOptions {
    readonly apiId: string;
    readonly apiName: string;
    readonly draftPortalId: string;
    readonly portalId: string;
    readonly parentId: string | null;
    readonly mode?: PublishMode;
}

export interface PublishApiDocumentationResult {
    readonly apiNavItemId: string;
    readonly publishedPageCount: number;
}

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function createItemSlug(title: string, id: string, existingItems: readonly { slug: string }[]): string {
    const existingSlugs = new Set(existingItems.map(item => item.slug));
    return ensureUniqueSlug(generateSlug(title, id), existingSlugs);
}

function getNextOrder(siblings: readonly PortalNavigationItem[]): number {
    if (siblings.length === 0) {
        return 0;
    }
    return Math.max(...siblings.map(item => item.order)) + 1;
}

async function deleteNavSubtree(items: readonly PortalNavigationItem[], rootId: string, portalId: string): Promise<void> {
    const idsToDelete = collectDescendantIds(items, rootId);
    const contents = await getPageContentsForPortal(portalId);

    await Promise.all(
        contents
            .filter(content => idsToDelete.includes(content.navigationItemId))
            .map(content => deletePageContent(content.id)),
    );

    for (const id of [...idsToDelete].reverse()) {
        await deleteNavItem(id);
    }
}

function remapDraftSubtree(
    draftItems: readonly PortalNavigationItem[],
    draftContents: readonly PageContent[],
    portalId: string,
    apiNavItemId: string,
    existingPortalItems: readonly PortalNavigationItem[],
): { navItems: PortalNavigationItem[]; pageContents: PageContent[] } {
    const draftRoots = draftItems.filter(item => item.parentId === null);
    const idMap = new Map<string, string>();
    const newNavItems: PortalNavigationItem[] = [];
    let portalItems = [...existingPortalItems];

    function walk(draftItem: PortalNavigationItem, newParentId: string): void {
        const newId = createUniqueId();
        idMap.set(draftItem.id, newId);
        const slug = createItemSlug(draftItem.title, newId, portalItems);
        const siblings = portalItems.filter(item => item.parentId === newParentId);
        const order = getNextOrder(siblings);

        const { id: _oldId, portalId: _oldPortalId, parentId: _oldParentId, slug: _oldSlug, order: _oldOrder, ...rest } =
            draftItem;

        const remapped = {
            ...rest,
            id: newId,
            portalId,
            parentId: newParentId,
            slug,
            order,
            published: true,
        } as PortalNavigationItem;

        newNavItems.push(remapped);
        portalItems = [...portalItems, remapped];

        const children = draftItems
            .filter(item => item.parentId === draftItem.id)
            .sort((a, b) => a.order - b.order);

        for (const child of children) {
            walk(child, newId);
        }
    }

    for (const root of draftRoots.sort((a, b) => a.order - b.order)) {
        walk(root, apiNavItemId);
    }

    const newPageContents: PageContent[] = [];
    for (const content of draftContents) {
        const newNavId = idMap.get(content.navigationItemId);
        if (!newNavId) {
            continue;
        }
        newPageContents.push({
            ...content,
            id: createUniqueId(),
            portalId,
            navigationItemId: newNavId,
        });
    }

    return { navItems: newNavItems, pageContents: newPageContents };
}

export async function publishApiDocumentationToPortal(
    options: PublishApiDocumentationOptions,
): Promise<PublishApiDocumentationResult> {
    const { apiId, apiName, draftPortalId, portalId, parentId, mode = 'replace' } = options;

    const [portalItems, draftItems, draftContents] = await Promise.all([
        getNavItems(portalId),
        getNavItems(draftPortalId),
        getPageContentsForPortal(draftPortalId),
    ]);

    const siblings = portalItems.filter(item => item.parentId === parentId);
    let apiNavItem = siblings.find(
        (item): item is PortalNavigationApi => item.type === 'API' && item.apiId === apiId,
    );

    if (apiNavItem && mode === 'replace') {
        await deleteNavSubtree(portalItems, apiNavItem.id, portalId);
        const refreshedItems = await getNavItems(portalId);
        apiNavItem = refreshedItems.find(
            (item): item is PortalNavigationApi => item.type === 'API' && item.apiId === apiId,
        ) as PortalNavigationApi | undefined;
    }

    if (!apiNavItem) {
        const apiItemId = createUniqueId();
        const refreshedSiblings = (await getNavItems(portalId)).filter(item => item.parentId === parentId);
        apiNavItem = {
            id: apiItemId,
            portalId,
            title: apiName,
            type: 'API',
            apiId,
            parentId,
            order: getNextOrder(refreshedSiblings),
            slug: createItemSlug(apiName, apiItemId, await getNavItems(portalId)),
            published: true,
        };
        await saveNavItem(apiNavItem);
    } else if (!apiNavItem.published) {
        await saveNavItem({ ...apiNavItem, published: true, title: apiName });
    }

    const latestPortalItems = await getNavItems(portalId);
    const { navItems: remappedNavItems, pageContents: remappedContents } = remapDraftSubtree(
        draftItems,
        draftContents,
        portalId,
        apiNavItem.id,
        latestPortalItems,
    );

    await Promise.all(remappedNavItems.map(item => saveNavItem(item)));
    await Promise.all(remappedContents.map(content => savePageContent(content)));

    return {
        apiNavItemId: apiNavItem.id,
        publishedPageCount: remappedNavItems.filter((item): item is PortalNavigationPage => item.type === 'PAGE').length,
    };
}
