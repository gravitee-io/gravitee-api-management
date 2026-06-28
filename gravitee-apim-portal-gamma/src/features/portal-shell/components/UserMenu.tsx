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
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { useCallback, useRef, useState } from 'react';

import type {
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../portals/types';
import { USER_MENU_PAGE_TYPE_OPTIONS, type AddPageOptions } from '../utils/page-type-options';
import type { EditorMode } from '../../editor/stores/editor.store';
import { isExternalUrl } from '../utils/link-target';
import {
    parsePortalPageSlug,
    resolveUserMenuItemPath,
} from '../utils/user-menu-url';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { NavLinkPagePicker } from './NavLinkPagePicker';
import { UserMenuItemRow } from './UserMenuItemRow';
import styles from './UserMenu.module.scss';

function UserIconGlyph() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
        </svg>
    );
}

interface UserMenuProps {
    readonly userMenuRootItems: readonly PortalNavigationItem[];
    readonly allNavItems: readonly PortalNavigationItem[];
    readonly hasUserMenuItems: boolean;
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onAddUserMenuNavItem: (
        type: PortalNavigationItemType,
        parentId: string | null,
        pageOptions?: AddPageOptions,
    ) => Promise<void>;
    readonly onAddUserMenuLink: (page: PortalNavigationPage, parentId: string | null) => Promise<void>;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onSelectNavItem?: (id: string) => void;
    readonly align?: 'start' | 'center' | 'end';
    readonly side?: 'top' | 'bottom';
    readonly className?: string;
}

export type UserMenuShellProps = Omit<
    UserMenuProps,
    'mode' | 'portalId' | 'portalPages' | 'getPagePath' | 'onNavigate' | 'align' | 'side' | 'className'
>;

export function UserMenu({
    userMenuRootItems,
    hasUserMenuItems,
    mode,
    portalId,
    portalPages,
    getPagePath,
    onNavigate,
    onAddUserMenuNavItem,
    onAddUserMenuLink,
    onUpdateNavItem,
    onRequestDeleteNavItem,
    onSelectNavItem,
    align = 'end',
    side = 'bottom',
    className,
}: UserMenuProps) {
    const isEditMode = mode === 'edit';
    const [menuOpen, setMenuOpen] = useState(false);
    const [linkPickerParentId, setLinkPickerParentId] = useState<string | null | undefined>(undefined);
    const contentRef = useRef<HTMLDivElement>(null);

    const isLinkPickerOpen = linkPickerParentId !== undefined;

    const closeMenu = useCallback(() => setMenuOpen(false), []);

    const handleOpenChange = useCallback((open: boolean) => {
        setMenuOpen(open);
        if (!open) {
            setLinkPickerParentId(undefined);
        }
    }, []);

    const handleItemSelect = useCallback((item: PortalNavigationItem) => {
        if (item.type === 'PAGE' || item.type === 'FOLDER') {
            onSelectNavItem?.(item.id);
            closeMenu();
            return;
        }

        const linkItem = item as PortalNavigationLink;
        const external = isExternalUrl(linkItem.url);
        const slug = parsePortalPageSlug(linkItem.url, portalPages, portalId);
        const useSpaNavigation = Boolean(slug || (!external && onNavigate));

        if (useSpaNavigation) {
            const path = resolveUserMenuItemPath(linkItem.url, portalPages, getPagePath, portalId);
            onNavigate?.(path);
            closeMenu();
        }
    }, [closeMenu, getPagePath, onNavigate, onSelectNavItem, portalId, portalPages]);

    if (!isEditMode && !hasUserMenuItems) {
        return null;
    }

    const handleAdd = (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => {
        if (type === 'LINK') {
            setLinkPickerParentId(parentId);
            return;
        }

        void onAddUserMenuNavItem(type, parentId, pageOptions);
    };

    const handlePageSelect = (page: PortalNavigationPage) => {
        const parentId = linkPickerParentId ?? null;
        void onAddUserMenuLink(page, parentId);
        setLinkPickerParentId(undefined);
    };

    const keepFocusInsideContent = (target: EventTarget | null) => {
        return target instanceof Node && contentRef.current?.contains(target);
    };

    const addSection = isLinkPickerOpen ? (
        <NavLinkPagePicker
            pages={portalPages}
            onSelect={handlePageSelect}
            onCancel={() => setLinkPickerParentId(undefined)}
        />
    ) : (
        <div className={styles.addItemRow}>
            <AddNavItemDropdown
                allowedTypes={['FOLDER', 'PAGE', 'LINK']}
                parentId={null}
                onAdd={handleAdd}
                className={styles.addDropdown}
                pageTypeOptions={USER_MENU_PAGE_TYPE_OPTIONS}
            />
        </div>
    );

    const editItems = userMenuRootItems.map(item => (
        <UserMenuItemRow
            key={item.id}
            item={item}
            portalId={portalId}
            portalPages={portalPages}
            onSelect={handleItemSelect}
            onUpdateNavItem={onUpdateNavItem}
            onRequestDeleteNavItem={onRequestDeleteNavItem}
        />
    ));

    const orderedEditContent = side === 'top' ? (
        <>
            {addSection}
            {editItems}
        </>
    ) : (
        <>
            {editItems}
            {addSection}
        </>
    );

    const renderPreviewItem = (item: PortalNavigationItem) => {
        if (item.type === 'PAGE' || item.type === 'FOLDER') {
            return (
                <DropdownMenuItem
                    key={item.id}
                    className={styles.menuLink}
                    onSelect={() => handleItemSelect(item)}
                >
                    {item.title}
                </DropdownMenuItem>
            );
        }

        const linkItem = item as PortalNavigationLink;
        const external = isExternalUrl(linkItem.url);
        const slug = parsePortalPageSlug(linkItem.url, portalPages, portalId);
        const useSpaNavigation = Boolean(slug || (!external && onNavigate));

        if (useSpaNavigation) {
            return (
                <DropdownMenuItem
                    key={item.id}
                    className={styles.menuLink}
                    onSelect={() => handleItemSelect(item)}
                >
                    {linkItem.title}
                </DropdownMenuItem>
            );
        }

        return (
            <DropdownMenuItem key={item.id} asChild>
                <a
                    href={linkItem.url}
                    className={styles.menuLink}
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    {linkItem.title}
                </a>
            </DropdownMenuItem>
        );
    };

    return (
        <DropdownMenu modal={false} open={menuOpen} onOpenChange={handleOpenChange}>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label="User menu"
                    className={`${styles.trigger} ${className ?? ''}`}
                >
                    <UserIconGlyph />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
                ref={contentRef}
                align={align}
                side={side}
                className={isLinkPickerOpen ? 'w-auto min-w-72' : 'w-auto min-w-56'}
                onFocusOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
                onPointerDownOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
                onInteractOutside={event => {
                    if (keepFocusInsideContent(event.target)) {
                        event.preventDefault();
                    }
                }}
                onEscapeKeyDown={event => {
                    if (!isLinkPickerOpen) {
                        return;
                    }

                    event.preventDefault();
                    setLinkPickerParentId(undefined);
                }}
            >
                {isEditMode ? (
                    orderedEditContent
                ) : (
                    userMenuRootItems.map(renderPreviewItem)
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
