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
import type { DeveloperPortal, PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage, UserMenuItem } from '../../portals/types';
import { ContentArea, type ContentAreaHandle } from './ContentArea';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { Sidebar } from './Sidebar';
import styles from './SidebarLayout.module.scss';

interface SidebarLayoutProps {
    readonly portal: DeveloperPortal;
    readonly navItems: PortalNavigationItem[];
    readonly rootItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onUpdateNavItem: (id: string, patch: { title?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onPortalLabelChange: (portalLabel: string) => void;
    readonly onUserMenuChange: (items: UserMenuItem[]) => void;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly getPagePath: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
    readonly notFoundHomePath?: string;
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
        onUpdateNavItem,
        onRequestDeleteNavItem,
        onPortalIconChange,
        onPortalLabelChange,
        onUserMenuChange,
        portalPages,
        getPagePath,
        onNavigate,
        notFoundHomePath,
    },
    ref,
) {
    return (
        <div className={styles.layout}>
            <Sidebar
                scope="full"
                rootItems={rootItems}
                allItems={navItems}
                selectedNavItemId={selectedNavItemId}
                mode={mode}
                portalId={portal.id}
                portalIconUrl={portal.portalIconUrl}
                portalLabel={portal.portalLabel}
                userMenuItems={portal.userMenuItems}
                portalPages={portalPages}
                getPagePath={getPagePath}
                onNavigate={onNavigate}
                onPortalIconChange={onPortalIconChange}
                onPortalLabelChange={onPortalLabelChange}
                onUserMenuChange={onUserMenuChange}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddApiNavItem={onAddApiNavItem}
                onUpdateNavItem={onUpdateNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
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
                    />
                )}
            </div>
        </div>
    );
});
