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
import { Button, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';
import { RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useRef } from 'react';

import { uploadFile } from '../../editor/utils/upload';
import type { PortalNavigationFolder, PortalNavigationItem, PortalNavigationItemType } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { InlineEdit } from './InlineEdit';
import { NavigationTree } from './NavigationTree';
import styles from './Sidebar.module.scss';

export type SidebarScope = 'folder' | 'full';

interface SidebarProps {
    readonly scope: SidebarScope;
    readonly rootFolder?: PortalNavigationFolder | null;
    readonly rootItems?: PortalNavigationItem[];
    readonly allItems: PortalNavigationItem[];
    readonly selectedNavItemId: string | null;
    readonly mode: EditorMode;
    readonly portalIconUrl?: string;
    readonly portalLabel?: string;
    readonly onPortalIconChange?: (portalIconUrl: string) => void;
    readonly onPortalLabelChange?: (portalLabel: string) => void;
    readonly onSelectNavItem: (id: string) => void;
    readonly onAddNavItem: (type: PortalNavigationItemType, parentId: string | null) => void;
    readonly onAddApiNavItem: (apiId: string, apiName: string, parentId: string | null) => Promise<void>;
    readonly onRequestDeleteNavItem: (item: PortalNavigationItem) => void;
}

function PortalIconGlyph() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <line x1="2" y1="12" x2="22" y2="12" />
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
        </svg>
    );
}

function UserIconGlyph() {
    return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
        </svg>
    );
}

function PortalIcon({
    portalIconUrl,
    editable,
    onChange,
}: {
    readonly portalIconUrl: string;
    readonly editable: boolean;
    readonly onChange?: (portalIconUrl: string) => void;
}) {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const hasCustomIcon = portalIconUrl.length > 0;

    const iconContent = hasCustomIcon ? (
        <img src={portalIconUrl} alt="Portal" className={styles.portalIcon} />
    ) : (
        <div className={styles.portalIconPlaceholder} aria-label="Portal icon">
            <PortalIconGlyph />
        </div>
    );

    if (!editable) {
        return (
            <div className={styles.portalIconWrapper}>
                <span className={styles.portalIconFrame}>{iconContent}</span>
            </div>
        );
    }

    return (
        <div className={styles.portalIconWrapper}>
            <button
                type="button"
                className={`${styles.portalIconFrame} ${styles.portalIconButton}`}
                aria-label="Change portal icon"
                onClick={() => fileInputRef.current?.click()}
            >
                {iconContent}
            </button>
            {hasCustomIcon && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className={styles.resetOverlay}
                            aria-label="Reset to default"
                            tabIndex={-1}
                            onClick={event => {
                                event.stopPropagation();
                                onChange?.('');
                            }}
                        >
                            <RefreshCwIcon className="size-3.5" aria-hidden />
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent>Reset to default</TooltipContent>
                </Tooltip>
            )}
            <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className={styles.hiddenFileInput}
                onChange={event => {
                    const file = event.target.files?.[0];
                    if (!file || !onChange) {
                        return;
                    }

                    void uploadFile(file).then(onChange);
                    event.target.value = '';
                }}
            />
        </div>
    );
}

export function Sidebar({
    scope,
    rootFolder,
    rootItems = [],
    allItems,
    selectedNavItemId,
    mode,
    portalIconUrl = '',
    portalLabel = DEFAULT_PORTAL_LABEL,
    onPortalIconChange,
    onPortalLabelChange,
    onSelectNavItem,
    onAddNavItem,
    onAddApiNavItem,
    onRequestDeleteNavItem,
}: SidebarProps) {
    const isFullScope = scope === 'full';
    const isFolderScope = scope === 'folder' && rootFolder != null;
    const treeItems = isFolderScope
        ? allItems
              .filter(item => item.parentId === rootFolder.id)
              .sort((left, right) => left.order - right.order)
        : rootItems;

    if (!isFullScope && !isFolderScope) {
        return null;
    }

    return (
        <aside className={styles.sidebar}>
            {isFullScope && (
                <div className={styles.top}>
                    <PortalIcon
                        portalIconUrl={portalIconUrl}
                        editable={mode === 'edit'}
                        onChange={onPortalIconChange}
                    />
                    <InlineEdit
                        value={portalLabel}
                        editable={mode === 'edit'}
                        className={styles.brandLabel}
                        ariaLabel="Portal label"
                        placeholder={DEFAULT_PORTAL_LABEL}
                        onChange={label => onPortalLabelChange?.(label)}
                    />
                </div>
            )}

            <div className={styles.treeRegion}>
                <NavigationTree
                    items={treeItems}
                    allItems={allItems}
                    selectedNavItemId={selectedNavItemId}
                    mode={mode}
                    showRootAddButton={isFullScope || isFolderScope}
                    rootAddParentId={isFolderScope ? rootFolder.id : null}
                    onSelectNavItem={onSelectNavItem}
                    onAddNavItem={onAddNavItem}
                    onAddApiNavItem={onAddApiNavItem}
                    onRequestDeleteNavItem={onRequestDeleteNavItem}
                />
            </div>

            {isFullScope && (
                <div className={styles.bottom}>
                    <Button variant="ghost" size="icon-sm" aria-label="User menu" className={styles.userIcon}>
                        <UserIconGlyph />
                    </Button>
                </div>
            )}
        </aside>
    );
}
