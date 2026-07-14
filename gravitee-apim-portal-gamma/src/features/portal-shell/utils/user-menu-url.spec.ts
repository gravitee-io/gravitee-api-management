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
import type { PortalNavigationPage } from '../../portals/types';
import {
    getUserMenuItemDisplayUrl,
    isPortalPageSlug,
    normalizePortalHtmlHref,
    parsePortalPageSlug,
    resolvePortalHtmlLink,
    resolveUserMenuItemPath,
} from './user-menu-url';

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId: 'p1', title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

describe('user-menu-url', () => {
    describe('isPortalPageSlug', () => {
        it('should return true for a known slug', () => {
            expect(isPortalPageSlug('home-abc123', portalPages)).toBe(true);
        });

        it('should return false for external urls', () => {
            expect(isPortalPageSlug('https://example.com', portalPages)).toBe(false);
        });

        it('should return false for paths with slashes', () => {
            expect(isPortalPageSlug('/profile', portalPages)).toBe(false);
        });
    });

    describe('parsePortalPageSlug', () => {
        it('should parse a stored slug', () => {
            expect(parsePortalPageSlug('about-def456', portalPages)).toBe('about-def456');
        });

        it('should parse legacy view paths', () => {
            expect(parsePortalPageSlug('/portals/p1/home-abc123', portalPages, 'p1')).toBe('home-abc123');
        });

        it('should parse legacy edit paths', () => {
            expect(parsePortalPageSlug('/portals/p1/edit/home-abc123', portalPages, 'p1')).toBe('home-abc123');
        });

        it('should return null for external urls', () => {
            expect(parsePortalPageSlug('https://example.com', portalPages)).toBeNull();
        });

        it('should return null for unknown slugs', () => {
            expect(parsePortalPageSlug('unknown-slug', portalPages)).toBeNull();
        });
    });

    describe('resolveUserMenuItemPath', () => {
        it('should resolve portal page slugs via getPagePath', () => {
            const getPagePath = (slug: string) => `/portals/p1/edit/${slug}`;

            expect(resolveUserMenuItemPath('home-abc123', portalPages, getPagePath, 'p1')).toBe(
                '/portals/p1/edit/home-abc123',
            );
        });

        it('should pass through custom relative paths', () => {
            const getPagePath = (slug: string) => `/portals/p1/${slug}`;

            expect(resolveUserMenuItemPath('/profile', portalPages, getPagePath)).toBe('/profile');
        });

        it('should pass through external urls', () => {
            const getPagePath = (slug: string) => `/portals/p1/${slug}`;

            expect(resolveUserMenuItemPath('https://example.com', portalPages, getPagePath)).toBe('https://example.com');
        });
    });

    describe('getUserMenuItemDisplayUrl', () => {
        it('should display slug for portal pages', () => {
            expect(getUserMenuItemDisplayUrl('home-abc123', portalPages)).toBe('home-abc123');
            expect(getUserMenuItemDisplayUrl('/portals/p1/home-abc123', portalPages, 'p1')).toBe('home-abc123');
        });

        it('should display full url for non-portal links', () => {
            expect(getUserMenuItemDisplayUrl('/profile', portalPages)).toBe('/profile');
            expect(getUserMenuItemDisplayUrl('https://example.com', portalPages)).toBe('https://example.com');
        });
    });

    describe('normalizePortalHtmlHref', () => {
        it('should strip ./ prefix from relative portal links', () => {
            expect(normalizePortalHtmlHref('./home-abc123')).toBe('home-abc123');
        });

        it('should leave bare slugs unchanged', () => {
            expect(normalizePortalHtmlHref('home-abc123')).toBe('home-abc123');
        });
    });

    describe('resolvePortalHtmlLink', () => {
        const getPagePath = (slug: string) => `/portals/p1/${slug}`;

        it('should resolve bare portal page slugs', () => {
            expect(resolvePortalHtmlLink('home-abc123', portalPages, getPagePath, 'p1')).toEqual({
                slug: 'home-abc123',
                path: '/portals/p1/home-abc123',
            });
        });

        it('should resolve ./relative portal page slugs', () => {
            expect(resolvePortalHtmlLink('./about-def456', portalPages, getPagePath, 'p1')).toEqual({
                slug: 'about-def456',
                path: '/portals/p1/about-def456',
            });
        });

        it('should return null for unknown slugs', () => {
            expect(resolvePortalHtmlLink('./unknown-slug', portalPages, getPagePath, 'p1')).toBeNull();
        });

        it('should return null for external urls', () => {
            expect(resolvePortalHtmlLink('https://example.com', portalPages, getPagePath, 'p1')).toBeNull();
        });
    });
});
