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
import type { BlockNoteEditorOptions } from '@blocknote/core';

import { schema } from '../../blocks/schema';
import { gmdToPartialBlocks } from '../gmd/gmd-parser';
import { looksLikeGmd } from '../gmd/gmd-utils';
import { looksLikeMarkdown, markdownToBlocks, type PartialBlock } from '../utils/markdown-to-blocks';

type GmdParseEditor = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseHTMLToBlocks: (html: string) => any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tryParseMarkdownToBlocks: (markdown: string) => any;
};

type PasteHandler = NonNullable<
    BlockNoteEditorOptions<
        typeof schema.blockSchema,
        typeof schema.inlineContentSchema,
        typeof schema.styleSchema
    >['pasteHandler']
>;

type MarkdownPasteEditor = {
    transact: (callback: (tr: {
        selection: {
            $from: { parent: { type: { spec: { code?: boolean } } } };
            $to: { parent: { type: { spec: { code?: boolean } } } };
        };
    }) => boolean) => boolean;
    getTextCursorPosition: () => { block: { id: string; type: string; content?: unknown } };
    replaceBlocks: (
        blocksToRemove: unknown[],
        blocksToInsert: PartialBlock[],
    ) => { insertedBlocks: Array<{ id: string }>; removedBlocks: Array<{ id: string }> };
    insertBlocks: (
        blocksToInsert: PartialBlock[],
        referenceBlock: unknown,
        placement: 'before' | 'after',
    ) => Array<{ id: string }>;
    setTextCursorPosition: (targetBlock: unknown, placement?: 'start' | 'end') => void;
};

function isEmptyParagraph(block: { type: string; content?: unknown }): boolean {
    if (block.type !== 'paragraph') {
        return false;
    }

    const content = block.content as Array<{ type: string; text?: string }> | undefined;
    return !content?.length || content.every((node) => node.type === 'text' && !node.text?.trim());
}

function isInCodeBlock(editor: MarkdownPasteEditor): boolean {
    return editor.transact((tr) =>
        Boolean(tr.selection.$from.parent.type.spec.code && tr.selection.$to.parent.type.spec.code),
    );
}

/**
 * Inserts parsed markdown blocks at the current text cursor position.
 */
export function insertMarkdownBlocksAtCursor(editor: MarkdownPasteEditor, blocks: PartialBlock[]): void {
    if (blocks.length === 0) {
        return;
    }

    const currentBlock = editor.getTextCursorPosition().block;
    const insertedBlocks = isEmptyParagraph(currentBlock)
        ? editor.replaceBlocks([currentBlock], blocks).insertedBlocks
        : editor.insertBlocks(blocks, currentBlock, 'after');

    const lastInsertedBlock = insertedBlocks[insertedBlocks.length - 1];
    if (lastInsertedBlock) {
        editor.setTextCursorPosition(lastInsertedBlock, 'end');
    }
}

export function createMarkdownPasteHandler(): PasteHandler {
    const handler: PasteHandler = ({ event, editor, defaultPasteHandler }) => {
        const markdownEditor = editor as unknown as MarkdownPasteEditor;

        if (isInCodeBlock(markdownEditor)) {
            return defaultPasteHandler();
        }

        const plainText = event.clipboardData?.getData('text/plain');

        if (plainText && looksLikeGmd(plainText)) {
            const blocks = gmdToPartialBlocks(plainText, editor as unknown as GmdParseEditor) as PartialBlock[];
            insertMarkdownBlocksAtCursor(markdownEditor, blocks);
            return true;
        }

        if (plainText && looksLikeMarkdown(plainText)) {
            const blocks = markdownToBlocks(plainText);
            insertMarkdownBlocksAtCursor(markdownEditor, blocks);
            return true;
        }

        return defaultPasteHandler({
            plainTextAsMarkdown: false,
            prioritizeMarkdownOverHTML: true,
        });
    };

    return handler;
}
