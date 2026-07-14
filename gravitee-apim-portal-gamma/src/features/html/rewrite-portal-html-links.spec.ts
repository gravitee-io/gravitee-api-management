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
import type { PortalNavigationPage } from '../portals/types';
import { rewritePortalHtmlLinks } from './rewrite-portal-html-links';

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId: 'p1', title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

const getPagePath = (slug: string) => `/portals/p1/${slug}`;

describe('rewritePortalHtmlLinks', () => {
    it('should rewrite bare portal page slugs', () => {
        const html = '<a href="home-abc123">Home</a>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(
            '<a href="/portals/p1/home-abc123">Home</a>',
        );
    });

    it('should rewrite ./relative portal page slugs', () => {
        const html = '<a href="./about-def456">About</a>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(
            '<a href="/portals/p1/about-def456">About</a>',
        );
    });

    it('should leave external links unchanged', () => {
        const html = '<a href="https://example.com">External</a>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(html);
    });

    it('should leave unknown slugs unchanged', () => {
        const html = '<a href="./unknown-slug">Unknown</a>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(html);
    });

    it('should return original html when no anchors are present', () => {
        const html = '<p>No links here</p>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(html);
    });

    it('should rewrite only matching anchors in mixed content', () => {
        const html = '<p><a href="./home-abc123">Home</a> and <a href="https://example.com">External</a></p>';
        expect(rewritePortalHtmlLinks(html, portalPages, getPagePath, 'p1')).toBe(
            '<p><a href="/portals/p1/home-abc123">Home</a> and <a href="https://example.com">External</a></p>',
        );
    });
});
