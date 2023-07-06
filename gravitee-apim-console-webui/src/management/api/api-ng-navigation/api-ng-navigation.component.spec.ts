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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UIRouterModule } from '@uirouter/angular';

import { ApiNgNavigationModule } from './api-ng-navigation.module';
import { ApiNgNavigationComponent } from './api-ng-navigation.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';
import { User as DeprecatedUser } from '../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { fakeApiV4 } from '../../../entities/management-api-v2';

describe('ApiNgNavigationComponent', () => {
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiNgNavigationComponent>;
  let apiNgNavigationComponent: ApiNgNavigationComponent;
  let httpTestingController: HttpTestingController;
  let apiV2Service: ApiV2Service;
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['environment-api-c'];

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('without quality score', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          ApiNgNavigationModule,
          MatIconTestingModule,
          GioUiRouterTestingModule,
          NoopAnimationsModule,
          GioHttpTestingModule,
          UIRouterModule.forRoot({
            useHash: true,
          }),
        ],
        providers: [
          { provide: UIRouterState, useValue: fakeUiRouter },
          { provide: UIRouterStateParams, useValue: {} },
          { provide: CurrentUserService, useValue: { currentUser } },
          { provide: 'Constants', useValue: CONSTANTS_TESTING },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiNgNavigationComponent);
      apiNgNavigationComponent = await fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
      apiV2Service = TestBed.inject(ApiV2Service);
    });
    describe('Banners', () => {
      it('should display "Out of sync" banner', async (done) => {
        const API_ID = 'apiId';
        apiV2Service.get(API_ID).subscribe();

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
            method: 'GET',
          })
          .flush(
            fakeApiV4({
              id: API_ID,
              deploymentState: 'NEED_REDEPLOY',
            }),
          );

        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners).toEqual([
            {
              title: 'This API is out of sync.',
              type: 'warning',
            },
          ]);
          done();
        });
      });
    });
  });
});
