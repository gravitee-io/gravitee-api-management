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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { deepClone } from '@gravitee/ui-components/src/lib/utils';
import { cloneDeep } from 'lodash';

import { ApiEndpointGroupsStandardComponent } from './api-endpoint-groups-standard.component';
import { ApiEndpointGroupsStandardHarness } from './api-endpoint-groups-standard.harness';

import { ApiEndpointGroupsModule } from '../api-endpoint-groups.module';
import { ApiV4, EndpointGroupV4, fakeApiV4, fakeConnectorPlugin } from '../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { expectTenantsGetRequest } from '../../../../../services-ngx/tenant.service.spec';

describe('ApiEndpointGroupsStandardComponent', () => {
  const API_ID = 'apiId';
  const group1: EndpointGroupV4 = {
    name: 'default-group',
    type: 'kafka',
    loadBalancer: { type: 'WEIGHTED_RANDOM' },
    endpoints: [
      {
        name: 'an endpoint',
        type: 'kafka',
        weight: 1,
        inheritConfiguration: false,
        configuration: {
          bootstrapServers: 'localhost:9092',
        },
      },
      {
        name: 'another endpoint',
        type: 'kafka',
        weight: 5,
        inheritConfiguration: false,
        configuration: {
          bootstrapServers: 'localhost:9093',
        },
      },
    ],
  };
  const group2: EndpointGroupV4 = {
    name: 'mock group',
    type: 'mock',
    loadBalancer: { type: 'WEIGHTED_ROUND_ROBIN' },
    endpoints: [
      {
        name: 'a mock',
        type: 'mock',
        weight: 1,
        inheritConfiguration: true,
        secondary: false,
      },
    ],
  };
  const dlqGroup: EndpointGroupV4 = {
    name: 'dlq-group',
    type: 'kafka',
    loadBalancer: { type: 'WEIGHTED_RANDOM' },
    endpoints: [
      {
        name: 'dlq-endpoint-one',
        type: 'kafka',
        weight: 1,
        inheritConfiguration: false,
        configuration: {
          bootstrapServers: 'localhost:9092',
        },
      },
      {
        name: 'dlq-endpoint-two',
        type: 'kafka',
        weight: 5,
        inheritConfiguration: false,
        configuration: {
          bootstrapServers: 'localhost:9093',
        },
      },
    ],
  };

  const kafkaGroup: EndpointGroupV4 = {
    name: 'kafka-group',
    type: 'native-kafka',
    loadBalancer: { type: 'WEIGHTED_RANDOM' },
    sharedConfiguration: {
      shared: 'configuration',
    },
    endpoints: [
      {
        name: 'kafka endpoint',
        type: 'native-kafka',
        weight: 1,
        inheritConfiguration: true,
        configuration: {
          bootstrapServers: 'localhost:9092',
        },
      },
    ],
  };

  const a2aGroup: EndpointGroupV4 = {
    name: 'A2A Proxy',
    type: 'a2a-proxy',
    loadBalancer: { type: 'WEIGHTED_RANDOM' },
    sharedConfiguration: {
      shared: 'configuration',
    },
    endpoints: [
      {
        name: 'A2A Proxy',
        type: 'a2a-proxy',
        weight: 1,
        inheritConfiguration: true,
        configuration: {
          target: 'https://dummy.restapiexample.com/api/v1/create',
        },
      },
    ],
  };

  let fixture: ComponentFixture<ApiEndpointGroupsStandardComponent>;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let componentHarness: ApiEndpointGroupsStandardHarness;

  const initComponent = async (api: ApiV4, permissions: string[] = ['api-definition-u', 'api-definition-c', 'api-definition-r']) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiEndpointGroupsModule, MatIconTestingModule, GioLicenseTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: permissions },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiEndpointGroupsStandardComponent);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupsStandardHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiGetRequest(api);
    expectEndpointsGetRequest();
    expectTenantsGetRequest(httpTestingController);
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('table display tests', () => {
    it('should display the endpoint groups tables', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);

      expect(await componentHarness.getTableRows(0)).toEqual([
        ['', 'an endpoint', 'localhost:9092', '1', ''],
        ['', 'another endpoint', 'localhost:9093', '5', ''],
      ]);
    });
    it('should a warning in kafka endpoint group if failover is enabled', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
        failover: {
          enabled: true,
        },
      });
      await initComponent(apiV4);

      expect(await componentHarness.getTableRows(0)).toEqual([
        ['', 'an endpoint', 'localhost:9092', '1', ''],
        ['', 'another endpoint', 'localhost:9093', '5', ''],
      ]);

      const warningFailoverBanner = await componentHarness.getWarningFailoverBanner();
      expect(warningFailoverBanner).toHaveLength(1);
      expect(await warningFailoverBanner[0].getText()).toContain(
        'Failover is enabled but it is not supported for Kafka endpoints. Use the native Kafka Failover by providing multiple bootstrap servers.',
      );
    });

    it('should allow to create another endpoint group for a NATIVE api', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        type: 'NATIVE',
        endpointGroups: [kafkaGroup],
      });
      await initComponent(apiV4);

      expect(await componentHarness.isAddEndpointGroupDisplayed()).toEqual(true);
    });

    it('should allow to add another endpoint to a NATIVE api', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        type: 'NATIVE',
        endpointGroups: [kafkaGroup],
      });
      await initComponent(apiV4);

      expect(await componentHarness.isAddEndpointButtonVisible()).toEqual(true);
    });

    it('should not allow to add another endpoint to a MCP_PROXY api', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        type: 'MCP_PROXY',
        endpointGroups: [kafkaGroup],
      });
      await initComponent(apiV4);

      expect(await componentHarness.isAddEndpointButtonVisible()).toEqual(false);
    });

    it('should not allow to create another endpoint group for a MESSAGE api if existing endpoint is agent to agent', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        type: 'MESSAGE',
        endpointGroups: [a2aGroup],
      });

      await initComponent(apiV4);
      fixture.componentInstance.isA2ASelcted = true;
      fixture.detectChanges();
      expect(await componentHarness.isAddEndpointGroupDisplayed()).toEqual(false);
    });

    it('should allow to drop endpoint and update his order in a group', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        type: 'NATIVE',
        endpointGroups: [cloneDeep(group1)],
      });
      await initComponent(apiV4);

      fixture.componentInstance.dropRow({ previousIndex: 1, currentIndex: 0 } as any, group1.name);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [{ ...group1, endpoints: [{ ...group1.endpoints[1] }, { ...group1.endpoints[0] }] }],
      });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });
  });

  describe('deleteEndpoint', () => {
    it('should delete the endpoint', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [deepClone(group1), deepClone(group2)],
      });
      await initComponent(apiV4);
      expect(await componentHarness.isEndpointDeleteButtonVisible()).toEqual(true);

      await componentHarness.deleteEndpoint(1, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [{ ...group1, endpoints: [{ ...group1.endpoints[0] }] }, { ...group2 }],
      });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });

    it('should delete the endpoint if used as dead letter queue', async () => {
      const apiV4 = deepClone(
        fakeApiV4({
          id: API_ID,
          endpointGroups: [deepClone(group1), deepClone(dlqGroup)],
          listeners: [
            {
              type: 'SUBSCRIPTION',
              entrypoints: [
                {
                  type: 'webhook',
                  dlq: {
                    endpoint: 'dlq-endpoint-one',
                  },
                },
              ],
            },
          ],
        }),
      );
      await initComponent(apiV4);
      expect(await componentHarness.isEndpointDeleteButtonVisible()).toEqual(true);

      // deleting the third endpoint of the flatten list of buttons: dlq-endpoint-one
      await componentHarness.deleteEndpoint(2, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [{ ...group1 }, { ...dlqGroup, endpoints: [{ ...dlqGroup.endpoints[1] }] }],
        listeners: [
          {
            type: 'SUBSCRIPTION',
            entrypoints: [
              {
                type: 'webhook',
                dlq: null,
              },
            ],
          },
        ],
      });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });

    it('should not be able to delete last endpoint', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group2],
      });
      await initComponent(apiV4);
      expect(await componentHarness.isEndpointDeleteDisabled(0)).toEqual(true);
    });
  });

  describe('deleteGroup', () => {
    it('should delete the endpoint group', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);
      await componentHarness.deleteEndpointGroup(0, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({ ...apiV4, endpointGroups: [group2] });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });

    it('should delete the endpoint group if used as dead letter queue', async () => {
      const apiV4 = deepClone(
        fakeApiV4({
          id: API_ID,
          endpointGroups: [group1, dlqGroup],
          listeners: [
            {
              type: 'SUBSCRIPTION',
              entrypoints: [
                {
                  type: 'webhook',
                  dlq: {
                    endpoint: 'dlq-group',
                  },
                },
              ],
            },
          ],
        }),
      );
      await initComponent(apiV4);
      await componentHarness.deleteEndpointGroup(1, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [group1],
        listeners: [
          {
            type: 'SUBSCRIPTION',
            entrypoints: [
              {
                type: 'webhook',
                dlq: null,
              },
            ],
          },
        ],
      });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });

    it('should delete the endpoint group if one of its item is used as dead letter queue', async () => {
      const apiV4 = deepClone(
        fakeApiV4({
          id: API_ID,
          endpointGroups: [group1, dlqGroup],
          listeners: [
            {
              type: 'SUBSCRIPTION',
              entrypoints: [
                {
                  type: 'webhook',
                  dlq: {
                    endpoint: 'dlq-endpoint-one',
                  },
                },
              ],
            },
          ],
        }),
      );
      await initComponent(apiV4);
      await componentHarness.deleteEndpointGroup(1, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [group1],
        listeners: [
          {
            type: 'SUBSCRIPTION',
            entrypoints: [
              {
                type: 'webhook',
                dlq: null,
              },
            ],
          },
        ],
      });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });
  });

  describe('reorderEndpointGroup', () => {
    it('should move down the first group', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);

      expect(await componentHarness.getMoveUpButton(0)).toBeNull();
      await componentHarness.moveGroupDown(0);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({ ...apiV4, endpointGroups: [group2, group1] });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });

    it('should move up the second group', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);

      expect(await componentHarness.getMoveDownButton(1)).toBeNull();
      await componentHarness.moveGroupUp(1);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({ ...apiV4, endpointGroups: [group2, group1] });
      expectApiGetRequest(apiV4);
      expectTenantsGetRequest(httpTestingController);
      expectEndpointsGetRequest();
    });
  });

  describe('read-only mode', () => {
    it('should not allow deleting, adding or editing endpoints if user can only read', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4, ['api-definition-r']);

      expect(await componentHarness.isEndpointDeleteButtonVisible()).toEqual(false);
      expect(await componentHarness.isAddEndpointButtonVisible()).toEqual(false);
      expect(await componentHarness.isEditEndpointButtonVisible()).toEqual(false);
      expect(await componentHarness.isViewEndpointButtonVisible()).toEqual(true);
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
    fixture.detectChanges();
  }

  function expectEndpointsGetRequest() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: 'kafka', name: 'kafka' }), fakeConnectorPlugin({ id: 'mock', name: 'mock' })]);
  }
});
