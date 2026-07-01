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

const mockEditor = {
    blocksToMarkdownLossy: jest.fn((blocks: Array<{ type: string; content?: Array<{ text?: string }> }>) => {
        const block = blocks[0];
        if (!block) return '';
        if (block.type === 'paragraph') {
            return block.content?.map(node => node.text ?? '').join('') ?? '';
        }
        if (block.type === 'heading') {
            return '# Heading';
        }
        return '';
    }),
    tryParseMarkdownToBlocks: jest.fn((markdown: string) => {
        if (markdown.includes('###')) {
            return [{ type: 'heading', props: { level: 3 }, content: [{ type: 'text', text: 'Your toolkit for building', styles: {} }] }];
        }
        if (markdown.includes('Hello GMD')) {
            return [{ type: 'paragraph', content: [{ type: 'text', text: 'Hello GMD', styles: {} }] }];
        }
        return [];
    }),
    tryParseHTMLToBlocks: jest.fn(() => []),
};

jest.mock('@blocknote/core', () => ({
    BlockNoteEditor: {
        create: jest.fn(() => mockEditor),
    },
}));

jest.mock('../../../blocks/schema', () => ({
    schema: {},
}));

import { BlockNoteEditor } from '@blocknote/core';

import { parseGmdToDocument, serializeDocumentToGmd } from './gmd-content';
import { gmdToPartialBlocks } from './gmd-parser';
import { blocksToGmd } from './gmd-serializer';

const LEGACY_CARD_GMD = `<gmd-card backgroundColor="none">
    <gmd-card-title>Your first API call</gmd-card-title>
    <gmd-md>Learn how to make a basic request.</gmd-md>
</gmd-card>`;

const INSTALL_MCP_GMD =
    '<gmd-install-mcp name="Payments API" transport="http" url="https://gateway.example.com/mcp" />';

const HOMEPAGE_SNIPPET = `### Your toolkit for building

<gmd-grid columns="3">
    <gmd-md>
        ![book](./assets/homepage/book.svg "Book icon")
        #### API catalog
        Browse and test all available APIs in one place.
    </gmd-md>
</gmd-grid>

<style>
  .homepage-title {
    text-align: center;
  }
</style>`;

describe('GMD serialization', () => {
    let editor: typeof mockEditor;

    beforeAll(() => {
        editor = BlockNoteEditor.create() as typeof mockEditor;
    });

    it('should round-trip a button block', () => {
        const blocks = [
            {
                type: 'graviteeButton',
                props: {
                    label: 'Get Started',
                    link: '/catalog',
                    appearance: 'filled',
                },
            },
        ];

        const gmd = blocksToGmd(blocks, editor);
        expect(gmd).toContain('<gmd-button');
        expect(gmd).toContain('Get Started');

        const parsed = gmdToPartialBlocks(gmd, editor);
        expect(parsed[0]?.type).toBe('graviteeButton');
        expect(parsed[0]?.props).toMatchObject({
            label: 'Get Started',
            link: '/catalog',
            appearance: 'filled',
        });
    });

    it('should import legacy gmd-card content', () => {
        const parsed = gmdToPartialBlocks(LEGACY_CARD_GMD, editor);
        expect(parsed[0]?.type).toBe('graviteeCard');
        expect(parsed[0]?.props).toMatchObject({
            title: 'Your first API call',
        });
    });

    it('should import gmd-install-mcp tags', () => {
        const parsed = gmdToPartialBlocks(INSTALL_MCP_GMD, editor);
        expect(parsed[0]?.type).toBe('graviteeInstallMcp');
        expect(parsed[0]?.props).toMatchObject({
            name: 'Payments API',
            transport: 'http',
            url: 'https://gateway.example.com/mcp',
        });
    });

    it('should parse standalone style blocks', () => {
        const parsed = gmdToPartialBlocks('<style>.homepage-title { text-align: center; }</style>', editor);
        expect(parsed[0]?.type).toBe('graviteeHtml');
        expect(parsed[0]?.props).toMatchObject({
            html: '',
            css: expect.stringContaining('.homepage-title'),
        });
    });

    it('should parse style blocks into graviteeHtml css', () => {
        const parsed = gmdToPartialBlocks(HOMEPAGE_SNIPPET, editor);
        const styleBlock = parsed.find(block => block.type === 'graviteeHtml');
        expect(styleBlock?.props).toMatchObject({
            html: '',
            css: expect.stringContaining('.homepage-title'),
        });
    });

    it('should parse markdown headings outside gmd tags', () => {
        const parsed = gmdToPartialBlocks(HOMEPAGE_SNIPPET, editor);
        expect(parsed.some(block => block.type === 'heading')).toBe(true);
    });

    it('should serialize and parse via gmd-content helpers', () => {
        const document = [
            {
                type: 'paragraph',
                content: [{ type: 'text', text: 'Hello GMD', styles: {} }],
            },
        ] as never;

        const gmd = serializeDocumentToGmd(document);
        expect(gmd).toContain('Hello GMD');

        const roundTripped = parseGmdToDocument(gmd);
        expect(roundTripped[0]?.type).toBe('paragraph');
    });
});
