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

import type { PortalNavigationArea, PortalNavigationItem, PortalNavigationItemType, PortalNavigationLink } from '../../portals/types';
import { getNavItems, saveNavItem, deleteNavItem as deleteNavItemStorage } from '../../portals/storage/navigation-items.storage';
import { deletePageContent, getPageContent, savePageContent } from '../../portals/storage/page-contents.storage';
import { createPlaceholderDocument } from '../../portals/storage/dummy-navigation';
import { collectIdsToDelete, isFooterNavItem, isHeaderRootNavItem } from '../utils/nav-items';

function createUniqueId(): string {
    return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

function generateSlug(title: string): string {
    const slug = title
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
    const shortId = createUniqueId().slice(0, 6);
    return `${slug || 'untitled'}-${shortId}`;
}

export interface UseNavigationResult {
    readonly navItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly loading: boolean;
    selectNavItem: (id: string) => void;
    addNavItem: (type: PortalNavigationItemType, parentId: string | null, area?: PortalNavigationArea) => Promise<PortalNavigationItem>;
    addFooterLink: () => Promise<PortalNavigationItem>;
    deleteNavItem: (id: string) => Promise<void>;
    getRootItems: () => PortalNavigationItem[];
    getFooterItems: () => PortalNavigationLink[];
    getChildren: (parentId: string) => PortalNavigationItem[];
    refresh: () => Promise<void>;
}

export function useNavigation(portalId: string | undefined): UseNavigationResult {
    const [navItems, setNavItems] = useState<PortalNavigationItem[]>([]);
    const [selectedNavItemId, setSelectedNavItemId] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

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

    useEffect(() => {
        if (navItems.length > 0 && !selectedNavItemId) {
            const firstPage = navItems.find(item => item.type === 'PAGE' && item.parentId === null)
                ?? navItems.find(item => item.type === 'PAGE');
            if (firstPage) {
                setSelectedNavItemId(firstPage.id);
            }
        }
    }, [navItems, selectedNavItemId]);

    const selectNavItem = useCallback((id: string) => {
        setSelectedNavItemId(id);
    }, []);

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
        const itemArea = parentId === null ? area : undefined;

        let item: PortalNavigationItem;
        if (type === 'LINK') {
            item = { id, portalId, title, type: 'LINK', parentId, order, slug: generateSlug(title), url: '#', area: itemArea };
        } else if (type === 'API') {
            item = { id, portalId, title, type: 'API', parentId, order, slug: generateSlug(title), apiId: '', area: itemArea };
        } else {
            item = { id, portalId, title, type, parentId, order, slug: generateSlug(title), area: itemArea } as PortalNavigationItem;
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
        }
        return item;
    }, [portalId, navItems, loadNavItems]);

    const addFooterLink = useCallback(async (): Promise<PortalNavigationItem> => {
        return addNavItem('LINK', null, 'FOOTER');
    }, [addNavItem]);

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
            setSelectedNavItemId(null);
        }
        await loadNavItems();
    }, [portalId, selectedNavItemId, loadNavItems]);

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
        selectNavItem,
        addNavItem,
        addFooterLink,
        deleteNavItem,
        getRootItems,
        getFooterItems,
        getChildren,
        refresh,
    };
}
