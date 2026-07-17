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
import type { PortalNavigationItem } from '../types';
import {
    ensureUniqueSlug,
    findFirstDescendantPageNavItem,
    findFirstVisibleDescendantPageNavItem,
    generateSlug,
    shortIdFromItemId,
    slugifyTitle,
} from './slug';

describe('slug utils', () => {
    describe('slugifyTitle', () => {
        it('should lowercase and hyphenate basic titles', () => {
            expect(slugifyTitle('Getting Started')).toBe('getting-started');
        });

        it('should strip special characters', () => {
            expect(slugifyTitle('API & Docs (v2)')).toBe('api-docs-v2');
        });

        it('should normalize unicode accents', () => {
            expect(slugifyTitle('Café Résumé')).toBe('cafe-resume');
        });

        it('should return untitled for empty strings', () => {
            expect(slugifyTitle('')).toBe('untitled');
            expect(slugifyTitle('   ')).toBe('untitled');
        });
    });

    describe('shortIdFromItemId', () => {
        it('should use the random tail of generated nav item ids', () => {
            expect(shortIdFromItemId('id-mvq7k2lm-k3j9f2a')).toBe('k3j9f2');
            expect(shortIdFromItemId('id-mvq7k2lm-abc123')).toBe('abc123');
        });

        it('should produce different suffixes for items created in the same millisecond', () => {
            const first = shortIdFromItemId('id-mvq7k2lm-k3j9f2a');
            const second = shortIdFromItemId('id-mvq7k2lm-x7p4q1');
            expect(first).not.toBe(second);
        });

        it('should use trailing characters for legacy ids', () => {
            expect(shortIdFromItemId('nav-getting-started')).toBe('tarted');
        });

        it('should fall back when id has no alphanumeric characters', () => {
            expect(shortIdFromItemId('---')).toBe('000000');
        });
    });

    describe('generateSlug', () => {
        it('should append a short id suffix from the random segment', () => {
            expect(generateSlug('Getting Started', 'id-lx8mvq0-k3j9f2a')).toBe('getting-started-k3j9f2');
        });
    });

    describe('ensureUniqueSlug', () => {
        it('should return the slug when it is unique', () => {
            expect(ensureUniqueSlug('home-abc123', new Set(['other-slug']))).toBe('home-abc123');
        });

        it('should append a numeric suffix on collision', () => {
            const existing = new Set(['home-abc123', 'home-abc123-2']);
            expect(ensureUniqueSlug('home-abc123', existing)).toBe('home-abc123-3');
        });
    });

    describe('findFirstDescendantPageNavItem', () => {
        const PORTAL_ID = 'test-portal';

        const nestedItems: PortalNavigationItem[] = [
            { id: 'folder-1', portalId: PORTAL_ID, title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
            { id: 'subfolder-1', portalId: PORTAL_ID, title: 'Basics', type: 'FOLDER', parentId: 'folder-1', order: 0, slug: 'basics' },
            { id: 'page-a', portalId: PORTAL_ID, title: 'Page A', type: 'PAGE', parentId: 'subfolder-1', order: 0, slug: 'page-a' },
            { id: 'page-b', portalId: PORTAL_ID, title: 'Page B', type: 'PAGE', parentId: 'folder-1', order: 1, slug: 'page-b' },
        ];

        it('should return the first page in depth-first order', () => {
            expect(findFirstDescendantPageNavItem(nestedItems, 'folder-1')?.id).toBe('page-a');
        });

        it('should return the first page inside a nested subfolder', () => {
            expect(findFirstDescendantPageNavItem(nestedItems, 'subfolder-1')?.id).toBe('page-a');
        });

        it('should skip link items and continue depth-first search', () => {
            const items: PortalNavigationItem[] = [
                ...nestedItems,
                {
                    id: 'link-1',
                    portalId: PORTAL_ID,
                    title: 'External',
                    type: 'LINK',
                    parentId: 'folder-1',
                    order: 0,
                    slug: 'external',
                    url: 'https://example.com',
                },
            ];

            expect(findFirstDescendantPageNavItem(items, 'folder-1')?.id).toBe('page-a');
        });

        it('should return undefined when no descendant page exists', () => {
            const items: PortalNavigationItem[] = [
                { id: 'empty-folder', portalId: PORTAL_ID, title: 'Empty', type: 'FOLDER', parentId: null, order: 0, slug: 'empty' },
            ];

            expect(findFirstDescendantPageNavItem(items, 'empty-folder')).toBeUndefined();
        });
    });

    describe('findFirstVisibleDescendantPageNavItem', () => {
        const PORTAL_ID = 'test-portal';

        it('should skip unpublished pages and containers', () => {
            const items: PortalNavigationItem[] = [
                { id: 'folder-1', portalId: PORTAL_ID, title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides', published: true },
                {
                    id: 'subfolder-1',
                    portalId: PORTAL_ID,
                    title: 'Hidden',
                    type: 'FOLDER',
                    parentId: 'folder-1',
                    order: 0,
                    slug: 'hidden',
                    published: false,
                },
                {
                    id: 'page-hidden',
                    portalId: PORTAL_ID,
                    title: 'Hidden Page',
                    type: 'PAGE',
                    parentId: 'subfolder-1',
                    order: 0,
                    slug: 'hidden-page',
                    published: true,
                },
                {
                    id: 'page-visible',
                    portalId: PORTAL_ID,
                    title: 'Visible Page',
                    type: 'PAGE',
                    parentId: 'folder-1',
                    order: 1,
                    slug: 'visible-page',
                    published: true,
                },
            ];

            expect(findFirstVisibleDescendantPageNavItem(items, 'folder-1')?.id).toBe('page-visible');
        });
    });
});
