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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { fakeKafkaMessageEndpoint } from '@gravitee/ui-policy-studio-angular/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { HarnessLoader } from '@angular/cdk/testing';
import { cloneDeep } from 'lodash';

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';
import { ApiEndpointGroupHarness } from './api-endpoint-group.harness';
import { ApiEndpointGroupModule } from './api-endpoint-group.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4, fakeProxyApiV4, fakeProxyTcpApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { fakeEndpointGroupV4, fakeHTTPProxyEndpointGroupV4 } from '../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { fakeKafkaListener } from '../../../../entities/management-api-v2/api/v4/listener.fixture';

/**
 * Expect that a single GET request has been made which matches the specified URL
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiGetRequest(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController) {
  httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  fixture.detectChanges();
}

/**
 * Search for API Schema GET requests that match the specified parameter
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiSchemaGetRequests(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController) {
  httpTestingController.match({
    url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${api.endpointGroups[0].type}/shared-configuration-schema`,
    method: 'GET',
  });
  fixture.detectChanges();
}

/**
 * Expect that a single PUT request has been made which matches the specified URL
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiPutRequest(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController: HttpTestingController): TestRequest {
  return httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
}

function expectHealthCheckSchemaGet(
  fixture: ComponentFixture<any>,
  httpTestingController: HttpTestingController,
  healthCheckSchema: any,
): void {
  httpTestingController
    .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/api-services/http-health-check/schema`, method: 'GET' })
    .flush(healthCheckSchema);
  fixture.detectChanges();
}

describe('ApiEndpointGroupComponent', () => {
  const API_ID = 'apiId';
  const HEALTH_CHECK_SCHEMA = {
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
  let fixture: ComponentFixture<ApiEndpointGroupComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiEndpointGroupHarness;
  let api: ApiV4;
  let rootLoader: HarnessLoader;

  const initComponent = async (api: ApiV4, routerParams: unknown = { apiId: API_ID, groupIndex: 0 }) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiEndpointGroupModule, MatIconTestingModule, FormsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: routerParams } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u', 'api-definition-c', 'api-definition-r'] },
        { provide: SnackBarService, useValue: SnackBarService },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiEndpointGroupComponent);
    fixture.componentInstance.api = api;
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    expectApiGetRequest(api, fixture, httpTestingController);

    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('endpoint group update - configuration', () => {
    it('should not update endpoint group with invalid name', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');

      // empty string is not a valid name
      await componentHarness.writeToEndpointGroupNameInput('');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeTruthy();

      // reset change will restore initial value
      await componentHarness.clickEndpointGroupDismissButton();
      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });

    it('should update endpoint group with valid name', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');

      // change to a valid name
      await componentHarness.writeToEndpointGroupNameInput('default-group updated');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // save the correct value
      await componentHarness.clickEndpointGroupSaveButton();
      expectApiGetRequest(api, fixture, httpTestingController);
      const endpointsGroup = expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups;
      expect(endpointsGroup[0].name).toEqual('default-group updated');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });

    it('should update endpoint group load balancing', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual('ROUND_ROBIN');

      // change load balancing strategy
      await componentHarness.writeToEndpointGroupLoadBalancerSelector('RANDOM');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // save the new strategy
      await componentHarness.clickEndpointGroupSaveButton();
      expectApiGetRequest(api, fixture, httpTestingController);
      const endpointsGroup = expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups;
      expect(endpointsGroup[0].name).toEqual('default-group');
      expect(endpointsGroup[0].loadBalancer.type).toEqual('RANDOM');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });

    it('should not show native api endpoint group load balancing', async () => {
      api = fakeApiV4({
        id: API_ID,
        type: 'NATIVE',
        listeners: [fakeKafkaListener()],
        endpointGroups: [fakeEndpointGroupV4({ type: 'native-kafka' })],
      });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.isEndpointGroupLoadBalancerSelectorShown()).toEqual(false);
    });
  });
  describe('endpoint group update - health check', () => {
    it('should not update endpoint group health check with invalid configuration', async () => {
      api = fakeProxyApiV4({ id: API_ID, endpointGroups: [fakeHTTPProxyEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickHealthCheckTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expectHealthCheckSchemaGet(fixture, httpTestingController, HEALTH_CHECK_SCHEMA);

      // enable health check configuration
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeTruthy();
      await componentHarness.toggleEnableHealthCheckInput();
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeFalsy();

      await componentHarness.writeToHealthCheckConfigurationValueInput('dummy', '');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeTruthy();
    });

    it('should reset health check not saved configuration', async () => {
      api = fakeProxyApiV4({ id: API_ID, endpointGroups: [fakeHTTPProxyEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickHealthCheckTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expectHealthCheckSchemaGet(fixture, httpTestingController, HEALTH_CHECK_SCHEMA);

      // enable health check configuration
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeTruthy();
      await componentHarness.toggleEnableHealthCheckInput();
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeFalsy();

      await componentHarness.writeToHealthCheckConfigurationValueInput('dummy', 'a value');

      // cancel update
      await componentHarness.clickEndpointGroupDismissButton();
      expect(await componentHarness.readHealthCheckConfigurationValueInput('dummy')).toEqual('');
    });

    it('should update endpoint group health check', async () => {
      api = fakeProxyApiV4({ id: API_ID, endpointGroups: [fakeHTTPProxyEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickHealthCheckTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expectHealthCheckSchemaGet(fixture, httpTestingController, HEALTH_CHECK_SCHEMA);

      // enable health check configuration
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeTruthy();
      await componentHarness.toggleEnableHealthCheckInput();
      expect(await componentHarness.isHealthCheckConfigurationInputDisabled('dummy')).toBeFalsy();

      await componentHarness.writeToHealthCheckConfigurationValueInput('dummy', 'New health check value');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // save the new health check value
      await componentHarness.clickEndpointGroupSaveButton();
      expectApiGetRequest(api, fixture, httpTestingController);
      const endpointsGroup = expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups;
      expect(endpointsGroup[0].services.healthCheck).toEqual({
        enabled: true,
        overrideConfiguration: false,
        type: 'http-health-check',
        configuration: { dummy: 'New health check value' },
      });
    });
  });
  describe('endpoint group update - used as dead letter queue', () => {
    const DLQ_PARTIAL_API: Partial<ApiV4> = {
      id: API_ID,
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
    };
    it('should update endpoint group used as dlq and its reference in entrypoint', async () => {
      api = fakeApiV4(cloneDeep(DLQ_PARTIAL_API));
      // select the second group, which is the dead letter queue one
      await initComponent(api, { apiId: API_ID, groupIndex: 1 });
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('dlq-group');

      // update value
      await componentHarness.writeToEndpointGroupNameInput('dlq-group updated');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // saving should display a validation dialog
      await componentHarness.clickEndpointGroupSaveButton();
      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.confirm();

      expectApiGetRequest(api, fixture, httpTestingController);
      const response = expectApiPutRequest(api, fixture, httpTestingController).request.body;
      const endpointsGroup = response.endpointGroups;
      expect(endpointsGroup[1].name).toEqual('dlq-group updated');
      const webhookDlq = response.listeners[0].entrypoints[0].dlq;
      expect(webhookDlq.endpoint).toEqual('dlq-group updated');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });
    it('should not update endpoint group used as dlq when canceling dialog', async () => {
      api = fakeApiV4(cloneDeep(DLQ_PARTIAL_API));
      // select the second group, which is the dead letter queue one
      await initComponent(api, { apiId: API_ID, groupIndex: 1 });
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('dlq-group');

      // update value
      await componentHarness.writeToEndpointGroupNameInput('dlq-group updated');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // saving should display a validation dialog
      await componentHarness.clickEndpointGroupSaveButton();
      const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await dialog.cancel();

      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });
  });
  describe('endpoint group specific cases limitations', () => {
    it('should not display configuration and health check for Mock endpoint group', async () => {
      const api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4({ type: 'mock' })] });
      await initComponent(api);
      expect(await componentHarness.configurationTabIsVisible()).toEqual(false);
      expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);
    });
    it('should not display health check for TCP-Proxy endpoint group', async () => {
      const api = fakeProxyTcpApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4({ services: {} })] });
      await initComponent(api);
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
      expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);
    });
    it('should not display health check for Message endpoint group', async () => {
      const api = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          fakeEndpointGroupV4({
            type: 'kafka',
            name: 'group name',
            endpoints: [fakeKafkaMessageEndpoint({ name: 'kafka' })],
          }),
        ],
      });
      await initComponent(api);
      expectApiSchemaGetRequests(api, fixture, httpTestingController);
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
      expect(await componentHarness.healthCheckTabIsVisible()).toEqual(false);
    });
  });
  describe('endpoint group name validation', () => {
    const TRIM_ENDPOINT_GROUP_NAME = 'Neat and trim';
    const SPACEY_ENDPOINT_GROUP_NAME = 'Space after ';
    const TRIM_ENDPOINT_NAME = 'Trim and neat';
    const SPACEY_ENDPOINT_NAME = ' Space before';

    const EXISTING_ENDPOINT_GROUP_0 = fakeEndpointGroupV4({
      type: 'kafka',
      name: TRIM_ENDPOINT_GROUP_NAME,
      endpoints: [fakeKafkaMessageEndpoint({ name: SPACEY_ENDPOINT_NAME }), fakeKafkaMessageEndpoint({ name: TRIM_ENDPOINT_NAME })],
    });
    const EXISTING_ENDPOINT_GROUP_1 = fakeEndpointGroupV4({
      type: 'kafka',
      name: SPACEY_ENDPOINT_GROUP_NAME,
      endpoints: [],
    });
    const API = fakeApiV4({ id: API_ID, type: 'MESSAGE', endpointGroups: [EXISTING_ENDPOINT_GROUP_0, EXISTING_ENDPOINT_GROUP_1] });

    beforeEach(async () => {
      await initComponent(API);

      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(API, fixture, httpTestingController);
    });

    afterEach(async () => {
      await componentHarness.writeToEndpointGroupNameInput('Unique name');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(false);
    });

    it('should not allow trim name of an existing spacey endpoint group', async () => {
      await componentHarness.writeToEndpointGroupNameInput(SPACEY_ENDPOINT_GROUP_NAME.trim());
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(true);
    });

    it('should not allow spacey name matching a spacey existing endpoint group name', async () => {
      await componentHarness.writeToEndpointGroupNameInput(' ' + SPACEY_ENDPOINT_GROUP_NAME + ' ');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(true);
    });

    it('should not allow spacey name of an existing spacey endpoint name', async () => {
      await componentHarness.writeToEndpointGroupNameInput(SPACEY_ENDPOINT_NAME + ' ');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(true);
    });

    it('should not allow trim name of an existing trim endpoint', async () => {
      await componentHarness.writeToEndpointGroupNameInput(TRIM_ENDPOINT_NAME);
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(true);
    });
  });
});
