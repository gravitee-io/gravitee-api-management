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
import type { DeveloperPortal, PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import { ContentArea, type ContentAreaHandle } from './ContentArea';
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
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly onPortalIconChange: (portalIconUrl: string) => void;
    readonly onPortalLabelChange: (portalLabel: string) => void;
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
        onRequestDeleteNavItem,
        onPortalIconChange,
        onPortalLabelChange,
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
                portalIconUrl={portal.portalIconUrl}
                portalLabel={portal.portalLabel}
                onPortalIconChange={onPortalIconChange}
                onPortalLabelChange={onPortalLabelChange}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={onAddNavItem}
                onAddApiNavItem={onAddApiNavItem}
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />
            <div className={styles.content}>
                <ContentArea
                    ref={ref}
                    portalId={portal.id}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    pageWidth={pageWidth}
                />
            </div>
        </div>
    );
});
