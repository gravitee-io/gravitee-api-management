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
import { TreeService } from './tree.service';
import { fakePortalNavigationFolder } from '../../../entities/portal-navigation/portal-navigation-item.fixture';
import { makeItem, MOCK_ITEMS } from '../../../mocks/portal-navigation-item.mocks';

const parentItem = fakePortalNavigationFolder();

let service: TreeService;

beforeEach(() => {
  service = new TreeService();
  service.init(parentItem, MOCK_ITEMS);
});

describe('DocumentationTreeService', () => {
  it('should create tree', () => {
    const itemsTree = service.getTree();

    expect(itemsTree).toBeTruthy();
    expect(itemsTree[0].id).toEqual('f1');

    expect(itemsTree[0].children).toHaveLength(3);
    expect(itemsTree[0].children![0].id).toEqual('f2');

    expect(itemsTree[0].children![0].children).toHaveLength(1);
    expect(itemsTree[0].children![0].children![0].id).toEqual('p1');

    expect(itemsTree[0].children![1].id).toEqual('api1');
    expect(itemsTree[0].children![1].children).toHaveLength(1);
    expect(itemsTree[0].children![1].children![0].id).toEqual('p-api1');

    expect(itemsTree[0].children![2].id).toEqual('p2');

    expect(itemsTree[1].id).toEqual('p3');
  });

  it('should find first page', () => {
    const page = service.findFirstPageId();
    expect(page).toBeTruthy();
    expect(page).toEqual('p1');
  });

  describe('test breadcrumbs', () => {
    it('should return default breadcrumb', () => {
      const breadcrumbs = service.getBreadcrumbsByDefault();
      expect(breadcrumbs).toBeTruthy();
      expect(breadcrumbs).toHaveLength(1);
      expect(breadcrumbs).toEqual([
        {
          id: 'nav-folder-1',
          label: 'Folder',
        },
      ]);
    });

    it('should return breadcrumb by id', () => {
      const breadcrumbs = service.getBreadcrumbsByNodeId('p1');
      expect(breadcrumbs).toBeTruthy();
      expect(breadcrumbs).toHaveLength(4);
      expect(breadcrumbs).toEqual([
        {
          id: 'nav-folder-1',
          label: 'Folder',
        },
        {
          id: 'f1',
          label: 'Folder 1',
        },
        {
          id: 'f2',
          label: 'Folder 2',
        },
        {
          id: 'p1',
          label: 'Page 1',
        },
      ]);
    });
  });

  describe('findFirstPageIdWithinNode', () => {
    it('should return first page within folder', () => {
      expect(service.findFirstPageIdWithinNode('f1')).toEqual('p1');
      expect(service.findFirstPageIdWithinNode('f2')).toEqual('p1');
    });

    it('should return null when node has no pages', () => {
      const items = [
        makeItem('f1', 'FOLDER', 'Folder 1', 0),
        makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
        makeItem('l1', 'LINK', 'Link 1', 0, 'f2'),
      ];
      service.init(parentItem, items);
      expect(service.findFirstPageIdWithinNode('f1')).toBeNull();
      expect(service.findFirstPageIdWithinNode('f2')).toBeNull();
    });

    it('should return first page within API node', () => {
      const items = [makeItem('api1', 'API', 'API 1', 0), makeItem('p-api1', 'PAGE', 'API doc', 0, 'api1')];
      service.init(parentItem, items);
      expect(service.findFirstPageIdWithinNode('api1')).toEqual('p-api1');
    });
  });

  describe('getAncestorApiId', () => {
    it('should return apiId when page is under API', () => {
      const items = [makeItem('api1', 'API', 'API 1', 0), makeItem('p-api1', 'PAGE', 'API doc', 0, 'api1')];
      service.init(parentItem, items);
      expect(service.getAncestorApiId('p-api1')).toEqual('api-api1');
    });

    it('should return null when page has no API ancestor', () => {
      expect(service.getAncestorApiId('p1')).toBeNull();
      expect(service.getAncestorApiId('p3')).toBeNull();
    });
  });
});
