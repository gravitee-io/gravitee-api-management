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
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';

import type { PortalNavigationLink } from '../../portals/types';
import type { EditorMode } from '../../editor/stores/editor.store';
import { NavItemButton } from './NavItemButton';
import styles from './PortalFooter.module.scss';

interface PortalFooterProps {
    readonly footerItems: readonly PortalNavigationLink[];
    readonly mode: EditorMode;
    readonly onAddLink: () => void;
    readonly onRequestDeleteNavItem: (item: PortalNavigationLink) => void;
}

export function PortalFooter({ footerItems, mode, onAddLink, onRequestDeleteNavItem }: PortalFooterProps) {
    const isEditMode = mode === 'edit';

    return (
        <footer className={styles.footer}>
            <div className={styles.links}>
                {footerItems.map(item =>
                    isEditMode ? (
                        <NavItemButton
                            key={item.id}
                            label={item.title}
                            selected={false}
                            showDelete
                            variant="footer"
                            className={styles.footerItem}
                            onSelect={() => undefined}
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
                    <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={onAddLink}
                        aria-label="Add footer link"
                    >
                        <PlusIcon className="size-4 text-muted-foreground" />
                    </Button>
                )}
            </div>
        </footer>
    );
}
