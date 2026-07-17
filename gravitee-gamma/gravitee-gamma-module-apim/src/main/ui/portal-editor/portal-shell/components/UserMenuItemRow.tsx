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
import { useEffect, useRef, useState, type KeyboardEvent, type MouseEvent } from 'react';
import { EyeIcon, EyeOffIcon, XIcon } from '@gravitee/graphene-core/icons';

import type { PortalNavigationItem, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import { InlineEdit } from '../../shared/components/InlineEdit';
import { getNavTypeIcon } from '../utils/nav-type-icons';
import { LinkUrlDropdown } from './LinkUrlDropdown';
import { UnpublishedNavIndicator } from './UnpublishedNavIndicator';
import styles from './UserMenuItemRow.module.scss';

interface UserMenuItemRowProps {
    readonly item: PortalNavigationItem;
    readonly portalId: string;
    readonly portalPages: readonly PortalNavigationPage[];
    readonly onSelect: (item: PortalNavigationItem) => void;
    readonly onUpdateNavItem: (id: string, patch: { title?: string; url?: string }) => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
    readonly unpublished?: boolean;
    readonly onTogglePublished?: (item: PortalNavigationItem) => void;
    readonly publishDisabled?: boolean;
    readonly publishDisabledReason?: string;
}

export function UserMenuItemRow({
    item,
    portalId,
    portalPages,
    onSelect,
    onUpdateNavItem,
    onRequestDeleteNavItem,
    unpublished = false,
    onTogglePublished,
    publishDisabled = false,
    publishDisabledReason,
}: UserMenuItemRowProps) {
    const [isRenaming, setIsRenaming] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const pendingSelectRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastFolderClickRef = useRef(0);

    const cancelPendingSelect = () => {
        if (pendingSelectRef.current != null) {
            clearTimeout(pendingSelectRef.current);
            pendingSelectRef.current = null;
        }
    };

    useEffect(() => () => cancelPendingSelect(), []);

    const handleEditingChange = (editing: boolean) => {
        if (editing) {
            cancelPendingSelect();
        }
        setIsRenaming(editing);
    };

    const handleSelect = () => {
        cancelPendingSelect();
        onSelect(item);
    };

    const handleLabelClick = () => {
        if (isRenaming) {
            return;
        }

        if (item.type === 'LINK') {
            return;
        }

        if (item.type !== 'FOLDER') {
            handleSelect();
            return;
        }

        const now = Date.now();
        if (now - lastFolderClickRef.current < 300) {
            lastFolderClickRef.current = 0;
            cancelPendingSelect();
            return;
        }

        lastFolderClickRef.current = now;
        cancelPendingSelect();
        pendingSelectRef.current = setTimeout(() => {
            pendingSelectRef.current = null;
            onSelect(item);
        }, 300);
    };

    const handleLabelDoubleClick = (event: MouseEvent) => {
        if (item.type === 'FOLDER') {
            lastFolderClickRef.current = 0;
            cancelPendingSelect();
            event.stopPropagation();
        }
    };

    const linkItem = item.type === 'LINK' ? (item as PortalNavigationLink) : null;

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (isRenaming) {
            return;
        }

        if (item.type === 'LINK') {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleSelect();
        }
    };

    const labelButton = (
        <div
            role="button"
            tabIndex={0}
            className={styles.labelButton}
            onClick={handleLabelClick}
            onKeyDown={handleKeyDown}
        >
            <span className={styles.icon} aria-hidden="true">
                {getNavTypeIcon(item.type)}
            </span>
            <span className={styles.labelArea} onDoubleClick={handleLabelDoubleClick}>
                <InlineEdit
                    value={item.title}
                    editable
                    activateOn="doubleClick"
                    onChange={title => onUpdateNavItem(item.id, { title })}
                    onEditingChange={handleEditingChange}
                    ariaLabel={`Edit ${item.title}`}
                    className={styles.inlineLabel}
                />
            </span>
            <UnpublishedNavIndicator show={unpublished} />
        </div>
    );

    return (
        <div
            ref={containerRef}
            className={`${styles.row} ${isRenaming ? styles.renaming : ''} ${unpublished ? styles.unpublished : ''}`}
        >
            {linkItem ? (
                <LinkUrlDropdown
                    url={linkItem.url}
                    portalPages={portalPages}
                    portalId={portalId}
                    onUrlChange={url => onUpdateNavItem(item.id, { url })}
                >
                    {labelButton}
                </LinkUrlDropdown>
            ) : (
                labelButton
            )}

            {!isRenaming && (
                <div className={styles.actions}>
                    {onTogglePublished ? (
                        <button
                            type="button"
                            className={styles.actionButton}
                            aria-label={unpublished ? 'Publish item' : 'Unpublish item'}
                            title={publishDisabled ? publishDisabledReason : undefined}
                            disabled={unpublished && publishDisabled}
                            onClick={event => {
                                event.stopPropagation();
                                onTogglePublished(item);
                            }}
                        >
                            {unpublished ? (
                                <EyeIcon className="size-3.5" aria-hidden="true" />
                            ) : (
                                <EyeOffIcon className="size-3.5" aria-hidden="true" />
                            )}
                        </button>
                    ) : null}
                    <button
                        type="button"
                        className={styles.actionButton}
                        aria-label={`Remove ${item.title}`}
                        onClick={event => {
                            event.stopPropagation();
                            onRequestDeleteNavItem(item);
                        }}
                    >
                        <XIcon className="size-3.5" aria-hidden="true" />
                    </button>
                </div>
            )}
        </div>
    );
}
