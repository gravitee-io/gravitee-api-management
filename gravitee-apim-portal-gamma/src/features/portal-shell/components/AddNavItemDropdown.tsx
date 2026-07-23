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
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@gravitee/graphene-core';
import { useRef, useState } from 'react';

import type { PageContentType, PortalNavigationItemType, PortalNavigationPage } from '../../portals/types';
import { ADD_NAV_ITEM_TYPE_LABELS, orderAddNavItemTypes } from '../utils/add-nav-item-menu';
import { PAGE_TYPE_OPTIONS, type AddPageOptions, type PageTypeOption } from '../utils/page-type-options';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import { AddButton } from './AddButton';
import { NavLinkPagePicker } from './NavLinkPagePicker';
import { PageTypeDialog } from './PageTypeDialog';

type AllowedType = Exclude<PortalNavigationItemType, never>;

interface AddNavItemDropdownProps {
    readonly allowedTypes?: AllowedType[];
    readonly parentId: string | null;
    readonly onAdd: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly className?: string;
    readonly pageTypeOptions?: ReadonlyArray<PageTypeOption>;
    readonly portalPages?: readonly PortalNavigationPage[];
    readonly onAddLinkFromPage?: (page: PortalNavigationPage, parentId: string | null) => void;
}

export function AddNavItemDropdown({
    allowedTypes = ['FOLDER', 'PAGE', 'LINK'],
    parentId,
    onAdd,
    className,
    pageTypeOptions = PAGE_TYPE_OPTIONS,
    portalPages,
    onAddLinkFromPage,
}: AddNavItemDropdownProps) {
    const orderedTypes = orderAddNavItemTypes(allowedTypes);
    const [open, setOpen] = useState(false);
    const [pageDialogOpen, setPageDialogOpen] = useState(false);
    const [linkPickerOpen, setLinkPickerOpen] = useState(false);
    const contentRef = useRef<HTMLDivElement>(null);

    const handlePageTypeSelect = (contentType: PageContentType) => {
        onAdd('PAGE', parentId, { contentType });
        setPageDialogOpen(false);
    };

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);
        if (!nextOpen) {
            setLinkPickerOpen(false);
        }
    };

    const keepFocusInsideContent = (target: EventTarget | null) => {
        return target instanceof Node && contentRef.current?.contains(target);
    };

    const handlePageSelect = (page: PortalNavigationPage) => {
        onAddLinkFromPage?.(page, parentId);
        setLinkPickerOpen(false);
        setOpen(false);
    };

    return (
        <>
            <DropdownMenu open={open} onOpenChange={handleOpenChange}>
                <DropdownMenuTrigger asChild>
                    <AddButton aria-label="Add navigation item" className={className} />
                </DropdownMenuTrigger>
                <DropdownMenuContent
                    ref={contentRef}
                    align="start"
                    className={linkPickerOpen ? 'w-auto min-w-72' : 'w-auto min-w-40'}
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
                        if (!linkPickerOpen) {
                            return;
                        }

                        event.preventDefault();
                        setLinkPickerOpen(false);
                    }}
                >
                    {linkPickerOpen && portalPages && onAddLinkFromPage ? (
                        <NavLinkPagePicker
                            pages={portalPages}
                            onSelect={handlePageSelect}
                            onCancel={() => setLinkPickerOpen(false)}
                        />
                    ) : (
                        orderedTypes.map(type => (
                            <DropdownMenuItem
                                key={type}
                                className="gap-2"
                                onSelect={event => {
                                    if (type === 'PAGE') {
                                        setOpen(false);
                                        setPageDialogOpen(true);
                                        return;
                                    }
                                    if (type === 'LINK' && portalPages && onAddLinkFromPage) {
                                        event.preventDefault();
                                        setLinkPickerOpen(true);
                                        return;
                                    }
                                    onAdd(type, parentId);
                                    setOpen(false);
                                }}
                            >
                                <span className="text-muted-foreground" aria-hidden="true">
                                    {getNavTypeIcon(type)}
                                </span>
                                {ADD_NAV_ITEM_TYPE_LABELS[type]}
                            </DropdownMenuItem>
                        ))
                    )}
                </DropdownMenuContent>
            </DropdownMenu>

            <PageTypeDialog
                open={pageDialogOpen}
                onOpenChange={setPageDialogOpen}
                onSelect={handlePageTypeSelect}
                options={pageTypeOptions}
            />
        </>
    );
}
