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
import { buildTagPageDefinitions } from '@apim/portal-editor/blocks/ApiSpecBlock/api-ref-page-generator';
import { serializeDocumentToGmd } from '@apim/portal-editor/editor/gmd/gmd-content';
import { looksLikeMarkdown } from '@apim/portal-editor/editor/utils/looks-like-markdown';
import type { BlockNoteDocument, BlockPageContent, PortalNavigationItem, PortalNavigationPage } from '@apim/portal-editor/portals/types';
import {
    deleteNavItemsForPortal,
    getNavItems,
    saveNavItem,
} from '@apim/portal-editor/portals/storage/navigation-items.storage';
import {
    deletePageContentsForPortal,
    getPageContent,
    savePageContent,
} from '@apim/portal-editor/portals/storage/page-contents.storage';
import { ensureUniqueSlug, generateSlug } from '@apim/portal-editor/portals/utils/slug';
import { getNextSiblingOrder } from '@apim/portal-editor/portal-shell/utils/nav-items';

export interface ApiDocumentationPublishPreferences {
    readonly portalId: string;
    readonly parentId: string | null;
}

const PREFERENCES_STORAGE_KEY = 'api-documentation-publish-preferences';

const ensureDraftInFlight = new Map<string, Promise<string>>();

function draftNavItemId(portalId: string, key: string): string {
    return `${portalId}-${key}`;
}

function tagNavItemId(portalId: string, tag: string): string {
    const slug = tag.toLowerCase().replace(/[^a-z0-9]+/g, '-');
    return draftNavItemId(portalId, `tag-${slug}`);
}

function pageContentId(navItemId: string): string {
    return `page-content-${navItemId}`;
}

function createItemSlug(title: string, id: string, existingItems: readonly { slug: string }[]): string {
    const existingSlugs = new Set(existingItems.map(item => item.slug));
    return ensureUniqueSlug(generateSlug(title, id), existingSlugs);
}

function hasDuplicateRootPageTitles(items: readonly PortalNavigationItem[]): boolean {
    const rootPageTitles = items
        .filter((item): item is PortalNavigationPage => item.type === 'PAGE' && item.parentId === null)
        .map(item => item.title);

    return rootPageTitles.length !== new Set(rootPageTitles).size;
}

function getBlockPlainText(block: Record<string, unknown>): string {
    const content = block.content;
    if (!Array.isArray(content)) {
        return '';
    }

    return content
        .map(node =>
            typeof node === 'object' && node !== null && 'text' in node ? String((node as { text?: string }).text ?? '') : '',
        )
        .join('');
}

function hasLegacyMarkdownParagraph(document: BlockNoteDocument): boolean {
    if (document.length < 5) {
        return false;
    }

    const tail = document.slice(4);
    if (tail.length !== 1) {
        return false;
    }

    const block = tail[0] as Record<string, unknown>;
    if (block.type !== 'paragraph') {
        return false;
    }

    return looksLikeMarkdown(getBlockPlainText(block));
}

function isBlockPageContent(content: { readonly contentType?: string; readonly document?: BlockNoteDocument }): content is BlockPageContent {
    return 'document' in content && content.document !== undefined;
}

async function hasLegacySeededDraftContent(portalId: string): Promise<boolean> {
    const overviewContent = await getPageContent(draftNavItemId(portalId, 'overview'));
    if (!overviewContent || !isBlockPageContent(overviewContent)) {
        return false;
    }

    return hasLegacyMarkdownParagraph(overviewContent.document);
}

async function shouldReseedDraft(portalId: string, items: readonly PortalNavigationItem[]): Promise<boolean> {
    if (hasDuplicateRootPageTitles(items)) {
        return true;
    }

    return hasLegacySeededDraftContent(portalId);
}

async function clearDraftPortal(portalId: string): Promise<void> {
    await Promise.all([deleteNavItemsForPortal(portalId), deletePageContentsForPortal(portalId)]);
}

export function getApiDocumentationDraftPortalId(apiId: string): string {
    return `api-doc-${apiId}`;
}

export function getPublishPreferences(apiId: string): ApiDocumentationPublishPreferences | null {
    try {
        const raw = localStorage.getItem(PREFERENCES_STORAGE_KEY);
        if (!raw) {
            return null;
        }
        const parsed = JSON.parse(raw) as Record<string, ApiDocumentationPublishPreferences>;
        return parsed[apiId] ?? null;
    } catch {
        return null;
    }
}

export function savePublishPreferences(apiId: string, preferences: ApiDocumentationPublishPreferences): void {
    try {
        const raw = localStorage.getItem(PREFERENCES_STORAGE_KEY);
        const parsed = raw ? (JSON.parse(raw) as Record<string, ApiDocumentationPublishPreferences>) : {};
        parsed[apiId] = preferences;
        localStorage.setItem(PREFERENCES_STORAGE_KEY, JSON.stringify(parsed));
    } catch {
        // Ignore storage errors.
    }
}

async function ensureApiDocumentationDraftInternal(apiId: string, apiName: string): Promise<string> {
    const portalId = getApiDocumentationDraftPortalId(apiId);
    let existingItems = await getNavItems(portalId);

    if (existingItems.length > 0) {
        if (await shouldReseedDraft(portalId, existingItems)) {
            await clearDraftPortal(portalId);
            existingItems = [];
        } else {
            return portalId;
        }
    }

    const { overviewDocument, tagPages } = await buildTagPageDefinitions(apiId, apiName);
    let items: PortalNavigationPage[] = [];

    const overviewPageId = draftNavItemId(portalId, 'overview');
    const overviewTitle = 'Overview';
    const overviewPage: PortalNavigationPage = {
        id: overviewPageId,
        portalId,
        title: overviewTitle,
        type: 'PAGE',
        parentId: null,
        order: getNextSiblingOrder(items),
        slug: createItemSlug(overviewTitle, overviewPageId, items),
        published: true,
    };
    await saveNavItem(overviewPage);
    await savePageContent({
        id: pageContentId(overviewPageId),
        portalId,
        navigationItemId: overviewPageId,
        document: overviewDocument,
        gmd: serializeDocumentToGmd(overviewDocument),
    });
    items = [overviewPage];

    for (const tagPage of tagPages) {
        const tagPageId = tagNavItemId(portalId, tagPage.tag ?? tagPage.title);
        const tagPageItem: PortalNavigationPage = {
            id: tagPageId,
            portalId,
            title: tagPage.title,
            type: 'PAGE',
            parentId: null,
            order: getNextSiblingOrder(items),
            slug: createItemSlug(tagPage.title, tagPageId, items),
            published: true,
        };
        await saveNavItem(tagPageItem);
        await savePageContent({
            id: pageContentId(tagPageId),
            portalId,
            navigationItemId: tagPageId,
            document: tagPage.document,
            gmd: serializeDocumentToGmd(tagPage.document),
        });
        items = [...items, tagPageItem];
    }

    return portalId;
}

export async function ensureApiDocumentationDraft(apiId: string, apiName: string): Promise<string> {
    const portalId = getApiDocumentationDraftPortalId(apiId);
    const inFlight = ensureDraftInFlight.get(portalId);
    if (inFlight) {
        return inFlight;
    }

    const promise = ensureApiDocumentationDraftInternal(apiId, apiName);
    ensureDraftInFlight.set(portalId, promise);

    try {
        return await promise;
    } finally {
        ensureDraftInFlight.delete(portalId);
    }
}
