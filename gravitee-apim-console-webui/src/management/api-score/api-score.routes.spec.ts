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
import { Component, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { API_SCORE_ROUTES } from './api-score.routes';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';
import { CONSTANTS_TESTING } from '../../shared/testing';
import { Constants } from '../../entities/Constants';

@Component({
  template: '<router-outlet></router-outlet>',
  standalone: false,
})
class TestRootComponent {}

interface RouteTestBed {
  router: Router;
  ngZone: NgZone;
}

function createApiScoreRouteTestBed(permissions: string[]): RouteTestBed {
  TestBed.configureTestingModule({
    declarations: [TestRootComponent],
    imports: [
      RouterTestingModule.withRoutes([
        {
          path: '',
          component: TestRootComponent,
          canActivateChild: [PermissionGuard.checkRouteDataPermissions],
          children: API_SCORE_ROUTES,
        },
      ]),
    ],
    providers: [
      GioPermissionService,
      { provide: Constants, useValue: CONSTANTS_TESTING },
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
    ],
  });
  const gioPermissionService = TestBed.inject(GioPermissionService);
  gioPermissionService._setPermissions(permissions);

  const router = TestBed.inject(Router);
  const ngZone = TestBed.inject(NgZone);
  const fixture = TestBed.createComponent(TestRootComponent);
  fixture.detectChanges();

  return { router, ngZone };
}

describe('ApiScoreRoutingModule permissions', () => {
  it('should allow navigation to rulesets/import with environment-api-c', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-c']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/import'));
    expect(router.url).toBe('/rulesets/import');
  });

  it('should block navigation to rulesets/import without environment-api-c', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-r']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/import'));
    expect(router.url).toBe('/');
  });

  it('should allow navigation to rulesets/:id/edit with environment-api-u', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-u']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/123/edit'));
    expect(router.url).toBe('/rulesets/123/edit');
  });

  it('should block navigation to rulesets/:id/edit without environment-api-u', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-r']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/123/edit'));
    expect(router.url).toBe('/');
  });

  it('should allow navigation to rulesets/import-function with environment-api-c', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-c']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/import-function'));
    expect(router.url).toBe('/rulesets/import-function');
  });

  it('should allow navigation to rulesets/import-function with environment-api-u', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-u']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/import-function'));
    expect(router.url).toBe('/rulesets/import-function');
  });

  it('should block navigation to rulesets/import-function with only environment-api-r', async () => {
    const { router, ngZone } = createApiScoreRouteTestBed(['environment-api-r']);

    await ngZone.run(() => router.navigateByUrl('/rulesets/import-function'));
    expect(router.url).toBe('/');
  });
});
