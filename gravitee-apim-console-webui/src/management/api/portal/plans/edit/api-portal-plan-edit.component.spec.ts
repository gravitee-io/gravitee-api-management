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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';

import { ApiPortalPlanEditComponent } from './api-portal-plan-edit.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiPortalPlansModule } from '../api-portal-plans.module';
import { Api } from '../../../../../entities/api';
import { fakeApi } from '../../../../../entities/api/Api.fixture';

describe('ApiPortalPlanEditComponent', () => {
  const API_ID = 'my-api';
  const currentUser = new User();
  const fakeUiRouter = { go: jest.fn() };
  currentUser.userPermissions = ['api-plan-u'];

  let fixture: ComponentFixture<ApiPortalPlanEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const configureTestingModule = (planId?: string) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalPlansModule, MatIconTestingModule],
      providers: [
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, planId } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        {
          provide: 'Constants',
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              oauth2: { enabled: false },
              jwt: { enabled: true },
            });
            return constants;
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ApiPortalPlanEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', () => {
    configureTestingModule();
    expectApiGetRequest(fakeApi({ id: API_ID }));
    expect(fixture.componentInstance).toBeTruthy();
    expect(loader).toBeTruthy();
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }
});
