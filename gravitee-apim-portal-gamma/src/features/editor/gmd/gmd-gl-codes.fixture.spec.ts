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
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { splitGmdDocument } from './gmd-segments';
import { gmdToPartialBlocks } from './gmd-parser';
import { looksLikeGmd } from './gmd-utils';

const GL_CODES_PATH = resolve(__dirname, '../../../../docs/GL Codes - Endpoints (1).md');

const mockEditor = {
    blocksToMarkdownLossy: jest.fn(() => ''),
    tryParseMarkdownToBlocks: jest.fn(() => []),
    tryParseHTMLToBlocks: jest.fn(() => []),
};

jest.mock('@blocknote/core', () => ({
    BlockNoteEditor: {
        create: jest.fn(() => mockEditor),
    },
}));

describe('GL Codes fixture', () => {
    const source = readFileSync(GL_CODES_PATH, 'utf-8');

    it('should detect and split the document into html and style segments', () => {
        expect(looksLikeGmd(source)).toBe(true);

        const segments = splitGmdDocument(source);
        expect(segments).toHaveLength(2);
        expect(segments[0]?.type).toBe('element');
        expect(segments[0]?.type === 'element' && segments[0].outerHtml).toContain('softco-doc');
        expect(segments[1]?.type).toBe('element');
        expect(segments[1]?.type === 'element' && segments[1].outerHtml).toContain('<style');
    });

    it('should parse into a single graviteeHtml block with html and css', () => {
        const parsed = gmdToPartialBlocks(source, mockEditor);

        const htmlBlocks = parsed.filter(block => block.type === 'graviteeHtml');
        const merged = htmlBlocks.find(block => {
            const html = String(block.props?.html ?? '');
            const css = String(block.props?.css ?? '');
            return html.includes('softco-doc') && css.includes('.softco-doc');
        });

        expect(parsed.length).toBe(1);
        expect(merged).toBeDefined();
        expect(String(merged?.props?.css ?? '')).toContain('--blue');
    });
});
