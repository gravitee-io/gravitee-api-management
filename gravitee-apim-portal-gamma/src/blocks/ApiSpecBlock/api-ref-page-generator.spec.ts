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
const mockBnMarkdownToBlocks = jest.fn((markdown: string) => {
    const blocks: Array<Record<string, unknown>> = [];

    const headingMatches = [...markdown.matchAll(/^(#{1,6})\s+(.+)$/gm)];
    for (const [, hashes, text] of headingMatches) {
        blocks.push({
            type: 'heading',
            props: { level: hashes?.length ?? 1 },
            content: [{ type: 'text', text, styles: {} }],
            children: [],
        });
    }

    if (markdown.includes('1. Create an application')) {
        blocks.push({ type: 'numberedListItem', children: [] });
    }

    if (markdown.includes('| Tier')) {
        blocks.push({ type: 'table', children: [] });
    }

    if (blocks.length === 0) {
        blocks.push({
            type: 'paragraph',
            content: [{ type: 'text', text: markdown.split('\n')[0] ?? markdown, styles: {} }],
            children: [],
        });
    }

    return blocks;
});

jest.mock('@blocknote/core', () => ({
    BlockNoteEditor: {
        create: jest.fn(() => ({
            tryParseMarkdownToBlocks: (markdown: string) => mockBnMarkdownToBlocks(markdown),
        })),
    },
}));

jest.mock('../../blocks/schema', () => ({
    schema: {},
}));

import { buildTagPageDefinitions, createOverviewReferenceDocument, createTagReferenceDocument } from './api-ref-page-generator';
import { DETAILED_DUMMY_OPENAPI_SPEC } from '../../features/portals/storage/dummy-openapi-spec';
import { parseOpenApiDocument } from './openapi-spec-utils';

function collectInlineText(blocks: Array<Record<string, unknown>>): string {
    return blocks
        .flatMap(block => {
            const content = block.content;
            if (!Array.isArray(content)) {
                return [];
            }

            return content.map(node =>
                typeof node === 'object' && node !== null && 'text' in node ? String((node as { text?: string }).text ?? '') : '',
            );
        })
        .join('\n');
}

describe('api-ref-page-generator', () => {
    it('creates a tag page with all API reference blocks', () => {
        const document = createTagReferenceDocument('Products');

        expect(document).toHaveLength(6);
        expect(document[2]).toMatchObject({
            type: 'graviteeApiOperations',
            props: { tag: 'Products', showResponses: 'true' },
        });
        expect(document[3]).toMatchObject({ type: 'graviteeApiSchemas', props: { tag: 'Products' } });
        expect(document[4]).toMatchObject({ type: 'graviteeApiTryIt', props: { tag: 'Products' } });
        expect(document[5]).toMatchObject({ type: 'graviteeApiCodeSamples', props: { tag: 'Products' } });
    });

    it('builds overview and tag pages from an API spec', async () => {
        const result = await buildTagPageDefinitions('api-payments', 'Payments API');

        expect(result.overviewDocument[0]).toMatchObject({ type: 'heading' });
        expect(result.tagPages.length).toBeGreaterThan(0);
        expect(result.tagPages[0].document[2]).toMatchObject({
            type: 'graviteeApiOperations',
        });
    });

    it('parses markdown OpenAPI descriptions into multiple blocks instead of one paragraph', () => {
        const parsed = parseOpenApiDocument(DETAILED_DUMMY_OPENAPI_SPEC);
        const document = createOverviewReferenceDocument('Payments API', parsed?.document.info?.description);

        expect(document.length).toBeGreaterThan(5);
        expect(document.some(block => (block as { type?: string }).type === 'heading' && (block as { props?: { level?: number } }).props?.level === 2)).toBe(
            true,
        );
        expect(collectInlineText(document as Array<Record<string, unknown>>)).not.toMatch(/\\\s*$/m);
    });
});
