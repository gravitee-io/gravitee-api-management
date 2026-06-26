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
import { XIcon } from '@gravitee/graphene-core/icons';

import type { PortalNavigationLink } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { AddButton } from './AddButton';
import { InlineEdit } from './InlineEdit';
import styles from './PortalFooter.module.scss';

interface PortalFooterProps {
    readonly footerItems: readonly PortalNavigationLink[];
    readonly mode: EditorMode;
    readonly onAddLink: () => void;
    readonly onUpdateLink: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationLink) => void;
}

export function PortalFooter({ footerItems, mode, onAddLink, onUpdateLink, onRequestDeleteNavItem }: PortalFooterProps) {
    const isEditMode = mode === 'edit';

    return (
        <footer className={`${styles.footer} portal-editable-region`}>
            <div className={styles.links}>
                {footerItems.map(item =>
                    isEditMode ? (
                        <div key={item.id} className={styles.editItem}>
                            <InlineEdit
                                value={item.title}
                                editable
                                onChange={title => onUpdateLink(item.id, { title })}
                                ariaLabel={`Footer link label: ${item.title}`}
                                className={styles.editLabel}
                            />
                            <InlineEdit
                                value={item.url}
                                editable
                                onChange={url => onUpdateLink(item.id, { url })}
                                ariaLabel={`Footer link URL: ${item.title}`}
                                className={styles.editUrl}
                                placeholder="https://"
                            />
                            <button
                                type="button"
                                className={styles.deleteButton}
                                aria-label={`Delete ${item.title}`}
                                onClick={() => onRequestDeleteNavItem(item)}
                            >
                                <XIcon className="size-3.5" aria-hidden="true" />
                            </button>
                        </div>
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
                    <AddButton aria-label="Add footer link" onClick={onAddLink} />
                )}
            </div>
        </footer>
    );
}
