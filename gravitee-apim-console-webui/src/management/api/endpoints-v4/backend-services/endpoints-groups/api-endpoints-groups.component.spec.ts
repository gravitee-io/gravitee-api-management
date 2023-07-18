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

import { ApiEndpointsGroupsComponent } from './api-endpoints-groups.component';
import { ApiEndpointsGroupsHarness } from './api-endpoints-groups.harness';
import { ApiEndpointsGroupsModule } from './api-endpoints-groups.module';

import { ApiV4, EndpointGroupV4, fakeApiV4, fakeConnectorPlugin } from '../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../../../entities/user';

@Component({
  template: ` <api-endpoints-groups #apiEndpointsGroups [api]="api"></api-endpoints-groups> `,
})
class TestComponent {
  @ViewChild('apiEndpointsGroups') apiEndpointsGroups: ApiEndpointsGroupsComponent;
  api?: ApiV4;
}

describe('ApiEndpointsGroupsComponent', () => {
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
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let componentHarness: ApiEndpointsGroupsHarness;

  const initComponent = async (api: ApiV4, permissions: string[] = ['api-definition-u', 'api-definition-c', 'api-definition-r']) => {
    const currentUser = new DeprecatedUser();
    currentUser.userPermissions = permissions;

    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointsGroupsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
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

    expectEndpointsGetRequest();
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
        ['an endpoint', '', '1', ''],
        ['another endpoint', '', '5', ''],
      ]);
    });
  });

  describe('deleteEndpoint', () => {
    it('should delete the endpoint', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);
      expect(await componentHarness.isEndpointDeleteButtonVisible()).toEqual(true);

      await componentHarness.deleteEndpoint(1, rootLoader);

      expectApiGetRequest(apiV4);
      expectApiPutRequest({
        ...apiV4,
        endpointGroups: [{ ...group1, endpoints: [{ ...group1.endpoints[0] }] }, { ...group2 }],
      });
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
      expectEndpointsGetRequest();
    });
  });

  describe('addEndpoint', () => {
    it('should navigate to endpoint creation page', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);
      expect(await componentHarness.isAddEndpointButtonVisible()).toEqual(true);
      await componentHarness.clickAddEndpoint(0);

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.ng.endpoint-new', { groupIndex: 0 });
    });
  });

  describe('editEndpoint', () => {
    it('should navigate to endpoint edition page', async () => {
      const apiV4 = fakeApiV4({
        id: API_ID,
        endpointGroups: [group1, group2],
      });
      await initComponent(apiV4);

      expect(await componentHarness.isEditEndpointButtonVisible()).toEqual(true);
      await componentHarness.clickEditEndpoint(0);

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.ng.endpoint-edit', { groupIndex: 0, endpointIndex: 0 });
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
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: 'kafka', name: 'kafka' }), fakeConnectorPlugin({ id: 'mock', name: 'mock' })]);
  }
});
