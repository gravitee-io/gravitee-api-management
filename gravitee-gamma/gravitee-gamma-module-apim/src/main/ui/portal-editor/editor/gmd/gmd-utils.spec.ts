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
import { isHtmlOnlyHtmlBlock, isStyleOnlyHtmlBlock, looksLikeGmd, mergeTrailingStyleBlocks } from './gmd-utils';

describe('looksLikeGmd', () => {
    it('should return false for empty or plain text', () => {
        expect(looksLikeGmd('')).toBe(false);
        expect(looksLikeGmd('   ')).toBe(false);
        expect(looksLikeGmd('Hello world')).toBe(false);
    });

    it('should return false for plain markdown without GMD tags', () => {
        expect(looksLikeGmd('# Title\n\nBody')).toBe(false);
        expect(looksLikeGmd('- list item')).toBe(false);
    });

    it('should return true for gmd tags', () => {
        expect(looksLikeGmd('<gmd-button link="/catalog">Explore</gmd-button>')).toBe(true);
        expect(looksLikeGmd('<gmd-grid columns="3"><gmd-md>Hello</gmd-md></gmd-grid>')).toBe(true);
        expect(looksLikeGmd('<gmd-install-mcp name="API" transport="http" url="https://example.com" />')).toBe(true);
    });

    it('should return true for style blocks', () => {
        expect(looksLikeGmd('<style>.homepage-title { text-align: center; }</style>')).toBe(true);
    });

    it('should return true for mixed markdown and gmd tags', () => {
        expect(looksLikeGmd('### Title\n\n<gmd-button link="/catalog">Explore</gmd-button>')).toBe(true);
    });

    it('should return false for generic HTML without gmd tags', () => {
        expect(looksLikeGmd('<div>Hello</div>')).toBe(false);
        expect(looksLikeGmd('<img src="photo.png" />')).toBe(false);
    });

    it('should return true for HTML documents with trailing style blocks', () => {
        expect(looksLikeGmd('<div class="doc">Hello</div>\n<style>.doc { color: red; }</style>')).toBe(true);
    });
});

describe('html style block helpers', () => {
    it('should identify style-only and html-only graviteeHtml blocks', () => {
        expect(isStyleOnlyHtmlBlock({ type: 'graviteeHtml', props: { html: '', css: '.doc { color: red; }' } })).toBe(true);
        expect(isHtmlOnlyHtmlBlock({ type: 'graviteeHtml', props: { html: '<div class="doc">Hello</div>', css: '' } })).toBe(true);
        expect(isStyleOnlyHtmlBlock({ type: 'graviteeHtml', props: { html: '<div>Hi</div>', css: 'a {}' } })).toBe(false);
    });

    it('should merge trailing style blocks into preceding html blocks', () => {
        const merged = mergeTrailingStyleBlocks([
            { type: 'graviteeHtml', props: { html: '<div class="softco-doc"><h1>Title</h1></div>', css: '' } },
            { type: 'graviteeHtml', props: { html: '', css: '.softco-doc h1 { color: blue; }' } },
        ]);

        expect(merged).toHaveLength(1);
        expect(merged[0]).toMatchObject({
            type: 'graviteeHtml',
            props: {
                html: '<div class="softco-doc"><h1>Title</h1></div>',
                css: '.softco-doc h1 { color: blue; }',
            },
        });
    });

    it('should merge multiple trailing style blocks', () => {
        const merged = mergeTrailingStyleBlocks([
            { type: 'graviteeHtml', props: { html: '<div>Body</div>', css: '' } },
            { type: 'graviteeHtml', props: { html: '', css: '.a { color: red; }' } },
            { type: 'graviteeHtml', props: { html: '', css: '.b { margin: 0; }' } },
        ]);

        expect(merged).toHaveLength(1);
        expect(merged[0]?.props?.css).toContain('.a { color: red; }');
        expect(merged[0]?.props?.css).toContain('.b { margin: 0; }');
    });
});
