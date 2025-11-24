/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ConfigService } from './config.service';
import { PortalNavigationItemsService } from './portal-navigation-items.service';
import { PortalNavigationItem } from '../entities/portal-navigation/portal-navigation';

describe('PortalNavigationItemsService', () => {
  let service: PortalNavigationItemsService;
  let httpMock: HttpTestingController;
  const baseURL = 'http://localhost/portal/environments/DEFAULT';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
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
      },
    ];

    service.loadTopNavBarItems().subscribe(items => {
      expect(items).toEqual(mockItems);
      expect(service.topNavbar()).toEqual(mockItems);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items` &&
        r.params.get('area') === 'TOP_NAVBAR' &&
        r.params.get('loadChildren') === 'true',
    );

    req.flush(mockItems);
  });

  it('should set topNavbar to empty array on HTTP error', done => {
    // set a non-empty value first to ensure it gets replaced
    service.topNavbar.set([
      {
        id: 'x',
        organizationId: 'org1',
        environmentId: 'env1',
        title: 'old',
        type: 'FOLDER',
        area: 'TOP_NAVBAR',
        order: 2,
      },
    ]);

    service.loadTopNavBarItems().subscribe(items => {
      expect(items).toEqual([]);
      expect(service.topNavbar()).toEqual([]);
      done();
    });

    const req = httpMock.expectOne(
      r =>
        r.method === 'GET' &&
        r.url === `${baseURL}/portal-navigation-items` &&
        r.params.get('area') === 'TOP_NAVBAR' &&
        r.params.get('loadChildren') === 'true',
    );

    req.flush('Server error', { status: 500, statusText: 'Server Error' });
  });
});
