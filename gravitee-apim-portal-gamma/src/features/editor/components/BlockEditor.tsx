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
import '../styles/blocknote.css';
import { BlockNoteView } from '@blocknote/mantine';
import {
    SuggestionMenuController,
    getDefaultReactSlashMenuItems,
    useCreateBlockNote,
} from '@blocknote/react';
import { combineByGroup } from '@blocknote/core';
import {
    filterSuggestionItems,
    insertOrUpdateBlockForSlashMenu,
} from '@blocknote/core/extensions';
import {
    multiColumnDropCursor,
    getMultiColumnSlashMenuItems,
    locales as multiColumnLocales,
} from '@blocknote/xl-multi-column';
import { en as coreEn } from '@blocknote/core/locales';
import { autoPlacement, offset, shift, size } from '@floating-ui/react';
import { forwardRef, useCallback, useImperativeHandle, useMemo } from 'react';
import { useTheme } from '@gravitee/graphene-core';

import { schema } from '../../../blocks/schema';
import type { BlockNoteDocument } from '../../portals/types';
import { createMarkdownPasteHandler } from '../hooks/useMarkdownPaste';
import { uploadFile } from '../utils/upload';
import { PAGE_WIDTH_VALUES, type PageWidth } from '../constants/page-width';
import styles from './BlockEditor.module.scss';

type EditorType = typeof schema.BlockNoteEditor;
type PartialBlockType = typeof schema.PartialBlock;

export interface BlockEditorHandle {
    save: () => Promise<void>;
}

export type { PageWidth };
export { PAGE_WIDTH_VALUES };

const topApisSlashItem = (editor: EditorType) => ({
    title: 'Top 5 APIs',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeApiList' as const,
            props: { title: 'Top 5 APIs', limit: '5', viewMode: 'cards' as const },
        }),
    aliases: ['top', 'top5', 'popular'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
    ),
    subtext: 'Display the 5 most popular APIs',
});

const catalogSlashItem = (editor: EditorType) => ({
    title: 'API Catalog',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeApiList' as const,
            props: { title: 'Catalog', limit: '50', viewMode: 'cards' as const },
        }),
    aliases: ['catalog', 'catalogue', 'all', 'apis'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
    ),
    subtext: 'Display the full API catalog',
});

const bannerSlashItem = (editor: EditorType) => ({
    title: 'Banner',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeBanner' as const,
        }),
    aliases: ['banner', 'hero', 'header', 'welcome'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <line x1="8" y1="10" x2="16" y2="10" />
            <line x1="10" y1="14" x2="14" y2="14" />
        </svg>
    ),
    subtext: 'Hero section with title, subtitle, and CTA button',
});

const cardSlashItem = (editor: EditorType) => ({
    title: 'Card',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeCard' as const,
        }),
    aliases: ['card', 'feature', 'info'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <rect x="3" y="3" width="18" height="18" rx="3" />
            <line x1="7" y1="9" x2="17" y2="9" />
            <line x1="7" y1="13" x2="13" y2="13" />
        </svg>
    ),
    subtext: 'Card with icon, title, and description',
});

const buttonSlashItem = (editor: EditorType) => ({
    title: 'Button',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeButton' as const,
        }),
    aliases: ['button', 'cta', 'link', 'action'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <rect x="3" y="8" width="18" height="8" rx="4" />
            <line x1="9" y1="12" x2="15" y2="12" />
        </svg>
    ),
    subtext: 'Action button with configurable link',
});

const htmlSlashItem = (editor: EditorType) => ({
    title: 'HTML',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeHtml' as const,
        }),
    aliases: ['html', 'code', 'custom', 'embed', 'web'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="16 18 22 12 16 6" />
            <polyline points="8 6 2 12 8 18" />
        </svg>
    ),
    subtext: 'Custom HTML/CSS block with preview',
});

const markdownSlashItem = (editor: EditorType) => ({
    title: 'Markdown',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeMarkdown' as const,
        }),
    aliases: ['markdown', 'md', 'text', 'rich'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 4h16v16H4z" />
            <path d="M7 15V9l3 3 3-3v6" />
            <path d="M17 12l-2 3h4l-2-3z" />
        </svg>
    ),
    subtext: 'Markdown block with preview',
});

const featuresSlashItem = (editor: EditorType) => ({
    title: 'Features',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeSection' as const,
        }),
    aliases: ['features', 'grid', 'showcase', 'capabilities'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="3" width="20" height="18" rx="2" />
            <line x1="2" y1="9" x2="22" y2="9" />
        </svg>
    ),
    subtext: 'Feature grid with icons, titles, and background',
});

const sectionSlashItem = (editor: EditorType) => ({
    title: 'Section',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeContainer' as const,
            children: [
                {
                    type: 'paragraph' as const,
                },
            ],
        }),
    aliases: ['section', 'container', 'block', 'area', 'wrapper', 'background'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2" />
        </svg>
    ),
    subtext: 'Content section with customizable background',
});

const subscriptionFlowSlashItem = (editor: EditorType) => ({
    title: 'Subscription Flow',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeSubscriptionFlow' as const,
        }),
    aliases: ['subscription', 'subscribe', 'flow', 'wizard'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 2v4" />
            <path d="M12 18v4" />
            <path d="M4.93 4.93l2.83 2.83" />
            <path d="M16.24 16.24l2.83 2.83" />
            <path d="M2 12h4" />
            <path d="M18 12h4" />
            <path d="M4.93 19.07l2.83-2.83" />
            <path d="M16.24 7.76l2.83-2.83" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    ),
    subtext: 'Multi-step wizard to subscribe to the current API',
});

const subscriptionViewerSlashItem = (editor: EditorType) => ({
    title: 'Subscription Viewer',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeSubscriptionViewer' as const,
        }),
    aliases: ['subscriptions', 'subscription-list', 'viewer'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <line x1="3" y1="10" x2="21" y2="10" />
            <line x1="7" y1="15" x2="7.01" y2="15" />
            <line x1="11" y1="15" x2="13" y2="15" />
        </svg>
    ),
    subtext: 'Table of subscriptions with details panel',
});

const applicationsSlashItem = (editor: EditorType) => ({
    title: 'Applications',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeApplications' as const,
        }),
    aliases: ['applications', 'apps', 'application-management'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
    ),
    subtext: 'Manage applications, settings, and members',
});

function groupSuggestionItems<T extends { group?: string }>(items: T[]): T[] {
    const groupOrder: (string | undefined)[] = [];
    const itemsByGroup = new Map<string | undefined, T[]>();

    for (const item of items) {
        if (!itemsByGroup.has(item.group)) {
            itemsByGroup.set(item.group, []);
            groupOrder.push(item.group);
        }
        itemsByGroup.get(item.group)!.push(item);
    }

    return groupOrder.flatMap(group => itemsByGroup.get(group)!);
}

function getCustomSlashMenuItems(editor: EditorType) {
    return combineByGroup(
        getDefaultReactSlashMenuItems(editor),
        getMultiColumnSlashMenuItems(editor),
        [
            bannerSlashItem(editor),
            sectionSlashItem(editor),
            featuresSlashItem(editor),
            topApisSlashItem(editor),
            catalogSlashItem(editor),
            cardSlashItem(editor),
            buttonSlashItem(editor),
            htmlSlashItem(editor),
            markdownSlashItem(editor),
            subscriptionFlowSlashItem(editor),
            subscriptionViewerSlashItem(editor),
            applicationsSlashItem(editor),
        ],
    );
}

interface BlockEditorProps {
    readonly document?: BlockNoteDocument;
    readonly pageWidth?: PageWidth;
    readonly onSave?: (document: BlockNoteDocument) => void | Promise<void>;
}

export const BlockEditor = forwardRef<BlockEditorHandle, BlockEditorProps>(function BlockEditor(
    { document, pageWidth = 'narrow', onSave },
    ref,
) {
    const initialContent = document as PartialBlockType[] | undefined;
    const pasteHandler = useMemo(() => createMarkdownPasteHandler(), []);

    const editor = useCreateBlockNote({
        schema,
        initialContent,
        pasteHandler,
        placeholders: {
            default: "Type '/' to insert a block...",
        },
        uploadFile,
        dropCursor: multiColumnDropCursor,
        dictionary: {
            ...coreEn,
            multi_column: multiColumnLocales.en,
        },
    });

    useImperativeHandle(ref, () => ({
        save: async () => {
            await onSave?.(editor.document as BlockNoteDocument);
        },
    }));

    const { resolvedTheme } = useTheme();
    const blockNoteTheme = resolvedTheme === 'dark' ? 'dark' : 'light';

    const getSlashMenuItems = useCallback(
        async (query: string) =>
            groupSuggestionItems(
                filterSuggestionItems(getCustomSlashMenuItems(editor), query),
            ),
        [editor],
    );

    const slashMenuFloatingOptions = useMemo(
        () => ({
            useFloatingOptions: {
                middleware: [
                    offset(10),
                    autoPlacement({
                        allowedPlacements: ['bottom-start', 'top-start'],
                        rootBoundary: 'viewport',
                        padding: 10,
                    }),
                    shift(),
                    size({
                        apply({ elements, availableHeight }) {
                            const maxHeight = Math.max(0, availableHeight) * (2 / 3);
                            elements.floating.style.maxHeight = `${maxHeight}px`;
                        },
                        rootBoundary: 'viewport',
                        padding: 10,
                    }),
                ],
            },
        }),
        [],
    );

    return (
        <div
            className={styles.editorWrapper}
            style={{ '--page-width': PAGE_WIDTH_VALUES[pageWidth] } as React.CSSProperties}
        >
            <BlockNoteView editor={editor} slashMenu={false} theme={blockNoteTheme}>
                <SuggestionMenuController
                    triggerCharacter="/"
                    getItems={getSlashMenuItems}
                    floatingUIOptions={slashMenuFloatingOptions}
                />
            </BlockNoteView>
        </div>
    );
});
