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

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { UIRouterModule } from '@uirouter/angular';

import { ApiEndpointComponent } from './api-endpoint.component';
import { ApiEndpointModule } from './api-endpoint.module';
import { ApiEndpointHarness } from './api-endpoint.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4, fakeConnectorPlugin } from '../../../../entities/management-api-v2';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { fakeEndpointGroupV4 } from '../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';

@Component({
  template: `<api-endpoint #apiEndpoint></api-endpoint>`,
})
class TestComponent {
  @ViewChild('apiEndpoint') apiEndpoint: ApiEndpointComponent;
  api?: ApiV4;
}

describe('ApiEndpointComponent', () => {
  const API_ID = 'apiId';
  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let componentHarness: ApiEndpointHarness;

  const initComponent = async (api: ApiV4, routerParams: unknown = { apiId: API_ID, groupIndex: 0 }) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        ApiEndpointModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
        GioUiRouterTestingModule,
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        { provide: UIRouterStateParams, useValue: routerParams },
      ],
    });

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    componentHarness = await loader.getHarness(ApiEndpointHarness);

    expectApiGetRequest(api);
    expectEndpointSchemaGetRequest(api.endpointGroups[0].type);
    expectEndpointsSharedConfigurationSchemaGetRequest(api.endpointGroups[0].type);
    expectEndpointPluginGetRequest(api.endpointGroups[0].type);
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('endpoints in a group', () => {
    it('should load kafka endpoint form dynamically', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
      });
      await initComponent(apiV4);
    });

    describe('should create new endpoint', () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
      });
      const apiV4WithSharedConfiguration: ApiV4 = {
        ...apiV4,
        endpointGroups: [
          {
            ...apiV4.endpointGroups[0],
            sharedConfiguration: {
              test: 'test-from-parent',
            },
          },
        ],
      };
      afterEach(async () => {
        expect(await componentHarness.isSaveButtonDisabled()).toBeFalsy();
        await componentHarness.clickSaveButton();

        expectApiGetRequest(apiV4WithSharedConfiguration);
        const updatedApi: ApiV4 = {
          ...apiV4WithSharedConfiguration,
          endpointGroups: [
            {
              ...apiV4WithSharedConfiguration.endpointGroups[0],
              endpoints: [
                { ...apiV4WithSharedConfiguration.endpointGroups[0].endpoints[0] },
                {
                  configuration: {
                    bootstrapServers: undefined,
                  },
                  inheritConfiguration: true,
                  sharedConfigurationOverride: {},
                  name: 'endpoint-name',
                  type: 'kafka',
                  weight: 10,
                },
              ],
            },
          ],
        };
        expectApiPutRequest(updatedApi);
        expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.endpoint-groups');
      });

      it('should trim new endpoint name', async () => {
        await initComponent(apiV4WithSharedConfiguration, { apiId: API_ID, groupIndex: 0 });

        expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();

        await componentHarness.fillInputName(' endpoint-name  ');
        await componentHarness.fillWeightButton(10);
      });

      it('should add a new endpoint', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
        });
        const apiV4WithSharedConfiguration: ApiV4 = {
          ...apiV4,
          endpointGroups: [
            {
              ...apiV4.endpointGroups[0],
              sharedConfiguration: {
                test: 'test-from-parent',
              },
            },
          ],
        };
        await initComponent(apiV4WithSharedConfiguration, { apiId: API_ID, groupIndex: 0 });

        expect(await componentHarness.isSaveButtonDisabled()).toBeTruthy();

        await componentHarness.fillInputName('endpoint-name');
        await componentHarness.fillWeightButton(10);
      });
    });

    describe('should update endpoint', () => {
      it('should edit and save an existing endpoint', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
        });

        await initComponent(apiV4, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });

        fixture.detectChanges();
        expect(await componentHarness.getEndpointName()).toStrictEqual('default');

        await componentHarness.fillInputName('endpoint-name updated');
        fixture.detectChanges();

        expect(await componentHarness.getEndpointName()).toStrictEqual('endpoint-name updated');

        await componentHarness.clickSaveButton();

        expectApiGetRequest(apiV4);

        const updatedApi: ApiV4 = {
          ...apiV4,
          endpointGroups: [
            {
              ...apiV4.endpointGroups[0],
              endpoints: [
                {
                  ...apiV4.endpointGroups[0].endpoints[0],
                  name: 'endpoint-name updated',
                  sharedConfigurationOverride: {
                    test: undefined,
                  },
                },
              ],
            },
          ],
        };

        expectApiPutRequest(updatedApi);
        expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.endpoint-groups');
      });

      it('should not be valid if input name has a space', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
          endpointGroups: [
            fakeEndpointGroupV4({
              endpoints: [
                {
                  name: 'default',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
                {
                  name: 'existing name',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
              ],
            }),
          ],
        });
        await initComponent(apiV4, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });

        await componentHarness.fillInputName(apiV4.endpointGroups[0].endpoints[1].name + ' ');
        expect(await componentHarness.isSaveButtonDisabled()).toEqual(true);

        await componentHarness.fillInputName('different name');
        expect(await componentHarness.isSaveButtonDisabled()).toEqual(false);
      });

      it('should not be valid if existing endpoint name has a space', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
          endpointGroups: [
            fakeEndpointGroupV4({
              endpoints: [
                {
                  name: 'default',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
                {
                  name: ' a spacey name ',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
              ],
            }),
          ],
        });

        await initComponent(apiV4, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });

        await componentHarness.fillInputName('a spacey name');
        expect(await componentHarness.isSaveButtonDisabled()).toEqual(true);

        await componentHarness.fillInputName('different name');
        expect(await componentHarness.isSaveButtonDisabled()).toEqual(false);
      });
    });

    it('should inherit configuration from parent', async () => {
      const anApi = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: 'a group',
            type: 'kafka',
            loadBalancer: {
              type: 'ROUND_ROBIN',
            },
            sharedConfiguration: {
              test: 'test',
            },
            endpoints: [
              {
                name: 'a kafka endpoint',
                type: 'kafka',
                weight: 1,
                inheritConfiguration: true,
              },
            ],
          },
        ],
      });
      await initComponent(anApi, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });

      await componentHarness.clickConfigurationTab();

      expect(await componentHarness.isConfigurationButtonToggled()).toBeTruthy();
      fixture.detectChanges();

      const inputHarness = await loader.getHarness(MatInputHarness.with({ selector: '[id*="test"]' }));
      expect(await inputHarness.isDisabled()).toBeTruthy();
      expect(await inputHarness.getValue()).toStrictEqual('test');

      await componentHarness.toggleConfigurationButton();
      fixture.detectChanges();
    });
  });

  function expectEndpointsSharedConfigurationSchemaGetRequest(id: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${id}/shared-configuration-schema`, method: 'GET' })
      .flush({
        $schema: 'http://json-schema.org/draft-07/schema#',
        type: 'object',
        properties: { test: { title: 'test', description: 'test', type: 'string' } },
      });
  }

  function expectEndpointSchemaGetRequest(id: string) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${id}/schema`, method: 'GET' }).flush({
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'object',
      properties: {
        bootstrapServers: { title: 'bootstrapServers', description: 'The list of kafka bootstrap servers.', type: 'string' },
      },
    });
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
  }

  function expectEndpointPluginGetRequest(pluginId: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${pluginId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: pluginId, name: pluginId })]);
  }
});
