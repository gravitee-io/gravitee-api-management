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
import type { PortalNavigationFolder, PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { NavigationTree } from './NavigationTree';
import styles from './Sidebar.module.scss';

export type SidebarScope = 'folder' | 'full';

interface SidebarProps {
    readonly scope: SidebarScope;
    readonly rootFolder?: PortalNavigationFolder | null;
    readonly rootItems?: PortalNavigationItem[];
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

export function Sidebar({
    scope,
    rootFolder,
    rootItems = [],
    allItems,
    selectedNavItemId,
    mode,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onRequestDeleteNavItem,
}: SidebarProps) {
    const treeItems = scope === 'folder' && rootFolder ? [rootFolder] : rootItems;

    if (treeItems.length === 0) {
        return null;
    }

    return (
        <aside className={styles.sidebar}>
            <NavigationTree
                items={treeItems}
                allItems={allItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                showRootAddButton={scope === 'full'}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddApiNavItem={onAddApiNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
        </aside>
    );
}
