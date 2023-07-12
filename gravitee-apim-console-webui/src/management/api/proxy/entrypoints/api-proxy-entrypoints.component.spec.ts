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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { UIRouterModule } from '@uirouter/angular';

import { ApiProxyEntrypointsModule } from './api-proxy-entrypoints.module';
import { ApiProxyEntrypointsComponent } from './api-proxy-entrypoints.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { Api } from '../../../../entities/api';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { Environment } from '../../../../entities/environment/environment';
import { fakeEnvironment } from '../../../../entities/environment/environment.fixture';
import { PortalSettings } from '../../../../entities/portal/portalSettings';

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
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        ApiProxyEntrypointsModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: AjsRootScope, useValue: null },
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
    httpTestingController.verify({ ignoreCancelled: true });
  });

  describe('context-path mode', () => {
    beforeEach(() => {
      exceptEnvironmentGetRequest(fakeEnvironment());
    });

    it('should update context-path', async () => {
      const api = fakeApi({ id: API_ID, proxy: { virtual_hosts: [{ path: '/path' }] } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const contextPathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=contextPath]' }));
      expect(await contextPathInput.isDisabled()).toEqual(false);
      expect(await contextPathInput.getValue()).toEqual('/path');

      await contextPathInput.setValue('/new-path');
      await expectVerifyContextPathGetRequest();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([{ path: '/new-path' }]);
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApi({ id: API_ID, proxy: { virtual_hosts: [{ path: '/path' }] }, definition_context: { origin: 'kubernetes' } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const contextPathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=contextPath]' }));
      expect(await contextPathInput.isDisabled()).toEqual(true);
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

    it('should update virtual-host', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          virtual_hosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const vhTable = await loader.getHarness(MatTableHarness.with({ selector: '#virtualHostsTable' }));
      const vhTableRows = await vhTable.getRows();
      const [vhTableFirstRowHostCell, vhTableFirstRowPathCell, vhTableFirstRowOverrideCell] = await vhTableRows[0].getCells();

      const vhTableFirstRowHostInput = await vhTableFirstRowHostCell.getHarness(MatInputHarness);
      expect(await vhTableFirstRowHostInput.getValue()).toEqual('host.io');
      await vhTableFirstRowHostInput.setValue('new-host');

      const vhTableFirstRowPathInput = await vhTableFirstRowPathCell.getHarness(MatInputHarness);
      expect(await vhTableFirstRowPathInput.getValue()).toEqual('/path-foo');
      await vhTableFirstRowPathInput.setValue('/new-path-foo');

      const vhTableFirstRowOverrideCheckbox = await vhTableFirstRowOverrideCell.getHarness(MatCheckboxHarness);
      expect(await vhTableFirstRowOverrideCheckbox.isChecked()).toEqual(false);
      await vhTableFirstRowOverrideCheckbox.check();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([
        {
          path: '/new-path-foo',
          host: 'new-host',
          override_entrypoint: true,
        },
        {
          host: 'host.io',
          override_entrypoint: false,
          path: '/path-bar',
        },
      ]);
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          virtual_hosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
        definition_context: { origin: 'kubernetes' },
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const vhTable = await loader.getHarness(MatTableHarness.with({ selector: '#virtualHostsTable' }));
      const vhTableRows = await vhTable.getRows();
      const [vhTableFirstRowHostCell, vhTableFirstRowPathCell, vhTableFirstRowOverrideCell, vhTableFirstRowRemoveCell] =
        await vhTableRows[0].getCells();

      const vhTableFirstRowHostInput = await vhTableFirstRowHostCell.getHarness(MatInputHarness);
      expect(await vhTableFirstRowHostInput.isDisabled()).toEqual(true);

      const vhTableFirstRowPathInput = await vhTableFirstRowPathCell.getHarness(MatInputHarness);
      expect(await vhTableFirstRowPathInput.isDisabled()).toEqual(true);

      const vhTableFirstRowOverrideCheckbox = await vhTableFirstRowOverrideCell.getHarness(MatCheckboxHarness);
      expect(await vhTableFirstRowOverrideCheckbox.isDisabled()).toEqual(true);

      const vhTableFirstRowButtons = await vhTableFirstRowRemoveCell.getAllHarnesses(MatButtonHarness);
      expect(vhTableFirstRowButtons.length).toEqual(0);

      expect((await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Add virtual-host' }))).length).toEqual(0);
    });

    it('should add virtual-host', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          virtual_hosts: [{ path: '/path-foo', host: 'host.io' }],
        },
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const addButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add virtual-host' }));
      await addButton.click();

      const vhTable = await loader.getHarness(MatTableHarness.with({ selector: '#virtualHostsTable' }));
      const vhTableRows = await vhTable.getRows();
      const [vhTableNewRowHostCell, vhTableNewRowPathCell] = await vhTableRows[1].getCells();

      const vhTableNewRowHostInput = await vhTableNewRowHostCell.getHarness(MatInputHarness);
      await vhTableNewRowHostInput.setValue('host-bar');

      const vhTableNewRowPathInput = await vhTableNewRowPathCell.getHarness(MatInputHarness);
      await vhTableNewRowPathInput.setValue('/path-bar');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([
        {
          path: '/path-foo',
          host: 'host.io',
          override_entrypoint: false,
        },
        {
          host: 'host-bar',
          override_entrypoint: false,
          path: '/path-bar',
        },
      ]);
    });

    it('should remove virtual-host', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          virtual_hosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      const vhTable = await loader.getHarness(MatTableHarness.with({ selector: '#virtualHostsTable' }));
      const vhTableRows = await vhTable.getRows();
      const [_1, _2, _3, vhTableFirstRowRemoveCell] = await vhTableRows[0].getCells();

      const vhTableFirstRowRemoveButton = await vhTableFirstRowRemoveCell.getHarness(MatButtonHarness);
      await vhTableFirstRowRemoveButton.click();

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([
        {
          host: 'host.io',
          override_entrypoint: false,
          path: '/path-bar',
        },
      ]);
    });

    it('should switch to context-path mode', async () => {
      const api = fakeApi({ id: API_ID, proxy: { virtual_hosts: [{ path: '/path-foo', host: 'host.io' }, { path: '/path-bar' }] } });
      expectApiGetRequest(api);

      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Switch to context-path mode' }));
      await switchButton.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#switchContextPathConfirmDialog' }));
      const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Switch' }));
      await confirmDialogSwitchButton.click();

      expect(await switchButton.getText()).toEqual('Switch to virtual-hosts mode');

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([{ path: '/path-foo' }]);
    });
  });

  describe('virtual-host mode with domain restriction', () => {
    beforeEach(() => {
      exceptEnvironmentGetRequest(fakeEnvironment({ domainRestrictions: ['fox.io', 'dog.io'] }));
    });

    it('should update virtual-host', async () => {
      const api = fakeApi({
        id: API_ID,
        proxy: {
          virtual_hosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.dog.io' },
            { path: '/path-joe', host: 'a' },
          ],
        },
      });
      expectApiGetRequest(api);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const vhTable = await loader.getHarness(MatTableHarness.with({ selector: '#virtualHostsTable' }));
      const vhTableRows = await vhTable.getRows();

      // Update /path-foo host
      const [vhTableFirstRowHostCell] = await vhTableRows[0].getCells();

      const vhTableFirstRowHostInput = await vhTableFirstRowHostCell.getHarness(MatInputHarness);
      expect(await vhTableFirstRowHostInput.getValue()).toEqual('host.io');
      await vhTableFirstRowHostInput.setValue('new-host');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

      const vhTableFirstRowHostAutocomplete = await vhTableFirstRowHostCell.getHarness(MatAutocompleteHarness);
      await vhTableFirstRowHostAutocomplete.selectOption({ text: /fox\.io$/ });

      // Update /path-joe host
      const [vhTableThirdRowHostCell] = await vhTableRows[2].getCells();
      const vhTableThirdRowHostAutocomplete = await vhTableThirdRowHostCell.getHarness(MatAutocompleteHarness);
      await vhTableThirdRowHostAutocomplete.selectOption({ text: /fox\.io$/ });

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtual_hosts).toEqual([
        {
          path: '/path-foo',
          host: 'new-host.fox.io',
          override_entrypoint: false,
        },
        {
          host: 'host.dog.io',
          override_entrypoint: false,
          path: '/path-bar',
        },
        {
          path: '/path-joe',
          host: 'a.fox.io',
          override_entrypoint: false,
        },
      ]);
    });
  });

  function expectApiGetPortalSettings() {
    const settings: PortalSettings = {
      portal: {
        entrypoint: 'entrypoint',
      },
    };
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/settings`, method: 'GET' }).flush(settings);
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function exceptEnvironmentGetRequest(environment: Environment) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}`, method: 'GET' }).flush(environment);
    fixture.detectChanges();
  }

  function expectVerifyContextPathGetRequest() {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' });
  }
});
