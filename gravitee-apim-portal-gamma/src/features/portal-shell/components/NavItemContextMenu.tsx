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
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator,
    ContextMenuTrigger,
} from '@gravitee/graphene-core';
import { EyeIcon, EyeOffIcon, PlusIcon, Wand2Icon } from '@gravitee/graphene-core/icons';
import { useRef, useState, type MouseEvent, type ReactNode } from 'react';

import { useCustomizePanel } from '../../theming/components/CustomizePanelContext';
import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import { isNavItemPublished } from '../utils/nav-items';
import type { AddPageOptions } from '../utils/page-type-options';
import {
    ADD_NAV_ITEM_TYPE_LABELS,
    getAllowedAddNavItemTypes,
    handleAddNavItemSelection,
    orderAddNavItemTypes,
} from '../utils/add-nav-item-menu';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import styles from './NavItemContextMenu.module.scss';

type MenuView = 'primary' | 'add';

interface NavItemContextMenuProps {
    readonly children: ReactNode;
    readonly item: PortalNavigationItem;
    readonly allItems: readonly PortalNavigationItem[];
    readonly enabled: boolean;
    readonly isContainer: boolean;
    readonly onAdd?: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onRequestApi?: (parentId: string | null) => void;
    readonly onRequestApiProduct?: (parentId: string | null) => void;
    readonly onRequestPage?: (parentId: string | null) => void;
    readonly onRequestLink?: (parentId: string | null) => void;
    readonly onTogglePublished: (item: PortalNavigationItem) => void;
    readonly publishDisabled?: boolean;
    readonly publishDisabledReason?: string;
}

function scheduleAfterContextMenu(action: () => void) {
    window.setTimeout(action, 0);
}

export function NavItemContextMenu({
    children,
    item,
    allItems,
    enabled,
    isContainer,
    onAdd,
    onRequestApi,
    onRequestApiProduct,
    onRequestPage,
    onRequestLink,
    onTogglePublished,
    publishDisabled = false,
    publishDisabledReason,
}: NavItemContextMenuProps) {
    const customizePanel = useCustomizePanel();
    const [menuView, setMenuView] = useState<MenuView>('primary');
    const wrapperRef = useRef<HTMLDivElement>(null);
    const contextMenuPositionRef = useRef({ x: 0, y: 0 });

    if (!enabled) {
        return children;
    }

    const allowedTypes = orderAddNavItemTypes(getAllowedAddNavItemTypes(allItems, item.id));
    const isPublished = isNavItemPublished(item);

    const handleContextMenu = (event: MouseEvent) => {
        event.stopPropagation();
        contextMenuPositionRef.current = { x: event.clientX, y: event.clientY };
        setMenuView('primary');
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setMenuView('primary');
        }
    };

    const openStylePanel = () => {
        const styleTarget = wrapperRef.current?.querySelector('[data-style-target="nav-item"]') as HTMLElement | null;
        if (styleTarget && customizePanel) {
            customizePanel.openCustomizePanel(styleTarget, contextMenuPositionRef.current);
        }
    };

    const handleAddItemSelect = (event: Event) => {
        event.preventDefault();
        setMenuView('add');
    };

    const handleStyleSelect = () => {
        scheduleAfterContextMenu(openStylePanel);
    };

    const handleTogglePublished = () => {
        onTogglePublished(item);
    };

    return (
        <div ref={wrapperRef} onContextMenu={handleContextMenu}>
            <ContextMenu onOpenChange={handleOpenChange}>
                <ContextMenuTrigger asChild>
                    <div className={styles.trigger}>{children}</div>
                </ContextMenuTrigger>
                <ContextMenuContent>
                    {menuView === 'primary' ? (
                        <>
                            {isContainer && onAdd && onRequestApi && onRequestPage ? (
                                <ContextMenuItem className="gap-2" onSelect={handleAddItemSelect}>
                                    <PlusIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    Add item
                                </ContextMenuItem>
                            ) : null}
                            <ContextMenuItem className="gap-2" onSelect={handleStyleSelect}>
                                <Wand2Icon className="size-4 text-muted-foreground" aria-hidden="true" />
                                Style
                            </ContextMenuItem>
                            <ContextMenuSeparator />
                            {isPublished ? (
                                <ContextMenuItem className="gap-2" onSelect={handleTogglePublished}>
                                    <EyeOffIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    Unpublish
                                </ContextMenuItem>
                            ) : (
                                <ContextMenuItem
                                    className="gap-2"
                                    disabled={publishDisabled}
                                    title={publishDisabled ? publishDisabledReason : undefined}
                                    onSelect={handleTogglePublished}
                                >
                                    <EyeIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    Publish
                                </ContextMenuItem>
                            )}
                        </>
                    ) : (
                        allowedTypes.map(type => (
                            <ContextMenuItem
                                key={type}
                                className="gap-2"
                                onSelect={() =>
                                    handleAddNavItemSelection(
                                        type,
                                        item.id,
                                        onAdd!,
                                        onRequestApi!,
                                        onRequestPage!,
                                        onRequestLink,
                                        onRequestApiProduct,
                                    )
                                }
                            >
                                <span className="text-muted-foreground" aria-hidden="true">
                                    {getNavTypeIcon(type)}
                                </span>
                                {ADD_NAV_ITEM_TYPE_LABELS[type]}
                            </ContextMenuItem>
                        ))
                    )}
                </ContextMenuContent>
            </ContextMenu>
        </div>
    );
}
