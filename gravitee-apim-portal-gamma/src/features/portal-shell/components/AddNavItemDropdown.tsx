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
import { useState } from 'react';

import type { PageContentType, PortalNavigationItemType } from '../../portals/types';
import { ADD_NAV_ITEM_TYPE_LABELS, orderAddNavItemTypes } from '../utils/add-nav-item-menu';
import { PAGE_TYPE_OPTIONS, type AddPageOptions, type PageTypeOption } from '../utils/page-type-options';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import { AddButton } from './AddButton';
import { PageTypeDialog } from './PageTypeDialog';

type AllowedType = Exclude<PortalNavigationItemType, never>;

interface AddNavItemDropdownProps {
    readonly allowedTypes?: AllowedType[];
    readonly parentId: string | null;
    readonly onAdd: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly className?: string;
    readonly pageTypeOptions?: ReadonlyArray<PageTypeOption>;
}

export function AddNavItemDropdown({
    allowedTypes = ['FOLDER', 'PAGE', 'LINK'],
    parentId,
    onAdd,
    className,
    pageTypeOptions = PAGE_TYPE_OPTIONS,
}: AddNavItemDropdownProps) {
    const orderedTypes = orderAddNavItemTypes(allowedTypes);
    const [open, setOpen] = useState(false);
    const [pageDialogOpen, setPageDialogOpen] = useState(false);

    const handlePageTypeSelect = (contentType: PageContentType) => {
        onAdd('PAGE', parentId, { contentType });
        setPageDialogOpen(false);
    };

    return (
        <>
            <DropdownMenu open={open} onOpenChange={setOpen}>
                <DropdownMenuTrigger asChild>
                    <AddButton aria-label="Add navigation item" className={className} />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start">
                    {orderedTypes.map(type => (
                        <DropdownMenuItem
                            key={type}
                            className="gap-2"
                            onSelect={() => {
                                if (type === 'PAGE') {
                                    setOpen(false);
                                    setPageDialogOpen(true);
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
                    ))}
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
