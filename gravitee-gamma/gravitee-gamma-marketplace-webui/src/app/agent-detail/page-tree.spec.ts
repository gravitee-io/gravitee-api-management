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
import { containsPage, findFirstPageId, mapToPageTree } from './page-tree';
import { buildPage } from '../../testing/factories';

describe('mapToPageTree', () => {
    it('should nest pages under folders and sort by order', () => {
        const tree = mapToPageTree([
            buildPage({ id: 'parent', name: 'Getting started', parent: undefined, order: 99, type: 'FOLDER' }),
            buildPage({ id: 'child-one', name: 'Overview', parent: 'parent', order: 1, type: 'MARKDOWN' }),
            buildPage({ id: 'child-two', name: 'Quick start', parent: 'parent', order: 0, type: 'FOLDER' }),
            buildPage({ id: 'grandchild', name: 'Skills', parent: 'child-two', order: 0, type: 'MARKDOWN' }),
            buildPage({ id: 'lone-page', name: 'Changelog', parent: undefined, order: 1, type: 'MARKDOWN' }),
        ]);

        expect(tree).toEqual([
            {
                id: 'lone-page',
                name: 'Changelog',
                isFolder: false,
                children: [],
            },
            {
                id: 'parent',
                name: 'Getting started',
                isFolder: true,
                children: [
                    {
                        id: 'child-two',
                        name: 'Quick start',
                        isFolder: true,
                        children: [{ id: 'grandchild', name: 'Skills', isFolder: false, children: [] }],
                    },
                    {
                        id: 'child-one',
                        name: 'Overview',
                        isFolder: false,
                        children: [],
                    },
                ],
            },
        ]);
    });

    it('should drop empty folders', () => {
        expect(
            mapToPageTree([
                buildPage({ id: 'empty', name: 'Empty', type: 'FOLDER' }),
                buildPage({ id: 'page', name: 'Page', type: 'MARKDOWN' }),
            ]),
        ).toEqual([{ id: 'page', name: 'Page', isFolder: false, children: [] }]);
    });

    it('should treat a missing parent as the root', () => {
        expect(mapToPageTree([buildPage({ id: 'root-page', parent: null, type: 'MARKDOWN' })])).toEqual([
            { id: 'root-page', name: 'Overview', isFolder: false, children: [] },
        ]);
    });
});

describe('findFirstPageId', () => {
    it('should return the first leaf in tree order', () => {
        const tree = mapToPageTree([
            buildPage({ id: 'folder', name: 'Guides', type: 'FOLDER', order: 0 }),
            buildPage({ id: 'nested', name: 'Skills', parent: 'folder', type: 'MARKDOWN', order: 0 }),
            buildPage({ id: 'root', name: 'Overview', type: 'MARKDOWN', order: 1 }),
        ]);

        expect(findFirstPageId(tree)).toBe('nested');
    });

    it('should return null when the tree is empty', () => {
        expect(findFirstPageId([])).toBeNull();
    });
});

describe('containsPage', () => {
    it('should find a nested page', () => {
        const tree = mapToPageTree([
            buildPage({ id: 'folder', name: 'Guides', type: 'FOLDER' }),
            buildPage({ id: 'nested', name: 'Skills', parent: 'folder', type: 'MARKDOWN' }),
        ]);
        const folder = tree[0];

        expect(folder?.id).toBe('folder');
        expect(folder ? containsPage(folder, 'nested') : false).toBe(true);
        expect(folder ? containsPage(folder, 'missing') : true).toBe(false);
    });
});
