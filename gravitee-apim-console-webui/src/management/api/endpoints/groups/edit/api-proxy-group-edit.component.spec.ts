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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiProxyGroupEditComponent } from './api-proxy-group-edit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiProxyGroupsModule } from '../api-proxy-groups.module';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ResourceListItem } from '../../../../../entities/resource/resourceListItem';
import { fakeResourceListItem } from '../../../../../entities/resource/resourceListItem.fixture';
import { ApiV2, fakeApiV2 } from '../../../../../entities/management-api-v2';
import { EndpointHttpConfigHarness } from '../../components/endpoint-http-config/endpoint-http-config.harness';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyGroupEditComponent', () => {
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';

  let fixture: ComponentFixture<ApiProxyGroupEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let serviceDiscovery: ResourceListItem;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiProxyGroupsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u'] },
      ],
    });

    serviceDiscovery = fakeResourceListItem({
      name: 'Consul.io Service Discovery',
      id: 'consul-service-discovery',
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Edit mode', () => {
    beforeEach(async () => {
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    describe('Edit general information of existing group', () => {
      let api: ApiV2;

      beforeEach(async () => {
        api = fakeApiV2((api) => ({
          ...api,
          id: API_ID,
          proxy: {
            ...api.proxy,
            groups: [
              {
                ...api.proxy.groups[0],
                name: DEFAULT_GROUP_NAME,
                endpoints: [],
                loadBalancer: { type: 'ROUND_ROBIN' },
                headers: [{ name: 'header1', value: 'value1' }],
              },
            ],
          },
        }));
        expectApiGetRequest(api);

        expectServiceDiscoveryRequest(serviceDiscovery);

        await loader.getHarness(MatTabHarness.with({ label: 'General' })).then((tab) => tab.select());
        fixture.detectChanges();
      });

      it('should edit group name and save it', async () => {
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
        expect(await nameInput.getValue()).toEqual(DEFAULT_GROUP_NAME);

        const newGroupName = 'new-group-name';
        await nameInput.setValue(newGroupName);
        expect(await nameInput.getValue()).toEqual(newGroupName);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

        expect(req.request.body.proxy.groups).toEqual([
          {
            name: newGroupName,
            endpoints: [],
            loadBalancer: { type: 'ROUND_ROBIN' },
            services: {
              discovery: {
                enabled: false,
              },
            },
            httpClientOptions: {
              clearTextUpgrade: undefined,
              connectTimeout: 5000,
              followRedirects: false,
              idleTimeout: 60000,
              keepAlive: true,
              maxConcurrentConnections: 100,
              pipelining: false,
              propagateClientAcceptEncoding: undefined,
              readTimeout: 10000,
              keepAliveTimeout: 30000,
              useCompression: true,
              version: 'HTTP_1_1',
            },
            httpClientSslOptions: {},
            headers: [{ name: 'header1', value: 'value1' }],
          },
        ]);
      });

      it('should not be able to save group when name is invalid', async () => {
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
        expect(await nameInput.getValue()).toEqual(DEFAULT_GROUP_NAME);

        const newGroupName = 'new-group-name : ';
        await nameInput.setValue(newGroupName);
        expect(await nameInput.getValue()).toEqual(newGroupName);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeTruthy();
      });

      it('should update load balancing type and save it', async () => {
        const lbSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }));
        expect(await lbSelect.getValueText()).toEqual('ROUND_ROBIN');

        const newLbType = 'RANDOM';
        await lbSelect.clickOptions({ text: newLbType });
        expect(await lbSelect.getValueText()).toEqual(newLbType);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);

        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

        expect(req.request.body.proxy.groups).toEqual([
          {
            name: DEFAULT_GROUP_NAME,
            endpoints: [],
            loadBalancer: { type: newLbType },
            services: {
              discovery: {
                enabled: false,
              },
            },
            httpClientOptions: {
              clearTextUpgrade: undefined,
              connectTimeout: 5000,
              followRedirects: false,
              idleTimeout: 60000,
              keepAlive: true,
              maxConcurrentConnections: 100,
              pipelining: false,
              propagateClientAcceptEncoding: undefined,
              readTimeout: 10000,
              keepAliveTimeout: 30000,
              useCompression: true,
              version: 'HTTP_1_1',
            },
            headers: [{ name: 'header1', value: 'value1' }],
            httpClientSslOptions: {},
          },
        ]);
      });

      it('should call snack bar error', async () => {
        const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
        const newGroupName = 'new-group-name';
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
        await nameInput.setValue(newGroupName);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);
        expectApiPutRequestError(API_ID);
        expect(snackBarSpy).toHaveBeenCalled();
      });
    });

    describe('Edit general information of API with service discovery', () => {
      let api: ApiV2;

      beforeEach(async () => {
        api = fakeApiV2((api) => ({
          ...api,
          id: API_ID,
          proxy: {
            ...api.proxy,
            groups: [
              {
                ...api.proxy.groups[0],
                name: DEFAULT_GROUP_NAME,
                endpoints: [],
                loadBalancer: { type: 'ROUND_ROBIN' },
                services: { discovery: { enabled: true, provider: 'consul-service-discovery' } },
              },
            ],
          },
        }));
        expectApiGetRequest(api);
        fixture.detectChanges();

        expectServiceDiscoveryRequest(serviceDiscovery);
        fixture.detectChanges();

        expectServiceDiscoverySchemaRequest();
        await loader.getHarness(MatTabHarness.with({ label: 'General' })).then((tab) => tab.select());
        fixture.detectChanges();
      });

      it('should keep service discovery info on save', async () => {
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
        expect(await nameInput.getValue()).toEqual(DEFAULT_GROUP_NAME);

        const newGroupName = 'new-group-name-service-discovery';
        await nameInput.setValue(newGroupName);
        expect(await nameInput.getValue()).toEqual(newGroupName);

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

        expect(req.request.body.proxy.groups).toEqual([
          {
            name: newGroupName,
            endpoints: [],
            loadBalancer: { type: 'ROUND_ROBIN' },
            services: {
              discovery: {
                enabled: true,
                provider: 'consul-service-discovery',
                configuration: {},
              },
            },
            httpClientOptions: api.proxy.groups[0].httpClientOptions,
            headers: [],
            httpClientSslOptions: {},
          },
        ]);
      });
    });

    describe('Edit configuration of existing group', () => {
      let api: ApiV2;

      beforeEach(async () => {
        api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);

        expectServiceDiscoveryRequest(serviceDiscovery);
        await loader.getHarness(MatTabHarness.with({ label: 'Configuration' })).then((tab) => tab.select());
        fixture.detectChanges();
      });

      it('should update api configuration', async () => {
        const endpointHttpConfigHarness = await loader.getHarness(EndpointHttpConfigHarness);
        await endpointHttpConfigHarness.setHttpVersion('HTTP/2');

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
        await gioSaveBar.clickSubmit();

        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

        expect(req.request.body.proxy.groups).toEqual([
          {
            ...api.proxy.groups[0],
            httpClientOptions: {
              ...api.proxy.groups[0].httpClientOptions,
              version: 'HTTP_2',
            },
          },
        ]);
      });
    });

    describe('Edit existing service discovery configuration', () => {
      let api: ApiV2;

      beforeEach(async () => {
        api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);

        expectServiceDiscoveryRequest(serviceDiscovery);

        await loader.getHarness(MatTabHarness.with({ label: 'Service discovery' })).then((tab) => tab.select());

        await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' })).then((slide) => slide.toggle());

        await loader
          .getHarness(MatSelectHarness.with({ selector: '[aria-label="Service discovery provider"]' }))
          .then((select) => select.clickOptions({ text: 'Consul.io Service Discovery' }));

        fixture.detectChanges();
        expectServiceDiscoverySchemaRequest();
      });

      it('should display service discovery gv-schema-form and save url', async () => {
        expect(fixture.debugElement.nativeElement.querySelector('gio-form-json-schema')).toBeTruthy();

        await loader.getHarness(GioSaveBarHarness).then((saveBar) => saveBar.clickSubmit());

        expectApiGetRequest(api);
        httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
      });

      it('should disable select', async () => {
        await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' })).then((slide) => slide.toggle());

        expect(
          (await loader.getAllHarnesses(MatSelectHarness.with({ selector: '[aria-label="Service discovery provider"]' }))).length,
        ).toEqual(0);
      });
    });

    describe('Reset', () => {
      let api: ApiV2;

      beforeEach(async () => {
        api = fakeApiV2({
          id: API_ID,
        });
        expectApiGetRequest(api);

        expectServiceDiscoveryRequest(serviceDiscovery);
      });

      it('should reset the forms', async () => {
        const newGroupName = 'new-group-name';
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
        await nameInput.setValue(newGroupName);
        expect(await nameInput.getValue()).toStrictEqual(newGroupName);

        const newLbType = 'RANDOM';
        const lbSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }));
        await lbSelect.clickOptions({ text: newLbType });
        expect(await lbSelect.getValueText()).toEqual(newLbType);
        fixture.detectChanges();

        const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
        await gioSaveBar.clickReset();

        expect(await nameInput.getValue()).toStrictEqual('default-group');
        expect(await lbSelect.getValueText()).toStrictEqual('ROUND_ROBIN');
      });
    });
  });

  describe('New mode ', () => {
    let api: ApiV2;

    beforeEach(() => {
      TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { apiId: API_ID, groupName: null } } } });
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApiV2({
        id: API_ID,
      });
      expectApiGetRequest(api);

      expectServiceDiscoveryRequest(serviceDiscovery);
    });

    it('should create new group', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
      expect(await nameInput.getValue()).toStrictEqual('');

      const newGroupName = 'new-group-name';
      await nameInput.setValue(newGroupName);
      expect(await nameInput.getValue()).toStrictEqual(newGroupName);

      const newLbType = 'RANDOM';
      const lbSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }));
      await lbSelect.clickOptions({ text: newLbType });
      expect(await lbSelect.getValueText()).toEqual(newLbType);
      fixture.detectChanges();

      await loader.getHarness(MatTabHarness.with({ label: 'Configuration' })).then((tab) => tab.select());

      const endpointHttpConfigHarness = await loader.getHarness(EndpointHttpConfigHarness);

      await endpointHttpConfigHarness.setHttpVersion('HTTP/2');

      const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
      await gioSaveBar.clickSubmit();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

      expect(req.request.body.proxy.groups).toEqual([
        api.proxy.groups[0],
        {
          loadBalancer: {
            type: 'RANDOM',
          },
          name: newGroupName,
          services: {
            discovery: {
              enabled: false,
            },
          },
          httpClientOptions: {
            version: 'HTTP_2',
            clearTextUpgrade: undefined,
            connectTimeout: 5000,
            followRedirects: undefined,
            idleTimeout: 60000,
            keepAlive: true,
            maxConcurrentConnections: 100,
            pipelining: undefined,
            propagateClientAcceptEncoding: undefined,
            readTimeout: 10000,
            keepAliveTimeout: 30000,
            useCompression: true,
          },
          headers: [],
          httpClientSslOptions: {},
        },
      ]);
    });

    it('should not be able to create new group when name is already used', async () => {
      const newGroupName = 'default-group';
      const groupNameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));

      await groupNameInput.setValue(newGroupName);

      const lbSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }));
      await lbSelect.clickOptions({ text: 'RANDOM' });

      const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await gioSaveBar.isSubmitButtonInvalid()).toBeTruthy();
      expect(fixture.componentInstance.generalForm.get('name').hasError('isUnique')).toBeTruthy();
    });
  });

  describe('Read only', () => {
    let api: ApiV2;

    beforeEach(async () => {
      TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { apiId: API_ID, groupName: null } } } });

      await TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      api = fakeApiV2({
        id: API_ID,
        definitionContext: {
          origin: 'KUBERNETES',
        },
      });
      expectApiGetRequest(api);

      expectServiceDiscoveryRequest(serviceDiscovery);
    });

    it('should not allow user to update the form', async () => {
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Group name input"]' }));
      expect(await nameInput.isDisabled()).toBeTruthy();

      expect(
        await loader
          .getHarness(MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }))
          .then((select) => select.isDisabled()),
      ).toBeTruthy();

      await loader.getHarness(MatTabHarness.with({ label: 'Configuration' })).then((tab) => tab.select());

      const endpointHttpConfigHarness = await loader.getHarness(EndpointHttpConfigHarness);

      expect(await endpointHttpConfigHarness.getMatInput('connectTimeout').then((input) => input.isDisabled())).toBeTruthy();

      await loader.getHarness(MatTabHarness.with({ label: 'Service discovery' })).then((tab) => tab.select());

      expect(
        await loader
          .getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }))
          .then((slide) => slide.isDisabled()),
      ).toBeTruthy();

      expect(
        (await loader.getAllHarnesses(MatSelectHarness.with({ selector: '[aria-label="Service discovery provider"]' }))).length,
      ).toEqual(0);
    });
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectServiceDiscoveryRequest(serviceDiscovery: ResourceListItem) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/services-discovery`, method: 'GET' })
      .flush([serviceDiscovery]);
  }

  function expectServiceDiscoverySchemaRequest() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/services-discovery/consul-service-discovery/schema`, method: 'GET' })
      .flush({});
    fixture.detectChanges();
  }

  function expectApiPutRequestError(apiId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`, method: 'PUT' })
      .error(new ErrorEvent('error'));
    fixture.detectChanges();
  }
});
