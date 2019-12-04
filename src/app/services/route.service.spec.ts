/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { RouteService } from './route.service';
import { provideMock } from '../test/mock.helper.spec';
import { TranslateService } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { getTranslateServiceMock, TranslateTestingModule } from '../test/helper.spec';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FeatureGuardService } from './feature-guard.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthGuardService } from './auth-guard.service';

describe('RouteService', () => {

  let router: Router;
  let translateService: jasmine.SpyObj<TranslateService>;
  let featureGuardService: jasmine.SpyObj<FeatureGuardService>;
  let authGuardService: jasmine.SpyObj<AuthGuardService>;

  let routeService: RouteService;

  beforeEach(() => {

    TestBed.configureTestingModule({
      imports: [
        TranslateTestingModule,
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([
          { path: 'foobar', redirectTo: '', children: [{ path: 'bar', redirectTo: '', children: [{ path: 'foo', redirectTo: '' }] }] },
          { path: 'catalog', redirectTo: '' },
          {
            path: 'catalogWithChildren',
            data: { menu: true },
            children: [
              { path: 'catalogChild', data: { title: 'Hey' }, redirectTo: '' },
              { path: 'otherChild', data: {}, redirectTo: '' }
            ]
          }
        ]),
      ],
      providers: [
        provideMock(TranslateService),
        provideMock(FeatureGuardService),
        provideMock(ActivatedRoute)
      ]
    });

    router = TestBed.get(Router);
    translateService = getTranslateServiceMock();
    featureGuardService = TestBed.get(FeatureGuardService);
    authGuardService = TestBed.get(AuthGuardService);

    routeService = new RouteService(router, translateService, featureGuardService, authGuardService);
  });

  it('should be created', () => {
    expect(routeService).toBeTruthy();
    expect(routeService.getRouteByPath).toBeDefined();
  });

  it('should get route by path', () => {
    const routeByPath = routeService.getRouteByPath('foobar');
    expect(routeByPath.path).toEqual('foobar');
  });

  it('should get child route by path', () => {
    const routeByPath = routeService.getRouteByPath('foo');
    expect(routeByPath.path).toEqual('foo');
  });

  it('should get null children nav if parent does not have data.menu', async () => {
    featureGuardService.canActivate.and.returnValue(true);
    const service: RouteService = TestBed.get(RouteService);
    const catalog = routeService.getRouteByPath('catalog');
    const routes = await service.getChildrenNav(catalog);
    expect(routes).toEqual(null);
  });

  it('should get children nav if parent have data.menu and child have data.title', async () => {
    featureGuardService.canActivate.and.returnValue(true);
    const service: RouteService = TestBed.get(RouteService);
    const catalog = routeService.getRouteByPath('catalogWithChildren');
    const routes = await service.getChildrenNav(catalog);
    expect(routes.length).toEqual(1);
  });

});
