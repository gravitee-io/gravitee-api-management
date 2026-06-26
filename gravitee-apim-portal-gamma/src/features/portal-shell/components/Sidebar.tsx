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
import type { PortalNavigationFolder, PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage, UserMenuItem } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { InlineEdit } from './InlineEdit';
import { NavigationTree } from './NavigationTree';
import { PortalIconEditor } from './PortalIconEditor';
import { UserMenu } from './UserMenu';
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
    readonly userMenuItems?: readonly UserMenuItem[];
    readonly portalPages?: readonly PortalNavigationPage[];
    readonly getPagePath?: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onUserMenuChange?: (items: UserMenuItem[]) => void;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
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
    userMenuItems = [],
    portalPages = [],
    getPagePath = () => '#',
    onNavigate,
    onUserMenuChange,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onUpdateNavItem,
    onRequestDeleteNavItem,
}: SidebarProps) {
    const isFullScope = scope === 'full';
    const isFolderScope = scope === 'folder' && rootFolder != null;
    const treeItems = isFolderScope
        ? allItems
              .filter(item => item.parentId === rootFolder.id)
              .sort((left, right) => left.order - right.order)
        : rootItems;

    if (!isFullScope && !isFolderScope) {
        return null;
    }

    return (
        <aside className={styles.sidebar}>
            {isFullScope && (
                <div className={`${styles.top} portal-editable-region`}>
                    <PortalIconEditor
                        portalIconUrl={portalIconUrl}
                        editable={mode === 'edit'}
                        onChange={onPortalIconChange}
                    />
                    <InlineEdit
                        value={portalLabel}
                        editable={mode === 'edit'}
                        className={styles.brandLabel}
                        ariaLabel="Portal label"
                        placeholder={DEFAULT_PORTAL_LABEL}
                        onChange={label => onPortalLabelChange?.(label)}
                    />
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

            {isFullScope && (
                <div className={styles.bottom}>
                    <UserMenu
                        items={userMenuItems}
                        mode={mode}
                        portalId={portalId}
                        portalPages={portalPages}
                        getPagePath={getPagePath}
                        onNavigate={onNavigate}
                        onChange={onUserMenuChange}
                        align="start"
                        side="top"
                        className={styles.userIcon}
                    />
                </div>
            )}
        </aside>
    );
}
