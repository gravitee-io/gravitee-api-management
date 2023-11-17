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
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApiNavigationModule } from './api-navigation.module';
import { ApiNavigationComponent } from './api-navigation.component';

import { CurrentUserService } from '../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeApiV1, fakeApiV4 } from '../../../entities/management-api-v2';

describe('ApiNavigationComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiNavigationComponent>;
  let apiNgNavigationComponent: ApiNavigationComponent;
  let httpTestingController: HttpTestingController;
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['environment-api-c'];

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('without quality score', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ApiNavigationModule, MatIconTestingModule, NoopAnimationsModule, GioHttpTestingModule],
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                params: {
                  apiId: API_ID,
                },
              },
            },
          },
          { provide: CurrentUserService, useValue: { currentUser } },
          { provide: 'Constants', useValue: CONSTANTS_TESTING },
          { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiNavigationComponent);
      apiNgNavigationComponent = await fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    describe('Banners', () => {
      it('should display "Out of sync" banner', async (done) => {
        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners).toEqual([
            {
              title: 'This API is out of sync.',
              type: 'warning',
            },
          ]);
          done();
        });

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
      });
      it('should display "API version out-of-date" banner', async (done) => {
        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners.length).toEqual(1);
          expect(banners[0]).toMatchObject({
            title: 'API version out-of-date',
            type: 'warning',
          });
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
            method: 'GET',
          })
          .flush(
            fakeApiV1({
              id: API_ID,
            }),
          );
      });
    });
  });
});
