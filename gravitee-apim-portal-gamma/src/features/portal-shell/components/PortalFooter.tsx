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
import type { PortalNavigationItem, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { canPublishNavItem, isNavItemPublished } from '../utils/nav-items';
import { AddLinkDropdown } from './AddLinkDropdown';
import { EditableLinkNavItem } from './EditableLinkNavItem';
import { NavItemContextMenu } from './NavItemContextMenu';
import styles from './PortalFooter.module.scss';

interface PortalFooterProps {
    readonly footerItems: readonly PortalNavigationLink[];
    readonly allNavItems: readonly PortalNavigationItem[];
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly onAddLinkFromPage: (page: PortalNavigationPage) => void;
    readonly onUpdateLink: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationLink) => void;
    readonly onTogglePublished: (item: PortalNavigationItem) => void;
    readonly instanceOverrides?: Record<string, Record<string, string>>;
}

export function PortalFooter({
    footerItems,
    allNavItems,
    mode,
    portalId,
    portalPages,
    onAddLinkFromPage,
    onUpdateLink,
    onRequestDeleteNavItem,
    onTogglePublished,
    instanceOverrides = {},
}: PortalFooterProps) {
    const isEditMode = mode === 'edit';

    return (
        <footer className={`${styles.footer} portal-editable-region`}>
            <div className={styles.links}>
                {footerItems.map(item => {
                    const isUnpublished = isEditMode && !isNavItemPublished(item);
                    const publishState = canPublishNavItem(item, allNavItems);

                    const linkItem = isEditMode ? (
                        <EditableLinkNavItem
                            item={item}
                            instanceStyle={instanceOverrides[item.id]}
                            portalId={portalId}
                            portalPages={portalPages}
                            showDelete
                            variant="footer"
                            className={styles.footerItem}
                            unpublished={isUnpublished}
                            onUpdate={patch => onUpdateLink(item.id, patch)}
                            onDelete={() => onRequestDeleteNavItem(item)}
                        />
                    ) : (
                        <a
                            href={item.url}
                            className={styles.link}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            {item.title}
                        </a>
                    );

                    if (!isEditMode) {
                        return <span key={item.id}>{linkItem}</span>;
                    }

                    return (
                        <NavItemContextMenu
                            key={item.id}
                            item={item}
                            allItems={allNavItems}
                            enabled
                            isContainer={false}
                            onTogglePublished={onTogglePublished}
                            publishDisabled={!publishState.allowed}
                            publishDisabledReason={publishState.reason}
                        >
                            {linkItem}
                        </NavItemContextMenu>
                    );
                })}
                {isEditMode && (
                    <AddLinkDropdown portalPages={portalPages} onAddLinkFromPage={onAddLinkFromPage} />
                )}
            </div>
        </footer>
    );
}
