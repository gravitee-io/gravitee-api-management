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
import { createMarkdownPasteHandler, insertMarkdownBlocksAtCursor } from './useMarkdownPaste';

const mockMarkdownToBlocks = jest.fn(() => [
    { type: 'heading', props: { level: 1 }, content: [{ type: 'text', text: 'Hello', styles: {} }], children: [] },
    { type: 'paragraph', content: [{ type: 'text', text: 'World', styles: {} }], children: [] },
]);

const mockGmdToPartialBlocks = jest.fn(() => [
    { type: 'graviteeButton', props: { label: 'Explore', link: '/catalog' }, children: [] },
]);

jest.mock('../utils/markdown-to-blocks', () => ({
    looksLikeMarkdown: (text: string) => /^#|\n- |```|\[.+\]\(/.test(text),
    markdownToBlocks: (...args: unknown[]) => mockMarkdownToBlocks(...args),
}));

jest.mock('../gmd/gmd-parser', () => ({
    gmdToPartialBlocks: (...args: unknown[]) => mockGmdToPartialBlocks(...args),
}));

jest.mock('../gmd/gmd-utils', () => ({
    looksLikeGmd: (text: string) => /<gmd-[a-z0-9-]+\b/i.test(text) || /<style\b/i.test(text),
}));

function createMockEditor({
    inCodeBlock = false,
    emptyParagraph = true,
}: {
    inCodeBlock?: boolean;
    emptyParagraph?: boolean;
} = {}) {
    const currentBlock = {
        id: 'block-1',
        type: 'paragraph',
        content: emptyParagraph ? [] : [{ type: 'text', text: 'Existing text', styles: {} }],
        children: [],
    };

    return {
        transact: jest.fn((callback: (tr: { selection: { $from: { parent: { type: { spec: { code?: boolean } } } }; $to: { parent: { type: { spec: { code?: boolean } } } } } }) => boolean) =>
            callback({
                selection: {
                    $from: { parent: { type: { spec: { code: inCodeBlock } } } },
                    $to: { parent: { type: { spec: { code: inCodeBlock } } } },
                },
            }),
        ),
        getTextCursorPosition: jest.fn(() => ({ block: currentBlock })),
        replaceBlocks: jest.fn(() => ({
            insertedBlocks: [
                { id: 'heading-1', type: 'heading' },
                { id: 'paragraph-1', type: 'paragraph' },
            ],
            removedBlocks: [{ id: 'block-1', type: 'paragraph' }],
        })),
        insertBlocks: jest.fn(() => [
            { id: 'heading-1', type: 'heading' },
            { id: 'paragraph-1', type: 'paragraph' },
        ]),
        setTextCursorPosition: jest.fn(),
    };
}

function createPasteEvent(text: string): ClipboardEvent {
    return {
        clipboardData: {
            getData: (type: string) => (type === 'text/plain' ? text : ''),
            types: ['text/plain'],
        },
    } as unknown as ClipboardEvent;
}

describe('createMarkdownPasteHandler', () => {
    beforeEach(() => {
        mockMarkdownToBlocks.mockClear();
        mockGmdToPartialBlocks.mockClear();
    });

    it('should insert parsed blocks when pasted text looks like markdown', () => {
        const editor = createMockEditor();
        const defaultPasteHandler = jest.fn();
        const handler = createMarkdownPasteHandler();

        const handled = handler({
            event: createPasteEvent('# Hello\n\nWorld'),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(handled).toBe(true);
        expect(mockMarkdownToBlocks).toHaveBeenCalledWith('# Hello\n\nWorld');
        expect(editor.replaceBlocks).toHaveBeenCalled();
        expect(defaultPasteHandler).not.toHaveBeenCalled();
    });

    it('should fall through to default paste for plain text', () => {
        const editor = createMockEditor();
        const defaultPasteHandler = jest.fn(() => true);
        const handler = createMarkdownPasteHandler();

        handler({
            event: createPasteEvent('Hello world'),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(mockMarkdownToBlocks).not.toHaveBeenCalled();
        expect(defaultPasteHandler).toHaveBeenCalledWith({
            plainTextAsMarkdown: false,
            prioritizeMarkdownOverHTML: true,
        });
    });

    it('should delegate to default paste handler inside code blocks', () => {
        const editor = createMockEditor({ inCodeBlock: true });
        const defaultPasteHandler = jest.fn(() => true);
        const handler = createMarkdownPasteHandler();

        handler({
            event: createPasteEvent('# Hello'),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(mockMarkdownToBlocks).not.toHaveBeenCalled();
        expect(defaultPasteHandler).toHaveBeenCalledWith();
    });

    it('should insert parsed blocks when pasted text looks like GMD', () => {
        const editor = createMockEditor();
        const defaultPasteHandler = jest.fn();
        const handler = createMarkdownPasteHandler();
        const gmdText = '<gmd-button link="/catalog">Explore</gmd-button>';

        const handled = handler({
            event: createPasteEvent(gmdText),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(handled).toBe(true);
        expect(mockGmdToPartialBlocks).toHaveBeenCalledWith(gmdText, editor);
        expect(mockMarkdownToBlocks).not.toHaveBeenCalled();
        expect(editor.replaceBlocks).toHaveBeenCalled();
        expect(defaultPasteHandler).not.toHaveBeenCalled();
    });

    it('should prefer GMD parsing for mixed markdown and GMD content', () => {
        const editor = createMockEditor();
        const defaultPasteHandler = jest.fn();
        const handler = createMarkdownPasteHandler();
        const mixedText = '### Title\n\n<gmd-button link="/catalog">Explore</gmd-button>';

        const handled = handler({
            event: createPasteEvent(mixedText),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(handled).toBe(true);
        expect(mockGmdToPartialBlocks).toHaveBeenCalledWith(mixedText, editor);
        expect(mockMarkdownToBlocks).not.toHaveBeenCalled();
    });

    it('should delegate GMD paste to default handler inside code blocks', () => {
        const editor = createMockEditor({ inCodeBlock: true });
        const defaultPasteHandler = jest.fn(() => true);
        const handler = createMarkdownPasteHandler();

        handler({
            event: createPasteEvent('<gmd-button link="/catalog">Explore</gmd-button>'),
            editor: editor as never,
            defaultPasteHandler,
        });

        expect(mockGmdToPartialBlocks).not.toHaveBeenCalled();
        expect(defaultPasteHandler).toHaveBeenCalledWith();
    });
});

describe('insertMarkdownBlocksAtCursor', () => {
    it('should replace an empty paragraph block', () => {
        const editor = createMockEditor({ emptyParagraph: true });
        const blocks = [{ type: 'heading', props: { level: 1 } }];

        insertMarkdownBlocksAtCursor(editor as never, blocks);

        expect(editor.replaceBlocks).toHaveBeenCalledWith([editor.getTextCursorPosition().block], blocks);
        expect(editor.setTextCursorPosition).toHaveBeenCalled();
    });

    it('should insert after a non-empty block', () => {
        const editor = createMockEditor({ emptyParagraph: false });
        const blocks = [{ type: 'paragraph' }];

        insertMarkdownBlocksAtCursor(editor as never, blocks);

        expect(editor.insertBlocks).toHaveBeenCalledWith(
            blocks,
            editor.getTextCursorPosition().block,
            'after',
        );
    });
});
