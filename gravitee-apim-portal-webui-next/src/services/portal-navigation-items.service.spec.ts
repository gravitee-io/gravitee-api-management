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
});
