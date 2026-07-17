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
import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import { canAddApiNavItem } from './can-add-api-nav-item';
import { canAddApiProductNavItem } from './can-add-api-product-nav-item';
import type { AddPageOptions } from './page-type-options';

export type { AddPageOptions } from './page-type-options';

export const ADD_NAV_ITEM_TYPE_ORDER: PortalNavigationItemType[] = [
    'API',
    'API_PRODUCT',
    'FOLDER',
    'PAGE',
    'LINK',
];

export const ADD_NAV_ITEM_TYPE_LABELS: Record<PortalNavigationItemType, string> = {
    PAGE: 'Page',
    FOLDER: 'Folder',
    LINK: 'Link',
    API: 'API',
    API_PRODUCT: 'API Product',
};

function isAddNavItemTypeAllowed(
    type: PortalNavigationItemType,
    allItems: readonly PortalNavigationItem[],
    parentId: string | null,
): boolean {
    if (type === 'API') {
        return canAddApiNavItem(allItems, parentId);
    }
    if (type === 'API_PRODUCT') {
        return canAddApiProductNavItem(allItems, parentId);
    }
    return true;
}

export function getAllowedAddNavItemTypes(
    allItems: readonly PortalNavigationItem[],
    parentId: string | null,
): PortalNavigationItemType[] {
    return ADD_NAV_ITEM_TYPE_ORDER.filter(type => isAddNavItemTypeAllowed(type, allItems, parentId));
}

export function orderAddNavItemTypes(allowedTypes: readonly PortalNavigationItemType[]): PortalNavigationItemType[] {
    return ADD_NAV_ITEM_TYPE_ORDER.filter(type => allowedTypes.includes(type));
}

export function handleAddNavItemSelection(
    type: PortalNavigationItemType,
    parentId: string | null,
    onAdd: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void,
    onRequestApi: (parentId: string | null) => void,
    onRequestPage: (parentId: string | null) => void,
    onRequestLink?: (parentId: string | null) => void,
    onRequestApiProduct?: (parentId: string | null) => void,
): void {
    if (type === 'API') {
        onRequestApi(parentId);
        return;
    }
    if (type === 'API_PRODUCT') {
        onRequestApiProduct?.(parentId);
        return;
    }
    if (type === 'PAGE') {
        onRequestPage(parentId);
        return;
    }
    if (type === 'LINK' && onRequestLink) {
        onRequestLink(parentId);
        return;
    }
    onAdd(type, parentId);
}
