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
import { PlusIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import type { PortalNavigationPage, UserMenuItem } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { useUserMenu } from '../hooks/useUserMenu';
import { isExternalUrl } from '../utils/link-target';
import {
    getUserMenuItemDisplayUrl,
    parsePortalPageSlug,
    resolveUserMenuItemPath,
} from '../utils/user-menu-url';
import { InlineEdit } from '../../../shared/components/InlineEdit';
import { UserMenuPagePicker } from './UserMenuPagePicker';
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
    readonly items: readonly UserMenuItem[];
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onChange?: (items: UserMenuItem[]) => void;
    readonly align?: 'start' | 'center' | 'end';
    readonly side?: 'top' | 'bottom';
    readonly className?: string;
}

interface EditMenuItemRowProps {
    readonly item: UserMenuItem;
    readonly displayUrl: string;
    readonly onUpdate: (patch: Partial<Pick<UserMenuItem, 'label' | 'url'>>) => void;
    readonly onRemove: () => void;
}

function EditMenuItemRow({ item, displayUrl, onUpdate, onRemove }: EditMenuItemRowProps) {
    return (
        <div className={styles.editRow}>
            <div className={styles.editRowHeader}>
                <InlineEdit
                    value={item.label}
                    editable
                    onChange={label => onUpdate({ label })}
                    ariaLabel={`User menu item label: ${item.label}`}
                    className={styles.editLabel}
                />
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    aria-label={`Remove ${item.label}`}
                    onClick={onRemove}
                >
                    <XIcon className="size-3.5" aria-hidden="true" />
                </Button>
            </div>
            <InlineEdit
                value={displayUrl}
                editable
                onChange={url => onUpdate({ url })}
                ariaLabel={`User menu item URL: ${item.label}`}
                className={styles.editUrl}
                placeholder="https://"
            />
        </div>
    );
}

export function UserMenu({
    items = [],
    mode,
    portalId,
    portalPages,
    getPagePath,
    onNavigate,
    onChange,
    align = 'end',
    side = 'bottom',
    className,
}: UserMenuProps) {
    const isEditMode = mode === 'edit';
    const [menuOpen, setMenuOpen] = useState(false);
    const [isAddingPage, setIsAddingPage] = useState(false);
    const contentRef = useRef<HTMLDivElement>(null);
    const { addPageItem, updateItem, removeItem } = useUserMenu(items, nextItems => onChange?.(nextItems));

    if (!isEditMode && items.length === 0) {
        return null;
    }

    const handleOpenChange = (open: boolean) => {
        setMenuOpen(open);
        if (!open) {
            setIsAddingPage(false);
        }
    };

    const handlePageSelect = (page: PortalNavigationPage) => {
        addPageItem({
            label: page.title,
            url: page.slug,
        });
        setIsAddingPage(false);
    };

    const handlePreviewItemClick = (item: UserMenuItem) => {
        const path = resolveUserMenuItemPath(item.url, portalPages, getPagePath, portalId);
        onNavigate?.(path);
        setMenuOpen(false);
    };

    const keepFocusInsideContent = (target: EventTarget | null) => {
        return target instanceof Node && contentRef.current?.contains(target);
    };

    const addSection = isAddingPage ? (
        <UserMenuPagePicker
            pages={portalPages}
            onSelect={handlePageSelect}
            onCancel={() => setIsAddingPage(false)}
        />
    ) : (
        <div className={styles.addItemRow}>
            <Button
                type="button"
                variant="ghost"
                size="sm"
                className={styles.addButton}
                aria-label="Add user menu item"
                onClick={() => setIsAddingPage(true)}
            >
                <PlusIcon className="size-4" aria-hidden="true" />
                Add menu item
            </Button>
        </div>
    );

    const editItems = items.map(item => (
        <div key={item.id} className={styles.editItem}>
            <EditMenuItemRow
                item={item}
                displayUrl={getUserMenuItemDisplayUrl(item.url, portalPages, portalId)}
                onUpdate={patch => updateItem(item.id, patch)}
                onRemove={() => removeItem(item.id)}
            />
        </div>
    ));

    const editContent = side === 'top' ? (
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
                className={isAddingPage ? 'w-auto min-w-72' : 'w-auto min-w-56'}
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
                    if (!isAddingPage) {
                        return;
                    }

                    event.preventDefault();
                    setIsAddingPage(false);
                }}
            >
                {isEditMode ? (
                    editContent
                ) : (
                    items.map(item => {
                        const external = isExternalUrl(item.url);
                        const slug = parsePortalPageSlug(item.url, portalPages, portalId);
                        const useSpaNavigation = Boolean(slug || (!external && onNavigate));

                        if (useSpaNavigation) {
                            return (
                                <DropdownMenuItem
                                    key={item.id}
                                    className={styles.menuLink}
                                    onSelect={() => handlePreviewItemClick(item)}
                                >
                                    {item.label}
                                </DropdownMenuItem>
                            );
                        }

                        return (
                            <DropdownMenuItem key={item.id} asChild>
                                <a
                                    href={item.url}
                                    className={styles.menuLink}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    {item.label}
                                </a>
                            </DropdownMenuItem>
                        );
                    })
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
