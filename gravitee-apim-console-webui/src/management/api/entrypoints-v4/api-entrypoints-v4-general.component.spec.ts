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
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatRowHarnessColumnsText } from '@angular/material/table/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiEntrypointsV4GeneralComponent } from './api-entrypoints-v4-general.component';
import { ApiEntrypointsV4Module } from './api-entrypoints-v4.module';
import { ApiEntrypointsV4GeneralHarness } from './api-entrypoints-v4-general.harness';
import { ApiEntrypointsV4AddDialogHarness } from './edit/api-entrypoints-v4-add-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Api, ApiV4, ConnectorPlugin, fakeApiV4, UpdateApiV4 } from '../../../entities/management-api-v2';
import { GioFormListenersContextPathHarness } from '../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { GioFormListenersVirtualHostHarness } from '../component/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.harness';
import { RestrictedDomain } from '../../../entities/restricted-domain/restrictedDomain';
import { fakeRestrictedDomain, fakeRestrictedDomains } from '../../../entities/restricted-domain/restrictedDomain.fixture';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioFormListenersTcpHostsHarness } from '../component/gio-form-listeners/gio-form-listeners-tcp-hosts/gio-form-listeners-tcp-hosts.harness';
import { GioLicenseBannerHarness } from '../../../shared/components/gio-license-banner/gio-license-banner.harness';
import { License } from '../../../entities/license/License';

const ENTRYPOINTS: Partial<ConnectorPlugin>[] = [
  { id: 'http-get', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'HTTP GET', deployed: true },
  { id: 'http-post', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'HTTP POST', deployed: true },
  { id: 'sse', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP', name: 'Server-Sent Events', deployed: false },
  { id: 'webhook', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION', name: 'Webhook', deployed: false },
];

describe('ApiProxyV4EntrypointsComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiEntrypointsV4GeneralComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  const createComponent = async (
    restrictedDomains: RestrictedDomain[],
    api: ApiV4,
    getPortalSettings = true,
    permissions?: string[],
    checkLicense = true,
  ) => {
    await init(permissions);
    fixture.detectChanges();

    expectGetRestrictedDomain(restrictedDomains);
    expectGetEntrypoints();
    expectGetApi(api);

    if (getPortalSettings) {
      expectGetPortalSettings();
    }

    if (api.type === 'MESSAGE' && checkLicense) {
      expectLicenseGetRequest({ tier: 'universe', features: [], packs: [], scope: 'PLATFORM' });
    }
  };

  const init = async (permissions: string[] = ['api-definition-u', 'api-definition-r']) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiEntrypointsV4Module, MatIconTestingModule, MatAutocompleteModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: permissions },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
        isTabbable: () => true,
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ApiEntrypointsV4GeneralComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  };

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  describe('When API has HTTP PROXY architecture type', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      type: 'PROXY',
      listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get' }], paths: [{ path: '/path' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should show context paths only', async () => {
      const contextPath = await loader.getAllHarnesses(GioFormListenersContextPathHarness);
      expect(contextPath.length).toEqual(1);
      const virtualHost = await loader.getAllHarnesses(GioFormListenersVirtualHostHarness);
      expect(virtualHost.length).toEqual(0);
      const addEntrypointButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(addEntrypointButton.length).toEqual(0);
      const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEntrypointsV4GeneralHarness);
      expect(await harness.hasEntrypointsTable()).toEqual(false);
    });
  });

  describe('When API has TCP PROXY architecture type', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      type: 'PROXY',
      listeners: [{ type: 'TCP', entrypoints: [{ type: 'tcp-proxy' }], hosts: ['host'] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, false);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should show context paths only', async () => {
      const hosts = await loader.getAllHarnesses(GioFormListenersTcpHostsHarness);
      expect(hosts.length).toEqual(1);
      const addEntrypointButton = await loader.getAllHarnesses(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(addEntrypointButton.length).toEqual(0);
      const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEntrypointsV4GeneralHarness);
      expect(await harness.hasEntrypointsTable()).toEqual(false);
    });
  });

  describe('API with subscription listener only', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({ listeners: [{ type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] }] });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, false);
    });

    it('should not show context paths', async () => {
      const contextPath = await loader.getAllHarnesses(GioFormListenersContextPathHarness);
      expect(contextPath.length).toEqual(0);
      const virtualHost = await loader.getAllHarnesses(GioFormListenersVirtualHostHarness);
      expect(virtualHost.length).toEqual(0);
    });

    it('should show listener', async () => {
      const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEntrypointsV4GeneralHarness);
      const rows = await harness.getEntrypointsTableRows();
      expect(rows.length).toEqual(1);
    });
  });

  describe('API with context path', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({ listeners: [{ type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] }] });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, undefined, undefined, false);
    });

    afterEach(() => {
      expectApiPathVerify();
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
      expectApiPathVerify();
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

    it('should reset context-paths', async () => {
      const harness = await loader.getHarness(GioFormListenersContextPathHarness);

      await harness.addListener({ path: '/new-context-path' });
      expectApiPathVerify();
      fixture.detectChanges();

      expect(await harness.getLastListenerRow().then((row) => row.pathInput.getValue())).toEqual('/new-context-path');

      const resetButton = await loader.getHarness(MatButtonHarness.with({ text: 'Reset' }));

      expect(await resetButton.isDisabled()).toBeFalsy();
      await resetButton.click();

      expect(await harness.getLastListenerRow().then((row) => row.pathInput.getValue())).toEqual('/context-path');
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

  describe('API with hosts', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      type: 'PROXY',
      listeners: [{ type: 'TCP', hosts: ['host', 'host2'], entrypoints: [{ type: 'tcp-proxy' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, false);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should show hosts', async () => {
      const hostsHarness = await loader.getHarness(GioFormListenersTcpHostsHarness);
      const hosts = await hostsHarness.getListenerRows();
      expect(hosts.length).toEqual(2);
      expect(await hosts[0].hostInput.getValue()).toEqual('host');
      expect(await hosts[1].hostInput.getValue()).toEqual('host2');
    });

    it('should save changes to hosts', async () => {
      const harness = await loader.getHarness(GioFormListenersTcpHostsHarness);

      await harness.addListener({ host: 'host3' });
      expectApiHostVerify();

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
            type: 'TCP',
            hosts: ['host', 'host2', 'host3'],
            entrypoints: API.listeners[0].entrypoints,
          },
        ],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(API);
    });

    it('should reset hosts', async () => {
      const harness = await loader.getHarness(GioFormListenersTcpHostsHarness);

      await harness.addListener({ host: 'host3' });
      expectApiHostVerify();

      expect(await harness.getLastListenerRow().then((row) => row.hostInput.getValue())).toEqual('host3');

      const resetButton = await loader.getHarness(MatButtonHarness.with({ text: 'Reset' }));

      expect(await resetButton.isDisabled()).toBeFalsy();
      await resetButton.click();

      expect(await harness.getLastListenerRow().then((row) => row.hostInput.getValue())).toEqual('host2');
    });
  });

  describe('API with virtual host', () => {
    const RESTRICTED_DOMAINS = fakeRestrictedDomains(['host', 'host2']);
    const API = fakeApiV4({
      listeners: [{ type: 'HTTP', paths: [{ path: '/context-path', host: 'host' }], entrypoints: [{ type: 'http-get' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, undefined, ['api-definition-u'], false);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should show virtual host and disable button', async () => {
      const listeners = await loader.getHarness(GioFormListenersVirtualHostHarness).then((h) => h.getListenerRows());
      expect(listeners.length).toEqual(1);
      expect(await listeners[0].pathInput.getValue()).toEqual('/context-path');

      expect(await listeners[0].hostDomainSuffix.getText()).toEqual('host');
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);
      expect(await harness.canToggleListenerMode()).toEqual(true);
    });

    it('should save changes to virtual host', async () => {
      const harness = await loader.getHarness(GioFormListenersVirtualHostHarness);

      await harness.addListener({ host: 'host2', path: '/new-context-path' });
      expectApiPathVerify();
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
  });
  describe('API with virtual host but no domain restrictions', () => {
    const RESTRICTED_DOMAINS = [fakeRestrictedDomain()];
    const API = fakeApiV4({
      listeners: [{ type: 'HTTP', paths: [{ path: '/context-path', host: 'host' }], entrypoints: [{ type: 'http-get' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, undefined, undefined, false);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should allow to switch to context path mode', async () => {
      const switchButton = await loader.getHarness(MatButtonHarness.with({ text: 'Disable virtual hosts' }));
      await switchButton.click();

      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.confirm();

      const harness = await loader.getHarness(GioFormListenersContextPathHarness);
      expect(harness).toBeDefined();
      expect(await harness.getLastListenerRow().then((row) => row.pathInput.getValue())).toEqual('/context-path');
      expectGetPortalSettings();
    });
  });

  describe('Entrypoints management', () => {
    const RESTRICTED_DOMAINS = [];
    const API = () =>
      fakeApiV4({
        id: API_ID,
        listeners: [
          { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }, { type: 'http-post' }] },
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
        ],
      });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API());
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should show entrypoints list with action buttons', async () => {
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);
      const rows = await harness.getEntrypointsTableRows();
      expect(rows.length).toEqual(3);
      const entrypointsTypes: MatRowHarnessColumnsText[] = await Promise.all(rows.map(async (row) => await row.getCellTextByColumnName()));
      expect(entrypointsTypes.map((cell) => cell.type)).toEqual(['HTTP GET', 'HTTP POST', 'Webhook']);

      const actionCell = await rows[0].getCells({ columnName: 'actions' });
      expect(actionCell.length).toEqual(1);
      const actionButtons = await actionCell[0].getAllHarnesses(MatButtonHarness);
      expect(actionButtons.length).toEqual(2);
      expect(await actionButtons[0].getHarness(MatIconHarness).then((icon) => icon.getName())).toEqual('edit-pencil');
      expect(await actionButtons[1].getHarness(MatIconHarness).then((icon) => icon.getName())).toEqual('trash');
    });

    it('should remove entrypoint and save changes', async () => {
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);

      const tableRows = await harness.getEntrypointsTableRows();
      expect(tableRows.length).toEqual(3);

      // Find row to delete
      const allEntrypointsType = await Promise.all(
        tableRows.map((row) => row.getCells({ columnName: 'type' }).then((cells) => cells[0].getText())),
      );
      const indexToRemove = allEntrypointsType.indexOf('HTTP POST');
      expect(indexToRemove).toEqual(1);

      // Delete
      await harness.deleteEntrypointByIndex(1);

      const deleteDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await deleteDialog.confirm();

      // GET
      const currentApi = API();
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(currentApi);
      // UPDATE
      const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      const expectedUpdateApi: UpdateApiV4 = {
        ...currentApi,
        listeners: [
          { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] },
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
        ],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      const updatedApi: ApiV4 = {
        ...currentApi,
        listeners: [
          { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] },
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
        ],
      };
      saveReq.flush(updatedApi);
    });

    it('should not remove entrypoint on cancel', async () => {
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);

      const tableRows = await harness.getEntrypointsTableRows();
      expect(tableRows.length).toEqual(3);

      // Find row to delete
      const allEntrypointsType = await Promise.all(
        tableRows.map((row) => row.getCells({ columnName: 'type' }).then((cells) => cells[0].getText())),
      );
      const indexToRemove = allEntrypointsType.indexOf('HTTP POST');
      expect(indexToRemove).toEqual(1);

      // Delete
      await harness.deleteEntrypointByIndex(1);

      const deleteDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await deleteDialog.cancel();

      const rows = await harness.getEntrypointsTableRows();
      expect(rows.length).toEqual(3);
    });

    it('should not add listener when clicking on cancel', async () => {
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);

      const tableRows = await harness.getEntrypointsTableRows();
      expect(tableRows.length).toEqual(3);

      const addListenerButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(await addListenerButton.isDisabled()).toEqual(false);
      await addListenerButton.click();

      const dialog = await rootLoader.getHarness(ApiEntrypointsV4AddDialogHarness);
      const entrypointList = await dialog.getEntrypointSelectionList();
      const items = await entrypointList.getItems();

      expect(items.length).toEqual(1);
      expect(await items[0].getText()).toContain('Server-Sent Events');

      // check new entrypoint type
      await items[0].select();

      // click on save
      await dialog.getCancelButton().then((btn) => btn.click());
    });

    it('should add a new listener', async () => {
      const addListenerButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(await addListenerButton.isDisabled()).toEqual(false);
      await addListenerButton.click();

      const dialog = await rootLoader.getHarness(ApiEntrypointsV4AddDialogHarness);
      const entrypointList = await dialog.getEntrypointSelectionList();
      const items = await entrypointList.getItems();
      // check new entrypoint type
      await items[0].select();

      // click on save
      await dialog.getSaveButton().then((btn) => btn.click());
      const currentApi = API();
      expectGetApi(currentApi);
      // UPDATE
      const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      const updatedApi: ApiV4 = {
        ...currentApi,
        listeners: [
          {
            type: 'HTTP',
            ...currentApi.listeners.find((l) => l.type === 'HTTP'),
            entrypoints: [...currentApi.listeners.find((l) => l.type === 'HTTP').entrypoints, { type: 'sse', configuration: {} }],
          },
          ...currentApi.listeners.filter((l) => l.type !== 'HTTP'),
        ],
      };
      const expectedUpdateApi: UpdateApiV4 = { ...updatedApi };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(updatedApi);
    });
  });

  describe('When deleting the only entrypoint for HTTP listener', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      id: API_ID,
      listeners: [
        { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] },
        { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
      ],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should remove empty HTTP listener and attached context-path on save', async () => {
      const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEntrypointsV4GeneralHarness);

      const tableRows = await harness.getEntrypointsTableRows();

      // Find row to delete
      const allEntrypointsType = await Promise.all(
        tableRows.map((row) => row.getCells({ columnName: 'type' }).then((cells) => cells[0].getText())),
      );
      const indexToRemove = allEntrypointsType.indexOf('HTTP GET');
      expect(indexToRemove).toEqual(0);

      // Delete
      await harness.deleteEntrypointByIndex(indexToRemove);

      const deleteDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await deleteDialog.confirm();

      // GET
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(API);
      // UPDATE
      const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      const expectedUpdateApi: UpdateApiV4 = {
        ...API,
        listeners: [{ type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] }],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(API);
    });
  });

  describe('When API does not have corresponding listener for entrypoint to add', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      listeners: [
        { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }, { type: 'http-post' }, { type: 'sse' }] },
      ],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API);
    });

    afterEach(() => {
      expectApiPathVerify();
    });

    it('should add the corresponding listener', async () => {
      const addListenerButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(await addListenerButton.isDisabled()).toBeFalsy();
      await addListenerButton.click();

      const dialog = await rootLoader.getHarness(ApiEntrypointsV4AddDialogHarness);
      const entrypointList = await dialog.getEntrypointSelectionList();
      const items = await entrypointList.getItems();
      // check new entrypoint type
      await items[0].select();

      // click on save
      await dialog.getSaveButton().then((btn) => btn.click());
      expectGetApi(API);
      // UPDATE
      const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      const expectedUpdateApi: UpdateApiV4 = {
        ...API,
        listeners: [
          { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }, { type: 'http-post' }, { type: 'sse' }] },
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook', configuration: {} }] },
        ],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(API);
    });
  });

  describe('When API already has all existing listener type', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      listeners: [
        { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }, { type: 'http-post' }, { type: 'sse' }] },
        { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
      ],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API);
      expectApiPathVerify();
    });

    it('should disable add button if no listener type available', async () => {
      const addListenerButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(await addListenerButton.isDisabled()).toBeTruthy();
    });
  });

  describe('When API does not have HTTP listener', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      listeners: [{ type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, false);
    });

    it('should ask for the context path', async () => {
      const addListenerButton = await loader.getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }));
      expect(await addListenerButton.isDisabled()).toBeFalsy();
      await addListenerButton.click();

      const dialog = await rootLoader.getHarness(ApiEntrypointsV4AddDialogHarness);
      const entrypointList = await dialog.getEntrypointSelectionList();
      const items = await entrypointList.getItems();
      // check new entrypoint type
      await items[0].select();

      // click on save
      await dialog.getSaveButton().then((btn) => btn.click());

      // dialog should show context path component
      const contextPathForm = await dialog.getContextPathForm();
      await contextPathForm.getLastListenerRow().then((row) => row.pathInput.setValue('/new-context-path'));
      expectApiPathVerify();
      fixture.detectChanges();

      const saveBtn = await dialog.getSaveWithContextPathButton();
      expect(await saveBtn.isDisabled()).toEqual(false);
      await saveBtn.click();

      expectGetApi(API);
      // UPDATE
      const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
      const expectedUpdateApi: UpdateApiV4 = {
        ...API,
        listeners: [
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
          { type: 'HTTP', paths: [{ path: '/new-context-path' }], entrypoints: [{ type: 'http-get', configuration: {} }] },
        ],
      };
      expect(saveReq.request.body).toEqual(expectedUpdateApi);
      saveReq.flush(API);
    });
  });

  describe('user cannot update', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      id: API_ID,
      listeners: [
        { type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] },
        { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
      ],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, undefined, ['api-definition-r']);
      expectApiPathVerify();
    });
    it('should not show buttons that require permissions', async () => {
      // switch to virtual host mode/context path mode
      await loader
        .getHarness(MatButtonHarness.with({ text: 'Enable virtual hosts' }))
        .then((_) => fail('A user should not be able to enable virtual hosts'))
        .catch((err) => expect(err).toBeTruthy());

      // save changes or reset for context path form
      await loader
        .getHarness(MatButtonHarness.with({ text: 'Save changes' }))
        .then((_) => fail('A user should not be able to save context path form'))
        .catch((err) => expect(err).toBeTruthy());

      await loader
        .getHarness(MatButtonHarness.with({ text: 'Reset' }))
        .then((_) => fail('A user should not be able to reset context path form'))
        .catch((err) => expect(err).toBeTruthy());

      // actions for a given entrypoint
      const harness = await loader.getHarness(ApiEntrypointsV4GeneralHarness);
      const rows = await harness.getEntrypointsTableRows();
      expect(rows.length).toEqual(2);

      const actionCell = await rows[0].getCells({ columnName: 'actions' });
      expect(await actionCell[0].getAllHarnesses(MatButtonHarness).then((buttons) => buttons?.length)).toEqual(0);

      // add an entrypoint
      await loader
        .getHarness(MatButtonHarness.with({ text: 'Add an entrypoint' }))
        .then((_) => fail('A user should not be able to add a new entrypoint'))
        .catch((err) => expect(err).toBeTruthy());
    });
  });

  describe('When API has entrypoints not available in license feature', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      listeners: [{ type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'sse' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API);
      expectApiPathVerify();
    });

    it('should show the license banner', async () => {
      const licenseBanner = await loader.getAllHarnesses(GioLicenseBannerHarness);
      expect(licenseBanner.length).toEqual(1);
    });
  });

  describe('When API has only entrypoints available in license', () => {
    const RESTRICTED_DOMAINS = [];
    const API = fakeApiV4({
      listeners: [{ type: 'HTTP', paths: [{ path: '/context-path' }], entrypoints: [{ type: 'http-get' }] }],
    });

    beforeEach(async () => {
      await createComponent(RESTRICTED_DOMAINS, API, undefined, undefined, false);
      expectApiPathVerify();
    });

    it('should show the license banner', async () => {
      const licenseBanner = await loader.getAllHarnesses(GioLicenseBannerHarness);
      expect(licenseBanner.length).toEqual(0);
    });
  });
  function expectGetRestrictedDomain(restrictedDomains: RestrictedDomain[]): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/restrictedDomains`, method: 'GET' }).flush(restrictedDomains);
  }
  function expectGetApi(api: Api): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
  }

  function expectGetPortalSettings(): void {
    const requests = httpTestingController.match({ url: `${CONSTANTS_TESTING.env.baseURL}/settings`, method: 'GET' });
    expect(requests.length).toBeLessThanOrEqual(1);
  }

  function expectApiPathVerify(): void {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`, method: 'POST' });
  }

  function expectApiHostVerify(): void {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
  }

  function expectGetEntrypoints(): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(ENTRYPOINTS);
  }

  function expectLicenseGetRequest(license: License) {
    httpTestingController.expectOne({ url: LICENSE_CONFIGURATION_TESTING.resourceURL, method: 'GET' }).flush(license);
  }
});
