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
import type { DeveloperPortal, PortalNavigationItem, PortalNavigationItemType, PortalNavigationLink } from '../../portals/types';
import { getSidebarRootFolder } from '../utils/sidebar-context';
import { ContentArea, type ContentAreaHandle } from './ContentArea';
import { NavigationSidebar } from './NavigationSidebar';
import { PortalFooter } from './PortalFooter';
import { PortalHeader } from './PortalHeader';
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
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddFooterLink: () => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
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
        onAddFooterLink,
        onRequestDeleteNavItem,
    },
    ref,
) {
    const sidebarRootFolder = getSidebarRootFolder(navItems, selectedNavItemId);

    return (
        <div className={styles.layout}>
            <PortalHeader
                portalIconUrl={portal.portalIconUrl}
                rootItems={rootItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
            <div className={styles.body}>
                {sidebarRootFolder && (
                    <NavigationSidebar
                        rootFolder={sidebarRootFolder}
                        allItems={navItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        onSelectNavItem={onSelectNavItem}
                        onAddNavItem={onAddNavItem}
                        onRequestDeleteNavItem={onRequestDeleteNavItem}
                    />
                )}
                <ContentArea
                    ref={ref}
                    portalId={portal.id}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    pageWidth={pageWidth}
                />
            </div>
            <PortalFooter
                footerItems={footerItems}
                mode={mode}
                onAddLink={onAddFooterLink}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
        </div>
    );
});
