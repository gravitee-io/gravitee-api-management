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
import { Component, ViewChild } from '@angular/core';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { ApiEndpointsGroupsComponent } from './api-endpoints-groups.component';
import { ApiEndpointsGroupsHarness } from './api-endpoints-groups.harness';
import { ApiEndpointsGroupsModule } from './api-endpoints-groups.module';

@Component({
  template: ` <api-endpoints-groups #apiPortalEndpoints [api]="api"></api-endpoints-groups> `,
})
class TestComponent {
  @ViewChild('apiPortalEndpoints') apiPortalEndpoints: ApiEndpointsGroupsComponent;
  api?: ApiV4;
}

describe('ApiEndpointsGroupsComponent', () => {
  const API_ID = 'apiId';
  const apiV4 = fakeApiV4({
    id: API_ID,
    endpointGroups: [
      {
        name: 'default-group',
        type: 'kafka',
        endpoints: [
          {
            name: 'a kafka',
            type: 'kafka',
            weight: 1,
            inheritConfiguration: false,
            configuration: {
              bootstrapServers: 'localhost:9092',
            },
          },
          {
            name: 'another kafka',
            type: 'kafka',
            weight: 5,
            inheritConfiguration: false,
            configuration: {
              bootstrapServers: 'localhost:9093',
            },
          },
        ],
      },
      {
        name: 'Another Mocked Kafka',
        type: 'mock',
        loadBalancer: {
          type: 'ROUND_ROBIN',
        },
        sharedConfiguration: {},
        endpoints: [
          {
            name: 'a mock',
            type: 'mock',
            weight: 1,
            inheritConfiguration: true,
            configuration: {
              headers: [],
              metadata: [],
              messageCount: 10,
              messageInterval: 1000,
              messageContent: 'mock message',
            },
            services: {},
            secondary: false,
          },
        ],
        services: {},
      },
    ],
  });

  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let componentHarness: ApiEndpointsGroupsHarness;

  const initComponent = async (api: ApiV4) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointsGroupsModule, MatIconTestingModule],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    componentHarness = await loader.getHarness(ApiEndpointsGroupsHarness);
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('table display tests', () => {
    it('should display the endpoint groups tables', async () => {
      await initComponent(apiV4);

      expect(await componentHarness.getTableRows(0)).toEqual([
        ['a kafka', '', '1', ''],
        ['another kafka', '', '5', ''],
      ]);
    });
  });

  describe('deleteEndpoint', () => {
    it('should delete the endpoint', async () => {
      await initComponent(apiV4);

      await componentHarness.deleteEndpoint(1, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [
          {
            name: 'default-group',
            type: 'kafka',
            endpoints: [
              {
                name: 'a kafka',
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
            name: 'default-mock',
            type: 'kafka',
            endpoints: [
              {
                name: 'a kafka',
                type: 'mock',
                weight: 1,
                inheritConfiguration: false,
                configuration: {},
              },
            ],
          },
        ],
      });
    });
  });

  describe('deleteGroup', () => {
    it('should delete the endpoint group', async () => {
      await initComponent(apiV4);
      await componentHarness.deleteEndpointGroup(0, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [
          {
            name: 'default-mock',
            type: 'kafka',
            endpoints: [
              {
                name: 'a kafka',
                type: 'mock',
                weight: 1,
                inheritConfiguration: false,
                configuration: {},
              },
            ],
          },
        ],
      });
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
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }
});
