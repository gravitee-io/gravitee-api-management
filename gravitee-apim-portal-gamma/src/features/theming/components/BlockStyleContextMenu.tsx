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
import {
    ContextMenu,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuTrigger,
} from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { BlockStylesState } from '../hooks/useBlockStyles';
import styles from './BlockStyleContextMenu.module.scss';

interface BlockStyleContextMenuProps {
    readonly blockId: string;
    readonly blockStyles: BlockStylesState;
    readonly children: ReactNode;
}

export function BlockStyleContextMenu({ blockId, blockStyles, children }: BlockStyleContextMenuProps) {
    const hasOverrides = Object.keys(blockStyles.getBlockOverrides(blockId)).length > 0;

    return (
        <ContextMenu>
            <ContextMenuTrigger asChild>
                {children}
            </ContextMenuTrigger>
            <ContextMenuContent className={styles.menu}>
                <ContextMenuItem
                    onClick={() => blockStyles.copyBlockStyle(blockId)}
                >
                    Copy Style
                </ContextMenuItem>
                <ContextMenuItem
                    onClick={() => blockStyles.pasteBlockStyle(blockId)}
                    disabled={!blockStyles.hasCopiedStyle}
                >
                    Paste Style
                </ContextMenuItem>
                {hasOverrides && (
                    <ContextMenuItem
                        onClick={() => blockStyles.resetBlockOverrides(blockId)}
                        className={styles.destructive}
                    >
                        Reset to Theme Defaults
                    </ContextMenuItem>
                )}
            </ContextMenuContent>
        </ContextMenu>
    );
}
