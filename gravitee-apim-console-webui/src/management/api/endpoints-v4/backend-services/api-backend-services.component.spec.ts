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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { UIRouterGlobals } from '@uirouter/core';

import { ApiBackendServicesComponent } from './api-backend-services.component';
import { ApiBackendServicesModule } from './api-backend-services.module';
import { ApiEndpointsGroupsHarness } from './endpoints-groups/api-endpoints-groups.harness';

import { User } from '../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api, fakeApiV2, fakeApiV4 } from '../../../../entities/management-api-v2';
import { ApiProxyEndpointListHarness } from '../../proxy/endpoints/list/api-proxy-endpoint-list.harness';

describe('ApiPortalProxyEndpointsComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-r'];
  const proxyApiV2 = fakeApiV2({
    id: API_ID,
  });
  const proxyApiV4 = fakeApiV4({
    id: API_ID,
  });

  let fixture: ComponentFixture<ApiBackendServicesComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiBackendServicesModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterGlobals, useValue: { current: { data: { baseRouteState: null } } } },
      ],
    });

    fixture = TestBed.createComponent(ApiBackendServicesComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should init the component with proxy API V2', async () => {
    expectApiGetRequest(proxyApiV2);

    expect(await loader.getHarness(ApiProxyEndpointListHarness)).toBeTruthy();
  });

  it('should init the component with message API V4', async () => {
    expectApiGetRequest(proxyApiV4);

    expect(await loader.getHarness(ApiEndpointsGroupsHarness)).toBeTruthy();
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
