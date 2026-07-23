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
import { serializeDocumentToGmd } from '../../editor/gmd/gmd-content';
import { buildAiWorkspacePageDefinitions } from '../../portals/storage/ai-workspace-pages';
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import { savePageContent } from '../../portals/storage/page-contents.storage';
import type {
    PortalNavigationAiWorkspace,
    PortalNavigationItem,
    PortalNavigationPage,
} from '../../portals/types';
import { ensureUniqueSlug, generateSlug } from '../../portals/utils/slug';

export interface CreateAiWorkspaceNavItemWithPagesResult {
    readonly workspaceItem: PortalNavigationAiWorkspace;
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

export async function createAiWorkspaceNavItemWithPages(
    portalId: string,
    aiWorkspaceId: string,
    workspaceName: string,
    parentId: string | null,
    order: number,
    existingItems: readonly PortalNavigationItem[],
): Promise<CreateAiWorkspaceNavItemWithPagesResult> {
    const workspaceItemId = createUniqueId();

    const workspaceItem: PortalNavigationAiWorkspace = {
        id: workspaceItemId,
        portalId,
        title: workspaceName,
        type: 'AI_WORKSPACE',
        parentId,
        order,
        slug: createItemSlug(workspaceName, workspaceItemId, existingItems),
        aiWorkspaceId,
        published: true,
    };
    await saveNavItem(workspaceItem);

    const pageDefinitions = buildAiWorkspacePageDefinitions(aiWorkspaceId);

    let nextItems: PortalNavigationItem[] = [...existingItems, workspaceItem];
    let firstPage: PortalNavigationPage | null = null;

    for (const [index, definition] of pageDefinitions.entries()) {
        const pageId = createUniqueId();
        const pageItem: PortalNavigationPage = {
            id: pageId,
            portalId,
            title: definition.title,
            type: 'PAGE',
            parentId: workspaceItemId,
            order: index,
            slug: createItemSlug(definition.title, pageId, nextItems),
            published: true,
        };
        await saveNavItem(pageItem);
        nextItems = [...nextItems, pageItem];
        if (!firstPage) {
            firstPage = pageItem;
        }

        await savePageContent({
            id: createUniqueId(),
            portalId,
            navigationItemId: pageId,
            document: definition.document,
            gmd: serializeDocumentToGmd(definition.document),
        });
    }

    return {
        workspaceItem,
        firstPage,
        nextItems,
    };
}
