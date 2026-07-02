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
import { createMarkdownPasteHandler } from '../hooks/useMarkdownPaste';

const mockMarkdownToBlocks = jest.fn(() => [
    {
        type: 'heading',
        props: { level: 1 },
        content: [{ type: 'text', text: 'Hello', styles: {} }],
        children: [],
    },
    {
        type: 'paragraph',
        content: [{ type: 'text', text: 'World', styles: {} }],
        children: [],
    },
]);

jest.mock('../utils/markdown-to-blocks', () => ({
    looksLikeMarkdown: (text: string) => text.includes('#'),
    markdownToBlocks: (...args: unknown[]) => mockMarkdownToBlocks(...args),
}));

const mockGmdToPartialBlocks = jest.fn(() => [
    {
        type: 'graviteeButton',
        props: { label: 'Explore', link: '/catalog' },
        children: [],
    },
]);

jest.mock('../gmd/gmd-parser', () => ({
    gmdToPartialBlocks: (...args: unknown[]) => mockGmdToPartialBlocks(...args),
}));

jest.mock('../gmd/gmd-utils', () => ({
    looksLikeGmd: (text: string) => text.includes('<gmd-'),
}));

function createIntegrationEditor() {
    const currentBlock = {
        id: 'block-1',
        type: 'paragraph',
        content: [],
        children: [],
    };

    const document = [currentBlock];

    return {
        document,
        transact: jest.fn((callback: (tr: { selection: { $from: { parent: { type: { spec: { code?: boolean } } } }; $to: { parent: { type: { spec: { code?: boolean } } } } } }) => boolean) =>
            callback({
                selection: {
                    $from: { parent: { type: { spec: { code: false } } } },
                    $to: { parent: { type: { spec: { code: false } } } },
                },
            }),
        ),
        getTextCursorPosition: jest.fn(() => ({ block: currentBlock })),
        replaceBlocks: jest.fn((_blocksToRemove, blocksToInsert) => {
            document.splice(0, document.length, ...blocksToInsert);
            const insertedBlocks = blocksToInsert.map((block, index) => ({ ...block, id: `block-${index + 1}` }));
            return {
                insertedBlocks,
                removedBlocks: [currentBlock],
            };
        }),
        insertBlocks: jest.fn(),
        setTextCursorPosition: jest.fn(),
    };
}

describe('BlockEditor markdown paste integration', () => {
    it('should convert pasted markdown into editor blocks', () => {
        const editor = createIntegrationEditor();
        const pasteHandler = createMarkdownPasteHandler();

        const handled = pasteHandler({
            event: {
                clipboardData: {
                    getData: (type: string) => (type === 'text/plain' ? '# Hello\n\nWorld' : ''),
                    types: ['text/plain'],
                },
            } as unknown as ClipboardEvent,
            editor: editor as never,
            defaultPasteHandler: jest.fn(),
        });

        expect(handled).toBe(true);
        expect(mockMarkdownToBlocks).toHaveBeenCalledWith('# Hello\n\nWorld');
        expect(editor.document.some((block) => block.type === 'heading')).toBe(true);
        expect(editor.document.some((block) => block.type === 'paragraph')).toBe(true);
    });
});

describe('BlockEditor GMD paste integration', () => {
    beforeEach(() => {
        mockGmdToPartialBlocks.mockClear();
    });

    it('should convert pasted GMD into editor blocks', () => {
        const editor = createIntegrationEditor();
        const pasteHandler = createMarkdownPasteHandler();
        const gmdText = '<gmd-button link="/catalog">Explore</gmd-button>';

        const handled = pasteHandler({
            event: {
                clipboardData: {
                    getData: (type: string) => (type === 'text/plain' ? gmdText : ''),
                    types: ['text/plain'],
                },
            } as unknown as ClipboardEvent,
            editor: editor as never,
            defaultPasteHandler: jest.fn(),
        });

        expect(handled).toBe(true);
        expect(mockGmdToPartialBlocks).toHaveBeenCalledWith(gmdText, editor);
        expect(editor.document.some((block) => block.type === 'graviteeButton')).toBe(true);
    });
});
