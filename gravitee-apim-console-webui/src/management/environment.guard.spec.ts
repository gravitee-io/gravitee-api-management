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
import { ActivatedRoute, Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { GioMenuSearchService } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { SettingsNavigationService } from './settings/settings-navigation/settings-navigation.service';
import { EnvironmentGuard } from './environment.guard';

import { EnvironmentSettingsService } from '../services-ngx/environment-settings.service';
import { Constants } from '../entities/Constants';
import { CONSTANTS_TESTING } from '../shared/testing';
import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { EnvironmentService } from '../services-ngx/environment.service';

@Component({
  template: '<router-outlet></router-outlet>',
})
class TestRootComponent {}

@Component({
  template: '',
})
class TestComponent {}

describe('EnvironmentGuard', () => {
  let router: Router;
  let ngZone: NgZone;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestRootComponent, TestComponent],
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'default',
            component: TestComponent,
            canActivate: [EnvironmentGuard.initEnvConfigAndLoadPermissions],
          },
          {
            path: '**',
            component: TestComponent,
            canActivate: [],
          },
        ]),
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { id: '123' } },
          },
        },
        { provide: ActivatedRoute, useValue: { params: { envHrid: 'DEFAULT' } } },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        {
          provide: EnvironmentService,
          useValue: {
            list: () =>
              of([
                {
                  id: 'default',
                  hrids: ['fr-apim-master-dev'],
                  organizationId: 'default',
                },
              ]),
          },
        },
        EnvironmentSettingsService,
        GioPermissionService,
        GioMenuSearchService,
        SettingsNavigationService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    ngZone = TestBed.inject(NgZone);
    fixture = TestBed.createComponent(TestRootComponent);
    fixture.detectChanges();
  });

  it('should navigate to default url form environment', async () => {
    expect(router.url).toBe('/');
    await ngZone.run(() => {
      return router.navigateByUrl('/default');
    });
    expect(router.url).toBe('/fr-apim-master-dev');
  });
});
