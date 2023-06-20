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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatAutocompleteModule } from '@angular/material/autocomplete';

import { ApiProxyV4EntrypointsComponent } from './api-proxy-v4-entrypoints.component';
import { ApiProxyV4Module } from './api-proxy-v4.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api, ApiV4, ConnectorPlugin, fakeApiV4, UpdateApiV4 } from '../../../entities/management-api-v2';
import { GioFormListenersContextPathHarness } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { GioFormListenersVirtualHostHarness } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.harness';
import { Environment } from '../../../entities/environment/environment';
import { fakeEnvironment } from '../../../entities/environment/environment.fixture';

describe('ApiProxyV4EntrypointsComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiProxyV4EntrypointsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const createComponent = (environment: Environment, api: ApiV4) => {
    fixture = TestBed.createComponent(ApiProxyV4EntrypointsComponent);
    fixture.detectChanges();

    expectGetCurrentEnvironment(environment);
    expectGetEntrypoints();
    expectGetApi(api);
    expectGetPortalSettings();
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyV4Module, MatIconTestingModule, MatAutocompleteModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: API_ID } }],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  describe('API with context path', () => {
    const ENV = fakeEnvironment();
    const API = fakeApiV4({ listeners: [{ type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] }] });

    beforeEach(() => {
      createComponent(ENV, API);
      fixture.detectChanges();
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

    it('should switch to virtual host mode', async () => {
      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Enable virtual hosts' }));
      await switchButton.click();

      const harness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      expect(harness).toBeDefined();
      expect(await harness.getLastListenerRow().then((row) => row.pathInput.getValue())).toEqual('/context-path');
      expectGetPortalSettings();
    });
  });

  describe('API with virtual host', () => {
    const ENV = fakeEnvironment({ domainRestrictions: ['host', 'host2'] });
    const API = fakeApiV4({
      listeners: [{ type: 'HTTP', paths: [{ path: '/context-path', host: 'host' }], entrypoints: [{ type: 'http-get' }] }],
    });

    beforeEach(() => {
      createComponent(ENV, API);
      fixture.detectChanges();
    });

    it('should show virtual host', async () => {
      const harness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      const listeners = await harness.getListenerRows();
      expect(listeners.length).toEqual(1);
      expect(await listeners[0].pathInput.getValue()).toEqual('/context-path');

      expect(await listeners[0].hostDomainSuffix.getText()).toEqual('host');
    });

    it('should save changes to virtual host', async () => {
      const harness = await loader.getHarness(GioFormListenersVirtualHostHarness);

      await harness.addListener({ host: 'host2', path: '/new-context-path' });
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
            paths: [
              { host: 'host', path: '/context-path', overrideAccess: false },
              { host: 'host2', path: '/new-context-path', overrideAccess: false },
            ],
            entrypoints: API.listeners[0].entrypoints,
          },
        ],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(API);
    });

    it('should switch to context path mode', async () => {
      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Disable virtual hosts' }));
      await switchButton.click();

      const dialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialog.confirm();

      const harness = await loader.getHarness(GioFormListenersContextPathHarness);
      expect(harness).toBeDefined();
      expect(await harness.getLastListenerRow().then((row) => row.pathInput.getValue())).toEqual('/context-path');
      expectGetPortalSettings();
    });
  });

  const expectGetCurrentEnvironment = (environment: Environment) => {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}`, method: 'GET' }).flush(environment);
  };
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
