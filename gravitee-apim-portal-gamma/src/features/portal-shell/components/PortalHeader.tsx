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
import { Button } from '@gravitee/graphene-core';

import type { PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { NavItemButton } from './NavItemButton';
import styles from './PortalHeader.module.scss';

interface PortalHeaderProps {
    readonly portalIconUrl: string;
    readonly rootItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

export function PortalHeader({
    portalIconUrl,
    rootItems,
    selectedNavItemId,
    mode,
    onSelectNavItem,
    onAddNavItem,
    onRequestDeleteNavItem,
}: PortalHeaderProps) {
    const isEditMode = mode === 'edit';

    return (
        <header className={styles.header}>
            <div className={styles.left}>
                {portalIconUrl ? (
                    <img src={portalIconUrl} alt="Portal" className={styles.portalIcon} />
                ) : (
                    <div className={styles.portalIconPlaceholder} aria-label="Portal icon">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="2" y1="12" x2="22" y2="12" />
                            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
                        </svg>
                    </div>
                )}
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
                <Button variant="ghost" size="icon-sm" aria-label="User menu" className={styles.userIcon}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                </Button>
            </div>
        </header>
    );
}
