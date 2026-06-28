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
import { useEffect, useRef, type KeyboardEvent, type MouseEvent } from 'react';

import type { PortalNavigationFolder, PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { InlineEdit } from '../../../shared/components/InlineEdit';
import { sortNavItemsByOrder } from '../utils/nav-items';
import { NavigationTree } from './NavigationTree';
import { PortalIconEditor } from './PortalIconEditor';
import { UserMenu, type UserMenuShellProps } from './UserMenu';
import styles from './Sidebar.module.scss';

export type SidebarScope = 'folder' | 'full';

interface SidebarProps {
    readonly scope: SidebarScope;
    readonly rootFolder?: PortalNavigationFolder | null;
    readonly rootItems?: PortalNavigationItem[];
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly portalIconUrl?: string;
    readonly portalLabel?: string;
    readonly onPortalIconChange?: (portalIconUrl: string) => void;
    readonly onPortalLabelChange?: (portalLabel: string) => void;
    readonly portalId?: string;
    readonly userMenuProps?: UserMenuShellProps;
    readonly portalPages?: readonly PortalNavigationPage[];
    readonly getPagePath?: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onBackToMainNavigation?: () => void;
    readonly showSidebarChrome?: boolean;
}

export function Sidebar({
    scope,
    rootFolder,
    rootItems = [],
    allItems,
    selectedNavItemId,
    mode,
    portalIconUrl = '',
    portalLabel = DEFAULT_PORTAL_LABEL,
    portalId = '',
    onPortalIconChange,
    onPortalLabelChange,
    userMenuProps,
    portalPages = [],
    getPagePath = () => '#',
    onNavigate,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onUpdateNavItem,
    onRequestDeleteNavItem,
    onBackToMainNavigation,
    showSidebarChrome = false,
}: SidebarProps) {
    const isFullScope = scope === 'full';
    const isFolderScope = scope === 'folder' && rootFolder != null;
    const showChrome = isFullScope || showSidebarChrome;
    const isTopBackTarget = Boolean(onBackToMainNavigation);
    const pendingBackRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastTopClickRef = useRef(0);
    const treeItems = isFolderScope
        ? sortNavItemsByOrder(allItems.filter(item => item.parentId === rootFolder.id))
        : rootItems;

    const cancelPendingBack = () => {
        if (pendingBackRef.current != null) {
            clearTimeout(pendingBackRef.current);
            pendingBackRef.current = null;
        }
    };

    useEffect(() => () => cancelPendingBack(), []);

    const triggerBack = () => {
        cancelPendingBack();
        onBackToMainNavigation?.();
    };

    const handleTopClick = () => {
        if (!onBackToMainNavigation) {
            return;
        }

        if (mode === 'preview') {
            triggerBack();
            return;
        }

        const now = Date.now();
        if (now - lastTopClickRef.current < 300) {
            lastTopClickRef.current = 0;
            cancelPendingBack();
            return;
        }

        lastTopClickRef.current = now;
        cancelPendingBack();
        pendingBackRef.current = setTimeout(() => {
            pendingBackRef.current = null;
            onBackToMainNavigation();
        }, 300);
    };

    const handleTopKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (!isTopBackTarget) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            triggerBack();
        }
    };

    const handleLabelDoubleClick = (event: MouseEvent) => {
        if (!isTopBackTarget || mode !== 'edit') {
            return;
        }

        lastTopClickRef.current = 0;
        cancelPendingBack();
        event.stopPropagation();
    };

    if (!isFullScope && !isFolderScope) {
        return null;
    }

    return (
        <aside className={styles.sidebar}>
            {showChrome && (
                <div
                    className={`${styles.top} ${isTopBackTarget ? styles.topBack : ''} portal-editable-region`}
                    onClick={isTopBackTarget ? handleTopClick : undefined}
                    onKeyDown={isTopBackTarget ? handleTopKeyDown : undefined}
                    role={isTopBackTarget ? 'button' : undefined}
                    tabIndex={isTopBackTarget ? 0 : undefined}
                    aria-label={isTopBackTarget ? 'Back to main navigation' : undefined}
                >
                    <div onClick={mode === 'edit' ? event => event.stopPropagation() : undefined}>
                        <PortalIconEditor
                            portalIconUrl={portalIconUrl}
                            editable={mode === 'edit'}
                            onChange={onPortalIconChange}
                        />
                    </div>
                    <span className={styles.brandLabelWrapper} onDoubleClick={handleLabelDoubleClick}>
                        <InlineEdit
                            value={portalLabel}
                            editable={mode === 'edit'}
                            activateOn="doubleClick"
                            className={styles.brandLabel}
                            ariaLabel="Portal label"
                            placeholder={DEFAULT_PORTAL_LABEL}
                            onChange={label => onPortalLabelChange?.(label)}
                        />
                    </span>
                </div>
            )}

            <div className={`${styles.treeRegion} portal-editable-region`}>
                <NavigationTree
                    items={treeItems}
                    allItems={allItems}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    showRootAddButton={isFullScope || isFolderScope}
                    rootAddParentId={isFolderScope ? rootFolder.id : null}
                    onSelectNavItem={onSelectNavItem}
                    onAddNavItem={onAddNavItem}
                    onAddApiNavItem={onAddApiNavItem}
                    onUpdateNavItem={onUpdateNavItem}
                    onRequestDeleteNavItem={onRequestDeleteNavItem}
                />
            </div>

            {showChrome && userMenuProps && (
                <div className={styles.bottom}>
                    <UserMenu
                        {...userMenuProps}
                        mode={mode}
                        portalId={portalId}
                        portalPages={portalPages}
                        getPagePath={getPagePath}
                        onNavigate={onNavigate}
                        align="start"
                        side="top"
                        className={styles.userIcon}
                    />
                </div>
            )}
        </aside>
    );
}
