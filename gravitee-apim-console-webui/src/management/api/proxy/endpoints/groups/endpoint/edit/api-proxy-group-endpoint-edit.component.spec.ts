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

import { ApiProxyGroupEndpointEditComponent } from './api-proxy-group-endpoint-edit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../../shared/testing';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ApiProxyGroupEndpointModule } from '../api-proxy-group-endpoint.module';
import { Api } from '../../../../../../../entities/api';
import { ConnectorListItem } from '../../../../../../../entities/connector/connector-list-item';
import { fakeApi } from '../../../../../../../entities/api/Api.fixture';
import { fakeConnectorListItem } from '../../../../../../../entities/connector/connector-list-item.fixture';
import { fakeTenant } from '../../../../../../../entities/tenant/tenant.fixture';
import { SnackBarService } from '../../../../../../../services-ngx/snack-bar.service';

describe('ApiProxyGroupEndpointEditComponent', () => {
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const DEFAULT_ENDPOINT_NAME = 'endpoint#1';
  const fakeUiRouter = { go: jest.fn() };
  const tenants = [fakeTenant({ id: 'tenant#1', name: 'tenant#1' }), fakeTenant({ id: 'tenant#2', name: 'tenant#2' })];
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };
  let fixture: ComponentFixture<ApiProxyGroupEndpointEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let connector: ConnectorListItem;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyGroupEndpointModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: DEFAULT_ENDPOINT_NAME } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: AjsRootScope, useValue: fakeRootScope },
      ],
    });

    connector = fakeConnectorListItem();
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
      expectConnectorRequest(connector);
      expectTenantsRequest();
      fixture.detectChanges();
    });

    it('should go back to endpoints', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-endpoints', { apiId: API_ID }, undefined);
    });

    it('should update existing endpoint', async () => {
      expect(fixture.componentInstance.supportedTypes).toEqual(['http', 'grpc']);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }));
      expect(await nameInput.getValue()).toEqual(DEFAULT_ENDPOINT_NAME);

      const newEndpointName = 'new-endpoint-name';
      await nameInput.setValue(newEndpointName);
      expect(await nameInput.getValue()).toEqual(newEndpointName);

      const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await gioSaveBar.isSubmitButtonInvalid()).toBeFalsy();
      await gioSaveBar.clickSubmit();

      expectApiGetRequest(api);
      expectApiPutRequest({
        ...api,
        proxy: {
          groups: [
            {
              name: newEndpointName,
              endpoints: [],
              load_balancing: { type: 'ROUND_ROBIN' },
            },
          ],
        },
      });
    });

    it('should not be able to save changes when endpoint name is already used', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Endpoint name input"]' }))
        .then((input) => input.setValue('endpoint#2'));

      const gioSaveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await gioSaveBar.isSubmitButtonInvalid()).toBeTruthy();
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
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectConnectorRequest(connector: ConnectorListItem) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/connectors?expand=schema`, method: 'GET' }).flush([connector]);
    fixture.detectChanges();
  }

  function expectTenantsRequest() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`, method: 'GET' }).flush(tenants);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }
});
