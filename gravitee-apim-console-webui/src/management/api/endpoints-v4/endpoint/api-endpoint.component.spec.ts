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
import { ActivatedRoute, Router } from '@angular/router';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiEndpointComponent } from './api-endpoint.component';
import { ApiEndpointModule } from './api-endpoint.module';
import { ApiEndpointHarness } from './api-endpoint.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4, fakeConnectorPlugin, fakeProxyApiV4 } from '../../../../entities/management-api-v2';
import { fakeEndpointGroupV4, fakeHTTPProxyEndpointGroupV4 } from '../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { fakeKafkaListener } from '../../../../entities/management-api-v2/api/v4/listener.fixture';

const healthCheckSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    dummy: {
      title: 'dummy',
      type: 'string',
      description: 'A dummy string',
      readOnly: true,
    },
  },
  required: ['dummy'],
};

function expectHealthCheckSchemaGet(fixture: ComponentFixture<any>, httpTestingController: HttpTestingController): void {
  httpTestingController
    .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/api-services/http-health-check/schema`, method: 'GET' })
    .flush(healthCheckSchema);
  fixture.detectChanges();
}

@Component({
  template: `<api-endpoint #apiEndpoint></api-endpoint>`,
  standalone: false,
})
class TestComponent {
  @ViewChild('apiEndpoint') apiEndpoint: ApiEndpointComponent;
  api?: ApiV4;
}

describe('ApiEndpointComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let componentHarness: ApiEndpointHarness;
  let routerNavigationSpy: jest.SpyInstance;

  const initComponent = async (api: ApiV4, routerParams: unknown = { apiId: API_ID, groupIndex: 0 }) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiEndpointModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: routerParams } } },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: Boolean,
          },
        },
      ],
    });

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigationSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();

    componentHarness = await loader.getHarness(ApiEndpointHarness);

    expectApiGetRequest(api);
    expectEndpointSchemaGetRequest(api.endpointGroups[0].type);
    expectEndpointsSharedConfigurationSchemaGetRequest(api.endpointGroups[0].type);
    expectEndpointPluginGetRequest(api.endpointGroups[0].type);
    expectHealthCheckSchemaGet(fixture, httpTestingController);
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
        expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);

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
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
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

        expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);

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
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
      });

      it('should edit and save an existing endpoint used by dead letter queue', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
          listeners: [
            {
              type: 'SUBSCRIPTION',
              entrypoints: [
                {
                  type: 'webhook',
                  dlq: {
                    endpoint: 'dlq-endpoint',
                  },
                },
              ],
            },
          ],
          endpointGroups: [
            {
              name: 'default-group',
              type: 'kafka',
              loadBalancer: {
                type: 'ROUND_ROBIN',
              },
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
              ],
            },
            {
              name: 'dlq-group',
              type: 'kafka',
              loadBalancer: {
                type: 'ROUND_ROBIN',
              },
              endpoints: [
                {
                  name: 'dlq-endpoint',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
              ],
            },
          ],
        });

        await initComponent(apiV4, { apiId: API_ID, groupIndex: 1, endpointIndex: 0 });

        fixture.detectChanges();
        expect(await componentHarness.getEndpointName()).toStrictEqual('dlq-endpoint');

        await componentHarness.fillInputName('dlq-endpoint updated');
        fixture.detectChanges();

        expect(await componentHarness.getEndpointName()).toStrictEqual('dlq-endpoint updated');

        await componentHarness.clickSaveButton();

        const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await dialog.confirm();

        expectApiGetRequest(apiV4);

        const updatedApi: ApiV4 = {
          ...apiV4,
          endpointGroups: [
            {
              ...apiV4.endpointGroups[0],
            },
            {
              ...apiV4.endpointGroups[1],
              endpoints: [
                {
                  ...apiV4.endpointGroups[1].endpoints[0],
                  name: 'dlq-endpoint updated',
                  sharedConfigurationOverride: {
                    test: undefined,
                  },
                },
              ],
            },
          ],
        };
        expectApiPutRequest(updatedApi);
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
      });

      it('should edit and save a native kafka endpoint without weight', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
          type: 'NATIVE',
          endpointGroups: [
            {
              type: 'native-kafka',
              endpoints: [
                {
                  type: 'native-kafka',
                  name: 'default',
                },
              ],
            },
          ],
          listeners: [fakeKafkaListener()],
        });

        await initComponent(apiV4, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });
        fixture.detectChanges();
        expect(await componentHarness.getEndpointName()).toStrictEqual('default');

        await componentHarness.fillInputName('endpoint-name updated');
        expect(await componentHarness.getEndpointName()).toStrictEqual('endpoint-name updated');

        expect(await componentHarness.isWeightButtonShown()).toEqual(false);
        expect(await componentHarness.isSaveButtonDisabled()).toEqual(false);

        await componentHarness.clickSaveButton();
        fixture.detectChanges();

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
                  configuration: {
                    bootstrapServers: undefined,
                  },
                  inheritConfiguration: null,
                  weight: null,
                },
              ],
            },
          ],
        };
        expectApiPutRequest(updatedApi);
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
      });

      it('should edit and not save an existing endpoint used by dead letter queue', async () => {
        const apiV4 = fakeApiV4({
          id: API_ID,
          listeners: [
            {
              type: 'SUBSCRIPTION',
              entrypoints: [
                {
                  type: 'webhook',
                  dlq: {
                    endpoint: 'dlq-endpoint',
                  },
                },
              ],
            },
          ],
          endpointGroups: [
            {
              name: 'default-group',
              type: 'kafka',
              loadBalancer: {
                type: 'ROUND_ROBIN',
              },
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
              ],
            },
            {
              name: 'dlq-group',
              type: 'kafka',
              loadBalancer: {
                type: 'ROUND_ROBIN',
              },
              endpoints: [
                {
                  name: 'dlq-endpoint',
                  type: 'kafka',
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: {
                    bootstrapServers: 'localhost:9092',
                  },
                },
              ],
            },
          ],
        });

        await initComponent(apiV4, { apiId: API_ID, groupIndex: 1, endpointIndex: 0 });

        fixture.detectChanges();
        expect(await componentHarness.getEndpointName()).toStrictEqual('dlq-endpoint');

        await componentHarness.fillInputName('dlq-endpoint updated');
        fixture.detectChanges();

        expect(await componentHarness.getEndpointName()).toStrictEqual('dlq-endpoint updated');

        await componentHarness.clickSaveButton();

        const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await dialog.cancel();

        expect(routerNavigationSpy).not.toHaveBeenCalledWith(['../../'], { relativeTo: expect.anything() });
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

        expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);

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

        expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);

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

      expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);

      await componentHarness.clickConfigurationTab();

      expect(await componentHarness.isConfigurationButtonToggled()).toBeTruthy();
      fixture.detectChanges();

      const inputHarness = await loader.getHarness(MatInputHarness.with({ selector: '[id*="test"]' }));
      expect(await inputHarness.isDisabled()).toBeTruthy();
      expect(await inputHarness.getValue()).toStrictEqual('test');

      await componentHarness.toggleConfigurationButton();
      fixture.detectChanges();
    });

    it('should inherit health-check from parent', async () => {
      const anApi = fakeProxyApiV4({
        id: API_ID,
        endpointGroups: [fakeHTTPProxyEndpointGroupV4()],
      });
      await initComponent(anApi, { apiId: API_ID, groupIndex: 0, endpointIndex: 0 });

      expect(await componentHarness.healthCheckTabIsVisible()).toEqual(true);

      await componentHarness.clickHealthCheckTab();

      expect(await componentHarness.isHealthCheckInheritButtonToggled()).toBeTruthy();
      fixture.detectChanges();

      const inputHarness = await loader.getHarness(MatInputHarness.with({ selector: '[id*="dummy"]' }));
      expect(await inputHarness.isDisabled()).toBeTruthy();

      await componentHarness.toggleHealthCheckInheritButton();
      fixture.detectChanges();
    });
  });

  function expectEndpointsSharedConfigurationSchemaGetRequest(id: string) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${id}/shared-configuration-schema`, method: 'GET' })
      .flush({
        $schema: 'http://json-schema.org/draft-07/schema#',
        type: 'object',
        properties: { test: { title: 'test', description: 'test', type: 'string' } },
      });
  }

  function expectEndpointSchemaGetRequest(id: string) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${id}/schema`, method: 'GET' }).flush({
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
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${pluginId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: pluginId, name: pluginId })]);
  }
});
