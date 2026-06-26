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
import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';

import type { PortalNavigationItemType } from '../../portals/types';
import { getNavTypeIcon } from '../utils/nav-type-icons';

type AllowedType = Exclude<PortalNavigationItemType, never>;

interface AddNavItemDropdownProps {
    readonly allowedTypes?: AllowedType[];
    readonly parentId: string | null;
    readonly onAdd: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly className?: string;
}

const TYPE_ORDER: PortalNavigationItemType[] = ['API', 'FOLDER', 'PAGE', 'LINK'];

const TYPE_LABELS: Record<PortalNavigationItemType, string> = {
    PAGE: 'Page',
    FOLDER: 'Folder',
    LINK: 'Link',
    API: 'API',
};

export function AddNavItemDropdown({
    allowedTypes = ['FOLDER', 'PAGE', 'LINK'],
    parentId,
    onAdd,
    className,
}: AddNavItemDropdownProps) {
    const orderedTypes = TYPE_ORDER.filter(type => allowedTypes.includes(type));

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="icon-sm"
                    className={className}
                    aria-label="Add navigation item"
                >
                    <PlusIcon className="size-4 text-muted-foreground" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
                {orderedTypes.map(type => (
                    <DropdownMenuItem key={type} className="gap-2" onClick={() => onAdd(type, parentId)}>
                        <span className="text-muted-foreground" aria-hidden="true">
                            {getNavTypeIcon(type)}
                        </span>
                        {TYPE_LABELS[type]}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
