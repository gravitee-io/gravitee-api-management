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
import { MOCK_ITEMS } from '../../../mocks/portal-navigation-item.mocks';

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

    expect(itemsTree[0].children).toHaveLength(2);
    expect(itemsTree[0].children![0].id).toEqual('f2');

    expect(itemsTree[0].children![0].children).toHaveLength(1);
    expect(itemsTree[0].children![0].children![0].id).toEqual('p1');

    expect(itemsTree[0].children![1].id).toEqual('p2');

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
});
