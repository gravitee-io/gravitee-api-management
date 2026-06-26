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
import { forwardRef, useCallback, useMemo, useState } from 'react';

import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type { DeveloperPortal, PortalLayout, PortalNavigationItem, PortalNavigationItemType, UserMenuItem } from '../../portals/types';
import { notify } from '../../../shared/notify/notify';
import { useNavigation } from '../hooks/useNavigation';
import { getPortalPages } from '../utils/portal-pages';
import { DeleteNavItemDialog } from './DeleteNavItemDialog';
import { type ContentAreaHandle } from './ContentArea';
import { HeaderLayout } from './HeaderLayout';
import { SidebarLayout } from './SidebarLayout';
import styles from './PortalShell.module.scss';

interface PortalShellProps {
    readonly portal: DeveloperPortal;
    readonly layout: PortalLayout;
    readonly mode: EditorMode;
    readonly pageWidth: PageWidth;
    readonly onPortalChange: (portal: DeveloperPortal) => void;
    readonly slug?: string;
    readonly getPagePath?: (slug: string) => string;
    readonly onNavigate?: (path: string, options?: { replace?: boolean }) => void;
}

export const PortalShell = forwardRef<ContentAreaHandle, PortalShellProps>(function PortalShell(
    { portal, layout, mode, pageWidth, onPortalChange, slug, getPagePath, onNavigate },
    ref,
) {
    const {
        navItems,
        selectedNavItemId,
        loading,
        selectNavItem,
        addNavItem,
        addApiNavItem,
        addFooterLink,
        deleteNavItem,
        getRootItems,
        getFooterItems,
    } = useNavigation(portal.id, { slug, getPagePath, onNavigate });
    const [deleteTarget, setDeleteTarget] = useState<PortalNavigationItem | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const handleAddNavItem = useCallback(
        async (type: PortalNavigationItemType, parentId: string | null) => {
            await addNavItem(type, parentId);
        },
        [addNavItem],
    );

    const handleAddApiNavItem = useCallback(
        async (apiId: string, apiName: string, parentId: string | null) => {
            await addApiNavItem(apiId, apiName, parentId);
        },
        [addApiNavItem],
    );

    const handleAddFooterLink = useCallback(async () => {
        await addFooterLink();
    }, [addFooterLink]);

    const handleConfirmDelete = useCallback(async () => {
        if (!deleteTarget) {
            return;
        }

        const title = deleteTarget.title;
        setIsDeleting(true);
        try {
            await deleteNavItem(deleteTarget.id);
            notify.success(`Navigation item "${title}" deleted`);
            setDeleteTarget(null);
        } catch (error) {
            notify.error(error, 'Failed to delete navigation item');
        } finally {
            setIsDeleting(false);
        }
    }, [deleteNavItem, deleteTarget]);

    const handlePortalIconChange = useCallback(
        (portalIconUrl: string) => {
            onPortalChange({ ...portal, portalIconUrl });
        },
        [onPortalChange, portal],
    );

    const handlePortalLabelChange = useCallback(
        (portalLabel: string) => {
            onPortalChange({ ...portal, portalLabel });
        },
        [onPortalChange, portal],
    );

    const handleUserMenuChange = useCallback(
        (userMenuItems: UserMenuItem[]) => {
            onPortalChange({ ...portal, userMenuItems });
        },
        [onPortalChange, portal],
    );

    const portalPages = useMemo(() => getPortalPages(navItems), [navItems]);
    const resolvePagePath = useCallback(
        (pageSlug: string) => getPagePath?.(pageSlug) ?? `/portals/${portal.id}/${pageSlug}`,
        [getPagePath, portal.id],
    );

    if (loading) {
        return (
            <div className={styles.shell}>
                <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>
            </div>
        );
    }

    const rootItems = getRootItems();
    const footerItems = getFooterItems();

    return (
        <>
            <div className={styles.shell} data-mode={mode}>
                {layout === 'header-content-footer' ? (
                    <HeaderLayout
                        ref={ref}
                        portal={portal}
                        navItems={navItems}
                        rootItems={rootItems}
                        footerItems={footerItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        pageWidth={pageWidth}
                        onSelectNavItem={selectNavItem}
                        onAddNavItem={handleAddNavItem}
                        onAddApiNavItem={handleAddApiNavItem}
                        onAddFooterLink={handleAddFooterLink}
                        onRequestDeleteNavItem={setDeleteTarget}
                        onUserMenuChange={handleUserMenuChange}
                        portalPages={portalPages}
                        getPagePath={resolvePagePath}
                        onNavigate={onNavigate}
                    />
                ) : (
                    <SidebarLayout
                        ref={ref}
                        portal={portal}
                        navItems={navItems}
                        rootItems={rootItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        pageWidth={pageWidth}
                        onSelectNavItem={selectNavItem}
                        onAddNavItem={handleAddNavItem}
                        onAddApiNavItem={handleAddApiNavItem}
                        onRequestDeleteNavItem={setDeleteTarget}
                        onPortalIconChange={handlePortalIconChange}
                        onPortalLabelChange={handlePortalLabelChange}
                        onUserMenuChange={handleUserMenuChange}
                        portalPages={portalPages}
                        getPagePath={resolvePagePath}
                        onNavigate={onNavigate}
                    />
                )}
            </div>

            <DeleteNavItemDialog
                item={deleteTarget}
                allItems={navItems}
                open={deleteTarget !== null}
                isPending={isDeleting}
                onOpenChange={open => {
                    if (!open && !isDeleting) {
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => void handleConfirmDelete()}
            />
        </>
    );
});
