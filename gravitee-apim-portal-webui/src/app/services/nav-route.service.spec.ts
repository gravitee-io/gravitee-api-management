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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { UserTestingModule } from '../test/user-testing-module';
import { NavRouteService } from './nav-route.service';
import { TranslateService } from '@ngx-translate/core';
import { FeatureGuardService } from './feature-guard.service';
import { AuthGuardService } from './auth-guard.service';
import { CurrentUserService } from './current-user.service';
import { PermissionGuardService } from './permission-guard.service';

describe('NavRouteService', () => {
  let service: SpectatorService<NavRouteService>;
  const createService = createServiceFactory({
    service: NavRouteService,
    imports: [
      UserTestingModule,
      HttpClientTestingModule,
      RouterTestingModule.withRoutes([
        { path: 'foobar', redirectTo: '', children: [{ path: 'bar', redirectTo: '', children: [{ path: 'foo', redirectTo: '' }] }] },
        { path: 'catalog', redirectTo: '' },
        {
          path: 'catalogWithChildren',
          data: { menu: true },
          children: [
            { path: 'catalogChild', data: { title: 'Hey' }, redirectTo: '' },
            { path: 'otherChild', data: {}, redirectTo: '' },
          ],
        },
      ]),
    ],
    providers: [mockProvider(FeatureGuardService), mockProvider(ActivatedRoute)],
  });
  let routeService: NavRouteService;
  let permissionGuardService: PermissionGuardService;
  let featureGuardService: FeatureGuardService;
  beforeEach(() => {
    service = createService();
    service.inject(CurrentUserService);
    service.inject(AuthGuardService);
    service.inject(TranslateService);
    featureGuardService = service.inject(FeatureGuardService);
    permissionGuardService = service.inject(PermissionGuardService);
    routeService = service.service;
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
    featureGuardService.canActivate = jest.fn(() => true);
    const catalog = routeService.getRouteByPath('catalog');
    const routes = await routeService.getChildrenNav(catalog);
    expect(routes).toEqual(null);
  });

  it('should get children nav if parent have data.menu and child have data.title', async () => {
    featureGuardService.canActivate = jest.fn(() => true);
    const catalog = routeService.getRouteByPath('catalogWithChildren');
    const routes = await routeService.getChildrenNav(catalog);
    expect(routes.length).toEqual(1);
  });
});
