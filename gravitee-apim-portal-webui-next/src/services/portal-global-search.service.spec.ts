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
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PortalGlobalSearchService } from './portal-global-search.service';
import { PortalNavigationItemsService } from './portal-navigation-items.service';

describe('PortalGlobalSearchService', () => {
  let service: PortalGlobalSearchService;
  let searchCatalogItems: jest.Mock;
  let searchNavigationItemsWithApis: jest.Mock;
  let getNavigationItems: jest.Mock;
  let getNavigationItemContent: jest.Mock;

  beforeEach(() => {
    searchCatalogItems = jest.fn();
    searchNavigationItemsWithApis = jest.fn();
    getNavigationItems = jest.fn();
    getNavigationItemContent = jest.fn();

    TestBed.configureTestingModule({
      providers: [
        PortalGlobalSearchService,
        {
          provide: PortalNavigationItemsService,
          useValue: {
            topNavbarItems: signal([{ id: 'doc-root', type: 'FOLDER', title: 'Docs' }]),
            searchCatalogItems,
            searchNavigationItemsWithApis,
            getNavigationItems,
            getNavigationItemContent,
          },
        },
      ],
    });

    service = TestBed.inject(PortalGlobalSearchService);
  });

  it('should return empty results for blank query', done => {
    service.search('   ').subscribe(results => {
      expect(results).toEqual([]);
      done();
    });
  });

  it('should merge API catalog hits with folder and page title matches', done => {
    searchCatalogItems.mockImplementation((_page: number, _query: string, size: number) => {
      if (size === -1) {
        return of({ data: [{ type: 'API', rootId: 'api-root', navItemId: 'api-nav', id: 'api-1', name: 'Payments API', version: '1.0' }] });
      }
      return of({
        data: [
          {
            type: 'API',
            id: 'api-1',
            name: 'Payments API',
            version: '1.0',
            rootId: 'api-root',
            navItemId: 'api-nav',
          },
        ],
      });
    });
    searchNavigationItemsWithApis.mockReturnValue(of({ data: [] }));
    getNavigationItems.mockReturnValue(
      of([
        {
          id: 'folder-1',
          title: 'Auth folder',
          type: 'FOLDER',
          published: true,
          rootId: 'doc-root',
        },
        {
          id: 'page-1',
          title: 'Authentication guide',
          type: 'PAGE',
          published: true,
          rootId: 'doc-root',
          portalPageContentId: 'content-1',
        },
      ]),
    );
    getNavigationItemContent.mockReturnValue(of({ type: 'GRAVITEE_MARKDOWN', content: '# Secret token lifecycle' }));

    service.search('auth').subscribe(results => {
      expect(results.some(result => result.kind === 'API' && result.title === 'Payments API')).toBe(true);
      expect(results.some(result => result.kind === 'FOLDER' && result.title === 'Auth folder')).toBe(true);
      expect(results.some(result => result.kind === 'PAGE' && result.title === 'Authentication guide')).toBe(true);
      done();
    });
  });

  it('should match page content when the title does not match', done => {
    searchCatalogItems.mockImplementation((_page: number, _query: string, size: number) => {
      if (size === -1) {
        return of({ data: [] });
      }
      return of({ data: [] });
    });
    searchNavigationItemsWithApis.mockReturnValue(of({ data: [] }));
    getNavigationItems.mockReturnValue(
      of([
        {
          id: 'page-2',
          title: 'Overview',
          type: 'PAGE',
          published: true,
          rootId: 'doc-root',
        },
      ]),
    );
    getNavigationItemContent.mockReturnValue(of({ type: 'GRAVITEE_MARKDOWN', content: 'Rotate the secret token lifecycle weekly.' }));

    service.search('token').subscribe(results => {
      expect(results).toEqual([
        expect.objectContaining({
          kind: 'PAGE',
          title: 'Overview',
          matchIn: 'content',
          subtitle: expect.stringContaining('token'),
        }),
      ]);
      done();
    });
  });
});
