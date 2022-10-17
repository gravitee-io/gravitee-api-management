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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';

import { ApiProxyGroupEditComponent } from './api-proxy-group-edit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiProxyGroupsModule } from '../api-proxy-groups.module';
import { fakeApi } from '../../../../../../entities/api/Api.fixture';
import { Api } from '../../../../../../entities/api';
import { ConnectorListItem } from '../../../../../../entities/connector/connector-list-item';
import { fakeConnectorListItem } from '../../../../../../entities/connector/connector-list-item.fixture';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

describe('ApiProxyGroupWrapperComponent', () => {
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const fakeUiRouter = { go: jest.fn() };
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };

  let fixture: ComponentFixture<ApiProxyGroupEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let connector: ConnectorListItem;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyGroupsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: AjsRootScope, useValue: fakeRootScope },
      ],
    });

    fixture = TestBed.createComponent(ApiProxyGroupEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    connector = fakeConnectorListItem();
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
    expectConnectorRequest(connector);
    const routerSpy = jest.spyOn(fakeUiRouter, 'go');

    await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' })).then((button) => button.click());

    expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-endpoints', { apiId: API_ID }, undefined);
  });

  describe('Edit general information of existing group', () => {
    let api: Api;

    beforeEach(async () => {
      api = fakeApi({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: DEFAULT_GROUP_NAME,
              endpoints: [],
              load_balancing: { type: 'ROUND_ROBIN' },
            },
          ],
        },
      });
      expectApiGetRequest(api);
      expectConnectorRequest(connector);

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
      expectApiPutRequest({
        ...api,
        proxy: {
          groups: [
            {
              name: newGroupName,
              endpoints: [],
              load_balancing: { type: 'ROUND_ROBIN' },
            },
          ],
        },
      });
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
      expectApiPutRequest({
        ...api,
        proxy: {
          groups: [
            {
              name: DEFAULT_GROUP_NAME,
              endpoints: [],
              load_balancing: { type: newLbType },
            },
          ],
        },
      });
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

  describe('Edit configuration of existing group', () => {
    let api: Api;

    beforeEach(async () => {
      api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);
      expectConnectorRequest(connector);
    });

    it('should mark form as invalid, touched and dirty', async () => {
      const component = fixture.componentInstance;
      expect(component.groupForm.valid).toStrictEqual(true);
      expect(component.groupForm.touched).toStrictEqual(false);
      expect(component.groupForm.dirty).toStrictEqual(false);

      component.onConfigurationChange({
        isSchemaValid: false,
        configuration: {},
      });

      expect(component.groupForm.valid).toStrictEqual(false);
      expect(component.groupForm.touched).toStrictEqual(true);
      expect(component.groupForm.dirty).toStrictEqual(true);
    });

    it('should mark unset configuration error', async () => {
      const component = fixture.componentInstance;
      component.onConfigurationChange({
        isSchemaValid: false,
        configuration: {},
      });

      expect(component.groupForm.valid).toStrictEqual(false);
      expect(component.groupForm.touched).toStrictEqual(true);
      expect(component.groupForm.dirty).toStrictEqual(true);

      component.onConfigurationChange({
        isSchemaValid: true,
        configuration: {},
      });
      expect(component.groupForm.valid).toStrictEqual(true);
    });

    it('should update api configuration', async () => {
      const component = fixture.componentInstance;
      component.onConfigurationChange({
        isSchemaValid: false,
        configuration: {
          http: {
            ...api.proxy.groups[0].http,
            connectTimeout: 1000,
          },
        },
      });

      component.onSubmit();
      expectApiGetRequest(api);
      expectApiPutRequest({
        ...api,
        proxy: {
          groups: [
            {
              ...api.proxy.groups[0],
              http: {
                ...api.proxy.groups[0].http,
                connectTimeout: 1000,
              },
            },
          ],
        },
      });
      expect(component.api.proxy.groups[0].http.connectTimeout).toStrictEqual(1000);
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

  function expectApiPutRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequestError(apiId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}`, method: 'PUT' })
      .error(new ErrorEvent('error'));
    fixture.detectChanges();
  }
});
