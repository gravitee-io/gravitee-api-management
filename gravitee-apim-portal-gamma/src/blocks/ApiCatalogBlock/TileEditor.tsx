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
import '../../features/editor/styles/blocknote.css';
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
import { autoPlacement, offset, shift, size } from '@floating-ui/react';
import { useTheme } from '@gravitee/graphene-core';
import { en as coreEn } from '@blocknote/core/locales';
import { useCallback, useMemo } from 'react';

import { schema } from '../schema';
import {
    API_METADATA_FIELD_LABELS,
    API_METADATA_FIELDS,
    type ApiMetadataField,
} from '../ApiMetadataBlock/ApiMetadataBlock';
import type { BlockNoteDocument } from '../../features/portals/types';
import { uploadFile } from '../../features/editor/utils/upload';
import styles from './TileEditorDialog.module.scss';

type EditorType = typeof schema.BlockNoteEditor;
type PartialBlockType = typeof schema.PartialBlock;

const ALLOWED_DEFAULT_TITLES = new Set(['Paragraph', 'Heading 1', 'Heading 2', 'Heading 3', 'Image']);

function metadataSlashItem(editor: EditorType, field: ApiMetadataField) {
    return {
        title: API_METADATA_FIELD_LABELS[field],
        onItemClick: () =>
            insertOrUpdateBlockForSlashMenu(editor, {
                type: 'graviteeApiMetadata' as const,
                props: { field },
            }),
        aliases: [field, API_METADATA_FIELD_LABELS[field].toLowerCase()],
        group: 'API Metadata',
        icon: (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 7h16M4 12h10M4 17h6" />
            </svg>
        ),
        subtext: `Insert ${API_METADATA_FIELD_LABELS[field].toLowerCase()} placeholder`,
    };
}

const markdownSlashItem = (editor: EditorType) => ({
    title: 'Markdown',
    onItemClick: () =>
        insertOrUpdateBlockForSlashMenu(editor, {
            type: 'graviteeMarkdown' as const,
        }),
    aliases: ['markdown', 'md', 'text', 'rich'],
    group: 'Gravitee',
    icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M4 4h16v16H4z" />
            <path d="M7 15V9l3 3 3-3v6" />
            <path d="M17 12l-2 3h4l-2-3z" />
        </svg>
    ),
    subtext: 'Markdown block with preview',
});

function getTileEditorSlashMenuItems(editor: EditorType) {
    const defaultItems = getDefaultReactSlashMenuItems(editor).filter(item => ALLOWED_DEFAULT_TITLES.has(item.title));

    return combineByGroup(defaultItems, [
        ...API_METADATA_FIELDS.map(field => metadataSlashItem(editor, field)),
        markdownSlashItem(editor),
    ]);
}

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

interface TileEditorProps {
    readonly document: BlockNoteDocument;
    readonly onChange: (document: BlockNoteDocument) => void;
}

export function TileEditor({ document, onChange }: TileEditorProps) {
    const initialContent = document as PartialBlockType[];

    const editor = useCreateBlockNote({
        schema,
        initialContent,
        placeholders: {
            default: "Type '/' to insert a block...",
        },
        uploadFile,
        dictionary: {
            ...coreEn,
        },
    });

    const { resolvedTheme } = useTheme();
    const blockNoteTheme = resolvedTheme === 'dark' ? 'dark' : 'light';

    const getSlashMenuItems = useCallback(
        async (query: string) =>
            groupSuggestionItems(filterSuggestionItems(getTileEditorSlashMenuItems(editor), query)),
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

    const handleChange = useCallback(() => {
        onChange(editor.document as BlockNoteDocument);
    }, [editor, onChange]);

    return (
        <div className={styles.editorWrapper}>
            <BlockNoteView editor={editor} slashMenu={false} theme={blockNoteTheme} onChange={handleChange}>
                <SuggestionMenuController
                    triggerCharacter="/"
                    getItems={getSlashMenuItems}
                    floatingUIOptions={slashMenuFloatingOptions}
                />
            </BlockNoteView>
        </div>
    );
}
