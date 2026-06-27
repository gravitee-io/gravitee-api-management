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
import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import {
    ADD_NAV_ITEM_TYPE_LABELS,
    getAllowedAddNavItemTypes,
    handleAddNavItemSelection,
    orderAddNavItemTypes,
} from '../utils/add-nav-item-menu';
import { getNavTypeIcon } from '../utils/nav-type-icons';

interface AddNavItemContextMenuProps {
    readonly children: ReactNode;
    readonly parentId: string;
    readonly allItems: readonly PortalNavigationItem[];
    readonly enabled: boolean;
    readonly onAdd: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestApi: (parentId: string | null) => void;
}

export function AddNavItemContextMenu({
    children,
    parentId,
    allItems,
    enabled,
    onAdd,
    onRequestApi,
}: AddNavItemContextMenuProps) {
    if (!enabled) {
        return children;
    }

    const allowedTypes = orderAddNavItemTypes(getAllowedAddNavItemTypes(allItems, parentId));

    return (
        <ContextMenu>
            <ContextMenuTrigger asChild>{children}</ContextMenuTrigger>
            <ContextMenuContent>
                {allowedTypes.map(type => (
                    <ContextMenuItem
                        key={type}
                        className="gap-2"
                        onClick={() => handleAddNavItemSelection(type, parentId, onAdd, onRequestApi)}
                    >
                        <span className="text-muted-foreground" aria-hidden="true">
                            {getNavTypeIcon(type)}
                        </span>
                        {ADD_NAV_ITEM_TYPE_LABELS[type]}
                    </ContextMenuItem>
                ))}
            </ContextMenuContent>
        </ContextMenu>
    );
}
