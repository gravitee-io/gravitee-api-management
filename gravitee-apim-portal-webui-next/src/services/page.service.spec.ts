/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { TestBed } from '@angular/core/testing';

import { PageService } from './page.service';
import { PageTreeNode } from '../components/page-tree/page-tree.component';
import { fakePage } from '../entities/page/page.fixtures';

describe('PageService', () => {
  let service: PageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PageService);
  });

  describe('mapToPageTreeNode', () => {
    it('should map page list to list of page tree nodes', () => {
      const treeNodes = service.mapToPageTreeNode(undefined, [
        fakePage({ id: 'parent', name: 'Parent', parent: undefined }),
        fakePage({ id: 'child-one', name: 'Child One', parent: 'parent' }),
        fakePage({ id: 'child-two', name: 'Child Two', parent: 'parent' }),
        fakePage({ id: 'grandchild', name: 'Grandchild', parent: 'child-two' }),
        fakePage({ id: 'lone-page', name: 'Lone Page', parent: undefined }),
      ]);

      const expectedTreeNodes: PageTreeNode[] = [
        {
          id: 'parent',
          name: 'Parent',
          children: [
            {
              id: 'child-one',
              name: 'Child One',
              children: [],
            },
            {
              id: 'child-two',
              name: 'Child Two',
              children: [{ id: 'grandchild', name: 'Grandchild', children: [] }],
            },
          ],
        },
        {
          id: 'lone-page',
          name: 'Lone Page',
          children: [],
        },
      ];

      expect(treeNodes).toEqual(expectedTreeNodes);
    });

    it('should map empty list', () => {
      const treeNodes = service.mapToPageTreeNode(undefined, []);
      expect(treeNodes).toEqual([]);
    });

    it('should map list with no root parent to empty list', () => {
      const treeNodes = service.mapToPageTreeNode(undefined, [
        fakePage({ id: 'child-one', name: 'Child One', parent: 'parent' }),
        fakePage({ id: 'child-two', name: 'Child Two', parent: 'parent' }),
        fakePage({ id: 'grandchild', name: 'Grandchild', parent: 'child-two' }),
      ]);
      expect(treeNodes).toEqual([]);
    });

    it('should sort page list to nodes', () => {
      const treeNodes = service.mapToPageTreeNode(undefined, [
        fakePage({ id: 'parent', name: 'Parent', parent: undefined, order: 99 }),
        fakePage({ id: 'child-one', name: 'Child One', parent: 'parent', order: 1 }),
        fakePage({ id: 'child-two', name: 'Child Two', parent: 'parent', order: 0 }),
        fakePage({ id: 'grandchild', name: 'Grandchild', parent: 'child-two', order: 0 }),
        fakePage({ id: 'lone-page', name: 'Lone Page', parent: undefined, order: 1 }),
      ]);

      const expectedTreeNodes: PageTreeNode[] = [
        {
          id: 'lone-page',
          name: 'Lone Page',
          children: [],
        },
        {
          id: 'parent',
          name: 'Parent',
          children: [
            {
              id: 'child-two',
              name: 'Child Two',
              children: [{ id: 'grandchild', name: 'Grandchild', children: [] }],
            },
            {
              id: 'child-one',
              name: 'Child One',
              children: [],
            },
          ],
        },
      ];

      expect(treeNodes).toEqual(expectedTreeNodes);
    });
  });
});
