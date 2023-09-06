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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormsModule } from '@angular/forms';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatStepHarness } from '@angular/material/stepper/testing';

import { ApiEndpointGroupCreateComponent } from './api-endpoint-group-create.component';
import { ApiEndpointGroupCreateHarness } from './api-endpoint-group-create.harness';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiEndpointGroupModule } from '../api-endpoint-group.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiV4, EndpointGroupV4, EndpointV4Default, fakeApiV4 } from '../../../../../entities/management-api-v2';

const API_ID = 'api-id';
const ENDPOINT_GROUP_NAME = 'My Endpoint Group';
const FAKE_UI_ROUTER = { go: jest.fn() };
const fakeKafkaSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    topic: {
      title: 'Kafka topic',
      type: 'string',
      description: 'A kafka topic',
    },
  },
  additionalProperties: false,
  required: ['topic'],
};

const fakeRabbitMqSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    rabbitFood: {
      title: 'Rabbit food',
      type: 'string',
      description: 'Some rabbit food',
    },
  },
  additionalProperties: false,
  required: ['rabbitFood'],
};

const fakeHttpProxySchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    proxyParam: {
      title: 'Proxy Param',
      type: 'string',
      description: 'Some param for your proxy',
    },
  },
  additionalProperties: false,
  required: ['proxyParam'],
};

const ENDPOINT_LIST = [
  {
    id: 'mock',
    name: 'Mock',
    description: 'Use a Mock backend to emulate the behaviour of a typical HTTP server and test processes',
    icon: 'mock-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
  },
  {
    id: 'rabbitmq',
    name: 'RabbitMQ',
    description: 'RabbitMQ Endpoint',
    icon: 'rabbitmq-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Publish and subscribe to messages from one or more Kafka topics',
    icon: 'kafka-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
  },
];
describe('ApiEndpointGroupCreateComponent', () => {
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<ApiEndpointGroupCreateComponent>;
  let harness: ApiEndpointGroupCreateHarness;
  let api: ApiV4;
  let routerSpy: any;

  const initComponent = async (testApi: ApiV4) => {
    const routerParams: unknown = { apiId: API_ID };

    api = testApi;

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointGroupModule, MatIconTestingModule, FormsModule],
      providers: [
        { provide: UIRouterState, useValue: FAKE_UI_ROUTER },
        { provide: UIRouterStateParams, useValue: routerParams },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiEndpointGroupCreateComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupCreateHarness);

    routerSpy = jest.spyOn(FAKE_UI_ROUTER, 'go');

    expectApiGet();
    fixture.detectChanges();

    if (api.type === 'MESSAGE') {
      expectEndpointListGet();
    }
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('V4 API - Message', () => {
    describe('Stepper', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID }));
      });

      it('should go back to endpoint groups page on exit', async () => {
        await harness.goBackToEndpointGroups();
        expect(routerSpy).toHaveBeenCalledWith('management.apis.endpoint-groups');
      });

      it('should not go to General step if endpoint type not selected', async () => {
        expect(await harness.getSelectedEndpointGroupId()).toBeNull();

        expect(await harness.canGoToGeneralStep()).toEqual(false);

        await harness.selectEndpointGroup('mock');
        expect(await harness.getSelectedEndpointGroupId()).toEqual('mock');

        expect(await harness.canGoToGeneralStep()).toEqual(true);
      });

      it('should not go to Configuration step if invalid General information', async () => {
        await fillOutAndValidateEndpointSelection();
        expect(await harness.canGoToConfigurationStep()).toEqual(false);

        expect(await harness.getNameValue()).toEqual('');
        expect(await harness.getLoadBalancerValue()).toEqual('');

        await harness.setNameValue('A new name');
        expect(await harness.canGoToConfigurationStep()).toEqual(false);

        await harness.setLoadBalancerValue('ROUND_ROBIN');
        expect(await harness.getNameValue()).toEqual('A new name');
        expect(await harness.getLoadBalancerValue()).toEqual('ROUND_ROBIN');
        expect(await harness.canGoToConfigurationStep()).toEqual(true);

        await harness.setNameValue('default-group'); // Name already exists in fakeApiV4
        expect(await harness.canGoToConfigurationStep()).toEqual(false);
      });

      it('should not save endpoint group if Configuration is invalid', async () => {
        await fillOutAndValidateEndpointSelection();
        await fillOutAndValidateGeneralInformation();

        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        expect(await harness.isConfigurationFormShown()).toEqual(true);

        expect(await harness.getConfigurationInputValue('topic')).toEqual('');

        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.getConfigurationInputValue('topic')).toEqual('my-kafka-topic');

        expect(await harness.isConfigurationStepValid()).toEqual(true);
        expect(await harness.canCreateEndpointGroup()).toEqual(true);
      });
    });

    describe('When creating a Kafka endpoint group', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID }));
        await fillOutAndValidateEndpointSelection();
        await fillOutAndValidateGeneralInformation();
        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.isConfigurationStepValid()).toEqual(true);
      });

      it('should be possible', async () => {
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('kafka')],
          sharedConfiguration: {
            topic: 'my-kafka-topic',
          },
        });
      });

      it('should show error in configuration after changing endpoint type to RabbitMQ', async () => {
        await chooseEndpointTypeAndGoToConfiguration('rabbitmq', fakeRabbitMqSchema);
        await harness.setConfigurationInputValue('rabbitFood', 'lettuce');

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'rabbitmq',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('rabbitmq')],
          sharedConfiguration: {
            rabbitFood: 'lettuce',
          },
        });
      });

      it('should not show error in configuration after changing endpoint type to Mock', async () => {
        await chooseEndpointTypeAndGoToConfiguration('mock');
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'mock',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('mock')],
        });
      });
    });

    describe('When creating a Mock endpoint group', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID }));
        await fillOutAndValidateEndpointSelection('mock');
        await fillOutAndValidateGeneralInformation();

        expect(await harness.isConfigurationFormShown()).toEqual(false);
        expect(await harness.isEndpointGroupMockBannerShown()).toEqual(true);
      });
      it('can create a mock endpoint group', async () => {
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'mock',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('mock')],
        });
      });

      it('should invalidate configuration if user switches to Kafka endpoint type', async () => {
        await chooseEndpointTypeAndGoToConfiguration();

        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.isConfigurationStepValid()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('kafka')],
          sharedConfiguration: {
            topic: 'my-kafka-topic',
          },
        });
      });
    });
  });

  describe('V4 API - Proxy', () => {
    beforeEach(async () => {
      await initComponent(fakeApiV4({ id: API_ID, type: 'PROXY' }));
      expectSchemaGet('http-proxy', fakeHttpProxySchema);
    });

    describe('Stepper', () => {
      it('should go back to endpoint groups page on exit', async () => {
        expect(await isStepActive(harness.getGeneralStep())).toEqual(true);
        await harness.goBackToEndpointGroups();
        expect(routerSpy).toHaveBeenCalledWith('management.apis.endpoint-groups');
      });
    });

    describe('When creating a http-proxy endpoint group', () => {
      it('should be possible', async () => {
        await fillOutAndValidateGeneralInformation();
        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        await harness.setConfigurationInputValue('proxyParam', 'my-proxy-param');
        expect(await harness.isConfigurationStepValid()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'http-proxy',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byType('http-proxy')],
          sharedConfiguration: {
            proxyParam: 'my-proxy-param',
          },
        });
      });
    });
  });

  /**
   * Expect requests
   */
  function expectApiGet(): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectSchemaGet(endpointId = 'kafka', schema: any = fakeKafkaSchema): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${endpointId}/shared-configuration-schema`, method: 'GET' })
      .flush(schema);
  }

  function expectApiPut(updatedApi: ApiV4): void {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body.endpointGroups).toEqual(updatedApi.endpointGroups);
    req.flush(updatedApi);
  }

  function expectEndpointListGet(): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints`, method: 'GET' }).flush(ENDPOINT_LIST);
  }

  /**
   * Helpers
   */
  async function isStepActive(step: Promise<MatStepHarness>): Promise<boolean> {
    return step.then((foundStep) => foundStep.isSelected());
  }

  async function fillOutAndValidateEndpointSelection(type = 'kafka'): Promise<void> {
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(true);
    await harness.selectEndpointGroup(type);
    if (type !== 'mock') {
      expectSchemaGet(type);
    }
    await harness.validateEndpointGroupSelection();
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(false);
    expect(await harness.isGeneralStepSelected()).toEqual(true);
  }

  async function fillOutAndValidateGeneralInformation(): Promise<void> {
    expect(await harness.isGeneralStepSelected()).toEqual(true);
    await harness.setNameValue(ENDPOINT_GROUP_NAME);
    await harness.setLoadBalancerValue('ROUND_ROBIN');
    await harness.validateGeneralInformation();
    expect(await harness.isGeneralStepSelected()).toEqual(false);
    expect(await isStepActive(harness.getConfigurationStep())).toEqual(true);
  }

  async function chooseEndpointTypeAndGoToConfiguration(type = 'kafka', schema: any = fakeKafkaSchema): Promise<void> {
    // Choose endpoint type
    await harness.getEndpointGroupTypeStep().then((step) => step.select());
    await harness.selectEndpointGroup(type);

    if (type !== 'mock') {
      expectSchemaGet(type, schema);
    }

    // Go to configuration page
    expect(await harness.canGoToGeneralStep()).toEqual(true);
    await harness.validateEndpointGroupSelection();
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(false);

    expect(await harness.isGeneralStepSelected()).toEqual(true);
    await harness.validateGeneralInformation();
    expect(await isStepActive(harness.getConfigurationStep())).toEqual(true);

    if (type === 'mock') {
      expect(await harness.isEndpointGroupMockBannerShown()).toEqual(true);
      expect(await harness.isConfigurationFormShown()).toEqual(false);
      expect(await harness.canCreateEndpointGroup()).toEqual(true);
    } else {
      expect(await harness.isInheritedConfigurationBannerShown()).toEqual(true);
      expect(await harness.isConfigurationFormShown()).toEqual(true);
      expect(await harness.canCreateEndpointGroup()).toEqual(false);
    }
  }

  async function createEndpointGroup(endpointGroup: EndpointGroupV4): Promise<void> {
    await harness.createEndpointGroup();
    const updatedApi = { ...api };
    updatedApi.endpointGroups = [...api.endpointGroups, endpointGroup];

    expectApiGet();
    expectApiPut(updatedApi);
    expect(routerSpy).toHaveBeenCalledWith('management.apis.endpoint-groups');
  }
});
