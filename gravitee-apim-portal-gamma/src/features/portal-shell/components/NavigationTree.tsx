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
import { useCallback, useState } from 'react';

import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import type { AddPageOptions } from '../utils/page-type-options';
import { sortNavItemsByOrder } from '../utils/nav-items';
import { ApiSelectionDialog } from './ApiSelectionDialog';
import { PageTypeDialog } from './PageTypeDialog';
import { TreeAddButton } from './TreeAddButton';
import { TreeNode } from './TreeNode';
import styles from './NavigationTree.module.scss';

interface NavigationTreeProps {
    readonly items: PortalNavigationItem[];
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly showRootAddButton?: boolean;
    readonly rootAddParentId?: string | null;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

export function NavigationTree({
    items,
    allItems,
    selectedNavItemId,
    mode,
    showRootAddButton = false,
    rootAddParentId = null,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onUpdateNavItem,
    onRequestDeleteNavItem,
}: NavigationTreeProps) {
    const isEditMode = mode === 'edit';
    const [apiDialogParentId, setApiDialogParentId] = useState<string | null | undefined>(undefined);
    const [pageDialogParentId, setPageDialogParentId] = useState<string | null | undefined>(undefined);

    const handleRequestApi = useCallback((parentId: string | null) => {
        setApiDialogParentId(parentId);
    }, []);

    const handleRequestPage = useCallback((parentId: string | null) => {
        setPageDialogParentId(parentId);
    }, []);

    const handleApiSelected = useCallback(
        async (apiId: string, apiName: string) => {
            if (apiDialogParentId === undefined) {
                return;
            }
            await onAddApiNavItem(apiId, apiName, apiDialogParentId);
            setApiDialogParentId(undefined);
        },
        [apiDialogParentId, onAddApiNavItem],
    );

    const handlePageTypeSelected = useCallback(
        (contentType: AddPageOptions['contentType']) => {
            if (pageDialogParentId === undefined) {
                return;
            }
            onAddNavItem('PAGE', pageDialogParentId, { contentType });
            setPageDialogParentId(undefined);
        },
        [onAddNavItem, pageDialogParentId],
    );

    const sortedItems = sortNavItemsByOrder(items);

    return (
        <>
            <nav className={styles.tree} aria-label="Portal navigation">
                {sortedItems.map(item => (
                    <TreeNode
                        key={item.id}
                        item={item}
                        allItems={allItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        depth={0}
                        onSelectNavItem={onSelectNavItem}
                        onAddNavItem={onAddNavItem}
                        onRequestApi={handleRequestApi}
                        onRequestPage={handleRequestPage}
                        onUpdateNavItem={onUpdateNavItem}
                        onRequestDeleteNavItem={onRequestDeleteNavItem}
                    />
                ))}
                {isEditMode && showRootAddButton && (
                    <TreeAddButton
                        parentId={rootAddParentId}
                        allItems={allItems}
                        depth={0}
                        onAdd={onAddNavItem}
                        onRequestApi={handleRequestApi}
                    />
                )}
            </nav>

            <ApiSelectionDialog
                open={apiDialogParentId !== undefined}
                onOpenChange={open => {
                    if (!open) {
                        setApiDialogParentId(undefined);
                    }
                }}
                onSelect={handleApiSelected}
            />

            <PageTypeDialog
                open={pageDialogParentId !== undefined}
                onOpenChange={open => {
                    if (!open) {
                        setPageDialogParentId(undefined);
                    }
                }}
                onSelect={handlePageTypeSelected}
            />
        </>
    );
}
