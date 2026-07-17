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

import { gmdToPartialBlocks } from './gmd-parser';
import type { GammaPartialBlock } from './gmd-types';

const CUSTOM_HOMEPAGE_PATH = resolve(__dirname, '../../../../docs/custom-homepage.md');

function collectBlocks(blocks: readonly GammaPartialBlock[]): GammaPartialBlock[] {
    const collected: GammaPartialBlock[] = [];

    for (const block of blocks) {
        collected.push(block);
        if (block.children?.length) {
            collected.push(...collectBlocks(block.children));
        }
    }

    return collected;
}

const mockEditor = {
    tryParseMarkdownToBlocks: jest.fn((markdown: string) => {
        const blocks: Array<{ type: string; props?: Record<string, unknown>; content?: Array<{ text: string }> }> = [];
        const lines = markdown.split('\n');

        for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) {
                continue;
            }

            if (trimmed.startsWith('# ')) {
                blocks.push({
                    type: 'heading',
                    props: { level: 1 },
                    content: [{ text: trimmed.slice(2) }],
                });
                continue;
            }

            if (trimmed.startsWith('## ')) {
                blocks.push({
                    type: 'heading',
                    props: { level: 2 },
                    content: [{ text: trimmed.slice(3) }],
                });
                continue;
            }

            if (trimmed.startsWith('#### ')) {
                blocks.push({
                    type: 'heading',
                    props: { level: 4 },
                    content: [{ text: trimmed.slice(5) }],
                });
                continue;
            }

            blocks.push({
                type: 'paragraph',
                content: [{ text: trimmed }],
            });
        }

        return blocks;
    }),
    tryParseHTMLToBlocks: jest.fn(() => []),
};

describe('custom-homepage fixture', () => {
    const source = readFileSync(CUSTOM_HOMEPAGE_PATH, 'utf-8');

    it('should parse hero markdown, buttons, and a two-column grid from the wrapped document', () => {
        const parsed = gmdToPartialBlocks(source, mockEditor);
        const allBlocks = collectBlocks(parsed);

        expect(allBlocks.some(block => block.type === 'heading' && block.content?.[0]?.text === 'Welcome to the Developer Portal')).toBe(true);
        expect(allBlocks.filter(block => block.type === 'graviteeButton').length).toBeGreaterThanOrEqual(2);
        expect(parsed.some(block => block.type === 'columnList')).toBe(true);

        const heroGrid = parsed.find(block => block.type === 'columnList');
        expect(heroGrid?.props).toMatchObject({ columns: '2' });
        expect(heroGrid?.children).toHaveLength(2);
    });
});
