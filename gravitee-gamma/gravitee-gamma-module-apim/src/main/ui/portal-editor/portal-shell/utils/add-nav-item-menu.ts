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
import type { AddPageOptions } from './page-type-options';

export type { AddPageOptions } from './page-type-options';

export const ADD_NAV_ITEM_TYPE_ORDER: PortalNavigationItemType[] = ['API', 'FOLDER', 'PAGE', 'LINK'];

export const ADD_NAV_ITEM_TYPE_LABELS: Record<PortalNavigationItemType, string> = {
    PAGE: 'Page',
    FOLDER: 'Folder',
    LINK: 'Link',
    API: 'API',
};

const ALL_ADD_TYPES: PortalNavigationItemType[] = ['API', 'FOLDER', 'PAGE', 'LINK'];
const NON_API_ADD_TYPES: PortalNavigationItemType[] = ['FOLDER', 'PAGE', 'LINK'];

export function getAllowedAddNavItemTypes(
    allItems: readonly PortalNavigationItem[],
    parentId: string | null,
): PortalNavigationItemType[] {
    return canAddApiNavItem(allItems, parentId) ? ALL_ADD_TYPES : NON_API_ADD_TYPES;
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
): void {
    if (type === 'API') {
        onRequestApi(parentId);
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
