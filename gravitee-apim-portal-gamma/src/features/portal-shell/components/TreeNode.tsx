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
import { useEffect, useState, type CSSProperties } from 'react';

import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import { isNavContainer } from '../utils/sidebar-context';
import { shouldExpandNode } from '../utils/tree-expand';
import { NavItemButton } from './NavItemButton';
import navItemStyles from './NavItemButton.module.scss';
import { TreeAddButton } from './TreeAddButton';
import styles from './NavigationTree.module.scss';

interface TreeNodeProps {
    readonly item: PortalNavigationItem;
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly depth: number;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestApi: (parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

export function TreeNode({
    item,
    allItems,
    selectedNavItemId,
    mode,
    depth,
    onSelectNavItem,
    onAddNavItem,
    onRequestApi,
    onUpdateNavItem,
    onRequestDeleteNavItem,
}: TreeNodeProps) {
    const isEditMode = mode === 'edit';
    const isContainer = isNavContainer(item.type);
    const children = allItems
        .filter(navItem => navItem.parentId === item.id)
        .sort((left, right) => left.order - right.order);
    const [expanded, setExpanded] = useState(() => shouldExpandNode(allItems, item.id, selectedNavItemId));

    useEffect(() => {
        if (shouldExpandNode(allItems, item.id, selectedNavItemId)) {
            setExpanded(true);
        }
    }, [allItems, item.id, selectedNavItemId]);

    const treeDepthStyle = { '--tree-depth': depth } as CSSProperties;

    return (
        <div className={styles.treeNode} style={treeDepthStyle}>
            <div className={styles.row}>
                {isContainer ? (
                    <button
                        type="button"
                        className={`${styles.chevron} ${expanded ? styles.chevronExpanded : ''}`}
                        aria-label={expanded ? `Collapse ${item.title}` : `Expand ${item.title}`}
                        aria-expanded={expanded}
                        onClick={event => {
                            event.stopPropagation();
                            setExpanded(current => !current);
                        }}
                    >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="9 18 15 12 9 6" />
                        </svg>
                    </button>
                ) : (
                    <span className={styles.chevronSpacer} aria-hidden="true" />
                )}
                <div className={styles.nodeButton}>
                    <NavItemButton
                        label={item.title}
                        selected={selectedNavItemId === item.id}
                        showDelete={isEditMode}
                        variant="sidebar"
                        className={navItemStyles.compactLeading}
                        icon={getNavTypeIcon(item.type)}
                        onSelect={() => onSelectNavItem(item.id)}
                        onDelete={() => onRequestDeleteNavItem(item)}
                        onLabelChange={isEditMode ? title => onUpdateNavItem(item.id, { title }) : undefined}
                    />
                </div>
            </div>
            {isContainer && expanded && (
                <>
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
                                onRequestApi={onRequestApi}
                                onUpdateNavItem={onUpdateNavItem}
                                onRequestDeleteNavItem={onRequestDeleteNavItem}
                            />
                        ))}
                    </div>
                    {isEditMode && (
                        <TreeAddButton
                            parentId={item.id}
                            allItems={allItems}
                            depth={depth + 1}
                            onAdd={onAddNavItem}
                            onRequestApi={onRequestApi}
                        />
                    )}
                </>
            )}
        </div>
    );
}
