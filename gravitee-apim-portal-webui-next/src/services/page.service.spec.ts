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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PageService } from './page.service';
import { PageTreeNode } from '../components/page-tree/page-tree.component';
import { fakePage, fakePagesResponse } from '../entities/page/page.fixtures';
import { PagesResponse } from '../entities/page/pages-response';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('PageService', () => {
  let service: PageService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PageService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('mapToPageTreeNode', () => {
    it('should map page list to list of page tree nodes', () => {
      const treeNodes = service.mapToPageTreeNode(undefined, [
        fakePage({ id: 'parent', name: 'Parent', parent: undefined, type: 'FOLDER' }),
        fakePage({ id: 'child-one', name: 'Child One', parent: 'parent', type: 'MARKDOWN' }),
        fakePage({ id: 'child-two', name: 'Child Two', parent: 'parent', type: 'FOLDER' }),
        fakePage({ id: 'grandchild', name: 'Grandchild', parent: 'child-two', type: 'MARKDOWN' }),
        fakePage({ id: 'lone-page', name: 'Lone Page', parent: undefined, type: 'MARKDOWN' }),
      ]);

      const expectedTreeNodes: PageTreeNode[] = [
        {
          id: 'parent',
          name: 'Parent',
          isFolder: true,
          children: [
            {
              id: 'child-one',
              name: 'Child One',
              isFolder: false,
              children: [],
            },
            {
              id: 'child-two',
              name: 'Child Two',
              isFolder: true,
              children: [{ id: 'grandchild', name: 'Grandchild', isFolder: false, children: [] }],
            },
          ],
        },
        {
          id: 'lone-page',
          name: 'Lone Page',
          isFolder: false,
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
        fakePage({ id: 'parent', name: 'Parent', parent: undefined, order: 99, type: 'FOLDER' }),
        fakePage({ id: 'child-one', name: 'Child One', parent: 'parent', order: 1, type: 'MARKDOWN' }),
        fakePage({ id: 'child-two', name: 'Child Two', parent: 'parent', order: 0, type: 'FOLDER' }),
        fakePage({ id: 'grandchild', name: 'Grandchild', parent: 'child-two', order: 0, type: 'MARKDOWN' }),
        fakePage({ id: 'lone-page', name: 'Lone Page', parent: undefined, order: 1, type: 'MARKDOWN' }),
      ]);

      const expectedTreeNodes: PageTreeNode[] = [
        {
          id: 'lone-page',
          name: 'Lone Page',
          isFolder: false,
          children: [],
        },
        {
          id: 'parent',
          name: 'Parent',
          isFolder: true,
          children: [
            {
              id: 'child-two',
              name: 'Child Two',
              isFolder: true,
              children: [{ id: 'grandchild', name: 'Grandchild', isFolder: false, children: [] }],
            },
            {
              id: 'child-one',
              name: 'Child One',
              isFolder: false,
              children: [],
            },
          ],
        },
      ];

      expect(treeNodes).toEqual(expectedTreeNodes);
    });
  });
  describe('listByApiId', () => {
    it('should return pages with default query parameters', done => {
      const pagesResponse: PagesResponse = fakePagesResponse();

      service.listByApiId('api-id').subscribe(response => {
        expect(response).toMatchObject(pagesResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages?homepage=false&page=1&size=-1`);
      expect(req.request.method).toEqual('GET');
      req.flush(pagesResponse);
    });

    it('should return pages with custom query parameters', done => {
      const pagesResponse: PagesResponse = fakePagesResponse();

      service.listByApiId('api-id', true, 2, 1).subscribe(response => {
        expect(response).toMatchObject(pagesResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages?homepage=true&page=2&size=1`);
      expect(req.request.method).toEqual('GET');
      req.flush(pagesResponse);
    });
  });

  describe('listByEnvironment', () => {
    it('should return pages with default query parameters', done => {
      const pagesResponse: PagesResponse = fakePagesResponse();

      service.listByEnvironment().subscribe(response => {
        expect(response).toMatchObject(pagesResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/pages?page=1&size=-1`);
      expect(req.request.method).toEqual('GET');
      req.flush(pagesResponse);
    });

    it('should return pages with custom query parameters', done => {
      const pagesResponse: PagesResponse = fakePagesResponse();

      service.listByEnvironment(2, 1).subscribe(response => {
        expect(response).toMatchObject(pagesResponse);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/pages?page=2&size=1`);
      expect(req.request.method).toEqual('GET');
      req.flush(pagesResponse);
    });
  });

  describe('getByApiIdAndId', () => {
    it('should return page with specified api id and id', done => {
      const page = fakePage({ id: 'page-id' });

      service.getByApiIdAndId('api-id', page.id).subscribe(response => {
        expect(response).toMatchObject(page);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages/${page.id}?include=content`);
      expect(req.request.method).toEqual('GET');
      req.flush(page);
    });
  });
  describe('getById', () => {
    it('should return page with specified api id and id', done => {
      const page = fakePage({ id: 'page-id' });

      service.getById(page.id).subscribe(response => {
        expect(response).toMatchObject(page);
        done();
      });

      const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/pages/${page.id}?include=content`);
      expect(req.request.method).toEqual('GET');
      req.flush(page);
    });
  });
});
