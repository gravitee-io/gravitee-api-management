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
import type { PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { AddLinkDropdown } from './AddLinkDropdown';
import { EditableLinkNavItem } from './EditableLinkNavItem';
import styles from './PortalFooter.module.scss';

interface PortalFooterProps {
    readonly footerItems: readonly PortalNavigationLink[];
    readonly mode: EditorMode;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly onAddLinkFromPage: (page: PortalNavigationPage) => void;
    readonly onUpdateLink: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationLink) => void;
}

export function PortalFooter({
    footerItems,
    mode,
    portalId,
    portalPages,
    onAddLinkFromPage,
    onUpdateLink,
    onRequestDeleteNavItem,
}: PortalFooterProps) {
    const isEditMode = mode === 'edit';

    return (
        <footer className={`${styles.footer} portal-editable-region`}>
            <div className={styles.links}>
                {footerItems.map(item =>
                    isEditMode ? (
                        <EditableLinkNavItem
                            key={item.id}
                            item={item}
                            portalId={portalId}
                            portalPages={portalPages}
                            showDelete
                            variant="footer"
                            className={styles.footerItem}
                            onUpdate={patch => onUpdateLink(item.id, patch)}
                            onDelete={() => onRequestDeleteNavItem(item)}
                        />
                    ) : (
                        <a
                            key={item.id}
                            href={item.url}
                            className={styles.link}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            {item.title}
                        </a>
                    ),
                )}
                {isEditMode && (
                    <AddLinkDropdown portalPages={portalPages} onAddLinkFromPage={onAddLinkFromPage} />
                )}
            </div>
        </footer>
    );
}
