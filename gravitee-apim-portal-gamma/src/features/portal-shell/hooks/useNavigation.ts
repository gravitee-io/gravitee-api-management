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
    PortalNavigationApi,
    PortalNavigationArea,
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../portals/types';
import { getNavItems, saveNavItem, deleteNavItem as deleteNavItemStorage } from '../../portals/storage/navigation-items.storage';
import { deletePageContent, getPageContent, savePageContent } from '../../portals/storage/page-contents.storage';
import { createPlaceholderDocument } from '../../portals/storage/dummy-navigation';
import {
    ensureUniqueSlug,
    findFirstPageNavItem,
    findNavItemBySlug,
    generateSlug,
} from '../../portals/utils/slug';

export interface UpdateNavItemPatch {
    readonly title?: string;
    readonly url?: string;
}
import { collectIdsToDelete, isFooterNavItem, isHeaderRootNavItem } from '../utils/nav-items';

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
}

export interface UseNavigationResult {
    readonly navItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly loading: boolean;
    readonly pageNotFound: boolean;
    selectNavItem: (id: string) => void;
    addNavItem: (type: PortalNavigationItemType, parentId: string | null, area?: PortalNavigationArea) => Promise<PortalNavigationItem>;
    addApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<PortalNavigationItem>;
    addFooterLink: () => Promise<PortalNavigationItem>;
    deleteNavItem: (id: string) => Promise<void>;
    updateNavItem: (id: string, patch: UpdateNavItemPatch) => Promise<void>;
    getRootItems: () => PortalNavigationItem[];
    getFooterItems: () => PortalNavigationLink[];
    getChildren: (parentId: string) => PortalNavigationItem[];
    refresh: () => Promise<void>;
}

export function useNavigation(
    portalId: string | undefined,
    options: UseNavigationOptions = {},
): UseNavigationResult {
    const { slug, getPagePath, onNavigate } = options;
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
        const items = await getNavItems(portalId);
        setNavItems(items);
        setLoading(false);
    }, [portalId]);

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
                setSelectedNavItemId(item.id);
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
            setSelectedNavItemId(id);
            const item = navItems.find(navItem => navItem.id === id);
            if (item) {
                navigateToPage(item);
            }
        },
        [navItems, navigateToPage],
    );

    const addNavItem = useCallback(async (
        type: PortalNavigationItemType,
        parentId: string | null,
        area: PortalNavigationArea = 'HEADER',
    ): Promise<PortalNavigationItem> => {
        if (!portalId) {
            throw new Error('No portal ID');
        }

        const siblings = parentId === null && area === 'FOOTER'
            ? navItems.filter(isFooterNavItem)
            : navItems.filter(item => item.parentId === parentId);
        const order = siblings.length;
        const title = type === 'FOLDER' ? 'New Folder' : type === 'LINK' ? 'New Link' : type === 'API' ? 'API' : 'New Page';
        const id = createUniqueId();
        const itemSlug = createItemSlug(title, id, navItems);
        const itemArea = parentId === null ? area : undefined;

        let item: PortalNavigationItem;
        if (type === 'LINK') {
            item = { id, portalId, title, type: 'LINK', parentId, order, slug: itemSlug, url: '#', area: itemArea };
        } else if (type === 'API') {
            item = { id, portalId, title, type: 'API', parentId, order, slug: itemSlug, apiId: '', area: itemArea };
        } else {
            item = { id, portalId, title, type, parentId, order, slug: itemSlug, area: itemArea } as PortalNavigationItem;
        }

        await saveNavItem(item);

        if (type === 'PAGE') {
            const pageContentId = createUniqueId();
            await savePageContent({
                id: pageContentId,
                portalId,
                navigationItemId: id,
                document: createPlaceholderDocument(title),
            });
        }

        await loadNavItems();
        if (type === 'PAGE') {
            setSelectedNavItemId(id);
            navigateToPage(item);
        }
        return item;
    }, [portalId, navItems, loadNavItems, navigateToPage]);

    const addApiNavItem = useCallback(async (
        apiId: string,
        apiName: string,
        parentId: string | null,
    ): Promise<PortalNavigationItem> => {
        if (!portalId) {
            throw new Error('No portal ID');
        }

        const siblings = navItems.filter(item => item.parentId === parentId);
        const order = siblings.length;
        const apiItemId = createUniqueId();
        const apiTitle = apiName;

        const apiItem: PortalNavigationApi = {
            id: apiItemId,
            portalId,
            title: apiTitle,
            type: 'API',
            parentId,
            order,
            slug: createItemSlug(apiTitle, apiItemId, navItems),
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
            slug: createItemSlug(pageTitle, pageId, [...navItems, apiItem]),
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
    }, [portalId, navItems, loadNavItems, navigateToPage]);

    const addFooterLink = useCallback(async (): Promise<PortalNavigationItem> => {
        return addNavItem('LINK', null, 'FOOTER');
    }, [addNavItem]);

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
        return navItems.filter(isHeaderRootNavItem);
    }, [navItems]);

    const getFooterItems = useCallback((): PortalNavigationLink[] => {
        return navItems.filter((item): item is PortalNavigationLink => isFooterNavItem(item) && item.type === 'LINK');
    }, [navItems]);

    const getChildren = useCallback((parentId: string) => {
        return navItems.filter(item => item.parentId === parentId);
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
        addFooterLink,
        deleteNavItem,
        updateNavItem,
        getRootItems,
        getFooterItems,
        getChildren,
        refresh,
    };
}
