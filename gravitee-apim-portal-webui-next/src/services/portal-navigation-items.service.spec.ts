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

import { ConfigService } from './config.service';
import { PortalNavigationItemsService } from './portal-navigation-items.service';
import { fakeApi } from '../entities/api/api.fixtures';
import { PortalNavigationItem } from '../entities/portal-navigation/portal-navigation-item';
import { fakePortalNavigationApiProduct } from '../entities/portal-navigation/portal-navigation-item.fixture';
import { AppTestingModule } from '../testing/app-testing.module';

describe('PortalNavigationItemsService', () => {
  let service: PortalNavigationItemsService;
  let httpMock: HttpTestingController;
  const baseURL = 'http://localhost/portal/environments/DEFAULT';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [{ provide: ConfigService, useValue: { baseURL } }],
    });

    service = TestBed.inject(PortalNavigationItemsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load top navbar items and update topNavbar signal', done => {
    const mockItems: PortalNavigationItem[] = [
      {
        id: '1',
        organizationId: 'org1',
        environmentId: 'env1',
        title: 'Home',
        type: 'PAGE',
        area: 'TOP_NAVBAR',
        order: 0,
        portalPageContentId: 'content1',
        published: true,
        rootId: '1',
      },
      {
        id: '2',
        organizationId: 'org1',
        environmentId: 'env1',
        title: 'APIs',
        type: 'LINK',
        area: 'TOP_NAVBAR',
        order: 1,
        url: '/apis',
        published: true,
        rootId: '2',
      },
    ];

    service.loadTopNavBarItems().subscribe(items => {
      expect(items).toBeUndefined();
      expect(service.topNavbarItems()).toEqual(mockItems);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items` &&
        r.params.get('area') === 'TOP_NAVBAR' &&
        r.params.get('loadChildren') === 'false',
    );

    req.flush(mockItems);
  });

  it('should set topNavbar to empty array on HTTP error', done => {
    // set a non-empty value first to ensure it gets replaced
    service.topNavbarItems.set([
      {
        id: 'x',
        organizationId: 'org1',
        environmentId: 'env1',
        title: 'old',
        type: 'FOLDER',
        area: 'TOP_NAVBAR',
        order: 2,
        published: true,
        rootId: 'x',
      },
    ]);

    service.loadTopNavBarItems().subscribe(items => {
      expect(items).toBeUndefined();
      expect(service.topNavbarItems()).toEqual([]);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items` &&
        r.params.get('area') === 'TOP_NAVBAR' &&
        r.params.get('loadChildren') === 'false',
    );

    req.flush('Server error', { status: 500, statusText: 'Server Error' });
  });

  it('should get navigation item content', done => {
    const mockContent = {
      type: 'GRAVITEE_MARKDOWN',
      content: '# Welcome to the portal\nThis is the home page content.',
    };

    service.getNavigationItemContent('1').subscribe(content => {
      expect(content).toEqual(mockContent);
      done();
    });

    const req = httpMock.expectOne(`${baseURL}/portal-navigation-items/1/content`);
    expect(req.request.method).toBe('GET');
    req.flush(mockContent);
  });

  it('should get navigation item', done => {
    const mockItem = {
      id: 'x',
      organizationId: 'org1',
      environmentId: 'env1',
      title: 'old',
      type: 'FOLDER',
      area: 'TOP_NAVBAR',
      order: 2,
    };
    const id = 'testId';

    service.getNavigationItem(id).subscribe(items => {
      expect(items).toEqual(mockItem);
      done();
    });

    const req = httpMock.expectOne(r => r.method === 'GET' && r.url === `${baseURL}/portal-navigation-items/${id}`);

    req.flush(mockItem);
  });

  it('should preserve API Product navigation items', done => {
    const apiProductItem = fakePortalNavigationApiProduct({ area: 'HOMEPAGE' });

    service.getNavigationItems('HOMEPAGE').subscribe(items => {
      expect(items).toEqual([apiProductItem]);
      expect(items[0]).toMatchObject({
        type: 'API_PRODUCT',
        apiProductId: apiProductItem.apiProductId,
      });
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items` &&
        r.params.get('area') === 'HOMEPAGE' &&
        r.params.get('loadChildren') === 'true',
    );

    req.flush([apiProductItem]);
  });

  it('should search APIs and map to PortalNavigationApisSearchResponse', done => {
    const api = fakeApi({ id: 'api-1', name: 'Test API', version: '1.0', description: 'Desc' });
    const rawResponse = {
      data: [{ type: 'API' as const, apiId: api.id, id: 'nav-1', rootId: 'root-1' }],
      apis: [api],
      links: {},
      metadata: {
        pagination: {
          current_page: 1,
          size: 10,
          total: 42,
          total_pages: 5,
        },
      },
    };

    service.searchNavigationItemsWithApis(1, '', 10).subscribe(res => {
      expect(res.data).toHaveLength(1);
      expect(res.data[0]).toEqual({
        id: api.id,
        name: api.name,
        version: api.version,
        description: api.description,
        _links: api._links,
        mcp: api.mcp,
        labels: api.labels,
        rootId: 'root-1',
        navItemId: 'nav-1',
      });
      expect(res.metadata?.pagination?.current_page).toBe(1);
      expect(res.metadata?.pagination?.size).toBe(10);
      expect(res.metadata?.pagination?.total).toBe(42);
      expect(res.metadata?.pagination?.total_pages).toBe(5);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items/_search` &&
        r.params.get('type') === 'api' &&
        r.params.get('include') === 'api' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '10',
    );
    req.flush(rawResponse);
  });

  it('should search the catalog and preserve the mixed navigation item order', done => {
    const api = fakeApi({ id: 'api-1', name: 'Weather API', version: '1.0', description: 'Weather data' });
    const apiProduct = {
      id: '1fd5b522-272b-4ed5-8ada-3b4b777bad9c',
      name: 'AI Workspace',
      description: 'APIs for AI applications',
      version: '2.0',
      navigationItemId: 'product-nav-1',
      apis: [{ id: api.id, name: api.name, version: api.version }],
    };
    const rawResponse = {
      data: [
        { type: 'API_PRODUCT' as const, apiProductId: apiProduct.id, id: 'product-nav-1', rootId: 'product-root-1' },
        { type: 'API' as const, apiId: api.id, id: 'api-nav-1', rootId: 'api-root-1' },
      ],
      apis: [api],
      apiProducts: [apiProduct],
      links: {},
      metadata: {
        pagination: {
          current_page: 2,
          size: 20,
          total: 21,
          total_pages: 2,
        },
      },
    };

    service.searchCatalogItems(2, 'workspace', 20).subscribe(res => {
      expect(res.data).toEqual([
        {
          type: 'API_PRODUCT',
          id: apiProduct.id,
          name: apiProduct.name,
          description: apiProduct.description,
          version: apiProduct.version,
          rootId: 'product-root-1',
          navItemId: 'product-nav-1',
          apis: apiProduct.apis,
        },
        {
          type: 'API',
          id: api.id,
          name: api.name,
          version: api.version,
          description: api.description,
          _links: api._links,
          mcp: api.mcp,
          labels: api.labels,
          rootId: 'api-root-1',
          navItemId: 'api-nav-1',
        },
      ]);
      expect(res.metadata?.pagination.total).toBe(21);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items/_search` &&
        r.params.get('type') === 'catalog' &&
        r.params.getAll('include')?.join(',') === 'api,api_product' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '20' &&
        r.params.get('query') === 'workspace',
    );
    req.flush(rawResponse);
  });
});
