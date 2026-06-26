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
import { isNavContainer } from '../utils/sidebar-context';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { NavItemButton } from './NavItemButton';
import styles from './NavigationSidebar.module.scss';

interface NavigationSidebarProps {
    readonly rootFolder: PortalNavigationFolder;
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

function TreeNode({
    item,
    allItems,
    selectedNavItemId,
    mode,
    depth,
    onSelectNavItem,
    onAddNavItem,
    onRequestDeleteNavItem,
}: {
    item: PortalNavigationItem;
    allItems: PortalNavigationItem[];
    selectedNavItemId: string | null;
    mode: EditorMode;
    depth: number;
    onSelectNavItem: (id: string) => void;
    onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}) {
    const isEditMode = mode === 'edit';
    const isContainer = isNavContainer(item.type);
    const children = allItems.filter(navItem => navItem.parentId === item.id);

    return (
        <div className={styles.treeNode}>
            <NavItemButton
                label={item.title}
                selected={selectedNavItemId === item.id}
                showDelete={isEditMode}
                variant="sidebar"
                icon={getTypeIcon(item.type)}
                style={{ paddingLeft: `${8 + depth * 16}px` }}
                onSelect={() => onSelectNavItem(item.id)}
                onDelete={() => onRequestDeleteNavItem(item)}
            />
            {isContainer && (
                <div className={styles.children}>
                    {children.map(child => (
                        <TreeNode
                            key={child.id}
                            item={child}
                            allItems={allItems}
                            selectedNavItemId={selectedNavItemId}
                            mode={mode}
                            depth={depth + 1}
                            onSelectNavItem={onSelectNavItem}
                            onAddNavItem={onAddNavItem}
                            onRequestDeleteNavItem={onRequestDeleteNavItem}
                        />
                    ))}
                    {isEditMode && (
                        <div style={{ paddingLeft: `${8 + (depth + 1) * 16}px` }}>
                            <AddNavItemDropdown
                                allowedTypes={['FOLDER', 'PAGE', 'LINK', 'API']}
                                parentId={item.id}
                                onAdd={onAddNavItem}
                            />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

function getTypeIcon(type: PortalNavigationItemType): React.ReactNode {
    switch (type) {
        case 'PAGE':
            return (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
            );
        case 'FOLDER':
            return (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                </svg>
            );
        case 'LINK':
            return (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                    <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                </svg>
            );
        case 'API':
            return (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                </svg>
            );
    }
}

export function NavigationSidebar({
    rootFolder,
    allItems,
    selectedNavItemId,
    mode,
    onSelectNavItem,
    onAddNavItem,
    onRequestDeleteNavItem,
}: NavigationSidebarProps) {
    return (
        <aside className={styles.sidebar}>
            <nav className={styles.tree}>
                <TreeNode
                    item={rootFolder}
                    allItems={allItems}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    depth={0}
                    onSelectNavItem={onSelectNavItem}
                    onAddNavItem={onAddNavItem}
                    onRequestDeleteNavItem={onRequestDeleteNavItem}
                />
            </nav>
        </aside>
    );
}
