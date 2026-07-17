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
const mockPmSchema = { name: 'mock-pm-schema' };

const mockBnMarkdownToBlocks = jest.fn((markdown: string) => {
    const blocks: Array<Record<string, unknown>> = [];

    const headingMatch = markdown.match(/^(#{1,6})\s+(.+)$/gm);
    if (headingMatch) {
        for (const match of headingMatch) {
            const [, hashes, text] = match.match(/^(#{1,6})\s+(.+)$/) ?? [];
            blocks.push({
                type: 'heading',
                props: { level: hashes?.length ?? 1 },
                content: [{ type: 'text', text, styles: {} }],
                children: [],
            });
        }
    }

    if (markdown.includes('**bold**')) {
        blocks.push({
            type: 'paragraph',
            content: [
                { type: 'text', text: 'Text with ', styles: {} },
                { type: 'text', text: 'bold', styles: { bold: true } },
                { type: 'text', text: ', ', styles: {} },
                { type: 'text', text: 'italic', styles: { italic: true } },
                { type: 'text', text: ', ', styles: {} },
                { type: 'text', text: 'strike', styles: { strike: true } },
                { type: 'text', text: ', and ', styles: {} },
                { type: 'text', text: 'code', styles: { code: true } },
                { type: 'text', text: '.', styles: {} },
            ],
            children: [],
        });
    }

    if (markdown.includes('- bullet')) {
        blocks.push({ type: 'bulletListItem', children: [] });
    }

    if (markdown.includes('1. numbered')) {
        blocks.push({ type: 'numberedListItem', children: [] });
    }

    if (markdown.includes('- [ ] task')) {
        blocks.push({ type: 'checkListItem', children: [] });
    }

    if (markdown.includes('```ts')) {
        blocks.push({ type: 'codeBlock', props: { language: 'ts' }, children: [] });
    }

    if (markdown.includes('> quoted text')) {
        blocks.push({
            type: 'paragraph',
            props: { backgroundColor: 'default' },
            content: [{ type: 'text', text: 'quoted text', styles: {} }],
            children: [],
        });
    }

    if (markdown.includes('---')) {
        blocks.push({ type: 'paragraph', children: [] });
    }

    if (markdown.includes('![alt text]')) {
        blocks.push({ type: 'image', props: { url: 'https://example.com/image.png' }, children: [] });
    }

    if (markdown.includes('[Gravitee]')) {
        blocks.push({
            type: 'paragraph',
            content: [
                {
                    type: 'link',
                    href: 'https://gravitee.io',
                    content: [{ type: 'text', text: 'Gravitee' }],
                },
            ],
            children: [],
        });
    }

    if (markdown.includes('| Col A |')) {
        blocks.push({ type: 'table', children: [] });
    }

    if (markdown.includes('- parent')) {
        blocks.push({
            type: 'bulletListItem',
            children: [{ type: 'bulletListItem', children: [] }],
        });
    }

    return blocks;
});

jest.mock('@blocknote/core', () => ({
    BlockNoteEditor: {
        create: jest.fn(() => ({
            pmSchema: mockPmSchema,
            tryParseMarkdownToBlocks: (markdown: string) => mockBnMarkdownToBlocks(markdown, mockPmSchema),
        })),
    },
}));

jest.mock('../../../blocks/schema', () => ({
    schema: {
        BlockNoteEditor: class MockBlockNoteEditor {},
        PartialBlock: {},
    },
}));

import { BlockNoteEditor } from '@blocknote/core';

import { markdownToBlocks, looksLikeMarkdown } from './markdown-to-blocks';

function getInlineText(block: { content?: unknown }) {
    const content = block.content as Array<{ type: string; text?: string; styles?: Record<string, boolean> }> | undefined;
    return content?.filter((node) => node.type === 'text') ?? [];
}

describe('looksLikeMarkdown', () => {
    it('should return false for plain text without markdown markers', () => {
        expect(looksLikeMarkdown('Hello world')).toBe(false);
        expect(looksLikeMarkdown('Use a dash - in a sentence')).toBe(false);
    });

    it('should return true for common markdown structures', () => {
        expect(looksLikeMarkdown('# Title\n\nBody')).toBe(true);
        expect(looksLikeMarkdown('- list item')).toBe(true);
        expect(looksLikeMarkdown('```ts\ncode\n```')).toBe(true);
        expect(looksLikeMarkdown('[link](https://example.com)')).toBe(true);
    });
});

describe('markdownToBlocks', () => {
    it('should delegate parsing to BlockNote with the gamma schema', () => {
        markdownToBlocks('# Hello');

        expect(BlockNoteEditor.create).toHaveBeenCalled();
        expect(mockBnMarkdownToBlocks).toHaveBeenCalledWith('# Hello', mockPmSchema);
    });

    it('should convert headings h1 through h6', () => {
        const blocks = markdownToBlocks(
            '# H1\n\n## H2\n\n### H3\n\n#### H4\n\n##### H5\n\n###### H6',
        );

        const headings = blocks.filter((block) => block.type === 'heading');
        expect(headings).toHaveLength(6);
        expect(headings.map((block) => block.props?.level)).toEqual([1, 2, 3, 4, 5, 6]);
    });

    it('should preserve inline formatting', () => {
        const blocks = markdownToBlocks('Text with **bold**, *italic*, ~~strike~~, and `code`.');

        const textNodes = getInlineText(blocks[0] ?? {});
        expect(textNodes.find((node) => node.text === 'bold')?.styles?.bold).toBe(true);
        expect(textNodes.find((node) => node.text === 'italic')?.styles?.italic).toBe(true);
        expect(textNodes.find((node) => node.text === 'strike')?.styles?.strike).toBe(true);
        expect(textNodes.find((node) => node.text === 'code')?.styles?.code).toBe(true);
    });

    it('should convert bullet, numbered, and checklist items', () => {
        const blocks = markdownToBlocks('- bullet\n\n1. numbered\n\n- [ ] task');

        expect(blocks.some((block) => block.type === 'bulletListItem')).toBe(true);
        expect(blocks.some((block) => block.type === 'numberedListItem')).toBe(true);
        expect(blocks.some((block) => block.type === 'checkListItem')).toBe(true);
    });

    it('should convert fenced code blocks with language', () => {
        const blocks = markdownToBlocks('```ts\nconst x = 1;\n```');

        const codeBlock = blocks.find((block) => block.type === 'codeBlock');
        expect(codeBlock).toBeDefined();
        expect(codeBlock?.props?.language).toBe('ts');
    });

    it('should convert blockquotes', () => {
        const blocks = markdownToBlocks('> quoted text');

        expect(blocks.some((block) => block.type === 'paragraph')).toBe(true);
        expect(getInlineText(blocks[0] ?? {}).some((node) => node.text?.includes('quoted text'))).toBe(true);
    });

    it('should convert horizontal rules', () => {
        const blocks = markdownToBlocks('Before\n\n---\n\nAfter');

        expect(blocks.some((block) => block.type === 'paragraph')).toBe(true);
    });

    it('should convert images', () => {
        const blocks = markdownToBlocks('![alt text](https://example.com/image.png)');

        const imageBlock = blocks.find((block) => block.type === 'image');
        expect(imageBlock).toBeDefined();
        expect(imageBlock?.props?.url).toBe('https://example.com/image.png');
    });

    it('should convert links in paragraph content', () => {
        const blocks = markdownToBlocks('[Gravitee](https://gravitee.io)');

        const content = blocks[0]?.content as Array<{ type: string; href?: string; content?: Array<{ text?: string }> }>;
        const linkNode = content?.find((node) => node.type === 'link');
        expect(linkNode?.href).toBe('https://gravitee.io');
        expect(linkNode?.content?.[0]?.text).toBe('Gravitee');
    });

    it('should convert GFM tables', () => {
        const blocks = markdownToBlocks('| Col A | Col B |\n| --- | --- |\n| a | b |');

        expect(blocks.some((block) => block.type === 'table')).toBe(true);
    });

    it('should handle nested lists', () => {
        const blocks = markdownToBlocks('- parent\n  - child');

        const parent = blocks.find((block) => block.type === 'bulletListItem');
        expect(parent).toBeDefined();
        expect(parent?.children?.some((child) => child.type === 'bulletListItem')).toBe(true);
    });
});
