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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApiProxyEntrypointsModule } from './api-proxy-entrypoints.module';
import { ApiProxyEntrypointsComponent } from './api-proxy-entrypoints.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { Api } from '../../../../entities/api';
import { CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { MatButtonHarness } from '@angular/material/button/testing';
import { Environment } from '../../../../entities/environment/environment';
import { fakeEnvironment } from '../../../../entities/environment/environment.fixture';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

describe('ApiProxyEntrypointsComponent', () => {
  let fixture: ComponentFixture<ApiProxyEntrypointsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-gateway_definition-u'];

  const API_ID = 'apiId';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyEntrypointsModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyEntrypointsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('context-path mode', () => {
    beforeEach(() => {
      exceptEnvironmentGetRequest(fakeEnvironment());
    });

    it('should update context-path', async () => {
      const api = fakeApi({ id: API_ID, proxy: { virtual_hosts: [{ path: '/path' }] } });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const contextPathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=contextPath]' }));
      expect(await contextPathInput.isDisabled()).toEqual(false);
      expect(await contextPathInput.getValue()).toEqual('/path');

      await contextPathInput.setValue('/new-path');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([{ path: '/new-path' }]);
    });

    it('should switch to virtual-host mode', async () => {
      expectApiGetRequest(fakeApi({ id: API_ID }));

      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Switch to virtual-hosts mode' }));
      await switchButton.click();

      expect(await switchButton.getText()).toEqual('Switch to context-path mode');
    });
  });

  describe('virtual-host mode', () => {
    beforeEach(() => {
      exceptEnvironmentGetRequest(fakeEnvironment());
    });

    it('should switch to context-path mode', async () => {
      const api = fakeApi({ id: API_ID, proxy: { virtual_hosts: [{ path: '/path-foo', host: 'host' }, { path: '/path-bar' }] } });
      expectApiGetRequest(api);

      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Switch to context-path mode' }));
      await switchButton.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#switchContextPathConfirmDialog' }));
      const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Switch' }));
      await confirmDialogSwitchButton.click();

      expect(await switchButton.getText()).toEqual('Switch to virtual-hosts mode');

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([{ path: '/path-foo' }]);
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function exceptEnvironmentGetRequest(environment: Environment) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}`, method: 'GET' }).flush(environment);
    fixture.detectChanges();
  }
});
