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

import type { PortalNavigationItem, PortalNavigationItemType, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { toInstanceInlineStyle } from '../../theming/utils/instance-style';
import { getAllowedAddNavItemTypes, handleAddNavItemSelection } from '../utils/add-nav-item-menu';
import type { AddPageOptions } from '../utils/page-type-options';
import { AddNavItemDropdown } from './AddNavItemDropdown';
import { ApiSelectionDialog } from './ApiSelectionDialog';
import { EditableLinkNavItem, PreviewLinkNavItem } from './EditableLinkNavItem';
import { MobileNavDrawer } from './MobileNavDrawer';
import { MobileNavTree } from './MobileNavTree';
import { NavItemButton } from './NavItemButton';
import { PortalIconEditor } from './PortalIconEditor';
import { UserMenu, type UserMenuShellProps } from './UserMenu';
import styles from './PortalHeader.module.scss';

interface PortalHeaderProps {
    readonly portalId: string;
    readonly portalIconUrl: string;
    readonly rootItems: PortalNavigationItem[];
    readonly mobileDrawerTreeItems?: PortalNavigationItem[];
    readonly mobileDrawerTreeParentId?: string | null;
    readonly allNavItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onAddLinkFromPage: (page: PortalNavigationPage, parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly userMenuProps: UserMenuShellProps;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly onAddApiNavItem?: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly instanceOverrides?: Record<string, Record<string, string>>;
}

export function PortalHeader({
    portalId,
    portalIconUrl,
    rootItems,
    mobileDrawerTreeItems = [],
    mobileDrawerTreeParentId = null,
    allNavItems,
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
    onAddApiNavItem = async () => undefined,
    instanceOverrides = {},
}: PortalHeaderProps) {
    const isEditMode = mode === 'edit';
    const [mobileNavOpen, setMobileNavOpen] = useState(false);
    const [apiDialogParentId, setApiDialogParentId] = useState<string | null | undefined>(undefined);
    const hasMobileDrawerTree = mobileDrawerTreeItems.length > 0;
    const allowedRootTypes = getAllowedAddNavItemTypes(allNavItems, null);

    const handleRequestApi = useCallback((parentId: string | null) => {
        setApiDialogParentId(parentId);
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

    const handleAddNavItem = useCallback(
        (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => {
            if (pageOptions) {
                onAddNavItem(type, parentId, pageOptions);
                return;
            }
            handleAddNavItemSelection(type, parentId, onAddNavItem, handleRequestApi, () => undefined);
        },
        [handleRequestApi, onAddNavItem],
    );

    const handleMobileSelect = (id: string) => {
        onSelectNavItem(id);
        setMobileNavOpen(false);
    };

    const renderRootNavItem = (item: PortalNavigationItem, onSelect: (id: string) => void) => {
        if (item.type === 'LINK' && isEditMode) {
            return (
                <EditableLinkNavItem
                    key={item.id}
                    item={item as PortalNavigationLink}
                    instanceStyle={instanceOverrides[item.id]}
                    portalId={portalId}
                    portalPages={portalPages}
                    selected={selectedNavItemId === item.id}
                    showDelete
                    variant="header"
                    className={styles.navButton}
                    onUpdate={patch => onUpdateNavItem(item.id, patch)}
                    onDelete={() => onRequestDeleteNavItem(item)}
                />
            );
        }

        if (item.type === 'LINK') {
            return (
                <PreviewLinkNavItem
                    key={item.id}
                    navItemId={item.id}
                    instanceStyle={instanceOverrides[item.id]}
                    label={item.title}
                    selected={selectedNavItemId === item.id}
                    variant="header"
                    className={styles.navButton}
                    onSelect={() => onSelect(item.id)}
                />
            );
        }

        return (
            <NavItemButton
                key={item.id}
                navItemId={item.id}
                instanceStyle={instanceOverrides[item.id]}
                label={item.title}
                selected={selectedNavItemId === item.id}
                showDelete={isEditMode}
                variant="header"
                className={styles.navButton}
                onSelect={() => onSelect(item.id)}
                onDelete={() => onRequestDeleteNavItem(item)}
                onLabelChange={isEditMode ? title => onUpdateNavItem(item.id, { title }) : undefined}
            />
        );
    };

    return (
        <>
            <header
                className={`${styles.header} portal-editable-region`}
                data-style-target="header"
                data-instance-id="shell:header"
                style={toInstanceInlineStyle(instanceOverrides['shell:header'])}
            >
                <div className={styles.left}>
                    <button
                        type="button"
                        className={styles.menuButton}
                        onClick={() => setMobileNavOpen(true)}
                        aria-label="Open navigation menu"
                    >
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                            <path d="M4 5h16" />
                            <path d="M4 12h16" />
                            <path d="M4 19h16" />
                        </svg>
                    </button>
                    <PortalIconEditor
                        portalIconUrl={portalIconUrl}
                        editable={isEditMode}
                        onChange={onPortalIconChange}
                    />
                </div>

                <nav className={styles.nav}>
                    {rootItems.map(item => renderRootNavItem(item, onSelectNavItem))}
                    {isEditMode && (
                        <AddNavItemDropdown
                            allowedTypes={allowedRootTypes}
                            parentId={null}
                            onAdd={handleAddNavItem}
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

            <MobileNavDrawer open={mobileNavOpen} onClose={() => setMobileNavOpen(false)}>
                <div className={styles.mobileNavSection}>
                    {rootItems.map(item => (
                        <div key={item.id} className={styles.mobileNavItem}>
                            {renderRootNavItem(item, handleMobileSelect)}
                        </div>
                    ))}
                </div>
                {hasMobileDrawerTree && (
                    <>
                        <div className={styles.mobileNavDivider} />
                        <MobileNavTree
                            items={mobileDrawerTreeItems}
                            allItems={allNavItems}
                            selectedNavItemId={selectedNavItemId}
                            mode={mode}
                            portalId={portalId}
                            portalPages={portalPages}
                            rootAddParentId={mobileDrawerTreeParentId}
                            onSelectNavItem={onSelectNavItem}
                            onAddNavItem={onAddNavItem}
                            onAddApiNavItem={onAddApiNavItem}
                            onAddLinkFromPage={onAddLinkFromPage}
                            onUpdateNavItem={onUpdateNavItem}
                            onRequestDeleteNavItem={onRequestDeleteNavItem}
                            onItemSelect={() => setMobileNavOpen(false)}
                            instanceOverrides={instanceOverrides}
                        />
                    </>
                )}
            </MobileNavDrawer>

            <ApiSelectionDialog
                open={apiDialogParentId !== undefined}
                onOpenChange={open => {
                    if (!open) {
                        setApiDialogParentId(undefined);
                    }
                }}
                onSelect={handleApiSelected}
            />
        </>
    );
}
