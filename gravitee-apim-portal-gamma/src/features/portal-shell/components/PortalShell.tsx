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
import { forwardRef, useCallback, useImperativeHandle, useMemo, useRef, useState } from 'react';

import { capturePortalScreenshot } from '../../portals/utils/capturePortalScreenshot';
import { createDefaultPortalScreenshot } from '../../portals/storage/dummy-portals';

import '../../editor/styles/edit-mode.scss';
import '../../theming/engine/graphene-bridge.css';
import type { PageWidth } from '../../editor/constants/page-width';
import type { EditorMode } from '../../editor/stores/editor.store';
import type { DeveloperPortal, PortalLayout, PortalNavigationArea, PortalNavigationItem, PortalNavigationItemType, PortalNavigationPage } from '../../portals/types';
import type { AddPageOptions } from '../utils/page-type-options';
import type { PortalTheme } from '../../theming/types';
import { useThemeInjection } from '../../theming/hooks/useThemeInjection';
import { notify } from '../../../shared/notify/notify';
import { useNavigation, type UpdateNavItemPatch } from '../hooks/useNavigation';
import { getPortalPages } from '../utils/portal-pages';
import { DeleteNavItemDialog } from './DeleteNavItemDialog';
import { type ContentAreaHandle } from './ContentArea';
import type { PortalShellHandle } from './PortalShellHandle';
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
    readonly theme?: PortalTheme | null;
    readonly themeReady?: boolean;
    readonly isDark?: boolean;
}

export const PortalShell = forwardRef<PortalShellHandle, PortalShellProps>(function PortalShell(
    { portal, layout, mode, pageWidth, onPortalChange, slug, getPagePath, onNavigate, theme, themeReady = true, isDark = false },
    ref,
) {
    const shellRef = useRef<HTMLDivElement>(null);
    const [shellEl, setShellEl] = useState<HTMLDivElement | null>(null);
    const setShellRef = useCallback((node: HTMLDivElement | null) => {
        shellRef.current = node;
        setShellEl(node);
    }, []);
    useThemeInjection(shellEl, theme, isDark, themeReady);
    const contentAreaRef = useRef<ContentAreaHandle>(null);
    const {
        navItems,
        selectedNavItemId,
        loading,
        pageNotFound,
        selectNavItem,
        addNavItem,
        addApiNavItem,
        addLinkFromPage,
        addUserMenuNavItem,
        addUserMenuLinkFromPage,
        deleteNavItem,
        updateNavItem,
        getRootItems,
        getFooterItems,
        getUserMenuRootItems,
        hasUserMenuItems,
    } = useNavigation(portal.id, {
        slug,
        getPagePath,
        onNavigate,
        portal,
        onPortalChange,
    });
    const [deleteTarget, setDeleteTarget] = useState<PortalNavigationItem | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const handleAddNavItem = useCallback(
        async (type: PortalNavigationItemType, parentId: string | null, pageOptions?: AddPageOptions) => {
            await addNavItem(type, parentId, 'HEADER', pageOptions);
        },
        [addNavItem],
    );

    const handleAddApiNavItem = useCallback(
        async (apiId: string, apiName: string, parentId: string | null) => {
            try {
                await addApiNavItem(apiId, apiName, parentId);
            } catch (error) {
                notify.error(error, 'Failed to add API navigation item');
            }
        },
        [addApiNavItem],
    );

    const handleAddLinkFromPage = useCallback(
        async (page: PortalNavigationPage, parentId: string | null, area: PortalNavigationArea) => {
            try {
                await addLinkFromPage(page, parentId, area);
            } catch (error) {
                notify.error(error, 'Failed to add link');
            }
        },
        [addLinkFromPage],
    );

    const handleAddHeaderLinkFromPage = useCallback(
        (page: PortalNavigationPage, parentId: string | null) => {
            void handleAddLinkFromPage(page, parentId, 'HEADER');
        },
        [handleAddLinkFromPage],
    );

    const handleAddFooterLinkFromPage = useCallback(
        (page: PortalNavigationPage) => {
            void handleAddLinkFromPage(page, null, 'FOOTER');
        },
        [handleAddLinkFromPage],
    );

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

    const handleAddUserMenuNavItem = useCallback(
        async (type: PortalNavigationItemType, parentId: string | null) => {
            try {
                await addUserMenuNavItem(type, parentId);
            } catch (error) {
                notify.error(error, 'Failed to add user menu item');
            }
        },
        [addUserMenuNavItem],
    );

    const handleAddUserMenuLink = useCallback(
        async (page: Parameters<typeof addUserMenuLinkFromPage>[0], parentId: string | null) => {
            try {
                await addUserMenuLinkFromPage(page, parentId);
            } catch (error) {
                notify.error(error, 'Failed to add user menu link');
            }
        },
        [addUserMenuLinkFromPage],
    );

    const handleUpdateNavItem = useCallback(
        (id: string, patch: UpdateNavItemPatch) => {
            void updateNavItem(id, patch);
        },
        [updateNavItem],
    );

    const portalPages = useMemo(() => getPortalPages(navItems), [navItems]);
    const resolvePagePath = useCallback(
        (pageSlug: string) => getPagePath?.(pageSlug) ?? `/portals/${portal.id}/${pageSlug}`,
        [getPagePath, portal.id],
    );
    const portalHomePath = mode === 'edit' ? `/portals/${portal.id}/edit` : `/portals/${portal.id}`;

    useImperativeHandle(
        ref,
        () => ({
            save: async () => {
                await contentAreaRef.current?.save();
            },
            captureScreenshot: async () => {
                const shellElement = shellRef.current;
                if (!shellElement) {
                    return createDefaultPortalScreenshot(portal.name);
                }

                return capturePortalScreenshot(shellElement, portal.name);
            },
            bindInstanceStyle: (blockId, prop, customVarName) => {
                contentAreaRef.current?.bindInstanceStyle(blockId, prop, customVarName);
            },
            unbindInstanceStyle: (blockId, prop) => {
                contentAreaRef.current?.unbindInstanceStyle(blockId, prop);
            },
            getInstanceStyle: blockId => contentAreaRef.current?.getInstanceStyle(blockId) ?? {},
        }),
        [portal.name],
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
    const userMenuRootItems = getUserMenuRootItems();
    const userMenuHasItems = hasUserMenuItems();

    const userMenuProps = {
        userMenuRootItems,
        allNavItems: navItems,
        hasUserMenuItems: userMenuHasItems,
        onAddUserMenuNavItem: handleAddUserMenuNavItem,
        onAddUserMenuLink: handleAddUserMenuLink,
        onUpdateNavItem: handleUpdateNavItem,
        onRequestDeleteNavItem: setDeleteTarget,
        onSelectNavItem: selectNavItem,
    };

    return (
        <>
            <div
                ref={setShellRef}
                className={`portal-scope ${styles.shell} ${mode === 'edit' ? 'edit-mode' : ''}`}
                data-mode={mode}
            >
                {layout === 'header-content-footer' ? (
                    <HeaderLayout
                        ref={contentAreaRef}
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
                        onAddLinkFromPage={handleAddHeaderLinkFromPage}
                        onAddFooterLinkFromPage={handleAddFooterLinkFromPage}
                        onUpdateNavItem={handleUpdateNavItem}
                        onPortalIconChange={handlePortalIconChange}
                        onRequestDeleteNavItem={setDeleteTarget}
                        userMenuProps={userMenuProps}
                        portalPages={portalPages}
                        getPagePath={resolvePagePath}
                        onNavigate={onNavigate}
                        notFoundHomePath={pageNotFound ? portalHomePath : undefined}
                        instanceOverrides={theme?.instanceOverrides ?? {}}
                        isDark={isDark}
                    />
                ) : (
                    <SidebarLayout
                        ref={contentAreaRef}
                        portal={portal}
                        navItems={navItems}
                        rootItems={rootItems}
                        selectedNavItemId={selectedNavItemId}
                        mode={mode}
                        pageWidth={pageWidth}
                        onSelectNavItem={selectNavItem}
                        onAddNavItem={handleAddNavItem}
                        onAddApiNavItem={handleAddApiNavItem}
                        onAddLinkFromPage={handleAddHeaderLinkFromPage}
                        onUpdateNavItem={handleUpdateNavItem}
                        onRequestDeleteNavItem={setDeleteTarget}
                        onPortalIconChange={handlePortalIconChange}
                        onPortalLabelChange={handlePortalLabelChange}
                        userMenuProps={userMenuProps}
                        portalPages={portalPages}
                        getPagePath={resolvePagePath}
                        onNavigate={onNavigate}
                        notFoundHomePath={pageNotFound ? portalHomePath : undefined}
                        instanceOverrides={theme?.instanceOverrides ?? {}}
                        isDark={isDark}
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
