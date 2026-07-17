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
import { forwardRef, useState } from 'react';

import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type {
    DeveloperPortal,
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationPage,
} from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import type { AddPageOptions } from '../utils/page-type-options';
import type { UpdateNavItemPatch } from '../hooks/useNavigation';
import { ContentArea, type ContentAreaHandle } from './ContentArea';
import { NotFoundPage } from '../../shared/components/NotFoundPage';
import { MobileNavDrawer } from './MobileNavDrawer';
import { MobileNavTree } from './MobileNavTree';
import { Sidebar } from './Sidebar';
import type { UserMenuShellProps } from './UserMenu';
import { findFirstPageNavItem } from '../../portals/utils/slug';
import { getSidebarRootFolder } from '../utils/sidebar-context';
import { belongsToUserMenu, sortNavItemsByOrder } from '../utils/nav-items';
import styles from './SidebarLayout.module.scss';

interface SidebarLayoutProps {
    readonly portal: DeveloperPortal;
    readonly navItems: PortalNavigationItem[];
    readonly rootItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onAddLinkFromPage: (page: PortalNavigationPage, parentId: string | null) => void;
    readonly onUpdateNavItem: (id: string, patch: UpdateNavItemPatch) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onTogglePublished: (item: PortalNavigationItem) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onPortalLabelChange: (portalLabel: string) => void;
    readonly userMenuProps: UserMenuShellProps;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly notFoundHomePath?: string;
    readonly instanceOverrides?: Record<string, Record<string, string>>;
    readonly isDark?: boolean;
}

export const SidebarLayout = forwardRef<ContentAreaHandle, SidebarLayoutProps>(function SidebarLayout(
    {
        portal,
        navItems,
        rootItems,
        selectedNavItemId,
        mode,
        pageWidth,
        onSelectNavItem,
        onAddNavItem,
        onAddApiNavItem,
        onAddLinkFromPage,
        onUpdateNavItem,
        onRequestDeleteNavItem,
        onTogglePublished,
        onPortalIconChange,
        onPortalLabelChange,
        userMenuProps,
        portalPages,
        getPagePath,
        onNavigate,
        notFoundHomePath,
        instanceOverrides = {},
        isDark = false,
    },
    ref,
) {
    const sidebarRootFolder = getSidebarRootFolder(navItems, selectedNavItemId);
    const userMenuFolder =
        sidebarRootFolder && belongsToUserMenu(sidebarRootFolder, navItems)
            ? sidebarRootFolder
            : null;
    const [mobileNavOpen, setMobileNavOpen] = useState(false);
    const mobileTreeItems = sortNavItemsByOrder(
        userMenuFolder
            ? navItems.filter(item => item.parentId === userMenuFolder.id)
            : rootItems,
    );

    return (
        <div className={styles.layout}>
            <div className={styles.mobileTopBar} aria-hidden="true">
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
                <span className={styles.mobileLabel}>{portal.portalLabel || DEFAULT_PORTAL_LABEL}</span>
            </div>

            <MobileNavDrawer open={mobileNavOpen} onClose={() => setMobileNavOpen(false)}>
                <MobileNavTree
                    items={mobileTreeItems}
                    allItems={navItems}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    portalId={portal.id}
                    portalPages={portalPages}
                    rootAddParentId={userMenuFolder?.id ?? null}
                    onSelectNavItem={onSelectNavItem}
                    onAddNavItem={onAddNavItem}
                    onAddApiNavItem={onAddApiNavItem}
                    onAddLinkFromPage={onAddLinkFromPage}
                    onUpdateNavItem={onUpdateNavItem}
                    onRequestDeleteNavItem={onRequestDeleteNavItem}
                    onTogglePublished={onTogglePublished}
                    onItemSelect={() => setMobileNavOpen(false)}
                    instanceOverrides={instanceOverrides}
                />
            </MobileNavDrawer>

            <div className={styles.mainRow}>
            <div className={styles.sidebarColumn}>
                <Sidebar
                scope={userMenuFolder ? 'folder' : 'full'}
                rootFolder={userMenuFolder ?? undefined}
                rootItems={rootItems}
                allItems={navItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                portalId={portal.id}
                portalIconUrl={portal.portalIconUrl}
                portalLabel={portal.portalLabel}
                userMenuProps={userMenuProps}
                portalPages={portalPages}
                getPagePath={getPagePath}
                onNavigate={onNavigate}
                onPortalIconChange={onPortalIconChange}
                onPortalLabelChange={onPortalLabelChange}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddApiNavItem={onAddApiNavItem}
                onAddLinkFromPage={onAddLinkFromPage}
                onUpdateNavItem={onUpdateNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
                onTogglePublished={onTogglePublished}
                showSidebarChrome={userMenuFolder != null}
                instanceOverrides={instanceOverrides}
                onBackToMainNavigation={
                    userMenuFolder
                        ? () => {
                              const firstPage = findFirstPageNavItem(navItems);
                              if (firstPage) {
                                  onSelectNavItem(firstPage.id);
                              }
                          }
                        : undefined
                }
            />
            </div>
            <div className={styles.content}>
                {notFoundHomePath ? (
                    <NotFoundPage homePath={notFoundHomePath} />
                ) : (
                    <ContentArea
                        ref={ref}
                        portalId={portal.id}
                        selectedNavItemId={selectedNavItemId}
                        navItems={navItems}
                        mode={mode}
                        pageWidth={pageWidth}
                        isDark={isDark}
                        onUpdateNavItem={onUpdateNavItem}
                        onSelectNavItem={onSelectNavItem}
                    />
                )}
            </div>
            </div>
        </div>
    );
});
