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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { GioPermissionService } from './gio-permission.service';
import { PermissionGuard } from './gio-permission.guard';

import { CONSTANTS_TESTING } from '../../testing';
import { Constants } from '../../../entities/Constants';

@Component({
  template: '<router-outlet></router-outlet>',
})
class TestRootComponent {}

@Component({
  template: '',
})
class TestComponent {}

describe('GioPermissionGuard', () => {
  let router: Router;
  let ngZone: NgZone;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestRootComponent, TestComponent],
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'test',
            component: TestComponent,
            canActivate: [PermissionGuard.checkRouteDataPermissions],
            data: {
              permissions: {
                anyOf: ['api-rating-r'],
              },
            },
          },
          {
            path: 'test2',
            component: TestComponent,
            canActivate: [PermissionGuard.checkRouteDataPermissions],
            data: {
              permissions: {
                anyOf: ['api-rating-u'],
              },
            },
          },
          {
            path: 'test3',
            component: TestComponent,
            canActivate: [PermissionGuard.checkRouteDataPermissions],
            data: {
              permissions: {
                anyOf: ['api-rating-u'],
                unauthorizedFallbackTo: '../test',
              },
            },
          },
          {
            path: '**',
            component: TestComponent,
            canActivate: [],
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
    router = TestBed.inject(Router);
    ngZone = TestBed.inject(NgZone);
    const gioPermissionService = TestBed.inject(GioPermissionService);

    gioPermissionService._setPermissions(['api-rating-r']);
    fixture = TestBed.createComponent(TestRootComponent);
    fixture.detectChanges();
  });

  it('should navigate to "test" if user has permission', async () => {
    expect(router.url).toBe('/');
    await ngZone.run(() => router.navigateByUrl('/test'));

    expect(router.url).toBe('/test');
  });

  it('should not navigate to "test2" if user has no permission', async () => {
    expect(router.url).toBe('/');
    await ngZone.run(() => router.navigateByUrl('/test2'));

    expect(router.url).toBe('/');
  });

  it('should navigate to "test" if user has no permission and unauthorizedFallbackTo is set', async () => {
    expect(router.url).toBe('/');
    await ngZone.run(() => router.navigateByUrl('/test3'));

    expect(router.url).toBe('/test');
  });
});
