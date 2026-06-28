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
import type { PortalNavigationItem, PortalNavigationItemType, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { EditableLinkNavItem, PreviewLinkNavItem } from './EditableLinkNavItem';
import { NavItemButton } from './NavItemButton';
import { PortalIconEditor } from './PortalIconEditor';
import { UserMenu, type UserMenuShellProps } from './UserMenu';
import styles from './PortalHeader.module.scss';

interface PortalHeaderProps {
    readonly portalId: string;
    readonly portalIconUrl: string;
    readonly rootItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddLinkFromPage: (page: PortalNavigationPage, parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly userMenuProps: UserMenuShellProps;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
}

export function PortalHeader({
    portalId,
    portalIconUrl,
    rootItems,
    selectedNavItemId,
    mode,
    onSelectNavItem,
    onAddNavItem,
    onAddLinkFromPage,
    onUpdateNavItem,
    onPortalIconChange,
    onRequestDeleteNavItem,
    userMenuProps,
    portalPages,
    getPagePath,
    onNavigate,
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
                {rootItems.map(item =>
                    item.type === 'LINK' && isEditMode ? (
                        <EditableLinkNavItem
                            key={item.id}
                            item={item as PortalNavigationLink}
                            portalId={portalId}
                            portalPages={portalPages}
                            selected={selectedNavItemId === item.id}
                            showDelete
                            variant="header"
                            className={styles.navButton}
                            onUpdate={patch => onUpdateNavItem(item.id, patch)}
                            onDelete={() => onRequestDeleteNavItem(item)}
                        />
                    ) : item.type === 'LINK' ? (
                        <PreviewLinkNavItem
                            key={item.id}
                            label={item.title}
                            selected={selectedNavItemId === item.id}
                            variant="header"
                            className={styles.navButton}
                            onSelect={() => onSelectNavItem(item.id)}
                        />
                    ) : (
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
                    ),
                )}
                {isEditMode && (
                    <AddNavItemDropdown
                        allowedTypes={['FOLDER', 'PAGE', 'LINK']}
                        parentId={null}
                        onAdd={onAddNavItem}
                        portalPages={portalPages}
                        onAddLinkFromPage={onAddLinkFromPage}
                    />
                )}
            </nav>

            <div className={styles.right}>
                <UserMenu
                    {...userMenuProps}
                    mode={mode}
                    portalId={portalId}
                    portalPages={portalPages}
                    getPagePath={getPagePath}
                    onNavigate={onNavigate}
                    className={styles.userIcon}
                />
            </div>
        </header>
    );
}
