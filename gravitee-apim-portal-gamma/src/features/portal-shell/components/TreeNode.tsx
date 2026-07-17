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

import type { PortalNavigationItem, PortalNavigationItemType, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { getPageContentTypeLabel, getPageNavIcon, getNavTypeIcon } from '../utils/nav-type-icons';
import { canPublishNavItem, isNavItemPublished, sortNavItemsByOrder } from '../utils/nav-items';
import { isNavContainer } from '../utils/sidebar-context';
import { shouldExpandNode } from '../utils/tree-expand';
import { NavItemContextMenu } from './NavItemContextMenu';
import { EditableLinkNavItem, PreviewLinkNavItem } from './EditableLinkNavItem';
import { NavItemButton } from './NavItemButton';
import navItemStyles from './NavItemButton.module.scss';
import styles from './NavigationTree.module.scss';

interface TreeNodeProps {
    readonly item: PortalNavigationItem;
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly depth: number;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestApi: (parentId: string | null) => void;
    readonly onRequestApiProduct: (parentId: string | null) => void;
    readonly onRequestPage: (parentId: string | null) => void;
    readonly onRequestLink: (parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onTogglePublished: (item: PortalNavigationItem) => void;
    readonly instanceOverrides?: Record<string, Record<string, string>>;
}

export function TreeNode({
    item,
    allItems,
    selectedNavItemId,
    mode,
    portalId,
    portalPages,
    depth,
    onSelectNavItem,
    onAddNavItem,
    onRequestApi,
    onRequestApiProduct,
    onRequestPage,
    onRequestLink,
    onUpdateNavItem,
    onRequestDeleteNavItem,
    onTogglePublished,
    instanceOverrides = {},
}: TreeNodeProps) {
    const isEditMode = mode === 'edit';
    const isContainer = isNavContainer(item.type);
    const isUnpublished = isEditMode && !isNavItemPublished(item);
    const publishState = canPublishNavItem(item, allItems);
    const children = sortNavItemsByOrder(allItems.filter(navItem => navItem.parentId === item.id));
    const [expanded, setExpanded] = useState(() => shouldExpandNode(allItems, item.id, selectedNavItemId));

    useEffect(() => {
        if (shouldExpandNode(allItems, item.id, selectedNavItemId)) {
            setExpanded(true);
        }
    }, [allItems, item.id, selectedNavItemId]);

    const treeDepthStyle = { '--tree-depth': depth } as CSSProperties;
    const pageIcon =
        item.type === 'PAGE'
            ? getPageNavIcon(item)
            : getNavTypeIcon(item.type);
    const pageTooltip = item.type === 'PAGE' ? getPageContentTypeLabel(item.contentType ?? 'BLOCK') : undefined;

    const nodeButton =
        item.type === 'LINK' && isEditMode ? (
            <EditableLinkNavItem
                item={item as PortalNavigationLink}
                instanceStyle={instanceOverrides[item.id]}
                portalId={portalId}
                portalPages={portalPages}
                selected={selectedNavItemId === item.id}
                showDelete
                variant="sidebar"
                className={navItemStyles.compactLeading}
                icon={pageIcon}
                title={pageTooltip}
                unpublished={isUnpublished}
                onUpdate={patch => onUpdateNavItem(item.id, patch)}
                onDelete={() => onRequestDeleteNavItem(item)}
            />
        ) : item.type === 'LINK' ? (
            <PreviewLinkNavItem
                navItemId={item.id}
                instanceStyle={instanceOverrides[item.id]}
                label={item.title}
                selected={selectedNavItemId === item.id}
                variant="sidebar"
                className={navItemStyles.compactLeading}
                icon={pageIcon}
                onSelect={() => onSelectNavItem(item.id)}
            />
        ) : (
            <NavItemButton
                navItemId={item.id}
                instanceStyle={instanceOverrides[item.id]}
                label={item.title}
                selected={selectedNavItemId === item.id}
                showDelete={isEditMode}
                variant="sidebar"
                className={navItemStyles.compactLeading}
                icon={pageIcon}
                title={pageTooltip}
                unpublished={isUnpublished}
                onSelect={() => onSelectNavItem(item.id)}
                onDelete={() => onRequestDeleteNavItem(item)}
                onLabelChange={isEditMode ? title => onUpdateNavItem(item.id, { title }) : undefined}
            />
        );

    const row = (
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
            <div className={styles.nodeButton}>{nodeButton}</div>
        </div>
    );

    return (
        <div className={styles.treeNode} style={treeDepthStyle}>
            <NavItemContextMenu
                item={item}
                allItems={allItems}
                enabled={isEditMode}
                isContainer={isContainer}
                onAdd={onAddNavItem}
                onRequestApi={onRequestApi}
                onRequestApiProduct={onRequestApiProduct}
                onRequestPage={onRequestPage}
                onRequestLink={onRequestLink}
                onTogglePublished={onTogglePublished}
                publishDisabled={!publishState.allowed}
                publishDisabledReason={publishState.reason}
            >
                {row}
            </NavItemContextMenu>
            {isContainer && expanded && (
                <div className={styles.children}>
                    {children.map(child => (
                        <TreeNode
                            key={child.id}
                            item={child}
                            allItems={allItems}
                            selectedNavItemId={selectedNavItemId}
                            mode={mode}
                            portalId={portalId}
                            portalPages={portalPages}
                            depth={depth + 1}
                            onSelectNavItem={onSelectNavItem}
                            onAddNavItem={onAddNavItem}
                            onRequestApi={onRequestApi}
                            onRequestApiProduct={onRequestApiProduct}
                            onRequestPage={onRequestPage}
                            onRequestLink={onRequestLink}
                            onUpdateNavItem={onUpdateNavItem}
                            onRequestDeleteNavItem={onRequestDeleteNavItem}
                            onTogglePublished={onTogglePublished}
                            instanceOverrides={instanceOverrides}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
