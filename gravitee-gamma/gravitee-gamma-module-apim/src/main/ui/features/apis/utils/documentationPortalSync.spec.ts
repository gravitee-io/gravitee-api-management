/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { DocumentationPage, PortalNavigationItem } from '../types/documentation';
import {
    breadcrumbFor,
    isUnderApiNode,
    mergeDocumentationWithPortal,
} from './documentationPortalSync';

const navItems: PortalNavigationItem[] = [
    { id: 'docs', type: 'FOLDER', title: 'Docs' },
    { id: 'api-nav', type: 'API', apiId: 'api-1', parentId: 'docs', title: 'Orders' },
    { id: 'guides', type: 'FOLDER', title: 'Guides', parentId: 'api-nav', published: true, order: 0 },
    { id: 'intro-nav', type: 'PAGE', title: 'Intro', parentId: 'guides', published: true, order: 0, portalPageContentId: 'c1' },
    { id: 'nav-only', type: 'PAGE', title: 'From navigation', parentId: 'api-nav', published: false, order: 1, portalPageContentId: 'c2' },
];

describe('documentationPortalSync', () => {
    it('overlays navigation metadata onto matching API pages and keeps navigation-only pages', () => {
        const classic: DocumentationPage[] = [
            { id: 'folder-1', name: 'Guides', type: 'FOLDER', parentId: undefined, published: false, order: 3 },
            { id: 'page-1', name: 'Intro', type: 'MARKDOWN', parentId: 'folder-1', published: false, order: 2 },
        ];

        const merged = mergeDocumentationWithPortal(classic, navItems, 'api-nav');

        expect(merged).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    id: 'folder-1',
                    name: 'Guides',
                    published: true,
                    order: 0,
                    portalNavId: 'guides',
                }),
                expect.objectContaining({
                    id: 'page-1',
                    name: 'Intro',
                    published: true,
                    order: 0,
                    portalNavId: 'intro-nav',
                    portalPageContentId: 'c1',
                }),
                expect.objectContaining({
                    id: 'nav-only',
                    name: 'From navigation',
                    type: 'MARKDOWN',
                    parentId: undefined,
                    portalNavId: 'nav-only',
                }),
            ]),
        );
        expect(merged).toHaveLength(3);
    });

    it('treats folders under an API node as API documentation, not destination folders', () => {
        const guides = navItems.find(item => item.id === 'guides')!;
        const docs = navItems.find(item => item.id === 'docs')!;
        expect(isUnderApiNode(navItems, guides)).toBe(true);
        expect(isUnderApiNode(navItems, docs)).toBe(false);
    });

    it('builds breadcrumbs from merged folder parents', () => {
        const pages: DocumentationPage[] = [
            { id: 'folder-1', name: 'Guides', type: 'FOLDER' },
            { id: 'page-1', name: 'Intro', type: 'MARKDOWN', parentId: 'folder-1' },
        ];
        expect(breadcrumbFor(pages, 'folder-1')).toEqual([{ id: 'folder-1', name: 'Guides', position: 1 }]);
        expect(breadcrumbFor(pages, null)).toEqual([]);
    });
});
