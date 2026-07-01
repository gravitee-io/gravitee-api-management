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
import { splitGmdDocument } from './gmd-segments';

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

describe('gmd-segments', () => {
    it('should split markdown, gmd tags, and style blocks', () => {
        const gmd = `### Heading

<gmd-button link="/catalog">Explore</gmd-button>

<style>
.title { color: red; }
</style>`;

        const segments = splitGmdDocument(gmd);
        expect(segments).toHaveLength(3);
        expect(segments[0]).toEqual({ type: 'markdown', content: '### Heading' });
        expect(segments[1]?.type).toBe('element');
        expect(segments[1]?.type === 'element' && segments[1].outerHtml).toContain('gmd-button');
        expect(segments[2]?.type).toBe('element');
        expect(segments[2]?.type === 'element' && segments[2].outerHtml).toContain('<style');
    });

    it('should split homepage-like content with trailing style', () => {
        const segments = splitGmdDocument(HOMEPAGE_SNIPPET);
        expect(segments.length).toBeGreaterThanOrEqual(3);
        expect(segments.some(segment => segment.type === 'element' && segment.outerHtml.includes('<style'))).toBe(true);
    });

    it('should keep trailing style blocks as separate segments', () => {
        const gmd = `<gmd-grid columns="1"><gmd-md>Hello</gmd-md></gmd-grid>

<style>
.homepage-title { text-align: center; }
</style>`;

        const segments = splitGmdDocument(gmd);
        expect(segments).toHaveLength(2);
        expect(segments[1]?.type).toBe('element');
        expect(segments[1]?.type === 'element' && segments[1].outerHtml).toContain('.homepage-title');
    });
});
