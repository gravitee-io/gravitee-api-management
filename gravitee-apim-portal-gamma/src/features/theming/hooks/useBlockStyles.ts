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
import { useCallback, useMemo, useState, type CSSProperties } from 'react';

import type { BlockStyleOverrides, PageBlockStyles } from '../types/block-styles.types';

export interface BlockStylesState {
    readonly getBlockStyle: (blockId: string) => CSSProperties;
    readonly getBlockOverrides: (blockId: string) => BlockStyleOverrides;
    readonly setBlockOverrides: (blockId: string, overrides: BlockStyleOverrides) => void;
    readonly resetBlockOverrides: (blockId: string) => void;
    readonly copyBlockStyle: (blockId: string) => void;
    readonly pasteBlockStyle: (blockId: string) => void;
    readonly hasCopiedStyle: boolean;
    readonly getAllStyles: () => PageBlockStyles;
}

function overridesToCss(overrides: BlockStyleOverrides): CSSProperties {
    const css: CSSProperties = {};
    if (overrides.color) css.color = overrides.color;
    if (overrides.backgroundColor) css.backgroundColor = overrides.backgroundColor;
    if (overrides.fontFamily) css.fontFamily = overrides.fontFamily;
    if (overrides.fontSize) css.fontSize = overrides.fontSize;
    if (overrides.borderRadius) css.borderRadius = overrides.borderRadius;
    if (overrides.borderWidth) css.borderWidth = overrides.borderWidth;
    if (overrides.borderColor) css.borderColor = overrides.borderColor;
    if (overrides.padding) css.padding = overrides.padding;
    if (overrides.maxWidth) css.maxWidth = overrides.maxWidth;
    if (overrides.maxHeight) css.maxHeight = overrides.maxHeight;
    if (overrides.borderWidth || overrides.borderColor) {
        css.borderStyle = 'solid';
    }
    return css;
}

export function useBlockStyles(pageId: string, initial?: PageBlockStyles): BlockStylesState {
    const [blocks, setBlocks] = useState<Record<string, BlockStyleOverrides>>(
        () => initial?.blocks ?? {},
    );
    const [copiedStyle, setCopiedStyle] = useState<BlockStyleOverrides | null>(null);

    const getBlockOverrides = useCallback(
        (blockId: string): BlockStyleOverrides => blocks[blockId] ?? {},
        [blocks],
    );

    const getBlockStyle = useCallback(
        (blockId: string): CSSProperties => overridesToCss(blocks[blockId] ?? {}),
        [blocks],
    );

    const setBlockOverrides = useCallback(
        (blockId: string, overrides: BlockStyleOverrides) => {
            setBlocks(prev => ({ ...prev, [blockId]: { ...prev[blockId], ...overrides } }));
        },
        [],
    );

    const resetBlockOverrides = useCallback(
        (blockId: string) => {
            setBlocks(prev => {
                const next = { ...prev };
                delete next[blockId];
                return next;
            });
        },
        [],
    );

    const copyBlockStyle = useCallback(
        (blockId: string) => {
            setCopiedStyle(blocks[blockId] ?? {});
        },
        [blocks],
    );

    const pasteBlockStyle = useCallback(
        (blockId: string) => {
            if (copiedStyle) {
                setBlocks(prev => ({ ...prev, [blockId]: copiedStyle }));
            }
        },
        [copiedStyle],
    );

    const getAllStyles = useCallback(
        (): PageBlockStyles => ({ pageId, blocks }),
        [pageId, blocks],
    );

    return useMemo(
        () => ({
            getBlockStyle,
            getBlockOverrides,
            setBlockOverrides,
            resetBlockOverrides,
            copyBlockStyle,
            pasteBlockStyle,
            hasCopiedStyle: copiedStyle !== null,
            getAllStyles,
        }),
        [getBlockStyle, getBlockOverrides, setBlockOverrides, resetBlockOverrides, copyBlockStyle, pasteBlockStyle, copiedStyle, getAllStyles],
    );
}
