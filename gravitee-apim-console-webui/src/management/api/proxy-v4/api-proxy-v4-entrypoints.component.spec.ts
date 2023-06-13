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

import { ApiProxyV4EntrypointsComponent } from './api-proxy-v4-entrypoints.component';
import { ApiProxyV4Module } from './api-proxy-v4.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api, ApiV4, ConnectorPlugin, fakeApiV4, UpdateApiV4 } from '../../../entities/management-api-v2';
import { GioFormListenersContextPathHarness } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { PortalSettings } from '../../../entities/portal/portalSettings';

describe('ApiProxyV4EntrypointsComponent', () => {
  const API_ID = 'apiId';
  const API = fakeApiV4({ listeners: [{ type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] }] });
  let fixture: ComponentFixture<ApiProxyV4EntrypointsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const createComponent = (api: ApiV4) => {
    fixture = TestBed.createComponent(ApiProxyV4EntrypointsComponent);
    fixture.detectChanges();

    expectGetEntrypoints();
    expectGetApi(api);
    expectGetPortalSettings();
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyV4Module, MatIconTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: API_ID } }],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  beforeEach(() => {
    createComponent(API);
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  it('should show context paths', async () => {
    const harness = await loader.getHarness(GioFormListenersContextPathHarness);
    const listeners = await harness.getListenerRows();
    expect(listeners.length).toEqual(1);
    expect(await listeners[0].pathInput.getValue()).toEqual('/context-path');
  });

  it('should save changes to context paths', async () => {
    const harness = await loader.getHarness(GioFormListenersContextPathHarness);

    await harness.addListener({ path: '/new-context-path' });
    expectApiVerify();
    fixture.detectChanges();

    const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));

    expect(await saveButton.isDisabled()).toBeFalsy();
    await saveButton.click();

    // GET
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(API);
    // UPDATE
    const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    const expectedUpdateApi: UpdateApiV4 = {
      ...API,
      listeners: [
        {
          type: 'HTTP',
          paths: [{ path: '/context-path' }, { path: '/new-context-path' }],
          entrypoints: API.listeners[0].entrypoints,
        },
      ],
    };
    expect(saveReq.request.body).toEqual(expectedUpdateApi);
    saveReq.flush(API);
  });

  // TODO: add test for virtual host mode

  const expectGetApi = (api: Api) => {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  };

  const expectGetPortalSettings = () => {
    const settings: PortalSettings = { portal: { entrypoint: 'localhost' } };
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/settings`, method: 'GET' }).flush(settings);
  };

  const expectApiVerify = () => {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' });
  };

  const expectGetEntrypoints = () => {
    const entrypoints: Partial<ConnectorPlugin>[] = [
      { id: 'http-get', supportedApiType: 'MESSAGE', name: 'HTTP GET' },
      { id: 'http-post', supportedApiType: 'MESSAGE', name: 'HTTP POST' },
    ];

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(entrypoints);
  };
});
