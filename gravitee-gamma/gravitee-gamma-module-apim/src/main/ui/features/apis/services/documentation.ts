/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ApimApiError, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import { getEnvironmentPortalSettings } from '../../settings/services/portalSettings';
import type {
    ApiPortalPlacement,
    CreateDocumentationPayload,
    DocumentationPage,
    DocumentationPagesResult,
    EditDocumentationPayload,
    FetcherListItem,
    PortalFolderOption,
    PortalNavigationItem,
    PortalNavigationItemsResponse,
    SpecGenRequestState,
} from '../types/documentation';
import { SUPPORTED_FOR_EDIT } from '../types/documentation';
import { fromPortalContentType, normalizeParentId, toPortalContentType } from '../utils/documentationFormatters';
import {
    apiDocumentationNavItems,
    breadcrumbFor,
    documentationPathKey,
    isDescendantOfApi,
    isUnderApiNode,
    mapNavItemToPage,
    mergeDocumentationWithPortal,
    navigationPathKey,
} from '../utils/documentationPortalSync';

function pagesPath(apiId: string, suffix = ''): string {
    return `/apis/${encodeURIComponent(apiId)}/pages${suffix}`;
}

export function isEditablePageType(type: string | undefined): boolean {
    return type === 'FOLDER' || (type !== undefined && (SUPPORTED_FOR_EDIT as readonly string[]).includes(type));
}

export function filterDocumentationPages(result: DocumentationPagesResult): DocumentationPagesResult {
    return {
        pages: (result.pages ?? []).filter(page => isEditablePageType(page.type)),
        breadcrumb: [...(result.breadcrumb ?? [])].sort((a, b) => a.position - b.position),
    };
}

async function fetchClassicApiPages(envId: string, apiId: string): Promise<DocumentationPagesResult> {
    const result = await apimFetchJsonV2<DocumentationPagesResult>(envId, pagesPath(apiId));
    return filterDocumentationPages(result);
}

async function fetchClassicApiPage(envId: string, apiId: string, pageId: string): Promise<DocumentationPage> {
    return apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(pageId)}`));
}

function isNotFound(error: unknown): boolean {
    return error instanceof ApimApiError && error.status === 404;
}

function withoutPortalFields<T extends object>(payload: T): T {
    const rest = { ...payload } as T & { portalNavId?: string; portalPageContentId?: string };
    delete rest.portalNavId;
    delete rest.portalPageContentId;
    return rest;
}

function mergePageEdit(page: DocumentationPage, payload: EditDocumentationPayload, pageId: string): DocumentationPage {
    return { ...page, ...payload, id: pageId, configuration: page.configuration };
}

export async function getAllApiPages(envId: string, apiId: string): Promise<DocumentationPagesResult> {
    const classic = await fetchClassicApiPages(envId, apiId);
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    // Include Navigation docs under this API whether the API nav node is published or not.
    if (!placement) return classic;

    const navItems = await listPortalNavigationItems(envId);
    const merged = mergeDocumentationWithPortal(classic.pages, navItems, placement.itemId);
    return {
        pages: await withPortalContentTypes(envId, merged),
        breadcrumb: classic.breadcrumb,
    };
}

export async function getApiPages(envId: string, apiId: string, parentId = 'ROOT'): Promise<DocumentationPagesResult> {
    const all = await getAllApiPages(envId, apiId);
    const parent = normalizeParentId(parentId);
    return {
        pages: all.pages.filter(page => normalizeParentId(page.parentId) === parent),
        breadcrumb: breadcrumbFor(all.pages, parent),
    };
}

export async function getApiPage(envId: string, apiId: string, pageId: string): Promise<DocumentationPage> {
    try {
        const classic = await fetchClassicApiPage(envId, apiId, pageId);
        return overlayPortalOnPage(envId, apiId, classic);
    } catch (error) {
        if (isNotFound(error)) {
            const fromPortal = await getPageFromPortal(envId, apiId, pageId);
            if (fromPortal) return fromPortal;
        }
        throw error;
    }
}

export async function createDocumentationPage(
    envId: string,
    apiId: string,
    payload: CreateDocumentationPayload,
): Promise<DocumentationPage> {
    // Place the API under the configured default Navigation folder when missing so the
    // new page/folder can be mirrored there immediately (unpublished).
    const placement = await ensureApiPlacement(envId, apiId);

    let classicPages: DocumentationPage[] = [];
    try {
        classicPages = (await fetchClassicApiPages(envId, apiId)).pages;
    } catch {
        classicPages = [];
    }

    const parentIsClassic = isClassicParent(payload.parentId, classicPages);
    if (!parentIsClassic) {
        return createPortalOnlyPage(envId, apiId, payload);
    }

    const created = await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId), {
        method: 'POST',
        body: JSON.stringify(withoutPortalFields(payload)),
    });
    return mirrorPageToPortal(envId, apiId, { ...created, published: false }, undefined, placement);
}

export async function updateDocumentationPage(
    envId: string,
    apiId: string,
    pageId: string,
    payload: EditDocumentationPayload,
): Promise<DocumentationPage> {
    try {
        const updated = await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(pageId)}`), {
            method: 'PUT',
            body: JSON.stringify(withoutPortalFields(payload)),
        });
        return tryMirrorPageToPortal(envId, apiId, mergePageEdit(updated, payload, pageId));
    } catch (error) {
        if (!isNotFound(error)) throw error;
        const fromPortal = await updatePortalOnlyPage(envId, apiId, pageId, payload);
        if (fromPortal) return fromPortal;
        throw error;
    }
}

export async function publishDocumentationPage(envId: string, apiId: string, pageId: string): Promise<DocumentationPage> {
    try {
        const published = await apimFetchJsonV2<DocumentationPage>(
            envId,
            pagesPath(apiId, `/${encodeURIComponent(pageId)}/_publish`),
            { method: 'POST', body: JSON.stringify({}) },
        );
        return tryMirrorPageToPortal(envId, apiId, { ...published, published: true });
    } catch (error) {
        if (!isNotFound(error)) throw error;
        const fromPortal = await setPortalOnlyPublished(envId, apiId, pageId, true);
        if (fromPortal) return fromPortal;
        throw error;
    }
}

export async function unpublishDocumentationPage(envId: string, apiId: string, pageId: string): Promise<DocumentationPage> {
    try {
        const unpublished = await apimFetchJsonV2<DocumentationPage>(
            envId,
            pagesPath(apiId, `/${encodeURIComponent(pageId)}/_unpublish`),
            { method: 'POST', body: JSON.stringify({}) },
        );
        return tryMirrorPageToPortal(envId, apiId, { ...unpublished, published: false });
    } catch (error) {
        if (!isNotFound(error)) throw error;
        const fromPortal = await setPortalOnlyPublished(envId, apiId, pageId, false);
        if (fromPortal) return fromPortal;
        throw error;
    }
}

export async function fetchDocumentationPage(envId: string, apiId: string, pageId: string): Promise<DocumentationPage> {
    const fetched = await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(pageId)}/_fetch`), {
        method: 'POST',
        body: JSON.stringify({}),
    });
    return tryMirrorPageToPortal(envId, apiId, fetched);
}

export async function deleteDocumentationPage(envId: string, apiId: string, pageId: string): Promise<void> {
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    const navItems = placement ? await listPortalNavigationItems(envId) : [];
    const apiNav =
        placement &&
        navItems.find(item => item.id === placement.itemId && item.type === 'API' && item.apiId === apiId);

    let portalNavId: string | undefined;
    let deleteClassic = true;

    try {
        const page = await getApiPage(envId, apiId, pageId);
        portalNavId = page.portalNavId;
        // Navigation-only docs use the nav id as the page id — skip classic DELETE.
        deleteClassic = Boolean(page.id && page.id !== page.portalNavId);
    } catch {
        if (navItems.some(item => item.id === pageId && (item.type === 'PAGE' || item.type === 'FOLDER'))) {
            portalNavId = pageId;
            deleteClassic = false;
        }
    }

    if (!portalNavId && apiNav) {
        try {
            const classic = await fetchClassicApiPages(envId, apiId);
            const page = classic.pages.find(item => item.id === pageId);
            if (page) {
                const parentNavId = resolveParentNavId(page, classic.pages, navItems, apiNav.id);
                portalNavId = findNavMirror(page, classic.pages, navItems, apiNav.id, parentNavId)?.id;
            }
        } catch {
            // ignore — classic may already be gone
        }
    }

    if (deleteClassic) {
        try {
            await apimFetchJsonV2<void>(envId, pagesPath(apiId, `/${encodeURIComponent(pageId)}`), { method: 'DELETE' });
        } catch (error) {
            if (!isNotFound(error)) throw error;
        }
    }

    if (!apiNav || !portalNavId) return;

    const docs = apiDocumentationNavItems(navItems, apiNav.id);
    const toDelete = docs
        .filter(item => item.id === portalNavId || isDescendantOfNav(navItems, item, portalNavId))
        .sort((left, right) => navDepth(navItems, right, apiNav.id) - navDepth(navItems, left, apiNav.id));

    for (const item of toDelete) {
        if (!item.id) continue;
        try {
            await deletePortalNavigationItem(envId, item.id);
        } catch (error) {
            if (!isNotFound(error)) throw error;
        }
    }

    const remaining = docs.filter(item => item.id && !toDelete.some(deleted => deleted.id === item.id));
    if (remaining.length === 0) {
        try {
            await deletePortalNavigationItem(envId, apiNav.id);
        } catch (error) {
            if (!isNotFound(error)) throw error;
        }
    }
}

async function deletePortalNavigationItem(envId: string, itemId: string): Promise<void> {
    await apimFetchJsonV2<void>(envId, `/portal-navigation-items/${encodeURIComponent(itemId)}`, { method: 'DELETE' });
}

function isDescendantOfNav(items: PortalNavigationItem[], item: PortalNavigationItem, ancestorId: string): boolean {
    const byId = new Map(items.filter(entry => entry.id).map(entry => [entry.id!, entry]));
    let current: PortalNavigationItem | undefined = item;
    const seen = new Set<string>();
    while (current?.parentId && current.id && !seen.has(current.id)) {
        seen.add(current.id);
        if (current.parentId === ancestorId) return true;
        current = byId.get(current.parentId);
    }
    return false;
}

function navDepth(items: PortalNavigationItem[], item: PortalNavigationItem, apiNavId: string): number {
    const byId = new Map(items.filter(entry => entry.id).map(entry => [entry.id!, entry]));
    let depth = 0;
    let parentId = item.parentId;
    const seen = new Set<string>();
    while (parentId && parentId !== apiNavId && byId.has(parentId) && !seen.has(parentId)) {
        seen.add(parentId);
        depth += 1;
        parentId = byId.get(parentId)?.parentId;
    }
    return depth;
}

export async function listFetchers(envId: string): Promise<FetcherListItem[]> {
    return apimFetchJsonV1Env<FetcherListItem[]>(envId, '/fetchers?expand=schema');
}

export async function getSpecGenState(envId: string, apiId: string): Promise<SpecGenRequestState> {
    return apimFetchJsonV2<SpecGenRequestState>(envId, `/apis/${encodeURIComponent(apiId)}/spec-gen/_state`);
}

export async function startSpecGen(envId: string, apiId: string): Promise<SpecGenRequestState> {
    return apimFetchJsonV2<SpecGenRequestState>(envId, `/apis/${encodeURIComponent(apiId)}/spec-gen/_start`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

const PORTAL_AREA = 'TOP_NAVBAR';

function flattenNavItems(items: PortalNavigationItem[], acc: PortalNavigationItem[] = []): PortalNavigationItem[] {
    for (const item of items) {
        acc.push(item);
        if (item.children?.length) flattenNavItems(item.children, acc);
    }
    return acc;
}

function folderPath(itemsById: Map<string, PortalNavigationItem>, folder: PortalNavigationItem): string {
    const parts: string[] = [];
    let current: PortalNavigationItem | undefined = folder;
    const seen = new Set<string>();
    while (current?.type === 'FOLDER' && current.id && !seen.has(current.id)) {
        seen.add(current.id);
        parts.unshift(current.title ?? 'Folder');
        current = current.parentId ? itemsById.get(current.parentId) : undefined;
    }
    return parts.join(' / ');
}

function foldersFromItems(items: PortalNavigationItem[], area: string): PortalFolderOption[] {
    const itemsById = new Map(items.filter(item => item.id).map(item => [item.id!, item]));
    const folders: PortalFolderOption[] = [];
    for (const item of items) {
        if (item.type !== 'FOLDER' || !item.id || isUnderApiNode(items, item) || folders.some(folder => folder.id === item.id)) continue;
        folders.push({ id: item.id, path: folderPath(itemsById, item) || (item.title ?? 'Folder'), area: item.area ?? area });
    }
    return folders;
}

function findApiPlacement(items: PortalNavigationItem[], apiId: string, foldersById: Map<string, PortalFolderOption>): ApiPortalPlacement | null {
    for (const item of items) {
        if (item.type !== 'API' || item.apiId !== apiId || !item.id) continue;
        const folder = item.parentId ? foldersById.get(item.parentId) : undefined;
        return {
            folderId: folder?.id ?? item.parentId ?? '',
            folderPath: folder?.path ?? 'Top navbar',
            itemId: item.id,
            area: folder?.area ?? item.area ?? PORTAL_AREA,
            published: Boolean(item.published),
        };
    }
    return null;
}

async function listPortalNavigationItems(
    envId: string,
    { optional = false }: { optional?: boolean } = {},
): Promise<PortalNavigationItem[]> {
    try {
        const response = await apimFetchJsonV2<PortalNavigationItemsResponse>(
            envId,
            `/portal-navigation-items?area=${PORTAL_AREA}&loadChildren=true&includes=apis`,
        );
        return flattenNavItems(response.items ?? []);
    } catch (error) {
        if (optional) return [];
        throw error;
    }
}

export async function listPortalDocumentationFolders(
    envId: string,
    apiId: string,
): Promise<{ folders: PortalFolderOption[]; placement: ApiPortalPlacement | null }> {
    const items = await listPortalNavigationItems(envId, { optional: true });
    const folders = foldersFromItems(items, PORTAL_AREA);
    return { folders, placement: findApiPlacement(items, apiId, new Map(folders.map(folder => [folder.id, folder]))) };
}

export async function placeApiInPortalFolder(
    envId: string,
    apiId: string,
    title: string,
    folder: PortalFolderOption,
    visibility: 'PUBLIC' | 'PRIVATE' = 'PUBLIC',
): Promise<PortalNavigationItem> {
    return apimFetchJsonV2<PortalNavigationItem>(envId, '/portal-navigation-items', {
        method: 'POST',
        body: JSON.stringify({
            type: 'API',
            apiId,
            title,
            parentId: folder.id,
            area: PORTAL_AREA,
            visibility,
            published: false,
        }),
    });
}

function folderOptionFromItem(
    items: PortalNavigationItem[],
    folderId: string,
    area: string,
): PortalFolderOption | undefined {
    const itemsById = new Map(items.filter(item => item.id).map(item => [item.id!, item]));
    const item = itemsById.get(folderId);
    if (!item || item.type !== 'FOLDER') return undefined;
    return {
        id: item.id,
        path: folderPath(itemsById, item) || (item.title ?? 'Folder'),
        area: item.area ?? area,
    };
}

/**
 * Returns the API's Navigation placement, placing it under the configured default folder when missing.
 * Throws when the default folder is not configured or no longer available.
 */
async function ensureApiPlacement(envId: string, apiId: string): Promise<ApiPortalPlacement> {
    const items = await listPortalNavigationItems(envId);
    const folders = foldersFromItems(items, PORTAL_AREA);
    const placement = findApiPlacement(items, apiId, new Map(folders.map(folder => [folder.id, folder])));
    if (placement) return placement;

    const settings = await getEnvironmentPortalSettings(envId);
    const defaultFolderId = settings.portalNext?.documentation?.defaultFolderId?.trim();
    if (!defaultFolderId) {
        throw new Error(
            'Configure a default Navigation folder in Portal Settings → Settings before creating API documentation.',
        );
    }

    const folder =
        folders.find(entry => entry.id === defaultFolderId) ?? folderOptionFromItem(items, defaultFolderId, PORTAL_AREA);
    if (!folder) {
        throw new Error(
            'The default Navigation folder configured in Portal Settings is missing or no longer available. Update it under Portal Settings → Settings.',
        );
    }

    let title = apiId;
    try {
        const api = await apimFetchJsonV2<{ name?: string }>(envId, `/apis/${encodeURIComponent(apiId)}`);
        if (api.name?.trim()) title = api.name.trim();
    } catch {
        // fall back to apiId
    }

    const folderItem = items.find(item => item.id === folder.id);
    const visibility = folderItem?.visibility === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC';
    const created = await placeApiInPortalFolder(envId, apiId, title, folder, visibility);
    if (!created.id) {
        throw new Error('Failed to place the API under the default Navigation folder.');
    }
    return {
        folderId: folder.id,
        folderPath: folder.path,
        itemId: created.id,
        area: folder.area,
        published: Boolean(created.published),
    };
}

async function createPortalNavigationItem(envId: string, payload: Record<string, unknown>): Promise<PortalNavigationItem> {
    return apimFetchJsonV2<PortalNavigationItem>(envId, '/portal-navigation-items', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

async function updatePortalNavigationItem(
    envId: string,
    itemId: string,
    payload: Record<string, unknown>,
    propagatePublishToChildren = false,
): Promise<void> {
    const suffix = propagatePublishToChildren ? '?propagatePublishToChildren=true' : '';
    await apimFetchJsonV2<unknown>(envId, `/portal-navigation-items/${encodeURIComponent(itemId)}${suffix}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

function navigationUpdatePayload(
    item: PortalNavigationItem,
    overrides: Partial<{
        title: string;
        published: boolean;
        visibility: string;
        order: number;
        parentId: string | null;
        apiId: string;
    }> = {},
): Record<string, unknown> {
    const parentId = overrides.parentId !== undefined ? overrides.parentId : (item.parentId ?? null);
    const payload: Record<string, unknown> = {
        type: item.type,
        title: overrides.title ?? item.title ?? 'Item',
        order: overrides.order ?? item.order ?? 0,
        published: overrides.published ?? item.published ?? false,
        visibility: overrides.visibility ?? item.visibility ?? 'PUBLIC',
        parentId,
    };
    if (item.type === 'API') {
        payload.apiId = overrides.apiId ?? item.apiId;
        payload.categoryIds = item.categoryIds ?? [];
    }
    return payload;
}

/**
 * Publish (and make PUBLIC when needed) every ancestor from root down to `itemId`,
 * so a published PUBLIC child can be created underneath.
 */
async function ensureAncestorsAllowPublishedPublicChild(
    envId: string,
    items: PortalNavigationItem[],
    itemId: string | undefined | null,
): Promise<void> {
    if (!itemId) return;
    const byId = new Map(items.filter(item => item.id).map(item => [item.id!, item]));
    const chain: PortalNavigationItem[] = [];
    let current = byId.get(itemId);
    const seen = new Set<string>();
    while (current?.id && !seen.has(current.id)) {
        seen.add(current.id);
        chain.unshift(current);
        current = current.parentId ? byId.get(current.parentId) : undefined;
    }

    for (const item of chain) {
        const needsPublish = item.published !== true;
        const needsPublic = item.visibility === 'PRIVATE';
        if (!needsPublish && !needsPublic) continue;
        await updatePortalNavigationItem(
            envId,
            item.id,
            navigationUpdatePayload(item, {
                published: true,
                visibility: 'PUBLIC',
            }),
        );
        item.published = true;
        item.visibility = 'PUBLIC';
    }
}

async function updatePortalPageContent(
    envId: string,
    contentId: string,
    content: string,
    type: ReturnType<typeof toPortalContentType>,
): Promise<void> {
    if (!type) return;
    await apimFetchJsonV2<unknown>(envId, `/portal-page-contents/${encodeURIComponent(contentId)}`, {
        method: 'PUT',
        body: JSON.stringify({ content, type }),
    });
}

async function getPortalPageContent(
    envId: string,
    contentId: string,
): Promise<{ content?: string; type?: string } | null> {
    try {
        return await apimFetchJsonV2<{ content?: string; type?: string }>(
            envId,
            `/portal-page-contents/${encodeURIComponent(contentId)}`,
        );
    } catch {
        return null;
    }
}

async function withPortalContentTypes(envId: string, pages: DocumentationPage[]): Promise<DocumentationPage[]> {
    return Promise.all(
        pages.map(async page => {
            if (page.type === 'FOLDER' || !page.portalPageContentId || page.id !== page.portalNavId) return page;
            const content = await getPortalPageContent(envId, page.portalPageContentId);
            const type = fromPortalContentType(content?.type);
            return type ? { ...page, type } : page;
        }),
    );
}

async function overlayPortalOnPage(envId: string, apiId: string, page: DocumentationPage): Promise<DocumentationPage> {
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    if (!placement) return page;
    const [classic, navItems] = await Promise.all([
        fetchClassicApiPages(envId, apiId).catch(() => ({ pages: [page], breadcrumb: [] })),
        listPortalNavigationItems(envId),
    ]);
    const merged = mergeDocumentationWithPortal(
        classic.pages.some(item => item.id === page.id) ? classic.pages : [...classic.pages, page],
        navItems,
        placement.itemId,
    );
    const overlay = merged.find(item => item.id === page.id);
    if (!overlay?.portalNavId) return page;

    let content = page.content;
    let type = overlay.type ?? page.type;
    if (overlay.portalPageContentId) {
        const portalContent = await getPortalPageContent(envId, overlay.portalPageContentId);
        if (portalContent?.content) content = portalContent.content;
        type = fromPortalContentType(portalContent?.type) ?? type;
    }
    return { ...page, ...overlay, content, type, source: page.source, configuration: page.configuration };
}

function isClassicParent(parentId: string | undefined, classicPages: DocumentationPage[]): boolean {
    const parent = normalizeParentId(parentId);
    if (!parent) return true;
    return classicPages.some(page => page.id === parent);
}

async function getPageFromPortal(envId: string, apiId: string, pageId: string): Promise<DocumentationPage | null> {
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    if (!placement) return null;
    const navItems = await listPortalNavigationItems(envId);
    const item = navItems.find(nav => nav.id === pageId);
    if (!item || (item.type !== 'FOLDER' && item.type !== 'PAGE') || !isDescendantOfApi(navItems, item, placement.itemId)) {
        return null;
    }
    const page = mapNavItemToPage(item, placement.itemId);
    if (!page.portalPageContentId) return page;
    const portalContent = await getPortalPageContent(envId, page.portalPageContentId);
    return {
        ...page,
        content: portalContent?.content ?? page.content,
        type: fromPortalContentType(portalContent?.type) ?? page.type,
    };
}

function resolveParentNavId(
    page: DocumentationPage,
    pages: DocumentationPage[],
    navItems: PortalNavigationItem[],
    apiNavId: string,
): string {
    const parentDocId = normalizeParentId(page.parentId);
    if (!parentDocId || parentDocId === apiNavId) return apiNavId;
    if (navItems.some(item => item.id === parentDocId)) return parentDocId;
    const parentPage = pages.find(item => item.id === parentDocId);
    if (parentPage?.portalNavId) return parentPage.portalNavId;
    if (parentPage) {
        const path = documentationPathKey(pages, parentPage);
        const match = navItems.find(
            item => (item.type === 'FOLDER' || item.type === 'PAGE') && navigationPathKey(navItems, item, apiNavId) === path,
        );
        if (match) return match.id;
    }
    return apiNavId;
}

function findNavMirror(
    page: DocumentationPage,
    pages: DocumentationPage[],
    navItems: PortalNavigationItem[],
    apiNavId: string,
    parentNavId: string,
): PortalNavigationItem | undefined {
    if (page.portalNavId) {
        const byId = navItems.find(item => item.id === page.portalNavId);
        if (byId) return byId;
    }
    const path = documentationPathKey(pages, page);
    const byPath = navItems.find(
        item => (item.type === 'FOLDER' || item.type === 'PAGE') && navigationPathKey(navItems, item, apiNavId) === path,
    );
    if (byPath) return byPath;
    const type = page.type === 'FOLDER' ? 'FOLDER' : 'PAGE';
    return findExistingNavItem(navItems, parentNavId, page.name, type);
}

async function createPortalOnlyPage(
    envId: string,
    apiId: string,
    payload: CreateDocumentationPayload,
): Promise<DocumentationPage> {
    const placement = await ensureApiPlacement(envId, apiId);
    const type = payload.type === 'FOLDER' ? 'FOLDER' : 'PAGE';
    const contentType = toPortalContentType(payload.type);
    if (type === 'PAGE' && !contentType) {
        return apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId), {
            method: 'POST',
            body: JSON.stringify(payload),
        });
    }
    const nav = await createPortalNavigationItem(envId, {
        type,
        title: payload.name,
        parentId: normalizeParentId(payload.parentId) ?? placement.itemId,
        area: PORTAL_AREA,
        visibility: payload.visibility ?? 'PUBLIC',
        published: false,
        ...(contentType ? { contentType } : {}),
    });
    if (type === 'PAGE' && nav.portalPageContentId && payload.content) {
        await updatePortalPageContent(envId, nav.portalPageContentId, payload.content, contentType);
    }
    return {
        ...mapNavItemToPage(nav, placement.itemId, payload.type),
        content: payload.content,
        visibility: payload.visibility,
        published: false,
    };
}

async function updatePortalOnlyPage(
    envId: string,
    apiId: string,
    pageId: string,
    payload: EditDocumentationPayload,
): Promise<DocumentationPage | null> {
    const fromPortal = await getPageFromPortal(envId, apiId, pageId);
    if (!fromPortal?.portalNavId) return null;
    const type = (payload.type ?? fromPortal.type) === 'FOLDER' ? 'FOLDER' : 'PAGE';
    await updatePortalNavigationItem(envId, fromPortal.portalNavId, {
        type,
        title: payload.name ?? fromPortal.name,
        parentId: normalizeParentId(payload.parentId ?? fromPortal.parentId) ?? (await apiNavParentId(envId, apiId)),
        order: payload.order ?? fromPortal.order,
        published: payload.published ?? fromPortal.published,
        visibility: payload.visibility ?? fromPortal.visibility ?? 'PUBLIC',
    });
    const contentType = toPortalContentType(payload.type ?? fromPortal.type);
    if (type === 'PAGE' && fromPortal.portalPageContentId && payload.content != null) {
        await updatePortalPageContent(envId, fromPortal.portalPageContentId, payload.content, contentType);
    }
    return {
        ...mergePageEdit(fromPortal, payload, pageId),
        portalNavId: fromPortal.portalNavId,
        portalPageContentId: fromPortal.portalPageContentId,
    };
}

async function setPortalOnlyPublished(
    envId: string,
    apiId: string,
    pageId: string,
    published: boolean,
): Promise<DocumentationPage | null> {
    const fromPortal = await getPageFromPortal(envId, apiId, pageId);
    if (!fromPortal?.portalNavId) return null;
    const type = fromPortal.type === 'FOLDER' ? 'FOLDER' : 'PAGE';
    await updatePortalNavigationItem(
        envId,
        fromPortal.portalNavId,
        navigationUpdatePayload(
            {
                id: fromPortal.portalNavId,
                type,
                title: fromPortal.name,
                parentId: normalizeParentId(fromPortal.parentId) ?? (await apiNavParentId(envId, apiId)) ?? null,
                order: fromPortal.order ?? 0,
                published: fromPortal.published,
                visibility: fromPortal.visibility ?? 'PUBLIC',
            },
            { published },
        ),
    );
    return { ...fromPortal, published };
}

async function apiNavParentId(envId: string, apiId: string): Promise<string | undefined> {
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    return placement?.itemId;
}

async function tryMirrorPageToPortal(envId: string, apiId: string, page: DocumentationPage): Promise<DocumentationPage> {
    try {
        return await mirrorPageToPortal(envId, apiId, page);
    } catch {
        return page;
    }
}

async function mirrorPageToPortal(
    envId: string,
    apiId: string,
    page: DocumentationPage,
    seen: Set<string> = new Set(),
    knownPlacement?: ApiPortalPlacement | null,
): Promise<DocumentationPage> {
    let placement: ApiPortalPlacement | null =
        knownPlacement ?? (await listPortalDocumentationFolders(envId, apiId)).placement;
    if (!placement) {
        placement = await ensureApiPlacement(envId, apiId);
    }
    if (!page.id) return page;
    if (seen.has(page.id)) return page;
    seen.add(page.id);

    let classicPages: DocumentationPage[] = [];
    try {
        classicPages = (await fetchClassicApiPages(envId, apiId)).pages;
    } catch {
        classicPages = [];
    }
    const pages = [...classicPages.filter(item => item.id !== page.id), page];
    const parentDocId = normalizeParentId(page.parentId);
    if (parentDocId) {
        const parent = pages.find(item => item.id === parentDocId && item.id !== item.portalNavId);
        if (parent?.id && parent.type === 'FOLDER' && !parent.portalNavId) {
            await mirrorPageToPortal(envId, apiId, parent, seen, placement);
        }
    }

    const navItems = await listPortalNavigationItems(envId);
    const parentNavId = resolveParentNavId(page, pages, navItems, placement.itemId);
    const type = page.type === 'FOLDER' ? 'FOLDER' : 'PAGE';
    const existing = findNavMirror(page, pages, navItems, placement.itemId, parentNavId);
    const published = page.published ?? existing?.published ?? false;
    const apiNav = navItems.find(item => item.id === placement!.itemId);
    const visibility =
        page.visibility ??
        existing?.visibility ??
        (apiNav?.visibility === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC');
    const payload = navigationUpdatePayload(
        {
            id: existing?.id ?? page.id,
            type,
            title: page.name ?? existing?.title,
            parentId: parentNavId,
            order: page.order ?? existing?.order ?? 0,
            published,
            visibility,
        },
        {
            title: page.name ?? (type === 'FOLDER' ? 'Folder' : 'Page'),
            parentId: parentNavId,
            order: page.order ?? existing?.order ?? 0,
            published,
            visibility,
        },
    );

    if (existing) {
        await updatePortalNavigationItem(envId, existing.id, payload);
        const contentType = toPortalContentType(page.type);
        if (type === 'PAGE' && existing.portalPageContentId && page.content != null) {
            await updatePortalPageContent(envId, existing.portalPageContentId, page.content, contentType);
        }
        return { ...page, portalNavId: existing.id, portalPageContentId: existing.portalPageContentId };
    }

    const contentType = toPortalContentType(page.type);
    if (type === 'PAGE' && !contentType) return page;
    const created = await createPortalNavigationItem(envId, {
        type,
        title: page.name ?? (type === 'FOLDER' ? 'Folder' : 'Page'),
        parentId: parentNavId,
        area: PORTAL_AREA,
        order: page.order ?? 0,
        visibility,
        published: false,
        ...(contentType ? { contentType } : {}),
    });
    if (type === 'PAGE' && created.portalPageContentId && page.content) {
        await updatePortalPageContent(envId, created.portalPageContentId, page.content, contentType);
    }
    // New Navigation mirrors are always created unpublished; publish happens via the Publish action.
    if (published) {
        await updatePortalNavigationItem(
            envId,
            created.id,
            navigationUpdatePayload({ ...created, parentId: parentNavId, type }, { published: true, visibility }),
        );
    }
    return { ...page, portalNavId: created.id, portalPageContentId: created.portalPageContentId, published };
}

function depthOf(pages: DocumentationPage[], page: DocumentationPage): number {
    const byId = new Map(pages.filter(item => item.id).map(item => [item.id!, item]));
    let depth = 0;
    let parent = normalizeParentId(page.parentId);
    const seen = new Set<string>();
    while (parent && byId.has(parent) && !seen.has(parent)) {
        seen.add(parent);
        depth += 1;
        parent = normalizeParentId(byId.get(parent)?.parentId);
    }
    return depth;
}

function pagesForPublish(pages: DocumentationPage[], ids: string[]): DocumentationPage[] {
    const selected = new Set(ids);
    return pages
        .filter(page => page.id && selected.has(page.id))
        .sort((left, right) => {
            const depthDiff = depthOf(pages, left) - depthOf(pages, right);
            if (depthDiff !== 0) return depthDiff;
            if (left.type === 'FOLDER' && right.type !== 'FOLDER') return -1;
            if (left.type !== 'FOLDER' && right.type === 'FOLDER') return 1;
            return (left.order ?? 0) - (right.order ?? 0);
        });
}

function findExistingNavItem(
    items: PortalNavigationItem[],
    parentId: string,
    title: string | undefined,
    type: string,
): PortalNavigationItem | undefined {
    return items.find(item => item.parentId === parentId && item.type === type && item.title === title);
}

async function resolvePageContent(envId: string, apiId: string, page: DocumentationPage): Promise<string> {
    if (page.content) return page.content;
    if (!page.id) return '';
    try {
        if (page.portalPageContentId && page.id === page.portalNavId) {
            const portalContent = await getPortalPageContent(envId, page.portalPageContentId);
            return portalContent?.content ?? '';
        }
        if (page.source) {
            return (await fetchDocumentationPage(envId, apiId, page.id)).content ?? '';
        }
        return (await fetchClassicApiPage(envId, apiId, page.id)).content ?? '';
    } catch {
        return '';
    }
}

export async function syncPublishedPagesToPortalNavigation(
    envId: string,
    apiId: string,
    apiTitle: string,
    folder: PortalFolderOption,
    placement: ApiPortalPlacement | null,
    pages: DocumentationPage[],
    pageIds: string[],
): Promise<void> {
    // Always publish under the folder the user selected. If the API already lives elsewhere
    // (e.g. the configured default folder from create), move it by updating parentId.
    const parentFolderId = folder.id;

    // Resolve an existing API Navigation node by placement or apiId before creating one.
    // Creating a second API item for the same apiId fails with "already used by another API navigation item".
    let navItems = await listPortalNavigationItems(envId);
    let apiNavId =
        (placement?.itemId &&
            navItems.find(item => item.id === placement.itemId && item.type === 'API' && item.apiId === apiId)?.id) ||
        navItems.find(item => item.type === 'API' && item.apiId === apiId)?.id;

    if (!apiNavId) {
        const created = await placeApiInPortalFolder(envId, apiId, apiTitle, folder);
        apiNavId = created.id;
        if (!apiNavId) {
            throw new Error('Failed to place the API under the selected Navigation folder.');
        }
        navItems = await listPortalNavigationItems(envId);
    }

    const knownItems = [...navItems];

    // Backend requires every ancestor to be PUBLISHED (and PUBLIC for PUBLIC children)
    // before a published child can be created/updated. Publish the destination folder
    // chain first, then the API node, then documentation under the API.
    await ensureAncestorsAllowPublishedPublicChild(envId, knownItems, parentFolderId || null);

    const apiNav = knownItems.find(item => item.id === apiNavId) ?? {
        id: apiNavId,
        type: 'API',
        apiId,
        title: apiTitle,
        parentId: parentFolderId || null,
        published: false,
        visibility: 'PUBLIC' as const,
        order: 0,
    };
    await updatePortalNavigationItem(
        envId,
        apiNavId,
        navigationUpdatePayload(apiNav, {
            title: apiTitle,
            apiId,
            parentId: parentFolderId || null,
            published: true,
            visibility: 'PUBLIC',
        }),
    );
    apiNav.published = true;
    apiNav.visibility = 'PUBLIC';
    apiNav.parentId = parentFolderId || null;
    if (!knownItems.some(item => item.id === apiNavId)) knownItems.push(apiNav);

    const docIdToNavId = new Map<string, string>();
    const ordered = pagesForPublish(pages, pageIds);

    for (const page of ordered) {
        if (!page.id) continue;
        const parentDocId = normalizeParentId(page.parentId);
        const parentNavId = (parentDocId && docIdToNavId.get(parentDocId)) || apiNavId;
        if (!parentNavId) continue;

        if (page.type === 'FOLDER') {
            const existing =
                (page.portalNavId && knownItems.find(item => item.id === page.portalNavId)) ||
                findExistingNavItem(knownItems, parentNavId, page.name, 'FOLDER');
            const nav =
                existing ??
                (await createPortalNavigationItem(envId, {
                    type: 'FOLDER',
                    title: page.name ?? 'Folder',
                    parentId: parentNavId,
                    area: PORTAL_AREA,
                    visibility: 'PUBLIC',
                    order: page.order ?? 0,
                }));
            if (!existing) knownItems.push({ ...nav, parentId: parentNavId });
            await updatePortalNavigationItem(
                envId,
                nav.id,
                navigationUpdatePayload(
                    { ...nav, parentId: parentNavId, type: 'FOLDER' },
                    {
                        title: page.name ?? nav.title ?? 'Folder',
                        order: page.order ?? nav.order ?? 0,
                        published: true,
                        visibility: 'PUBLIC',
                        parentId: parentNavId,
                    },
                ),
            );
            docIdToNavId.set(page.id, nav.id);
            continue;
        }

        const contentType = toPortalContentType(page.type);
        if (!contentType) continue;

        const existing =
            (page.portalNavId && knownItems.find(item => item.id === page.portalNavId)) ||
            findExistingNavItem(knownItems, parentNavId, page.name, 'PAGE');
        const nav =
            existing ??
            (await createPortalNavigationItem(envId, {
                type: 'PAGE',
                title: page.name ?? 'Page',
                parentId: parentNavId,
                area: PORTAL_AREA,
                visibility: 'PUBLIC',
                contentType,
                order: page.order ?? 0,
            }));
        if (!existing) knownItems.push({ ...nav, parentId: parentNavId });
        const contentId = nav.portalPageContentId;
        const content = await resolvePageContent(envId, apiId, page);
        if (contentId && content) {
            await updatePortalPageContent(envId, contentId, content, contentType);
        }
        await updatePortalNavigationItem(
            envId,
            nav.id,
            navigationUpdatePayload(
                { ...nav, parentId: parentNavId, type: 'PAGE' },
                {
                    title: page.name ?? nav.title ?? 'Page',
                    order: page.order ?? nav.order ?? 0,
                    published: true,
                    visibility: 'PUBLIC',
                    parentId: parentNavId,
                },
            ),
        );
        docIdToNavId.set(page.id, nav.id);
    }
}

export async function unpublishPagesFromPortalNavigation(
    envId: string,
    apiId: string,
    placement: ApiPortalPlacement | null,
    pages: DocumentationPage[],
    pageIds: string[],
): Promise<void> {
    if (!placement) return;
    const navItems = await listPortalNavigationItems(envId);
    const apiNav = navItems.find(item => item.id === placement.itemId && item.type === 'API' && item.apiId === apiId);
    if (!apiNav) return;

    const ordered = pagesForPublish(pages, pageIds);
    const docIdToNavId = new Map<string, string>();

    for (const page of ordered) {
        if (!page.id) continue;
        const parentDocId = normalizeParentId(page.parentId);
        const parentNavId = (parentDocId && docIdToNavId.get(parentDocId)) || apiNav.id;
        const type = page.type === 'FOLDER' ? 'FOLDER' : 'PAGE';
        const existing =
            (page.portalNavId && navItems.find(item => item.id === page.portalNavId)) ||
            findExistingNavItem(navItems, parentNavId, page.name, type);
        if (!existing) continue;
        docIdToNavId.set(page.id, existing.id);
        await updatePortalNavigationItem(
            envId,
            existing.id,
            navigationUpdatePayload(existing, {
                title: existing.title ?? page.name,
                published: false,
                parentId: existing.parentId ?? null,
            }),
        );
    }

    // When nothing under the API remains published in Navigation, unpublish the API node too.
    const unpublishedNavIds = new Set(docIdToNavId.values());
    const remainingPublishedDocs = apiDocumentationNavItems(navItems, apiNav.id).filter(
        item => item.published === true && item.id && !unpublishedNavIds.has(item.id),
    );
    if (remainingPublishedDocs.length === 0 && apiNav.published !== false) {
        await updatePortalNavigationItem(
            envId,
            apiNav.id,
            navigationUpdatePayload(apiNav, {
                title: apiNav.title ?? 'API',
                apiId,
                published: false,
                parentId: apiNav.parentId ?? null,
            }),
        );
    }
}

/** Default Overview page created when publishing an API that has no documentation yet. */
export function defaultOverviewPagePayload(apiName: string): CreateDocumentationPayload {
    const title = apiName.trim() || 'API';
    return {
        name: 'Overview',
        type: 'MARKDOWN',
        visibility: 'PUBLIC',
        content: `# ${title}\n\nDocumentation for this API. Update this Overview page with getting-started details for API consumers.\n`,
    };
}

/**
 * Creates a classic API documentation page without placing or mirroring into Navigation.
 * Used when the caller will publish the API under a user-selected folder next.
 */
export async function createClassicDocumentationPage(
    envId: string,
    apiId: string,
    payload: CreateDocumentationPayload,
): Promise<DocumentationPage> {
    return apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId), {
        method: 'POST',
        body: JSON.stringify(withoutPortalFields(payload)),
    });
}

/** Publishes a classic API documentation page without mirroring into Navigation. */
export async function publishClassicDocumentationPage(
    envId: string,
    apiId: string,
    pageId: string,
): Promise<DocumentationPage> {
    return apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(pageId)}/_publish`), {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

/**
 * Empty-docs Publish API: create + classic-publish Overview, then place/publish the API parent
 * under the selected Navigation folder and publish Overview as its child.
 */
export async function publishApiWithDefaultOverview(
    envId: string,
    apiId: string,
    apiTitle: string,
    folder: PortalFolderOption,
    placement: ApiPortalPlacement | null,
): Promise<DocumentationPage> {
    const overview = await createClassicDocumentationPage(envId, apiId, defaultOverviewPagePayload(apiTitle));
    if (!overview.id) {
        throw new Error('Failed to create the default Overview page.');
    }
    const published = await publishClassicDocumentationPage(envId, apiId, overview.id);
    const page: DocumentationPage = { ...published, published: true };
    // Parent (API) is published first inside sync, then the Overview child under it.
    await syncPublishedPagesToPortalNavigation(envId, apiId, apiTitle, folder, placement, [page], [overview.id]);
    return page;
}

/**
 * Unpublishes the API navigation node and all of its documentation children in the Next Gen Portal.
 * Also unpublishes matching classic API documentation pages when they are still marked published.
 */
export async function unpublishApiFromPortalNavigation(
    envId: string,
    apiId: string,
    placement: ApiPortalPlacement | null,
    pages: DocumentationPage[],
): Promise<void> {
    if (!placement) return;

    const classicIds = pages
        .filter(page => page.published && page.id && page.id !== page.portalNavId)
        .map(page => page.id!)
        .filter(Boolean);
    for (const pageId of classicIds) {
        try {
            await unpublishDocumentationPage(envId, apiId, pageId);
        } catch {
            // Continue unpublishing portal navigation even if a classic page fails.
        }
    }

    const navItems = await listPortalNavigationItems(envId);
    const apiNav = navItems.find(item => item.id === placement.itemId && item.type === 'API' && item.apiId === apiId);
    if (!apiNav || !apiNav.id) return;

    await updatePortalNavigationItem(
        envId,
        apiNav.id,
        navigationUpdatePayload(apiNav, {
            title: apiNav.title ?? 'API',
            apiId,
            published: false,
            parentId: apiNav.parentId ?? null,
        }),
        true,
    );
}

function orderPagesForCascadeDelete(pages: DocumentationPage[]): DocumentationPage[] {
    return [...pages]
        .filter(page => !!page.id)
        .sort((left, right) => {
            const depthDiff = depthOf(pages, right) - depthOf(pages, left);
            if (depthDiff !== 0) return depthDiff;
            if (left.type === 'FOLDER' && right.type !== 'FOLDER') return 1;
            if (left.type !== 'FOLDER' && right.type === 'FOLDER') return -1;
            return 0;
        });
}

function orderNavItemsForCreate(items: PortalNavigationItem[], apiNavId: string): PortalNavigationItem[] {
    const byNavId = new Map(items.map(item => [item.id, item]));
    const depthOfNav = (item: PortalNavigationItem): number => {
        let depth = 0;
        let parentId = item.parentId;
        const seen = new Set<string>();
        while (parentId && parentId !== apiNavId && byNavId.has(parentId) && !seen.has(parentId)) {
            seen.add(parentId);
            depth += 1;
            parentId = byNavId.get(parentId)?.parentId;
        }
        return depth;
    };
    return [...items].sort((left, right) => {
        const depthDiff = depthOfNav(left) - depthOfNav(right);
        if (depthDiff !== 0) return depthDiff;
        if (left.type === 'FOLDER' && right.type !== 'FOLDER') return -1;
        if (left.type !== 'FOLDER' && right.type === 'FOLDER') return 1;
        return (left.order ?? 0) - (right.order ?? 0);
    });
}

/**
 * Overwrites API proxy documentation with the PAGE/FOLDER tree under this API in Portal Navigation.
 * Does not modify Navigation items.
 */
export async function syncPortalDocumentationToApiProxy(envId: string, apiId: string): Promise<DocumentationPagesResult> {
    const { placement } = await listPortalDocumentationFolders(envId, apiId);
    if (!placement) {
        throw new Error('This API is not present in Portal Navigation. Add it to Navigation before syncing.');
    }

    const navItems = await listPortalNavigationItems(envId);
    const portalDocs = apiDocumentationNavItems(navItems, placement.itemId);

    const classic = await fetchClassicApiPages(envId, apiId);
    for (const page of orderPagesForCascadeDelete(classic.pages)) {
        if (!page.id) continue;
        try {
            await apimFetchJsonV2<void>(envId, pagesPath(apiId, `/${encodeURIComponent(page.id)}`), { method: 'DELETE' });
        } catch {
            // Continue clearing remaining pages even if one delete fails (e.g. already gone).
        }
    }

    const navIdToClassicId = new Map<string, string>();
    for (const nav of orderNavItemsForCreate(portalDocs, placement.itemId)) {
        const parentNavId = !nav.parentId || nav.parentId === placement.itemId ? null : nav.parentId;
        const parentId = parentNavId ? (navIdToClassicId.get(parentNavId) ?? 'ROOT') : 'ROOT';

        if (nav.type === 'FOLDER') {
            const created = await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId), {
                method: 'POST',
                body: JSON.stringify({
                    name: nav.title ?? 'Folder',
                    type: 'FOLDER',
                    parentId,
                    visibility: nav.visibility ?? 'PUBLIC',
                    order: nav.order,
                }),
            });
            if (created.id) navIdToClassicId.set(nav.id, created.id);
            if (nav.published && created.id) {
                await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(created.id)}/_publish`), {
                    method: 'POST',
                    body: JSON.stringify({}),
                });
            }
            continue;
        }

        const portalContent = nav.portalPageContentId ? await getPortalPageContent(envId, nav.portalPageContentId) : null;
        const type = fromPortalContentType(portalContent?.type) ?? fromPortalContentType(nav.contentType) ?? 'MARKDOWN';
        const created = await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId), {
            method: 'POST',
            body: JSON.stringify({
                name: nav.title ?? 'Page',
                type,
                parentId,
                content: portalContent?.content ?? '',
                visibility: nav.visibility ?? 'PUBLIC',
                order: nav.order,
            }),
        });
        if (created.id) navIdToClassicId.set(nav.id, created.id);
        if (nav.published && created.id) {
            await apimFetchJsonV2<DocumentationPage>(envId, pagesPath(apiId, `/${encodeURIComponent(created.id)}/_publish`), {
                method: 'POST',
                body: JSON.stringify({}),
            });
        }
    }

    return fetchClassicApiPages(envId, apiId);
}
