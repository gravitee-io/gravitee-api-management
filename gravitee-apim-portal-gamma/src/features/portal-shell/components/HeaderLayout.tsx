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
import { forwardRef } from 'react';

import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type {
    DeveloperPortal,
    PortalNavigationItem,
    PortalNavigationItemType,
    PortalNavigationLink,
    PortalNavigationPage,
} from '../../portals/types';
import type { AddPageOptions } from '../utils/page-type-options';
import type { UpdateNavItemPatch } from '../hooks/useNavigation';
import { getSidebarRootFolder } from '../utils/sidebar-context';
import { ContentArea, type ContentAreaHandle } from './ContentArea';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { Sidebar } from './Sidebar';
import { PortalFooter } from './PortalFooter';
import { PortalHeader } from './PortalHeader';
import type { UserMenuShellProps } from './UserMenu';
import styles from './HeaderLayout.module.scss';

interface HeaderLayoutProps {
    readonly portal: DeveloperPortal;
    readonly navItems: PortalNavigationItem[];
    readonly rootItems: PortalNavigationItem[];
    readonly footerItems: readonly PortalNavigationLink[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onAddLinkFromPage: (page: PortalNavigationPage, parentId: string | null) => void;
    readonly onAddFooterLinkFromPage: (page: PortalNavigationPage) => void;
    readonly onUpdateNavItem: (id: string, patch: UpdateNavItemPatch) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly userMenuProps: UserMenuShellProps;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly notFoundHomePath?: string;
}

export const HeaderLayout = forwardRef<ContentAreaHandle, HeaderLayoutProps>(function HeaderLayout(
    {
        portal,
        navItems,
        rootItems,
        footerItems,
        selectedNavItemId,
        mode,
        pageWidth,
        onSelectNavItem,
        onAddNavItem,
        onAddApiNavItem,
        onAddLinkFromPage,
        onAddFooterLinkFromPage,
        onUpdateNavItem,
        onPortalIconChange,
        onRequestDeleteNavItem,
        userMenuProps,
        portalPages,
        getPagePath,
        onNavigate,
        notFoundHomePath,
    },
    ref,
) {
    const sidebarRootFolder = getSidebarRootFolder(navItems, selectedNavItemId);

    return (
        <div className={styles.layout}>
            <PortalHeader
                portalId={portal.id}
                portalIconUrl={portal.portalIconUrl}
                rootItems={rootItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                portalPages={portalPages}
                getPagePath={getPagePath}
                onNavigate={onNavigate}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddLinkFromPage={onAddLinkFromPage}
                onUpdateNavItem={onUpdateNavItem}
                onPortalIconChange={onPortalIconChange}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
                userMenuProps={userMenuProps}
            />
            <div className={styles.body}>
                {sidebarRootFolder && (
                    <Sidebar
                        scope="folder"
                        rootFolder={sidebarRootFolder}
                        allItems={navItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        portalId={portal.id}
                        portalPages={portalPages}
                        onSelectNavItem={onSelectNavItem}
                        onAddNavItem={onAddNavItem}
                        onAddApiNavItem={onAddApiNavItem}
                        onAddLinkFromPage={onAddLinkFromPage}
                        onUpdateNavItem={onUpdateNavItem}
                        onRequestDeleteNavItem={onRequestDeleteNavItem}
                    />
                )}
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
                        onUpdateNavItem={onUpdateNavItem}
                    />
                )}
            </div>
            <PortalFooter
                footerItems={footerItems}
                mode={mode}
                portalId={portal.id}
                portalPages={portalPages}
                onAddLinkFromPage={onAddFooterLinkFromPage}
                onUpdateLink={onUpdateNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
        </div>
    );
});
