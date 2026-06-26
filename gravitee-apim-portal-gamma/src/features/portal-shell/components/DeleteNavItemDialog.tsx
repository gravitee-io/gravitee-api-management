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
import { Trash2Icon } from '@gravitee/graphene-core/icons';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import type { PortalNavigationItem } from '../../portals/types';
import { hasNavItemChildren } from '../utils/nav-items';

interface DeleteNavItemDialogProps {
    readonly item: PortalNavigationItem | null;
    readonly allItems: readonly PortalNavigationItem[];
    readonly open: boolean;
    readonly isPending: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onConfirm: () => void;
}

function getTypeLabel(type: PortalNavigationItem['type']): string {
    switch (type) {
        case 'PAGE':
            return 'page';
        case 'FOLDER':
            return 'folder';
        case 'LINK':
            return 'link';
        case 'API':
            return 'API';
    }
}

export function DeleteNavItemDialog({
    item,
    allItems,
    open,
    isPending,
    onOpenChange,
    onConfirm,
}: DeleteNavItemDialogProps) {
    if (!item) {
        return null;
    }

    const typeLabel = getTypeLabel(item.type);
    const hasChildren = hasNavItemChildren(allItems, item.id);
    const description = hasChildren
        ? `This ${typeLabel} and all its nested items will be permanently deleted. This cannot be undone.`
        : `This ${typeLabel} will no longer appear on your site.`;

    return (
        <ConfirmDialog
            open={open}
            onOpenChange={onOpenChange}
            title={`Delete "${item.title}" ${typeLabel}`}
            description={description}
            confirmLabel="Delete"
            pendingLabel="Deleting…"
            destructive
            isPending={isPending}
            confirmKeyword={item.title}
            icon={<Trash2Icon className="size-4" aria-hidden="true" />}
            onConfirm={onConfirm}
        />
    );
}
