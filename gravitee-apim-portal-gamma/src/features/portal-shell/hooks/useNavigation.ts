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
import { useCallback, useEffect, useState } from 'react';

import type {
    DeveloperPortal,
    OpenApiRenderer,
    OpenApiSpecSource,
    PortalNavigationApi,
    PortalNavigationArea,
    PortalNavigationAsyncApiPage,
    PortalNavigationHtmlPage,
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationLink,
    PortalNavigationOpenApiPage,
    PortalNavigationPage,
} from '../../portals/types';
import { getNavItems, saveNavItem, deleteNavItem as deleteNavItemStorage } from '../../portals/storage/navigation-items.storage';
import { deletePageContent, getPageContent, savePageContent } from '../../portals/storage/page-contents.storage';
import { createPlaceholderDocument } from '../../portals/storage/dummy-navigation';
import { DEFAULT_OPENAPI_PAGE_SPEC } from '../../editor/services/openapi.service';
import {
    ensureUniqueSlug,
    findFirstPageNavItem,
    findNavItemBySlug,
    generateSlug,
} from '../../portals/utils/slug';

export interface UpdateNavItemPatch {
    readonly title?: string;
    readonly url?: string;
    readonly renderer?: OpenApiRenderer;
    readonly specSource?: OpenApiSpecSource;
}
import { canAddApiNavItem } from '../utils/can-add-api-nav-item';
import { isExternalUrl } from '../utils/link-target';
import { normalizeOpenApiRenderer, type AddPageOptions } from '../utils/page-type-options';
import { findApiAncestor } from '../utils/find-api-ancestor';
import { migrateUserMenuItems } from '../utils/migrate-user-menu-items';
import { getPortalPages } from '../utils/portal-pages';
import { parsePortalPageSlug, resolveUserMenuItemPath } from '../utils/user-menu-url';
import {
    belongsToUserMenu,
    collectIdsToDelete,
    getNextSiblingOrder,
    isFooterNavItem,
    isHeaderRootNavItem,
    isUserMenuRootItem,
    sortNavItemsByOrder,
} from '../utils/nav-items';

function getSiblingsForAdd(
    navItems: readonly PortalNavigationItem[],
    parentId: string | null,
    area: PortalNavigationArea,
): PortalNavigationItem[] {
    if (parentId === null && area === 'FOOTER') {
        return navItems.filter(isFooterNavItem);
    }

    if (parentId === null && area === 'USER_MENU') {
        return navItems.filter(isUserMenuRootItem);
    }

    return navItems.filter(item => item.parentId === parentId);
}

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function createItemSlug(title: string, id: string, existingItems: readonly PortalNavigationItem[]): string {
    const existingSlugs = new Set(existingItems.map(item => item.slug));
    return ensureUniqueSlug(generateSlug(title, id), existingSlugs);
}

export interface UseNavigationOptions {
    readonly slug?: string;
    readonly getPagePath?: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly portal?: DeveloperPortal;
    readonly onPortalChange?: (portal: DeveloperPortal) => void;
}

export interface UseNavigationResult {
    readonly navItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly loading: boolean;
    readonly pageNotFound: boolean;
    selectNavItem: (id: string) => void;
    addNavItem: (
        type: PortalNavigationItemType,
        parentId: string | null,
        area?: PortalNavigationArea,
        pageOptions?: AddPageOptions,
    ) => Promise<PortalNavigationItem>;
    addApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<PortalNavigationItem>;
    addLinkFromPage: (
        page: PortalNavigationPage,
        parentId: string | null,
        area: PortalNavigationArea,
    ) => Promise<PortalNavigationItem>;
    addUserMenuNavItem: (
        type: PortalNavigationItemType,
        parentId: string | null,
        pageOptions?: AddPageOptions,
    ) => Promise<PortalNavigationItem>;
    addUserMenuLinkFromPage: (page: PortalNavigationPage, parentId: string | null) => Promise<PortalNavigationItem>;
    deleteNavItem: (id: string) => Promise<void>;
    updateNavItem: (id: string, patch: UpdateNavItemPatch) => Promise<void>;
    getRootItems: () => PortalNavigationItem[];
    getFooterItems: () => PortalNavigationLink[];
    getUserMenuRootItems: () => PortalNavigationItem[];
    hasUserMenuItems: () => boolean;
    getChildren: (parentId: string) => PortalNavigationItem[];
    refresh: () => Promise<void>;
}

export function useNavigation(
    portalId: string | undefined,
    options: UseNavigationOptions = {},
): UseNavigationResult {
    const { slug, getPagePath, onNavigate, portal, onPortalChange } = options;
    const [navItems, setNavItems] = useState<PortalNavigationItem[]>([]);
    const [selectedNavItemId, setSelectedNavItemId] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [pageNotFound, setPageNotFound] = useState(false);

    const loadNavItems = useCallback(async () => {
        if (!portalId) {
            setNavItems([]);
            setLoading(false);
            return;
        }

        let items = await getNavItems(portalId);

        if (portal) {
            const migrationResult = await migrateUserMenuItems(portal, items);
            if (migrationResult) {
                items = [...items, ...migrationResult.migratedNavItems];
                onPortalChange?.(migrationResult.clearedPortal);
            }
        }

        setNavItems(items);
        setLoading(false);
    }, [portalId, portal, onPortalChange]);

    useEffect(() => {
        void loadNavItems();
    }, [loadNavItems]);

    const navigateToPage = useCallback(
        (item: PortalNavigationItem, replace = false) => {
            if (item.type !== 'PAGE' || !getPagePath || !onNavigate) {
                return;
            }
            onNavigate(getPagePath(item.slug), { replace });
        },
        [getPagePath, onNavigate],
    );

    useEffect(() => {
        if (loading || navItems.length === 0) {
            return;
        }

        const firstPage = findFirstPageNavItem(navItems);

        if (slug) {
            const item = findNavItemBySlug(navItems, slug);
            if (item?.type === 'PAGE') {
                setPageNotFound(false);
                setSelectedNavItemId(current => {
                    if (current) {
                        const currentItem = navItems.find(navItem => navItem.id === current);
                        if (
                            currentItem
                            && belongsToUserMenu(currentItem, navItems)
                            && currentItem.id !== item.id
                        ) {
                            return current;
                        }
                    }
                    return item.id;
                });
                return;
            }

            setPageNotFound(true);
            setSelectedNavItemId(null);
            return;
        }

        setPageNotFound(false);
        setSelectedNavItemId(current => current ?? firstPage?.id ?? null);
    }, [loading, navItems, slug, getPagePath, onNavigate, navigateToPage]);

    const selectNavItem = useCallback(
        (id: string) => {
            const item = navItems.find(navItem => navItem.id === id);
            if (!item) {
                return;
            }

            if (item.type === 'LINK') {
                const portalPages = getPortalPages(navItems);
                const slug = parsePortalPageSlug(item.url, portalPages, portalId);

                if (slug) {
                    const targetPage = findNavItemBySlug(navItems, slug);
                    if (targetPage?.type === 'PAGE') {
                        setSelectedNavItemId(targetPage.id);
                        navigateToPage(targetPage);
                        return;
                    }
                }

                if (isExternalUrl(item.url)) {
                    window.open(item.url.trim(), '_blank', 'noopener,noreferrer');
                    return;
                }

                if (getPagePath && onNavigate) {
                    const path = resolveUserMenuItemPath(item.url, portalPages, getPagePath, portalId);
                    onNavigate(path);
                }
                return;
            }

            setSelectedNavItemId(id);
            navigateToPage(item);
        },
        [navItems, navigateToPage, getPagePath, onNavigate, portalId],
    );

    const addNavItem = useCallback(async (
        type: PortalNavigationItemType,
        parentId: string | null,
        area: PortalNavigationArea = 'HEADER',
        pageOptions?: AddPageOptions,
    ): Promise<PortalNavigationItem> => {
        if (!portalId) {
            throw new Error('No portal ID');
        }

        if (type === 'LINK') {
            throw new Error('Use addLinkFromPage to create link navigation items');
        }

        const currentItems = await getNavItems(portalId);
        const siblings = getSiblingsForAdd(currentItems, parentId, area);
        const order = getNextSiblingOrder(siblings);
        const title = type === 'FOLDER' ? 'New Folder' : type === 'API' ? 'API' : 'New Page';
        const id = createUniqueId();
        const itemSlug = createItemSlug(title, id, currentItems);
        const itemArea = parentId === null ? area : undefined;

        const pageContentType = pageOptions?.contentType ?? 'BLOCK';
        const apiAncestor = findApiAncestor(currentItems, parentId);

        let item: PortalNavigationItem;
        if (type === 'API') {
            item = { id, portalId, title, type: 'API', parentId, order, slug: itemSlug, apiId: '', area: itemArea };
        } else if (type === 'PAGE' && pageContentType === 'OPENAPI') {
            const renderer: OpenApiRenderer = normalizeOpenApiRenderer(pageOptions?.renderer);
            const specSource: OpenApiSpecSource = apiAncestor
                ? { type: 'API', apiId: apiAncestor.apiId }
                : { type: 'INLINE', content: DEFAULT_OPENAPI_PAGE_SPEC };
            const openApiItem: PortalNavigationOpenApiPage = {
                id,
                portalId,
                title,
                type: 'PAGE',
                contentType: 'OPENAPI',
                renderer,
                specSource,
                parentId,
                order,
                slug: itemSlug,
                area: itemArea,
            };
            item = openApiItem;
        } else if (type === 'PAGE' && pageContentType === 'HTML') {
            const htmlItem: PortalNavigationHtmlPage = {
                id,
                portalId,
                title,
                type: 'PAGE',
                contentType: 'HTML',
                parentId,
                order,
                slug: itemSlug,
                area: itemArea,
            };
            item = htmlItem;
        } else if (type === 'PAGE' && pageContentType === 'ASYNCAPI') {
            const specSource = apiAncestor
                ? { type: 'API' as const, apiId: apiAncestor.apiId }
                : { type: 'INLINE' as const, content: '' };
            const asyncApiItem: PortalNavigationAsyncApiPage = {
                id,
                portalId,
                title,
                type: 'PAGE',
                contentType: 'ASYNCAPI',
                specSource,
                parentId,
                order,
                slug: itemSlug,
                area: itemArea,
            };
            item = asyncApiItem;
        } else {
            item = { id, portalId, title, type, parentId, order, slug: itemSlug, area: itemArea } as PortalNavigationItem;
        }

        await saveNavItem(item);

        if (type === 'PAGE') {
            const pageContentId = createUniqueId();
            if (pageContentType === 'OPENAPI') {
                const openApiPage = item as PortalNavigationOpenApiPage;
                await savePageContent({
                    id: pageContentId,
                    portalId,
                    navigationItemId: id,
                    contentType: 'OPENAPI',
                    renderer: openApiPage.renderer,
                    specContent: DEFAULT_OPENAPI_PAGE_SPEC,
                });
            } else if (pageContentType === 'HTML') {
                await savePageContent({
                    id: pageContentId,
                    portalId,
                    navigationItemId: id,
                    contentType: 'HTML',
                    html: '<p>New HTML page</p>',
                });
            } else if (pageContentType === 'ASYNCAPI') {
                await savePageContent({
                    id: pageContentId,
                    portalId,
                    navigationItemId: id,
                    contentType: 'ASYNCAPI',
                    specContent: '',
                });
            } else {
                await savePageContent({
                    id: pageContentId,
                    portalId,
                    navigationItemId: id,
                    contentType: 'BLOCK',
                    document: createPlaceholderDocument(title),
                });
            }
        }

        await loadNavItems();
        if (type === 'PAGE') {
            setSelectedNavItemId(id);
            navigateToPage(item);
        }
        return item;
    }, [portalId, loadNavItems, navigateToPage]);

    const addApiNavItem = useCallback(async (
        apiId: string,
        apiName: string,
        parentId: string | null,
    ): Promise<PortalNavigationItem> => {
        if (!portalId) {
            throw new Error('No portal ID');
        }

        const currentItems = await getNavItems(portalId);
        if (!canAddApiNavItem(currentItems, parentId)) {
            throw new Error('Parent hierarchy cannot include API items.');
        }

        const siblings = currentItems.filter(item => item.parentId === parentId);
        const order = getNextSiblingOrder(siblings);
        const apiItemId = createUniqueId();
        const apiTitle = apiName;

        const apiItem: PortalNavigationApi = {
            id: apiItemId,
            portalId,
            title: apiTitle,
            type: 'API',
            parentId,
            order,
            slug: createItemSlug(apiTitle, apiItemId, currentItems),
            apiId,
        };
        await saveNavItem(apiItem);

        const pageId = createUniqueId();
        const pageTitle = 'Overview';
        const pageItem: PortalNavigationPage = {
            id: pageId,
            portalId,
            title: pageTitle,
            type: 'PAGE',
            parentId: apiItemId,
            order: 0,
            slug: createItemSlug(pageTitle, pageId, [...currentItems, apiItem]),
        };
        await saveNavItem(pageItem);

        const pageContentId = createUniqueId();
        await savePageContent({
            id: pageContentId,
            portalId,
            navigationItemId: pageId,
            document: createPlaceholderDocument(pageTitle),
        });

        await loadNavItems();
        setSelectedNavItemId(pageId);
        navigateToPage(pageItem);
        return apiItem;
    }, [portalId, loadNavItems, navigateToPage]);

    const addLinkFromPage = useCallback(async (
        page: PortalNavigationPage,
        parentId: string | null,
        area: PortalNavigationArea,
    ): Promise<PortalNavigationItem> => {
        if (!portalId) {
            throw new Error('No portal ID');
        }

        const currentItems = await getNavItems(portalId);
        const siblings = getSiblingsForAdd(currentItems, parentId, area);
        const order = getNextSiblingOrder(siblings);
        const title = page.title;
        const id = createUniqueId();
        const itemSlug = createItemSlug(title, id, currentItems);
        const itemArea = parentId === null ? area : undefined;

        const item: PortalNavigationLink = {
            id,
            portalId,
            title,
            type: 'LINK',
            parentId,
            order,
            slug: itemSlug,
            url: page.slug,
            area: itemArea,
        };

        await saveNavItem(item);
        await loadNavItems();
        return item;
    }, [portalId, loadNavItems]);

    const addUserMenuNavItem = useCallback(async (
        type: PortalNavigationItemType,
        parentId: string | null,
        pageOptions?: AddPageOptions,
    ): Promise<PortalNavigationItem> => {
        return parentId === null
            ? addNavItem(type, parentId, 'USER_MENU', pageOptions)
            : addNavItem(type, parentId, 'HEADER', pageOptions);
    }, [addNavItem]);

    const addUserMenuLinkFromPage = useCallback(async (
        page: PortalNavigationPage,
        parentId: string | null,
    ): Promise<PortalNavigationItem> => {
        const area: PortalNavigationArea = parentId === null ? 'USER_MENU' : 'HEADER';
        return addLinkFromPage(page, parentId, area);
    }, [addLinkFromPage]);

    const updateNavItem = useCallback(async (id: string, patch: UpdateNavItemPatch) => {
        if (!portalId) {
            return;
        }

        const item = navItems.find(navItem => navItem.id === id);
        if (!item) {
            return;
        }

        const nextTitle = patch.title?.trim();
        let updatedItem: PortalNavigationItem = item;

        if (nextTitle && nextTitle !== item.title) {
            const existingSlugs = new Set(navItems.filter(navItem => navItem.id !== id).map(navItem => navItem.slug));
            updatedItem = {
                ...updatedItem,
                title: nextTitle,
                slug: ensureUniqueSlug(generateSlug(nextTitle, id), existingSlugs),
            };
        }

        if (patch.url !== undefined && item.type === 'LINK') {
            const linkItem = updatedItem as PortalNavigationLink;
            updatedItem = {
                ...linkItem,
                url: patch.url.trim() || '#',
            };
        }

        if (item.type === 'PAGE' && item.contentType === 'OPENAPI' && (patch.renderer !== undefined || patch.specSource !== undefined)) {
            const openApiPage = updatedItem as PortalNavigationOpenApiPage;
            updatedItem = {
                ...openApiPage,
                renderer: patch.renderer !== undefined
                    ? normalizeOpenApiRenderer(patch.renderer)
                    : normalizeOpenApiRenderer(openApiPage.renderer),
                specSource: patch.specSource ?? openApiPage.specSource,
            } as PortalNavigationOpenApiPage;
        }

        await saveNavItem(updatedItem);

        if (updatedItem.type === 'PAGE' && updatedItem.slug !== item.slug && selectedNavItemId === id) {
            navigateToPage(updatedItem, true);
        }

        await loadNavItems();
    }, [portalId, navItems, selectedNavItemId, loadNavItems, navigateToPage]);

    const deleteNavItem = useCallback(async (id: string) => {
        if (!portalId) {
            return;
        }

        const items = await getNavItems(portalId);
        const idsToDelete = collectIdsToDelete(items, id);
        const itemById = new Map(items.map(item => [item.id, item]));

        await Promise.all(
            idsToDelete.map(async itemId => {
                const item = itemById.get(itemId);
                if (item?.type === 'PAGE') {
                    const content = await getPageContent(itemId);
                    if (content) {
                        await deletePageContent(content.id);
                    }
                }
                await deleteNavItemStorage(itemId);
            }),
        );

        if (selectedNavItemId && idsToDelete.includes(selectedNavItemId)) {
            const remainingItems = items.filter(item => !idsToDelete.includes(item.id));
            const nextPage = findFirstPageNavItem(remainingItems);
            setSelectedNavItemId(nextPage?.id ?? null);
            if (nextPage) {
                navigateToPage(nextPage, true);
            }
        }
        await loadNavItems();
    }, [portalId, selectedNavItemId, loadNavItems, navigateToPage]);

    const getRootItems = useCallback(() => {
        return sortNavItemsByOrder(navItems.filter(isHeaderRootNavItem));
    }, [navItems]);

    const getFooterItems = useCallback((): PortalNavigationLink[] => {
        const footerLinks = navItems.filter(
            (item): item is PortalNavigationLink => isFooterNavItem(item) && item.type === 'LINK',
        );
        return sortNavItemsByOrder(footerLinks);
    }, [navItems]);

    const getUserMenuRootItems = useCallback(() => {
        return sortNavItemsByOrder(navItems.filter(isUserMenuRootItem));
    }, [navItems]);

    const hasUserMenuItems = useCallback(() => {
        return navItems.some(item => belongsToUserMenu(item, navItems));
    }, [navItems]);

    const getChildren = useCallback((parentId: string) => {
        return sortNavItemsByOrder(navItems.filter(item => item.parentId === parentId));
    }, [navItems]);

    const refresh = useCallback(async () => {
        await loadNavItems();
    }, [loadNavItems]);

    return {
        navItems,
        selectedNavItemId,
        loading,
        pageNotFound,
        selectNavItem,
        addNavItem,
        addApiNavItem,
        addLinkFromPage,
        addUserMenuNavItem,
        addUserMenuLinkFromPage,
        deleteNavItem,
        updateNavItem,
        getRootItems,
        getFooterItems,
        getUserMenuRootItems,
        hasUserMenuItems,
        getChildren,
        refresh,
    };
}
