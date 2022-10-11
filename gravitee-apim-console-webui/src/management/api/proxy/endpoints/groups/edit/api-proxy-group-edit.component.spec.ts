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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiProxyGroupEditComponent } from './api-proxy-group-edit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiProxyGroupsModule } from '../api-proxy-groups.module';
import { fakeApi } from '../../../../../../entities/api/Api.fixture';
import { Api } from '../../../../../../entities/api';

describe('ApiProxyGroupWrapperComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };

  let fixture: ComponentFixture<ApiProxyGroupEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyGroupsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: AjsRootScope, useValue: fakeRootScope },
      ],
    });

    fixture = TestBed.createComponent(ApiProxyGroupEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should go back to endpoints', async () => {
    const api = fakeApi({
      id: API_ID,
    });
    expectApiGetRequest(api);
    const routerSpy = jest.spyOn(fakeUiRouter, 'go');

    await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' })).then((button) => button.click());

    expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-endpoints', { apiId: API_ID }, undefined);
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
