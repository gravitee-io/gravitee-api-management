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
    PortalNavigationAiWorkspace,
    PortalNavigationApi,
    PortalNavigationApiProduct,
    PortalNavigationFolder,
    PortalNavigationItem,
} from '../../portals/types';

export type PortalNavigationContainer =
    | PortalNavigationFolder
    | PortalNavigationApi
    | PortalNavigationApiProduct
    | PortalNavigationAiWorkspace;

export function findRootNavItem(
    navItems: readonly PortalNavigationItem[],
    itemId: string,
): PortalNavigationItem | undefined {
    let item = navItems.find(navItem => navItem.id === itemId);
    while (item?.parentId) {
        item = navItems.find(navItem => navItem.id === item?.parentId);
    }
    return item;
}

export function getSidebarRootFolder(
    navItems: readonly PortalNavigationItem[],
    selectedNavItemId: string | null,
): PortalNavigationContainer | null {
    if (!selectedNavItemId) {
        return null;
    }

    const rootItem = findRootNavItem(navItems, selectedNavItemId);
    if (!rootItem || !isNavContainerItem(rootItem)) {
        return null;
    }

    return rootItem;
}

export function isNavContainerItem(item: PortalNavigationItem): item is PortalNavigationContainer {
    return item.type === 'FOLDER' || item.type === 'API' || item.type === 'API_PRODUCT' || item.type === 'AI_WORKSPACE';
}

export function isNavContainer(type: PortalNavigationItem['type']): boolean {
    return type === 'FOLDER' || type === 'API' || type === 'API_PRODUCT' || type === 'AI_WORKSPACE';
}
