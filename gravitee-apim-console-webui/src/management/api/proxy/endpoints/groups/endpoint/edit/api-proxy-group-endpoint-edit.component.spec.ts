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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

import { ApiProxyGroupEndpointEditComponent } from './api-proxy-group-endpoint-edit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ApiProxyGroupEndpointModule } from '../api-proxy-group-endpoint.module';
import { Api } from '../../../../../../../entities/api';
import { ConnectorListItem } from '../../../../../../../entities/connector/connector-list-item';
import { fakeApi } from '../../../../../../../entities/api/Api.fixture';
import { fakeConnectorListItem } from '../../../../../../../entities/connector/connector-list-item.fixture';
import { fakeTenant } from '../../../../../../../entities/tenant/tenant.fixture';
import { SnackBarService } from '../../../../../../../services-ngx/snack-bar.service';
import { User } from '../../../../../../../entities/user';

describe('ApiProxyGroupEndpointEditComponent', () => {
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const DEFAULT_ENDPOINT_NAME = 'endpoint#1';
  const fakeUiRouter = { go: jest.fn() };
  const tenants = [fakeTenant({ id: 'tenant#1', name: 'tenant#1-name' }), fakeTenant({ id: 'tenant#2', name: 'tenant#2' })];
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  let fixture: ComponentFixture<ApiProxyGroupEndpointEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let httpConnector: ConnectorListItem;
  let grpcConnector: ConnectorListItem;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyGroupEndpointModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: DEFAULT_ENDPOINT_NAME } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });

    httpConnector = fakeConnectorListItem({ schema: '{ "name": "http-schema" }', supportedTypes: ['http'] });
    grpcConnector = fakeConnectorListItem({ schema: '{ "name": "grpc-schema" }', supportedTypes: ['grpc'] });
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Edit mode', () => {
    let api: Api;

    beforeEach(async () => {
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'endpoint#1',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: true,
                },
                {
                  name: 'endpoint#2',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: true,
                },
              ],
            },
          ],
        },
      });

      expectApiGetRequest(api);
      expectConnectorRequest();
      expectTenantsRequest();
    });

    it('should go back to endpoints', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.endpoints', { apiId: API_ID }, undefined);
    });

    it('should warn the user on update error', async () => {
      const snackBarServiceSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
        .then((input) => input.setValue('endpoint#1 updated'));

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      await saveBar.clickSubmit();

      expectApiGetRequest(api);
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' })
        .error(new ErrorEvent('error', { message: 'An error occurred' }));
      expect(snackBarServiceSpy).toHaveBeenCalledWith('An error occurred');
    });

    describe('Edit general information of existing endpoint', () => {
      it('should not be able to save changes when endpoint name is already used', async () => {
        await loader
          .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
          .then((input) => input.setValue('endpoint#2'));

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeTruthy();
      });

      it('should update existing endpoint', async () => {
        expect(fixture.componentInstance.supportedTypes).toEqual(['http', 'grpc']);

        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }));
        expect(await nameInput.getValue()).toEqual(DEFAULT_ENDPOINT_NAME);

        const newEndpointName = 'new-endpoint-name';
        await nameInput.setValue(newEndpointName);
        expect(await nameInput.getValue()).toEqual(newEndpointName);

        const tenantsSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint tenants"]' }));
        await tenantsSelect.clickOptions({ text: tenants[0].name });
        expect(await tenantsSelect.getValueText()).toEqual(tenants[0].name);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });
        expect(req.request.body.proxy).toStrictEqual({
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: newEndpointName,
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: true,
                  tenants: [tenants[0].id],
                  healthcheck: {
                    enabled: false,
                  },
                },
                {
                  backup: false,
                  inherit: true,
                  name: 'endpoint#2',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  type: 'HTTP',
                  weight: 1,
                },
              ],
            },
          ],
        });
      });

      it('should find connector schema', async () => {
        expect(fixture.componentInstance.configurationSchema).toBeTruthy();
        expect(fixture.componentInstance.configurationSchema['name']).toEqual('http-schema');

        await loader
          .getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint type"]' }))
          .then((select) => select.clickOptions({ text: 'grpc' }));

        expect(fixture.componentInstance.configurationSchema).toBeTruthy();
        expect(fixture.componentInstance.configurationSchema['name']).toEqual('grpc-schema');
      });
    });

    describe('Edit configuration of existing endpoint', () => {
      beforeEach(async () => {
        await loader.getHarness(MatTabHarness.with({ label: 'Configuration' })).then((tab) => tab.select());
        fixture.detectChanges();
      });

      it('should update existing endpoint configuration', async () => {
        const inherit = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="inherit"]' }));
        expect(fixture.debugElement.nativeElement.querySelector('gv-schema-form-group')).toBeFalsy();

        await inherit.toggle();

        expect(fixture.debugElement.nativeElement.querySelector('gv-schema-form-group')).toBeTruthy();
        expect(fixture.componentInstance.configurationForm.getRawValue().inherit).toStrictEqual(false);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);

        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });
        expect(req.request.body.proxy).toStrictEqual({
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'endpoint#1',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  tenants: null,
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                  headers: [],
                  http: {},
                  proxy: { enabled: false },
                  ssl: {},
                  healthcheck: {
                    enabled: false,
                  },
                },
                {
                  backup: false,
                  inherit: true,
                  name: 'endpoint#2',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  type: 'HTTP',
                  weight: 1,
                },
              ],
            },
          ],
        });
      });
    });
  });

  describe('Create mode with existing endpoints', () => {
    let api: Api;

    beforeEach(async () => {
      TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: null } });
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'default',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                },
              ],
            },
          ],
        },
      });

      expectApiGetRequest(api);
      expectConnectorRequest();
      expectTenantsRequest();
    });

    it('should create new endpoint', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
        .then((input) => input.setValue('endpoint#3'));

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint type"]' }))
        .then((select) => select.clickOptions({ text: 'http' }));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint weight input"]' }))
        .then((input) => input.setValue('42'));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint target input"]' }))
        .then((input) => input.setValue('https//dummy.com'));
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      await saveBar.clickSubmit();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });
      expect(req.request.body.proxy).toStrictEqual({
        groups: [
          {
            name: 'default-group',
            endpoints: [
              {
                name: 'default',
                target: 'https://api.le-systeme-solaire.net/rest/',
                weight: 1,
                backup: false,
                type: 'HTTP',
                inherit: false,
              },
              {
                name: 'endpoint#3',
                target: 'https//dummy.com',
                tenants: null,
                weight: 42,
                backup: false,
                type: 'http',
                inherit: true,
                healthcheck: {
                  enabled: true,
                  inherit: true,
                },
              },
            ],
          },
        ],
      });
    });
  });

  describe('Create mode without existing endpoints', () => {
    let api: Api;

    beforeEach(async () => {
      TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: null } });
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
            },
          ],
        },
      });

      expectApiGetRequest(api);
      expectConnectorRequest();
      expectTenantsRequest();
    });

    it('should create new endpoint', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
        .then((input) => input.setValue('endpoint#3'));

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint type"]' }))
        .then((select) => select.clickOptions({ text: 'http' }));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint weight input"]' }))
        .then((input) => input.setValue('42'));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint target input"]' }))
        .then((input) => input.setValue('https//dummy.com'));
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      await saveBar.clickSubmit();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });
      expect(req.request.body.proxy).toStrictEqual({
        groups: [
          {
            name: 'default-group',
            endpoints: [
              {
                name: 'endpoint#3',
                target: 'https//dummy.com',
                tenants: null,
                weight: 42,
                backup: false,
                type: 'http',
                inherit: true,
                healthcheck: {
                  enabled: true,
                  inherit: true,
                },
              },
            ],
          },
        ],
      });
    });
  });

  describe('Create mode with API health check deactivated ', () => {
    let api: Api;

    beforeEach(async () => {
      TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: null } });
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
            },
          ],
        },
        services: {
          'health-check': {
            enabled: false,
          },
        },
      });

      expectApiGetRequest(api);
      expectConnectorRequest();
      expectTenantsRequest();
    });

    it('should create new endpoint', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
        .then((input) => input.setValue('endpoint#3'));

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint type"]' }))
        .then((select) => select.clickOptions({ text: 'http' }));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint weight input"]' }))
        .then((input) => input.setValue('42'));

      expect(await saveBar.isSubmitButtonInvalid()).toBeTruthy();

      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint target input"]' }))
        .then((input) => input.setValue('https//dummy.com'));
      expect(await saveBar.isSubmitButtonInvalid()).toBeFalsy();
      await saveBar.clickSubmit();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });
      expect(req.request.body.proxy).toStrictEqual({
        groups: [
          {
            name: 'default-group',
            endpoints: [
              {
                name: 'endpoint#3',
                target: 'https//dummy.com',
                tenants: null,
                weight: 42,
                backup: false,
                type: 'http',
                inherit: true,
                healthcheck: {
                  enabled: false,
                },
              },
            ],
          },
        ],
      });
    });
  });

  describe('Read only', () => {
    let api: Api;

    beforeEach(() => {
      TestBed.overrideProvider(UIRouterStateParams, {
        useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: DEFAULT_ENDPOINT_NAME },
      });
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'endpoint#1',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                },
              ],
            },
          ],
        },
        definition_context: {
          origin: 'kubernetes',
        },
      });
      expectApiGetRequest(api);
      expectConnectorRequest();
      expectTenantsRequest();
    });

    it('should not allow user to update the form', async () => {
      expect(
        await loader
          .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
          .then((input) => input.isDisabled()),
      ).toBeTruthy();

      expect(
        await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint type"]' })).then((select) => select.isDisabled()),
      ).toBeTruthy();

      expect(
        await loader
          .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint weight input"]' }))
          .then((input) => input.isDisabled()),
      ).toBeTruthy();

      expect(
        await loader
          .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint target input"]' }))
          .then((input) => input.isDisabled()),
      ).toBeTruthy();

      expect(
        await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Endpoint tenants"]' })).then((input) => input.isDisabled()),
      ).toBeTruthy();

      expect(
        await loader.getHarness(MatCheckboxHarness.with({ selector: '[formControlName="backup"]' })).then((input) => input.isDisabled()),
      ).toBeTruthy();

      await loader.getHarness(MatTabHarness.with({ label: 'Configuration' })).then((tab) => tab.select());
      fixture.detectChanges();

      expect(
        await loader
          .getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="inherit"]' }))
          .then((slider) => slider.isDisabled()),
      ).toBeTruthy();
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectConnectorRequest() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/connectors?expand=schema`, method: 'GET' })
      .flush([httpConnector, grpcConnector]);
    fixture.detectChanges();
  }

  function expectTenantsRequest() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`, method: 'GET' }).flush(tenants);
    fixture.detectChanges();
  }
});
