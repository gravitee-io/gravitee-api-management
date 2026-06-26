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
import type { PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage, UserMenuItem } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { NavItemButton } from './NavItemButton';
import { PortalIconEditor } from './PortalIconEditor';
import { UserMenu } from './UserMenu';
import styles from './PortalHeader.module.scss';

interface PortalHeaderProps {
    readonly portalId: string;
    readonly portalIconUrl: string;
    readonly rootItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly userMenuItems: readonly UserMenuItem[];
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onUserMenuChange?: (items: UserMenuItem[]) => void;
}

export function PortalHeader({
    portalId,
    portalIconUrl,
    rootItems,
    selectedNavItemId,
    mode,
    onSelectNavItem,
    onAddNavItem,
    onUpdateNavItem,
    onPortalIconChange,
    onRequestDeleteNavItem,
    userMenuItems,
    portalPages,
    getPagePath,
    onNavigate,
    onUserMenuChange,
}: PortalHeaderProps) {
    const isEditMode = mode === 'edit';

    return (
        <header className={`${styles.header} portal-editable-region`}>
            <div className={styles.left}>
                <PortalIconEditor
                    portalIconUrl={portalIconUrl}
                    editable={isEditMode}
                    onChange={onPortalIconChange}
                />
            </div>

            <nav className={styles.nav}>
                {rootItems.map(item => (
                    <NavItemButton
                        key={item.id}
                        label={item.title}
                        selected={selectedNavItemId === item.id}
                        showDelete={isEditMode}
                        variant="header"
                        className={styles.navButton}
                        onSelect={() => onSelectNavItem(item.id)}
                        onDelete={() => onRequestDeleteNavItem(item)}
                        onLabelChange={isEditMode ? title => onUpdateNavItem(item.id, { title }) : undefined}
                    />
                ))}
                {isEditMode && (
                    <AddNavItemDropdown
                        allowedTypes={['FOLDER', 'PAGE', 'LINK']}
                        parentId={null}
                        onAdd={onAddNavItem}
                    />
                )}
            </nav>

            <div className={styles.right}>
                <UserMenu
                    items={userMenuItems}
                    mode={mode}
                    portalId={portalId}
                    portalPages={portalPages}
                    getPagePath={getPagePath}
                    onNavigate={onNavigate}
                    onChange={onUserMenuChange}
                    className={styles.userIcon}
                />
            </div>
        </header>
    );
}
