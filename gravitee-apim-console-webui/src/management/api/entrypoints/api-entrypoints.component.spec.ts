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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiEntrypointsModule } from './api-entrypoints.module';
import { ApiEntrypointsComponent } from './api-entrypoints.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { ApiV1, ApiV2, fakeApiV1, fakeApiV2 } from '../../../entities/management-api-v2';
import { GioFormListenersContextPathHarness } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { GioFormListenersVirtualHostHarness } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.harness';
import { RestrictedDomain } from '../../../entities/restricted-domain/restrictedDomain';
import { fakeRestrictedDomains } from '../../../entities/restricted-domain/restrictedDomain.fixture';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyEntrypointsComponent', () => {
  let fixture: ComponentFixture<ApiEntrypointsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const API_ID = 'apiId';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiEntrypointsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u', 'api-gateway_definition-u'] },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiEntrypointsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  function setupWithPermissions(permissions: string[]) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [ApiEntrypointsComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiEntrypointsModule, MatIconTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    });
    TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: permissions });
    fixture = TestBed.createComponent(ApiEntrypointsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  describe('context-path mode', () => {
    beforeEach(() => {
      exceptRestrictedDomainsGetRequest([]);
    });

    it('should update context-path', async () => {
      const api = fakeApiV2({ id: API_ID, proxy: { virtualHosts: [{ path: '/path' }] } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.isDisabled()).toBe(true);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersContextPathHarness);
      const contextPathInput = await formListenersContextPathHarness.getLastListenerRow().then(row => row.pathInput);
      expect(await contextPathInput.isDisabled()).toEqual(false);
      expect(await contextPathInput.getValue()).toEqual('/path');

      await contextPathInput.setValue('/new-path');
      expectVerifyContextPath();

      expect(await saveButton.isDisabled()).toEqual(false);
      await saveButton.click();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtualHosts).toEqual([{ path: '/new-path' }]);
    });

    it('should disable or hide save button with only api-definition-u permission', async () => {
      setupWithPermissions(['api-definition-u']);
      const saveButtons = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Save changes' }));
      expect(saveButtons.length === 0 || (saveButtons.length > 0 && (await saveButtons[0].isDisabled()))).toBe(true);
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApiV2({ id: API_ID, proxy: { virtualHosts: [{ path: '/path' }] }, definitionContext: { origin: 'KUBERNETES' } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Save changes' }));
      expect(saveButton.length).toEqual(0);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersContextPathHarness);
      const contextPathInput = await formListenersContextPathHarness.getLastListenerRow().then(row => row.pathInput);
      expect(await contextPathInput.isDisabled()).toEqual(true);
      expectVerifyContextPath();
    });

    it('should disable field when API definition version is V1', async () => {
      const api = fakeApiV1({ id: API_ID, proxy: { virtualHosts: [{ path: '/path' }] } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.length).toEqual(0);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersContextPathHarness);
      const contextPathInput = await formListenersContextPathHarness.getLastListenerRow().then(row => row.pathInput);
      expect(await contextPathInput.isDisabled()).toEqual(true);
      expectVerifyContextPath();
    });

    it('should switch to virtual-host mode', async () => {
      const api = fakeApiV2({ id: API_ID });
      expectApiGetRequest(api);
      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Enable virtual hosts' }));
      expect(await switchButton.isDisabled()).toEqual(false);
      await switchButton.click();

      expectApiGetPortalSettings();
      expectVerifyContextPath();
      expect(await switchButton.getText()).toEqual('Disable virtual hosts');
    });
  });

  describe('virtual-host mode', () => {
    beforeEach(() => {
      exceptRestrictedDomainsGetRequest([]);
    });

    it('should update virtual-host', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          virtualHosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.isDisabled()).toBe(true);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      const row = await formListenersContextPathHarness.getLastListenerRow();
      const vhTableFirstRowHostInput = row.hostSubDomainInput;
      expect(await vhTableFirstRowHostInput.getValue()).toEqual('host.io');
      await vhTableFirstRowHostInput.setValue('new-host');

      const vhTableFirstRowPathInput = row.pathInput;
      expect(await vhTableFirstRowPathInput.getValue()).toEqual('/path-bar');
      await vhTableFirstRowPathInput.setValue('/new-path-bar');

      const vhTableFirstRowOverrideCheckbox = row.overrideAccessInput;
      expect(await vhTableFirstRowOverrideCheckbox.isChecked()).toEqual(false);
      await vhTableFirstRowOverrideCheckbox.check();

      expect(await saveButton.isDisabled()).toEqual(false);
      await saveButton.click();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      expectVerifyContextPath();

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtualHosts).toEqual([
        {
          path: '/path-foo',
          host: 'host.io',
          overrideEntrypoint: false,
        },
        {
          host: 'new-host',
          path: '/new-path-bar',
          overrideEntrypoint: true,
        },
      ]);
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          virtualHosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
        definitionContext: { origin: 'KUBERNETES' },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const saveButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Save changes' }));
      expect(saveButton.length).toBe(0);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      const row = (await formListenersContextPathHarness.getListenerRows())[0];
      const vhTableFirstRowHostInput = row.hostSubDomainInput;
      expect(await vhTableFirstRowHostInput.isDisabled()).toEqual(true);

      const vhTableFirstRowPathInput = row.pathInput;
      expect(await vhTableFirstRowPathInput.isDisabled()).toEqual(true);

      const vhTableFirstRowOverrideCheckbox = row.overrideAccessInput;
      expect(await vhTableFirstRowOverrideCheckbox.isDisabled()).toEqual(true);

      const vhTableFirstRowButtons = row.removeButton;
      expect(vhTableFirstRowButtons).toBeNull();

      expect((await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Add virtual-host' }))).length).toEqual(0);
    });

    it('should disable when API definition version is V1', async () => {
      const api = fakeApiV1({
        id: API_ID,
        proxy: {
          virtualHosts: [{ path: '/path-foo', host: 'host.io' }],
        },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const saveButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Save changes' }));
      expect(saveButton.length).toBe(0);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      const row = (await formListenersContextPathHarness.getListenerRows())[0];
      const vhTableFirstRowHostInput = row.hostSubDomainInput;
      expect(await vhTableFirstRowHostInput.isDisabled()).toEqual(true);

      const vhTableFirstRowPathInput = row.pathInput;
      expect(await vhTableFirstRowPathInput.isDisabled()).toEqual(true);

      const vhTableFirstRowOverrideCheckbox = row.overrideAccessInput;
      expect(await vhTableFirstRowOverrideCheckbox.isDisabled()).toEqual(true);

      const vhTableFirstRowButtons = row.removeButton;
      expect(vhTableFirstRowButtons).toBeNull();

      expect((await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Add virtual-host' }))).length).toEqual(0);
    });

    it('should add virtual-host', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          virtualHosts: [{ path: '/path-foo', host: 'host.io' }],
        },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.isDisabled()).toBe(true);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      await formListenersContextPathHarness.addListenerRow();

      const { hostSubDomainInput, pathInput } = await formListenersContextPathHarness.getLastListenerRow();
      await hostSubDomainInput.setValue('host-bar');
      await pathInput.setValue('/path-bar');
      expectVerifyContextPath();

      expect(await saveButton.isDisabled()).toEqual(false);
      await saveButton.click();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtualHosts).toEqual([
        {
          path: '/path-foo',
          host: 'host.io',
          overrideEntrypoint: false,
        },
        {
          host: 'host-bar',
          overrideEntrypoint: false,
          path: '/path-bar',
        },
      ]);
    });

    it('should remove virtual-host', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          virtualHosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.io' },
          ],
        },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.isDisabled()).toBe(true);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);
      const { removeButton } = (await formListenersContextPathHarness.getListenerRows())[0];
      await removeButton.click();
      expectVerifyContextPath();

      expect(await saveButton.isDisabled()).toEqual(false);
      await saveButton.click();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtualHosts).toEqual([
        {
          host: 'host.io',
          overrideEntrypoint: false,
          path: '/path-bar',
        },
      ]);
    });

    it('should switch to context-path mode', async () => {
      const api = fakeApiV2({ id: API_ID, proxy: { virtualHosts: [{ path: '/path-foo', host: 'host.io' }, { path: '/path-bar' }] } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Disable virtual hosts' }));
      await switchButton.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#switchContextPathConfirmDialog' }));
      const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Switch' }));
      await confirmDialogSwitchButton.click();
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      expect(await switchButton.getText()).toEqual('Enable virtual hosts');

      // Expect fetch api get and update proxy
      expect(
        fixture.componentInstance.pathsFormControl.value.map(v => {
          return { host: v.host, path: v.path };
        }),
      ).toEqual([
        { host: '', path: '/path-foo' },
        { host: '', path: '/path-bar' },
      ]);
    });
  });

  describe('virtual-host mode with domain restriction', () => {
    beforeEach(() => {
      exceptRestrictedDomainsGetRequest(fakeRestrictedDomains(['fox.io', 'dog.io']));
    });

    it('should update virtual-host', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          virtualHosts: [
            { path: '/path-foo', host: 'host.io' },
            { path: '/path-bar', host: 'host.dog.io' },
            { path: '/path-joe', host: 'a' },
          ],
        },
      });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const saveButton = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
      expect(await saveButton.isDisabled()).toBe(true);

      const formListenersContextPathHarness = await loader.getHarness(GioFormListenersVirtualHostHarness);

      // Update /path-foo host
      const { hostSubDomainInput } = (await formListenersContextPathHarness.getListenerRows())[0];

      expect(await hostSubDomainInput.getValue()).toEqual('host.io');
      await hostSubDomainInput.setValue('new-host');

      // invalid host: save button should be disabled
      expect(await saveButton.isDisabled()).toEqual(true);

      await hostSubDomainInput.setValue('new-host.fox.io');
      // Update /path-joe host
      const secondRow = (await formListenersContextPathHarness.getListenerRows())[2];
      await secondRow.hostSubDomainInput.setValue('a.fox.io');
      expectVerifyContextPath();

      expect(await saveButton.isDisabled()).toEqual(false);
      await saveButton.click();

      // Expect fetch api get and update proxy
      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
      expect(req.request.body.proxy.virtualHosts).toEqual([
        {
          path: '/path-foo',
          host: 'new-host.fox.io',
          overrideEntrypoint: false,
        },
        {
          host: 'host.dog.io',
          overrideEntrypoint: false,
          path: '/path-bar',
        },
        {
          path: '/path-joe',
          host: 'a.fox.io',
          overrideEntrypoint: false,
        },
      ]);
    });

    it('should allow to switch to context-path mode', async () => {
      const api = fakeApiV2({ id: API_ID, proxy: { virtualHosts: [{ path: '/path-foo', host: 'host.io' }, { path: '/path-bar' }] } });
      expectApiGetRequest(api);
      expectApiGetPortalSettings();
      expectVerifyContextPath();

      const switchButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Disable virtual hosts' }));
      expect(switchButton.length).toEqual(1);
    });
  });

  function expectApiGetPortalSettings() {
    const settings: PortalSettings = {
      portal: {
        entrypoint: 'entrypoint',
      },
    };
    httpTestingController
      .match({ url: `${CONSTANTS_TESTING.env.baseURL}/portal`, method: 'GET' })
      .filter(r => !r.cancelled)
      .forEach(r => r.flush(settings));
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: ApiV2 | ApiV1) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function exceptRestrictedDomainsGetRequest(restrictedDomains: RestrictedDomain[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/restrictedDomains`, method: 'GET' }).flush(restrictedDomains);
    fixture.detectChanges();
  }

  function expectVerifyContextPath() {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`, method: 'POST' });
  }
});
